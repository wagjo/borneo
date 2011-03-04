;; Copyright (C) 2011, Jozef Wagner. All rights reserved.
;; This ns is forked from hgavin/clojure-neo4j

(ns neo4j.core
  "Neo4j wrapper, forked from hgavin/clojure-neo4j"
  (:import (org.neo4j.graphdb Direction
                              Node
                              PropertyContainer
                              Relationship
                              RelationshipType
                              ReturnableEvaluator
                              StopEvaluator
                              Transaction
                              TraversalPosition
                              Traverser
                              Traverser$Order)
	   (org.neo4j.kernel EmbeddedGraphDatabase)))

;; Wrapper for Neo4j. Because I was not happy with the current state
;; (01/2011) of existing Neo4j wrappers for clojure, I've decided to
;; create my own.
;;
;; Purpose of this ns is to provide intiutive access to commonly used
;; Neo4j operations. It uses official Neo4j Java bindings. It does not
;; use Blueprints interface.
;;
;; Disclaimer: I have forked hgavin/clojure-neo4j and modified it
;; heavily.
;;
;; Disclaimer: Some comments and docs are taken from official Neo4j javadocs.
;;
;;; Usage:
;; - use with-db! to establish a connection to the database
;; - all db operations must be inside with-db! body
;;
;;; Code notes:
;; - *neo-db* holds the current db instance, so that users do not have
;;   to supply db instance at each call to db operations. This
;;   approach has of course its drawbacks (e.g. only one connection at
;;   time), but I've found it suitable for my purposes.

;;;; Implementation details

(defonce ^{:private true
           :doc "Holds the current database instance"
           :tag EmbeddedGraphDatabase}
  *neo-db* nil)

(defn- array? [x]
  "Determine whether x is an array or not"
  (-> x class .isArray))

(defn- name-or-str [x]
  "If x is keyword, returns its name. If not, stringify the value"
  (if (keyword? x) 
    (name x) 
    (str x)))

(defn- process-position [^TraversalPosition p]
  "Translate TraversalPosition into a map"
  {:pos p
   :node (.currentNode p)
   :depth (.depth p)
   :start? (.isStartNode p)
   :last-rel (.lastRelationshipTraversed p)
   ;; FIXME: Following call causes NullPointerException
   ;; :prev-node (.previousNode p)
   :count (.returnedNodesCount p)})

(defn- ^RelationshipType rel-type* [k]
  "Create java class implementing RelationshipType. Used for interop.
  TODO: Should we cache these instances to save memory?
  Or will they be GCd?"
  (proxy [RelationshipType] []
    (name [] (name k))))

(defn- ^Direction rel-dir [k]
  "Translates keyword to the respective relationship direction.
  Valid values are :out :in and :both
  See javadoc for Direction for more info on directions"
  (condp = k
      :out Direction/OUTGOING
      :in Direction/INCOMING
      :both Direction/BOTH
      (throw (IllegalArgumentException.))))

(defn- rel-dir-map [r]
  "Constructs an array or relation types and directions. Accepts
  map consisting of relation type keywords as keys and
  relation directions keywords (:in or :out) as values.
  r canalso be a single keyword; in that case, a default :out will
  be added to the array."
  (if (keyword? r)
    (rel-dir-map {r :out})   
    (into-array Object
                (mapcat (fn [[k v]] [(rel-type* k) (rel-dir v)])
                        r))))

(defn- order* [o]
  "Translates keyword to the respective traverser order.
  Valid values are :breadth, :depth and nil (will be :depth)
  See javadoc for Traverser for more info in orders."
  (cond
   (= o :breadth) Traverser$Order/BREADTH_FIRST
   (or (= o :depth) (nil? o)) Traverser$Order/DEPTH_FIRST
   :else (throw (IllegalArgumentException.))))

(defn- stop-if [f]
  "Custom StopEvaluator, f should return true when at stop node. f takes one
  argument. The function will be passed the current position map to act on."
  (proxy [StopEvaluator] []
    (isStopNode [^TraversalPosition p] (f (process-position p)))))

(defn- depth-of [d]
  "Return a StopEvaluator for the given traversal depth."
  (if (== d 1) 
    StopEvaluator/DEPTH_ONE
    (stop-if #(== (:depth %) d))))

(defn- stop-evaluator [e]
  "Translates value to the respective stop evaluator.
  Valid values are:
    - :end or nil - end of graph
    - :1, :2, :X (X being any positive integer) - depth of X
    - Custom function which takes one argument, current position
      and should return true when at stop node.
  Examples: (stop-evaluator :8)
            (stop-evaluator :1)
            (stop-evaluator :end)
            (stop-evaluator nil)
            (stop-evaluator custom-fn)
  See javadoc for StopEvaluator for more info."
  (cond
   (or (= :end e) (nil? e)) StopEvaluator/END_OF_GRAPH
   (keyword? e) (depth-of (Integer/parseInt (name e)))
   (fn? e) (stop-if e)))

(defn- return-if [f]
  "Custom ReturnableEvaluator, f should return true if node at current
  position should be returned. Takes a function of one argument.
  The function will be passed the current position map to act on."
  (proxy [ReturnableEvaluator] []
    (isReturnableNode [^TraversalPosition p] (f (process-position p)))))

(defn- return-by-props [props]
  "Creates a ReturnableEvaluator for use with a traverser that returns
  nodes that match the specified property values. propmap is a map that
  defines key value pairs that ReturnableEvaluator should match on."
  (return-if
   (fn [pos]
     (let [^Node node (:node pos)
           check-prop (fn [[k v]] (= v (.getProperty node (name-or-str k) nil)))]
       (every? check-prop props)))))

(defn- return-evaluator [e]
  "Translates value to the respective return evaluator.
  Valid values are:
    - :all-but-start or nil  - all but start nodes
    - :all - all nodes
    - Custom function which takes one argument, current position map
      and should return true when node at current position should be returned.
    - Map that defines key-value pairs that ReturnableEvaluator
      should match on specified property values.
  Examples: (return-evaluator :all)
            (return-evaluator :all-but-start)
            (return-evaluator nil)
            (return-evaluator custom-fn)
            (return-evaluator {:uid \"johndoe1\"})
  See javadoc for ReturnEvaluator for more info."
  (cond
   (or (= :all-but-start e) (nil? e)) ReturnableEvaluator/ALL_BUT_START_NODE
   (= :all e) ReturnableEvaluator/ALL
   (map? e) (return-by-props e)
   (fn? e) (return-if e)))

(defn- start! [path]
  "Establish a connection to the database.
  Uses *neo-db* to hold the connection."
  (io!)
  (let [n (EmbeddedGraphDatabase. path)]
    (alter-var-root #'*neo-db* (fn [_] n))))

(defn- stop! []
  "Closes a connection stored in *neo-db*"
  (io!)
  (.shutdown *neo-db*))


;;;; Public API

(defmacro with-db! [path & body]
  "Establish a connection to the neo db.
  Because there is an overhead when establishing connection, users should
  not call this macro often. Also note that this macro is not threadsafe."
  (io!)
  `(do
     ;; Not using binding macro, because db should be accessible
     ;; from multiple threads.
     (start! path)
     (try
       ~@body
       (finally (stop!)))))

(defmacro with-tx [& body]
  "Establish a transaction. Use it for mutable db operations.
  If you do not want to commit it, throw an exception."
  `(let [tx# (.beginTx *neo-db*)]
     (try
       (let [val# (do ~@body)]
         (.success tx#)
         val#)
       (finally (.finish tx#)))))

(defn id [item]
  "Returns id for a given node or relationship"
  (.getId item))

(defn delete! [item]
  "Deletes node or relationship.
  Only node which has no relationships attached to it can be deleted."
  (io!)
  (with-tx
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

(defn rel?
  "Returns true if there are relationships attached to this node. Syntax:
  [node]                - All relationships
  [node type-or-types]  - Relationships of any of specified types with
                          any direction
  [node type direction] - Relationships of specified type and
                          of specified direction. You can supply nil for
                          one of the arguments if you do not care for
                          either direction of relationship type
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
  "Returns all relationships attached to this node. Syntax:
  [node]                - All relationships
  [node type-or-types]  - Relationships of any of specified types with
                          any direction
  [node type direction] - Relationships of specified type and
                          of specified direction. You can supply nil for
                          one of the arguments if you do not care for
                          either direction of relationship type
  Valid directions are :in :out and :both, parameter type can be any keyword.
  Examples: (rel node)                  ; All rels
            (rel node :foo)             ; Rels of :foo type of any direction
            (rel node [:foo :bar :baz]) ; Rels of any of specified types,
                                        ; any directions
            (rel node :foo :in)         ; Rels of :foo type, :in direction
            (rel node nil :in)          ; Rels of any type of :in direction
            (rel node :foo nil)         ; Use (rel node :foo) instead"
  ([^Node node]
     (.getRelationships node))
  ([^Node node type-or-types]
     (let [t (map rel-type* (flatten [type-or-types]))]
       ;; TODO: Is there a way to type hint array in following call?
       (.getRelationships node (into-array RelationshipType t))))
  ([^Node node type direction]
     (cond
      (nil? type) (.getRelationships node (rel-dir direction))
      (nil? direction) (rels node type)
      :else (.getRelationships node (rel-type* type) (rel-dir direction)))))

(defn single-rel 
  "Returns the only relationship for the node of the given type and
  direction.
  Valid directions are :in :out and :both, defaults to :out"
  ([^Node node type]
     (single-rel node type :out))
  ([^Node node type direction]
     (.getSingleRelationship node (rel-type* type) (rel-dir direction))))

(defn create-rel! [^Node from type ^Node to]
  "Create relationship of a supplied type between from and to nodes."
  (io!)
  (with-tx
    (.createRelationshipTo from to (rel-type* type))))

(defn nodes [^Relationship r]
  "Returns the two nodes attached to the given relationship."
  (.getNodes r))

(defn other [^Relationship r ^Node node]
  "Returns other node for given relationship."  
  (.getOtherNode r node))

(defn rel-type [^Relationship r]
  "Returns type of given relationship."  
  (keyword (.name (.getType r))))

;;; Properties

(defn prop? [^PropertyContainer c k]
  "Returns true if given node or relationship contains
  property with a given key"
  (.hasProperty c (name-or-str k)))

;; TODO: add fns which are less resource consuming :)

;; Props fetch all properties and can be very resource consuming if
;; node contains many properties.
(defn props [^PropertyContainer c]
  "Return map of properties for a given node or relationship."
  (let [keys (.getPropertyKeys c)
        convert-fn (fn [k] [(keyword k)
                           (let [v (.getProperty c k)]
                             (if (array? v) ; handle multiple values
                               (seq v)
                               v))])]
    (into {} (map convert-fn keys))))

(defn set-prop!
  "Sets or remove property for a given node or relationship.
  The property value must be one of the valid property types (see Neo4j docs).
  If a property value is nil, removes this property from the given
  node or relationship."
  ([^PropertyContainer c key]
     (set-prop! c key nil))
  ([^PropertyContainer c key value]
     (io!)
     (with-tx
       (if value
         ;; TODO: better support primivives and arrays of primitives
         (.setProperty c (name-or-str key)
                       (if (coll? value) ; handle multiple values
                         (into-array String value)
                         value))
         (.removeProperty c (name-or-str key))))))

(defn set-props! [^PropertyContainer c props]
  "Sets properties for a given node or relationship.
  The property value must be one of the valid property types (see Neo4j docs).
  If a property value is nil, removes this property from the given
  node or relationship."
  (io!)
  (with-tx
    (doseq [[k v] props]
      (set-prop! c k v))))

;;; Nodes

(defn root []
  "Returns reference/root node."
  (.getReferenceNode *neo-db*))

(defn create-node!
  "Creates a new node."
  ([]
     (io!)
     (with-tx
       (.createNode *neo-db*)))
  ([props]
     (with-tx
       (doto (create-node!)
         (set-props! props)))))

(defn create-child!
  "Creates a node that is a child of the specified parent node
  (or root node) along the specified relationship.
  props is a map that defines the properties of the node."
  ([type props]
     (create-child! (root) type props))
  ([node type props]
     (io!)
     (with-tx
       (let [child (create-node! props)]
         (create-rel! node type child)
         child))))

(defn delete-node! [node]
  "Delete node and all its relationships."
  (io!)
  (with-tx
    (doseq [r (rels node)]
      (delete! r))
    (delete! node)))

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

(defn go [^Node node & types]
  "Walk through the graph by following specified relations. Returns last node.
  Throws NullPointerException if path is wrong.
  Throws NotFoundException if path is ambiguous."
  (let [next-node (fn [^Node node type]
                    (second (nodes (single-rel node type))))]
    (reduce next-node node types)))

(defn traverse 
  "Traverse the graph. Starting at the given node, traverse the graph
  in specified order, stopping based on stop-eval. The return-eval
  decides which nodes make it into the result. The rel is used to
  decide which edges to traverse.
  order - :breadth, :depth. Will be :depth if nil supplied.
  stop-eval accepts following values:
    - :end or nil - end of graph
    - :1, :2, :X (X being any positive integer) - depth of X
    - Custom function which takes one argument, current position
      and should return true when at stop node.
  return-eval accepts following values:
    - :all-but-start or nil  - all but start nodes
    - :all - all nodes
    - Custom function which takes one argument, current position map
      and should return true when node at current position should be returned.
    - Map defining key-value pairs that will be matched against
      properties of wanna-be returned nodes.
  rel - a keyword representing relation type or a map where keys are
        relation type keywords and values are directions (:in or :out)"
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
                 (return-evaluator return-eval)
                 (rel-dir-map rel)))))

;;;; Examples

(comment

  (start! "neo-db")

  (traverse (root)
            :depth
            :end
            :all
            {:foo :out})

  (map props (traverse (root)
                       {:property1 "value"}
                       {:foo :out :bar :out}))

  (count (traverse (go (root) :foo)
                   :bar))
  
)
