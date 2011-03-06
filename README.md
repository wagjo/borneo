# borneo

Wrapper for Neo4j, a graph database.

Purpose of this library is to provide intiutive access to commonly used
Neo4j operations. It uses official Neo4j Java bindings. It does not
use Blueprints interface.

## Usage

Add the following dependency to your project.clj file:

    [borneo "0.1.0-SNAPSHOT"]

## Documentation

Detailed API docs are at [http://wagjo.github.com/borneo/](http://wagjo.github.com/borneo/)

Quick overview of available functions (most important ones are
emphasized):

* _Database management_
  * _[\*neo-db\*](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/*neo-db*)_ - Holds current database instance
  * _[start!](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/start!)_ - Establish a connection to the database
  * _[stop!](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/stop!)_ - Closes a connection stored in \*neo-db\*
  * ___[with-db!](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/with-db!)_ - establish a connection to the database__
  * _[with-local-db!](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/with-local-db!)_ - establish a thread local connection to the database
  * ___[with-tx](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/with-tx)_ - establish a transaction__
  * _[get-path](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/get-path)_ - get path to where database is stored
  * _[read-only?](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/read-only?)_ - returns true if database is read only
  * _[index](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/index)_ - returns Index Manager
* _Property Containers (both Nodes and Relationships)_
  * _[prop?](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/prop?)_ - returns true if node or relationship contains given property
  * _[prop](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/prop)_ - returns specific property value for a given node or relationship
  * ___[props](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/props)_ - returns map of properties for a given node or relationship__
  * _[set-prop!](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/set-prop!)_ - sets or removes property in a given node or relationship
  * ___[set-props!](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/set-props!)_ - sets (or removes) properties for a given node or relationships__
  * _[get-id](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/get-id)_ - returns id of a given node or relationship
  * ___[delete!](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/delete!)_ - deletes relationship or free node__
* _Relationships_
  * ___[rel-nodes](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/rel-nodes)_ - returns the two nodes attached to the given relationship__
  * _[start-node](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/start-node)_ - returns start node for given relationsip
  * _[end-node](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/end-node)_ - returns end node for given relationsip
  * _[other-node](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/other-node)_ - returns other node for given relationsip
  * ___[rel-type](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/rel-type)_ - returns type of given relationship__
  * ___[create-rel!](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/create-rel!)_ - create relationship between two nodes__
  * _[all-rel-types](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/all-rel-types)_ - returns lazy seq of all relationship types in database
* _Nodes_
  * _[rel?](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/rel?)_ - returns true if node has given relationship(s)
  * ___[rels](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/rels)_ - returns relationships attached to given node__
  * _[single-rel](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/single-rel)_ - returns single relationship for given node
  * _[create-node!](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/create-node!)_ - creates new node, not linked with any other nodes
  * ___[create-child!](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/create-child!)_ - creates a child node of a given parent__
  * ___[delete-node!](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/delete-node!)_ - delete node and all its relationships__
* _Graph traversal protocols_
  * _[ReturnableEvaluator](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/ReturnableEvaluator)_ - Protocol for return evaluation. Used for graph traversing.
  * _[StopEvaluator](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/StopEvaluator)_ - Protocol for stop evaluation. Used for graph traversing.
* _Graph traversal_
  * _[all-nodes](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/all-nodes)_ - returns lazy-seq of all nodes in database
  * _[node-by-id](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/node-by-id)_ - returns node with a given id
  * _[rel-by-id](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/rel-by-id)_ - returns relationship with a given id
  * ___[root](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/root)_ - returns root/reference node__
  * ___[walk](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/walk)_ - walk though the graph by following through given single relations__
  * ___[traverse](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/traverse)_ - traverse the graph__

## Examples

Following examples show basic borneo functions. Code presented here is
not meant to be an idiomatic clojure code, e.g. you should wrap most
of your operations in separate functions, and use let instead of def
to store a reference to a node.

### Basic usage

Require a borneo ns and wrap all borneo related stuff in a with-db! macro:

    (ns foo.example
      (:require [borneo.core :as neo]))

    (neo/with-db! "matrix-db"

      ;; use borneo here

    )

### Populate database

Populate database with graph inspired by [Neo4j Matrix social
graph](http://dist.neo4j.org/basic-neo4j-code-examples-2008-05-08.pdf)
(for simplicity I do not check if graph already exists):

    ;; basic layout
    (def humans (neo/create-child! :humans nil))
    (def programs (neo/create-child! :programs nil))

    ;; add programs
    (def smith (neo/create-child! programs :program
                                  {:name "Agent Smith"
                                   :language "C++"
                                   :age 40}))
    (def architect (neo/create-child! programs :program
                                      {:name "Architect"
                                       :language "Clojure"
                                       :age 600}))

    ;; add humans
    (def the-one (neo/create-child! humans :human
                                    {:name "Thomas Anderson"
                                     :age 29}))
    (def trinity (neo/create-child! humans :human
                                    {:name "Trinity"
                                     :age 27}))
    (def morpheus (neo/create-child! humans :human
                                     {:name "Morpheus"
                                      :rank "Captain"
                                      :age 35}))
    (def cypher (neo/create-child! humans :human
                                   {:name "Cypher"}))

    ;; add relationships

    (neo/create-rel! the-one :knows trinity)
    (neo/create-rel! the-one :knows morpheus)
    (neo/create-rel! morpheus :knows trinity)
    (neo/create-rel! morpheus :knows cypher)
    (neo/set-props! (neo/create-rel! cypher :knows smith)
                    {:disclosure "secret"
                     :age 6})
    (neo/create-rel! smith :knows architect)
    (neo/create-rel! trinity :loves the-one)

### Basic traversal

Assuming I do not have any of previous references to nodes.
    
Get me all human nodes:

    (let [humans (neo/walk (neo/root) :humans)]
      (neo/traverse humans :human))
    ;; evals to:
    ;; (#<NodeProxy Node[5]> #<NodeProxy Node[6]>
    ;;  #<NodeProxy Node[7]> #<NodeProxy Node[8]>)
                 
I want to see their properties:

    (let [human-nodes (neo/traverse (neo/walk (neo/root) :humans) :human)]
      (map neo/props human-nodes))
    ;; evals to:
    ;; ({:name "Thomas Anderson", :age 29}
    ;;  {:name "Trinity", :age 27}
    ;;  {:name "Morpheus", :rank "Captain", :age 35}
    ;;  {:name "Cypher"})

I want to find Mr. Andersons node, assuming I don't have one:

    (def the-one (first (neo/traverse (neo/walk (neo/root) :humans)
                                      {:name "Thomas Anderson"}
                                      {:human :out})))
    ;; Or if I want to traverse from root
    (def the-one (first (neo/traverse (neo/root)
                                      {:name "Thomas Anderson"}
                                      {:humans :out
                                       :human :out})))

### Properties and Relationships
    
Andersons properties (this fetches all properties and may be
resource intensive if node has e.g. large binary properties):

    (neo/props the-one)
    ;; evals to:
    ;; {:name "Thomas Anderson", :age 29}

Andersons age:

    (neo/prop the-one :age)
    ;; evals to:
    ;; 29

Andersons relationships:

    (neo/rels the-one)
    ;; evals to:
    ;; (#<RelationshipProxy Relationship[4]>
    ;;  #<RelationshipProxy Relationship[8]>
    ;;  #<RelationshipProxy Relationship[9]>
    ;;  #<RelationshipProxy Relationship[14]>)

But I want to see their types:

    (map neo/rel-type (neo/rels the-one))
    ;; evals to:
    ;; (:human :knows :knows :loves)

Get :knows or :loves type relationships:

    (neo/rels the-one [:knows :loves])

Get love relationships only:

    (neo/rels the-one :loves)

Get incoming relationships only:

    (neo/rels the-one nil :in)

### Advanced Traversal

Who does Anderson know?:

    (map #(neo/prop % :name)
         (neo/traverse the-one
                       :1 nil
                       {:knows :out}))
    ;; ("Trinity" "Morpheus")

Go one level deeper:

    (map #(neo/prop % :name)
         (neo/traverse the-one
                       :2 nil
                       {:knows :out}))
    ;; ("Trinity" "Morpheus" "Cypher")

Go all the way down:

    (map #(neo/prop % :name)
         (neo/traverse the-one
                       nil nil
                       {:knows :out}))
    ;; ("Trinity" "Morpheus" "Cypher" "Agent Smith" "Architect")

Return every human who does not have his age set. Create a
custom returnable evaluator function first:

    (defn age-not-present? [pos]
      (and
       (not (:start? pos))              ; eliminate start node
       (not (neo/prop (:node pos) :age))))

Now find every human without his age set:

    (map neo/props (neo/traverse (neo/walk (neo/root) :humans)
                                 age-not-present?
                                 {:human :out}))
    ;; ({:name "Cypher"})

Return anybody between specified age range. Create
custom return evaluator:

    (defrecord AgeRangeEvaluator [from to]
      neo/ReturnableEvaluator
      (returnable-node? [this pos] (let [age (neo/prop (:node pos) :age)]
                                     (when age
                                       (and
                                        (>= age (:from this))
                                        (<= age (:to this)))))))

Traverse:

    (map neo/props (neo/traverse (neo/root)
                                 (AgeRangeEvaluator. 30 40)
                                 {:humans :out
                                  :human :out
                                  :programs :out
                                  :program :out}))
    ;; evals to:
    ;; ({:name "Agent Smith", :language "C++", :age 40}
    ;;  {:name "Morpheus", :rank "Captain", :age 35})

## Contact

You can contact Jozef Wagner through:

* [http://github.com/wagjo](http://github.com/wagjo)

* [http://www.google.com/profiles/jozef.wagner](http://www.google.com/profiles/jozef.wagner)

## License

Disclaimer: Forked from [hgavin/clojure-neo4j](http://github.com/hgavin/clojure-neo4j)

Disclaimer: Small amount of comments and docs are based on official
Neo4j javadocs. 

Copyright (C) 2011, Jozef Wagner. All rights reserved.

The use and distribution terms for this software are covered by the
Eclipse Public License 1.0 ([http://opensource.org/licenses/eclipse-1.0.php](http://opensource.org/licenses/eclipse-1.0.php))
which can be found in the file epl-v10.html at the root of this 
distribution.

By using this software in any fashion, you are agreeing to be bound by
the terms of this license.

You must not remove this notice, or any other, from this software.
