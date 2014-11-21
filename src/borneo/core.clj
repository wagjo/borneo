;; Copyright (C) 2011, Jozef Wagner. All rights reserved.
;;
;; Disclaimer: Forked from hgavin/clojure-neo4j (no longer available).
;;
;; Disclaimer: Small amount of comments and docs are based on official
;; Neo4j javadocs.
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0
;; (http://opensource.org/licenses/eclipse-1.0.php) which can be found
;; in the file epl-v10.html at the root of this distribution.
;;
;; By using this software in any fashion, you are agreeing to be bound
;; by the terms of this license.
;;
;; You must not remove this notice, or any other, from this software.

(ns borneo.core
  "Clojure wrapper for Neo4j, a graph database.

  See project page (http://github.com/wagjo/borneo) for usage instructions,
  documentation and examples.

  Notes:
  - Using official Neo4j bindings.
  - Not using Blueprints interface.
  - neo-db holds the current db instance, so that users do not have
    to supply db instance at each call to db operations. This approach has
    of course some drawbacks , but I've found it suitable for my purposes.
  - All mutable functions are by default wrapped in transactions. That
    means you don't have to explicitly put them in transactions. The Neo4j
    transaction model allows for fast transaction nesting, so you can easily
    have your own transaction if you have a group of mutable functions.
    In that case just wrap your functions inside with-tx.
  - NullPointerException is thrown if there is no open connection to the db."
  (:import (org.neo4j.graphdb Direction
                              DynamicLabel
                              GraphDatabaseService
                              Label
                              Node
                              NotFoundException
                              PropertyContainer
                              Relationship
                              RelationshipType
                              Transaction
                              TraversalPosition
                              Traverser$Order)
           (org.neo4j.cypher.javacompat ExecutionEngine)
           (org.neo4j.graphdb.factory GraphDatabaseFactory)
           (org.neo4j.tooling GlobalGraphOperations)))

;;;; Implementation details

(defonce ^{:doc "Holds the current database instance."
           :tag GraphDatabaseService
           :dynamic true}
  *neo-db* {})

(comment
  ; no longer being used
  (defonce ^{:doc "Holds an execution engine around the graph database to use for Cypher queries."
             :tag ExecutionEngine
             :dynamic true}
    *exec-eng* nil))

(defn- array?
  "Determines whether x is an array or not."
  [x]
  (when x
    (-> x class .isArray)))

(defn- encode-property-key
  "Encodes property key. If x is keyword, store its name."
  [value]
  (if (keyword? value)
    (name value)
    value))

(defn- decode-property-key
  "Decodes property key. Always convert property key to the keyword."
  [value]
  (keyword value))

(defn- encode-property-value
  "Encodes property value. If value is a keyword, store it as a string."
  [value]
  (if (keyword? value)
    (str value)
    value))

(defn- decode-property-value
  "Decodes property value.
  If value is string begining with colon, interpret it as a keyword."
  [value]
  (if (and (string? value)
           (= \: (first value)))
    (keyword (subs value 1))
    value))

(defn- process-position
  "Translates TraversalPosition into a map."
  [^TraversalPosition p]
  {:pos p
   :node (.currentNode p)
   :depth (.depth p)
   :start? (.isStartNode p)
   :last-rel (.lastRelationshipTraversed p)
   ;; FIXME: Following call causes NullPointerException
   ;; :prev-node (.previousNode p)
   :count (.returnedNodesCount p)})

(defn- ^RelationshipType rel-type*
  "Creates java class implementing RelationshipType. Used for interop.
  TODO: Should we cache these instances to save memory?
  Or will they be GCd?"
  [k]
  (reify RelationshipType
    (^String name [this] (name k))))

(defn- ^Direction rel-dir
  "Translates keyword to the respective relationship direction.
  Valid values are :out :in and :both.
  See javadoc for Direction for more info on directions."
  [k]
  (condp = k
      :out Direction/OUTGOING
      :in Direction/INCOMING
      :both Direction/BOTH
      (throw (IllegalArgumentException.))))

(defn- rel-dir-map
  "Constructs an array or relation types and directions. Accepts
  map consisting of relation type keywords as keys and
  relation directions keywords (:in or :out) as values.
  r can also be a single keyword; in that case, a default :out will
  be added to the array."
  [r]
  (if (keyword? r)
    (rel-dir-map {r :out})
    (into-array Object
                (mapcat (fn [[k v]] [(rel-type* k) (rel-dir v)])
                        r))))

(defn- order*
  "Translates keyword to the respective traverser order.
  Valid values are :breadth, :depth and nil (will be :depth)
  See javadoc for Traverser for more info in orders."
  [o]
  (cond
   (= o :breadth) Traverser$Order/BREADTH_FIRST
   (or (= o :depth) (nil? o)) Traverser$Order/DEPTH_FIRST
   :else (throw (IllegalArgumentException.))))

(defprotocol StopEvaluator
  "Protocol for stop evaluation. Used for graph traversing.
  Functions:
  (stop-node? [this pos]) - Should return true if at stop node.
                            pos will be current position map."
  (stop-node? [this pos]))

(extend-protocol StopEvaluator
  ;; Implements StopEvaluator for simple functions
  clojure.lang.Fn
  (stop-node? [this pos] (this pos)))

(defn- stop-with-protocol
  "Custom StopEvaluator which dispatch on StopEvaluator protocol."
  [f]
  (reify org.neo4j.graphdb.StopEvaluator
    (^boolean isStopNode [this ^TraversalPosition p]
              (or (stop-node? f (process-position p))
                  false))))            ; nil is not valid return value

(defn- stop-evaluator
  "Translates value to the respective stop evaluator.
  Valid values are:
    - :end or nil - end of graph
    - :1, :2, :X (X being any positive integer) - depth of X
    - Custom function which takes one argument, current position
      and should return true when at stop node.
    - Anything extending StopEvaluator protocol.
  Examples: (stop-evaluator :8)
            (stop-evaluator :1)
            (stop-evaluator :end)
            (stop-evaluator nil)
            (stop-evaluator custom-fn)
  See javadoc for StopEvaluator for more info."
  [e]
  (cond
   (or (= :end e) (nil? e)) org.neo4j.graphdb.StopEvaluator/END_OF_GRAPH
   (= :1 e) org.neo4j.graphdb.StopEvaluator/DEPTH_ONE
   (keyword? e) (let [d (Integer/parseInt (name e))]
                  (stop-with-protocol #(>= (:depth %) d)))
   :else (stop-with-protocol e)))

(defprotocol ReturnableEvaluator
  "Protocol for return evaluation. Used for graph traversing.
  Functions:
  (returnable-node? [this pos]) - Should return true if node should
                                  be returned. pos will be current
                                  position map."
  (returnable-node? [this pos]))

(declare prop)

(extend-protocol ReturnableEvaluator
  ;; Implements ReturnableEvaluator for simple functions
  clojure.lang.Fn
  (returnable-node? [this pos] (this pos))
  ;; Implements ReturnableEvaluator for property map
  clojure.lang.IPersistentMap
  (returnable-node? [this pos] (let [^Node node (:node pos)
                                     check-prop (fn [[k v]]
                                                  (= v (prop node k)))]
                                 (every? check-prop this))))

(defn- returnable-with-protocol
  "Custom ReturnableEvaluator which dispatch on ReturnableEvaluator protocol."
  [f]
  (reify org.neo4j.graphdb.ReturnableEvaluator
    (^boolean isReturnableNode [this ^TraversalPosition p]
              (or (returnable-node? f (process-position p))
                  false))))            ; nil is not valid return value

(defn- returnable-evaluator
  "Translates value to the respective returnable evaluator.
  Valid values are:
    - :all-but-start or nil  - all but start nodes
    - :all - all nodes
    - Custom function which takes one argument, current position map
      and should return true when node at current position should be returned.
    - Map that defines key-value pairs that ReturnableEvaluator
      should match on specified property values.
    - anything extending ReturnableEvaluator protocol
  Examples: (returnable-evaluator :all)
            (returnable-evaluator :all-but-start)
            (returnable-evaluator nil)
            (returnable-evaluator custom-fn)
            (returnable-evaluator {:uid \"johndoe1\"})
  See javadoc for ReturnableEvaluator for more info."
  [e]
  (cond
   (or (= :all-but-start e) (nil? e)) org.neo4j.graphdb.ReturnableEvaluator/ALL_BUT_START_NODE
   (= :all e) org.neo4j.graphdb.ReturnableEvaluator/ALL
   :else (returnable-with-protocol e)))


(defn conn-utils
  "Responsible for establishing a connection to the database located at path and intializing utilities."
  [path]
  (#(assoc {}
             :db %
             :exec-eng (ExecutionEngine. %)
             :idx-mgr (.index %))
          (.newEmbeddedDatabase (GraphDatabaseFactory.) path)))

;;;; Public API

(defn start!
  "Establish a connection to the database.
  /-Uses *neo-db* Var to hold the connection.-/
  /-Uses *exec-eng* Var to reuse for Cypher queries.-/
  Associates a map with database connection and utilities to the keyword kw in var *neo-db*
  Do not use this function, use with-db! or with-local-db! instead."
  [kw path]
  (io!)
  (alter-var-root #'*neo-db* (fn [m] (assoc m kw (conn-utils path)))))

(defn stop!
  "Closes a connection associated to kw stored in *neo-db*.
  Do not use this function, use with-db! or with-local-db! instead."
  [kw]
  (io!)
  (.shutdown (-> *neo-db* kw :db))
  (alter-var-root #'*neo-db* (fn [m] (dissoc m kw))))

(comment
  ; These do not work.
  ; Is this just for statelessness?
  ; Would a function be more effective?
  (defmacro with-db!
  "Establish a connection to the neo db.
  Because there is an overhead when establishing connection, users should
  not call this macro often. Also note that this macro is not threadsafe."
  [kw path & body]
  (io!)
  `(do
     ;; Not using binding macro, because db should be accessible
     ;; from multiple threads.
     (start! ~kw ~path)
     (try
       ~@body
       (finally (stop! kw)))))

  (defmacro with-local-db!
  "Establish a connection to the neo db. Connection is visible
  only in current thread, and is given the keyword :local in map *neo-db*. Because there is an overhead when
  establishing connection, users should not call this macro often.
  This is a treadsafe version, which limits connection to
  the current thread only. This allows you to have parallel
  connections to different databases. It is not recommended to use
  this function together with with-db! in one program."
  [path & body]
  (io!)
  ;; Using binding macro, db is accessible only in this thread
  `(binding [*neo-db* '~(assoc *neo-db* :local (conn-utils path))]
     (try
       ~@body
       (finally (stop!))))))

(defmacro with-tx
  "Establish a transaction with the database db. Use it for mutable db operations.
  If you do not want to commit it, throw an exception.
  All mutable functions use transactions by default, so you don't
  have to use this macro. You should use this macro to group your
  functions into a bigger transactions."
  [db & body]
  `(let [tx# (.beginTx ~db)]
     (try
       (let [val# (do ~@body)]
         (.success tx#)
         val#)
       (finally (.finish tx#)))))

(defn get-path
  "Returns path to where the database referred to by kw is stored."
  [kw]
  (.getStoreDir (-> *neo-db* kw :db)))

(defn read-only?
  "Returns true if database referred to by kw is read only."
  [kw]
  (.isReadOnly (-> *neo-db* kw :db)))

(defn index
  "Returns the IndexManager paired with this graph database service referred to by kw."
  [kw]
  (.index (-> *neo-db* kw :db)))

(declare all-nodes)

(declare rels)

(declare delete!)

(declare get-id)
  
(defn purge!
  "Deletes all nodes from database referred to by kw together with all relationships."
  [kw]
  (io!)
  ;; first delete all relationships
  (doseq [node (all-nodes)]
    (with-tx (-> *neo-db* kw :db)
      (doseq [r (rels node)]
        (delete! r))))
  (with-tx (-> *neo-db* kw :db)
    ;; now delete nodes, except reference node
    (doseq [node (all-nodes)]
      (delete! node))))

;;; Property Containers

(defn prop?
  "Returns true if given node or relationship contains
  property with a given key."
  [^PropertyContainer c k]
  (.hasProperty c (encode-property-key k)))

(defn prop
  "Returns property value based on its key.
  If property is not found, returns nil.
  If property value is string which starts with colon,
  it is converted to keyword."
  [^PropertyContainer c k]
  (let [v (.getProperty c (encode-property-key k) nil)]
    (if (array? v)                      ; handle multiple values
      (map decode-property-value v)
      (decode-property-value v))))

;; TODO: add fns which are less resource consuming :), like map which
;; has lazy values

(defn props
  "Returns map of properties for a given node or relationship.
  Fetches all properties and can be very resource consuming if node
  contains many large properties. This is a convenience function.
  If property value is string which starts with colon,
  is is converted to keyword."
  [^PropertyContainer c]
  (let [keys (.getPropertyKeys c)
        convert-fn (fn [k] [(keyword k) (prop c k)])]
    (into {} (map convert-fn keys))))

(defn set-prop!
  "Sets or remove property for a given node or relationship.
  The property value must be one of the valid property types
  (see Neo4j docs) or a keyword.
  If a property value is nil, removes this property from the given
  node or relationship."
  ([kw ^PropertyContainer c key]
     (set-prop! kw c key nil))
  ([kw ^PropertyContainer c key value]
     (io!)
     (with-tx (-> *neo-db* kw :db)
       (if (not (nil? value))
         (.setProperty c (encode-property-key key)
                       (if (coll? value) ; handle multiple values
                         (into-array (map encode-property-value value))
                         (encode-property-value value)))
         (.removeProperty c (encode-property-key key))))))

(defn set-props!
  "Sets properties for a given node or relationship.
  The property value must be one of the valid property types
  (see Neo4j docs) or a keyword.
  If a property value is nil, removes this property from the given
  node or relationship. This is a convenience function."
  [kw ^PropertyContainer c props]
  (io!)
  (with-tx (-> *neo-db* kw :db)
    (doseq [[k v] props]
      (set-prop! kw c k v))))

(defn get-id
  "Returns id for a given node or relationship.
  Note that ids are not very good as unique identifiers."
  [item]
  (.getId item))

(defn delete!
  "Deletes node or relationship.
  Only node which has no relationships attached to it can be deleted."
  [kw item]
  (io!)
  (with-tx (-> *neo-db* kw :db)
    (.delete item)))

;;; Relationships

;; Relationship directions used when getting relationships from a
;; node or when creating traversers.
;;
;; A relationship has a direction from a node's point of view. If a
;; node is the start node of a relationship it will be an :out
;; relationship from that node's point of view. If a node is the end
;; node of a relationship it will be an :in relationship from
;; that node's point of view. The :both keyword is used when
;; direction is of no importance, such as "give me all" or "traverse
;; all" relationships that are either :out or :in. (from Neo4j
;; javadoc)

(defn rel-nodes
  "Returns the two nodes attached to the given relationship.
  This is a convenience function."
  [^Relationship r]
  (.getNodes r))

(defn start-node
  "Returns start node for given relationship."
  [^Relationship r]
  (.getStartNode r))

(defn end-node
  "Returns end node for given relationship."
  [^Relationship r]
  (.getEndNode r))

(defn other-node
  "Returns other node for given relationship.
  This is a convenience function."
  [^Relationship r ^Node node]
  (.getOtherNode r node))

(defn rel-type
  "Returns type of given relationship."
  [^Relationship r]
  (keyword (.name (.getType r))))

(defn create-rel!
  "Creates relationship of a supplied type between from and to nodes."
  [kw ^Node from type ^Node to]
  (io!)
  (with-tx (-> *neo-db* kw :db)
    (.createRelationshipTo from to (rel-type* type))))

(defn all-rel-types
  "Returns lazy seq of all relationship types currently in database referred to by kw."
  [kw]
  (lazy-seq
   (.getRelationshipTypes (-> *neo-db* kw :db))))


;;; Labels

(defn dynamic-label
  "Creates a label with the supplied name"
  [name]
  (DynamicLabel/label name))

 (defn label?
   "Returns true if the given node
   has a label with the supplied name"
   [^Node node label-name]
   (.hasLabel node (dynamic-label label-name)))

(defn add-label!
  "Adds the given label to the node."
  [kw ^Node node name]
  (io!)
  (with-tx (-> *neo-db* kw :db)
    (.addLabel node (dynamic-label name))))

(defn remove-label!
  "Removes the supplied label from the node."
  [kw ^Node node name]
  (io!)
  (with-tx (-> *neo-db* kw :db)
    (.removeLabel node (dynamic-label name))))

(defn labels
  "Lists all labels attached to this node."
  [^Node node]
  (map (fn [^DynamicLabel l] (.name l))
       (.getLabels node)))

;;; Nodes

(defn rel?
  "Returns true if there are relationships attached to this node. Syntax:
  [node]                - All relationships.
  [node type-or-types]  - Relationships of any of specified types with
                          any direction.
  [node type direction] - Relationships of specified type and
                          of specified direction. You can supply nil for
                          one of the arguments if you do not care for
                          either direction of relationship type.
  Valid directions are :in :out and :both, parameter type can be any keyword.
  Examples: (rel? node)                  ; All rels
            (rel? node :foo)             ; Rels of :foo type of any direction
            (rel? node [:foo :bar :baz]) ; Rels of any of specified types,
                                         ; any directions
            (rel? node :foo :in)         ; Rels of :foo type, :in direction
            (rel? node nil :in)          ; Rels of any type of :in direction
            (rel? node :foo nil)         ; Use (rel? node :foo) instead"
  ([^Node node]
     (.hasRelationship node))
  ([^Node node type-or-types]
     (let [t (map rel-type* (flatten [type-or-types]))]
       ;; TODO: Is there a way to type hint array in following call?
       (.hasRelationship node (into-array RelationshipType t))))
  ([^Node node type direction]
     (cond
      (nil? type) (.hasRelationship node (rel-dir direction))
      (nil? direction) (rel? node type)
      :else (.hasRelationship node (rel-type* type) (rel-dir direction)))))

(defn rels
  "Returns relationships attached to this node. Syntax:
  [node]                - All relationships.
  [node type-or-types]  - Relationships of any of specified types with
                          any direction.
  [node type direction] - Relationships of specified type and
                          of specified direction. You can supply nil for
                          one of the arguments if you do not care for
                          either direction of relationship type.
  Valid directions are :in :out and :both, parameter type can be any keyword.
  Examples: (rels node)                  ; All rels
            (rels node :foo)             ; Rels of :foo type of any direction
            (rels node [:foo :bar :baz]) ; Rels of any of specified types,
                                         ; any directions
            (rels node :foo :in)         ; Rels of :foo type, :in direction
            (rels node nil :in)          ; Rels of any type of :in direction
            (rels node :foo nil)         ; Use (rel node :foo) instead"
  ([^Node node]
     (seq (.getRelationships node)))
  ([^Node node type-or-types]
     (let [t (map rel-type* (flatten [type-or-types]))]
       ;; TODO: Is there a way to type hint array in following call?
       (seq (.getRelationships node (into-array RelationshipType t)))))
  ([^Node node type direction]
     (cond
      (nil? type) (seq (.getRelationships node (rel-dir direction)))
      (nil? direction) (rels node type)
      :else (seq (.getRelationships node (rel-type* type) (rel-dir direction))))))

(defn single-rel
  "Returns the only relationship for the node of the given type and
  direction.
  Valid directions are :in :out and :both, defaults to :out."
  ([^Node node type]
     (single-rel node type :out))
  ([^Node node type direction]
     (.getSingleRelationship node (rel-type* type) (rel-dir direction))))

(defn create-labeled-node!
  [kw & label-names]
  (io!)
  (with-tx (-> *neo-db* kw :db)
    (.createNode (-> *neo-db* kw :db) (into-array Label (map dynamic-label label-names)))))

(defn create-node!
  "Creates a new node, not linked with any other nodes.
  Labels can optionally be provided to add to the node."
  ([kw]
    (io!)
    (with-tx (-> *neo-db* kw :db)
      (.createNode (-> *neo-db* kw :db))))
  ([kw props & label-names]
     (doto (apply create-labeled-node! (cons kw label-names))
          (set-props! props))))

(defn create-child!
  "Creates a node that is a child of the specified parent node
  along the specified relationship.
  props is a map that defines the properties of the node.
  This is a convenience function."
  [kw ^Node node type props]
     (io!)
     (with-tx (-> *neo-db* kw :db)
       (let [child (create-node! props)]
         (create-rel! node type child)
         child)))

(defn delete-node!
  "Delete node and all its relationships.
  This is a convenience function."
  [kw node]
  (io!)
  (with-tx (-> *neo-db* kw :db)
    (doseq [r (rels node)]
      (delete! r))
    (delete! node)))

(defn find-nodes
  "Finds nodes in the database referred to by kw with the supplied label and predicate"
  [kw label-name key val]
  (io!)
  (lazy-seq (.findNodesByLabelAndProperty
             (-> *neo-db* kw :db)
             (dynamic-label label-name)
             (encode-property-key key)
             (encode-property-value val))))


;;; Graph traversal helpers

;; Nodes can be traversed either :breadth first or :depth first. A
;; :depth first traversal is often more likely to find one matching
;; node before a :breadth first traversal. A :breadth first traversal
;; will always find the closest matching nodes first, which means that
;; TraversalPosition.depth() will return the length of the shortest
;; path from the start node to the node at that position, which is not
;; guaranteed for :depth first traversals. (from Neo4j docs)

;; A :breadth first traversal usually needs to store more state about
;; where the traversal should go next than a :depth first traversal
;; does. Depth first traversals are thus more memory efficient.
;; (from Neo4j docs)

;;; Graph traversal

(defn all-nodes
  "Returns lazy-seq of all nodes in the db."
  [kw]
  (lazy-seq (.getAllNodes (GlobalGraphOperations/at (-> *neo-db* kw :db)))))

(defn all-nodes-with-label
  "Returns lazy-seq of all nodes with the given label."
  [kw label-name]
  (lazy-seq (.getAllNodesWithLabel (GlobalGraphOperations/at (-> *neo-db* kw :db))
                                   (dynamic-label label-name))))

(defn node-by-id
  "Returns node with a given id, or nil if no such node exists.
  Note that ids are not very good as unique identifiers."
  [kw id]
  (try
    (.getNodeById (-> *neo-db* kw :db) id)
  (catch NotFoundException e nil)))

(defn rel-by-id
  "Returns relationship with a given id, or nil if no such relationship exists.
  Note that ids are not very good as unique identifiers."
  [kw id]
  (try
    (.getRelationshipById (-> *neo-db* kw :db) id)
  (catch NotFoundException e nil)))

(defn walk
  "Walks through the graph by following specified relations. Returns last node.
  Throws NullPointerException if path is wrong.
  Throws NotFoundException if path is ambiguous."
  [^Node node & types]
  (let [next-node (fn [^Node node type]
                    (second (rel-nodes (single-rel node type))))]
    (reduce next-node node types)))

(defn traverse
  "Traverses the graph. Starting at the given node, traverse the graph
  in specified order, stopping based on stop-eval. The return-eval
  decides which nodes make it into the result. The rel is used to
  decide which edges to traverse.
  order - :breadth, :depth. Will be :depth if nil supplied.
  stop-eval accepts following values:
    - :end or nil - End of graph.
    - :1, :2, :X (X being any positive integer) - Depth of X.
    - Custom function which takes one argument, current position
      and should return true when at stop node.
  return-eval accepts following values:
    - :all-but-start or nil  - all but start nodes.
    - :all - all nodes.
    - Custom function which takes one argument, current position map
      and should return true when node at current position should be returned.
    - Map defining key-value pairs that will be matched against
      properties of wanna-be returned nodes.
  rel - Keyword representing relation type or a map where keys are
        relation type keywords and values are directions (:in or :out)."
  ([^Node node rel]
     (traverse node nil nil nil rel))
  ([^Node node return-eval rel]
     (traverse node nil nil return-eval rel))
  ([^Node node stop-eval return-eval rel]
     (traverse node nil stop-eval return-eval rel))
  ([^Node node order stop-eval return-eval rel]
     (lazy-seq
      (.traverse node
                 (order* order)
                 (stop-evaluator stop-eval)
                 (returnable-evaluator return-eval)
                 (rel-dir-map rel)))))

;;; Cypher queries

(defn cypher
  "Returns lazy-seq of results from a Cypher query"
  ([kw query] (lazy-seq (.execute (-> *neo-db* kw :exec-eng) query)))
  ([kw query params] (lazy-seq (.execute (-> *neo-db* kw :exec-eng)
                                      query
                                      (java.util.HashMap. params)))))

;;;; Examples

(comment

;;; See README for usage instructions, documentation and examples.

  (start! :test-db "test-db")

  (all-nodes :test-db)

  (stop! :test-db)

  (purge! :test-db)

  (require ['borneo.core :as 'db])

;;; Populate database with graph inspired by Neo4j Matrix social graph (for simplicity I do not check if graph already exists):

  (do
    ;; add programs
    (def smith (db/create-node-with-props! :test-db
      {:name "Agent Smith"
      :language "C++"
      :age 40}
      "Program"))

    (def architect (db/create-node-with-props! :test-db
      {:name "Architect"
      :language "Clojure"
      :age 600}
      "Program"))

    ;; add humans
    (def neo (db/create-node-with-props! :test-db
      {:name "Thomas Anderson"
      :age 29}
      "Human"))

    (def trinity (db/create-node-with-props! :test-db
      {:name "Trinity"
      :age 27}
      "Human"))

    (def morpheus (db/create-node-with-props! :test-db
      {:name "Morpheus"
      :rank "Captain"
      :age 35}
      "Human"))

    (def cypher (db/create-node-with-props! :test-db
      {:name "Cypher"}
      "Human"))

    ;; add relationships

    (db/create-rel! neo :knows trinity)
    (db/create-rel! neo :knows morpheus)
    (db/create-rel! morpheus :knows trinity)
    (db/create-rel! morpheus :knows cypher)
    (db/set-props! (db/create-rel! cypher :knows smith)
                    {:disclosure "secret"
                     :age 6})
    (db/create-rel! smith :knows architect)
    (db/create-rel! trinity :loves neo))

;;; Basic traversal:

;;; Assuming I do not have any previous references to nodes.
;;; Get me all human nodes:

  (let [humans (db/walk (db/root) :humans)]
    (db/traverse humans :human))
  ;; evals to:
  ;; (#<NodeProxy Node[5]> #<NodeProxy Node[6]>
  ;;  #<NodeProxy Node[7]> #<NodeProxy Node[8]>)

;;; I want to see their properties:

  (let [human-nodes (db/traverse (db/walk (db/root) :humans) :human)]
    (map db/props human-nodes))
  ;; evals to:
  ;; ({:name "Thomas Anderson", :age 29}
  ;;  {:name "Trinity", :age 27}
  ;;  {:name "Morpheus", :rank "Captain", :age 35}
  ;;  {:name "Cypher"})

;;; Want to find Mr. Andersons node, assuming I don't have one:

  (def neo (first (db/traverse (db/walk (db/root) :humans)
                                    {:name "Thomas Anderson"}
                                    :human)))
  ;; Or if I want to traverse from root
  (def neo (first (db/traverse (db/root)
                                    {:name "Thomas Anderson"}
                                    {:humans :out
                                     :human :out})))

;;; Properties and Relationships:

;;; Andersons properties (this fetches all properties and may be resource intensive if node has e.g. large binary properties):

  (db/props neo)
  ;; evals to:
  ;; {:name "Thomas Anderson", :age 29}

;;; Andersons age:

  (db/prop neo :age)
  ;; evals to:
  ;; 29

;;; Andersons relationships:

  (db/rels neo)
  ;; evals to:
  ;; (#<RelationshipProxy Relationship[4]>
  ;;  #<RelationshipProxy Relationship[8]>
  ;;  #<RelationshipProxy Relationship[9]>
  ;;  #<RelationshipProxy Relationship[14]>)

;;; But I want to see their types:

  (map db/rel-type (db/rels neo))
  ;; evals to:
  ;; (:human :knows :knows :loves)

;;; Get :knows or :loves type relationships:

  (db/rels neo [:knows :loves])

;;; Get love relationships only:

  (db/rels neo :loves)

;;; Get incoming relationships only:

  (db/rels neo nil :in)

;;; Advanced Traversal

;;; Who does Anderson know?:

  (map #(db/prop % :name)
       (db/traverse neo :1 nil :knows))
  ;; ("Trinity" "Morpheus")

;;; Go one level deeper:

  (map #(db/prop % :name)
       (db/traverse neo :2 nil :knows))
  ;; ("Trinity" "Morpheus" "Cypher")

;;; Go all the way down:

  (map #(db/prop % :name)
       (db/traverse neo nil nil :knows))
  ;; ("Trinity" "Morpheus" "Cypher" "Agent Smith" "Architect")

;;; Return every human who does not have his age set. Create a custom returnable evaluator function first:

  (defn age-not-present? [pos]
    (and
     (not (:start? pos))                ; eliminate start node
     (not (db/prop (:node pos) :age))))

;;; Now find every human without his age set:

  (map db/props (db/traverse (db/walk (db/root) :humans)
                               age-not-present? :human))
  ;; ({:name "Cypher"})

;;; Return anybody between specified age range. Create custom return evaluator:

  (deftype AgeRangeEvaluator [from to]
    db/ReturnableEvaluator
    (returnable-node? [this pos] (let [age (db/prop (:node pos) :age)]
                                   (when age
                                     (and
                                      (>= age from)
                                      (<= age to))))))
;;; Traverse:

  (map db/props (db/traverse (db/root)
                               (AgeRangeEvaluator. 30 40)
                               {:humans :out
                                :human :out
                                :programs :out
                                :program :out}))
  ;; evals to:
  ;; ({:name "Agent Smith", :language "C++", :age 40}
  ;;  {:name "Morpheus", :rank "Captain", :age 35})

)
