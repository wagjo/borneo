# borneo

Clojure wrapper for Neo4j, a graph database.

Purpose of this library is to provide intiutive access to commonly used
Neo4j operations. It uses official Neo4j Java bindings. It does not
use Blueprints interface.

## Rationale

I've decided to create my own Neo4j wrapper, because I was not happy
with the current state _(01/2011)_ of existing ones. While I initially
forked from late 
[hgavin/clojure-neo4j](http://github.com/hgavin/clojure-neo4j), I
quickly realized that drastic changes will be needed. This chapter
summarizes my motivation behind changes and decisions I made.

I believe that adapters are a sub-optimal solution (see
[Chris Housers talk on the Expression
Problem](http://www.infoq.com/presentations/Clojure-Expression-Problem)). Problem
with adapters is that they are no longer original types so you will 
not be able to use existing functions which accept original type. That
is why borneo works mainly with Neo4j classes _(Node,
Relationship)_, and does not automatically convert nodes to property
maps. 

It is tedious to provide a connection for every operation on
database. That is why I chose to have a dedicated Var for storing
current connection. That of course brings several problems to the
scene. Sometimes you want to have a connection which is shared between
threads and sometimes you want to have parallel connections to
multiple databases. _(Embedded Neo4j does not allow for parallel
connections to single database)_. Both cases are supported in
borneo. You can use
[with-db!](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/with-db!)
for a connection accessible from every thread, and by using
[with-local-db!](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/with-local-db!),
you can have a thread local connection. One drawback is that you
cannot have both at one moment, so be careful.

Because I like simple things _(see [Stuart Halloways talk on
simplicity](http://clojure.blip.tv/file/4824610/))_, I tried to
provide simple functions. That is why properties handling function are
divided into two separate ones, one for reading and one for
mutating. I also provide some "compound" functions, like
[props](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/props), 
[create-child!](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/create-child!)
or
[delete-node!](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/delete-node!),
but they are here only for convenience and their simple counterparts
are also provided. 

While mentioned in previous section, I'd like to stress that
separation of mutable and immutable world is very important in
Clojure. That is why all mutable functions in borneo are clearly
separated from their read-only parts and cannot be used in
Clojure transactions.

I have added support for custom Returnable and Stop evaluators through
protocols. I think it will allow for greater flexibility _(see last
example at the bottom of this page)_.

Another thing I wanted very much in a Neo4j wrapper was to use
keywords instead of custom static types/enums, to feel more like you
are in Clojure and not in Java... It turned out to be fairly easy to
implement.

All mutable operations are automatically wrapped in transactions _(read
only operations don't need transactions in recent Neo4j)_. By the way
Neo4j handles transactions, it should be pretty cheap to have nested
transactions so you can use
[with-tx](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/with-tx)
to group mutable operations into one big transactions if you need
it. Needs some field testing to prove this design decision though.

If you get properties for a node with
[props](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/props)
function, you fetch all properties at once. This may be very resource
intensive, when you have large binary data stored in nodes
properties. One big wish I had is to have some king of lazy
PersistentMap, where value would be fetched on demand. I've thougt of
using delay/lazy-seq on values to achieve that, 
but user would have to manually deref the value, which is not very
intuitive and does not look good. This data structure could also allow
for even less intrusive interface so you could work with data stored
in Neo4j more like working with traditional Clojure map, without
serious performance impact. I didn't have time to seriously think
about this approach yet. More hammock time needed.

## Usage

Add the following dependency to your project.clj file:

    [borneo "0.5.0"]

## Documentation

Detailed API docs are at [http://wagjo.github.com/borneo/](http://wagjo.github.com/borneo/)

Quick overview of available functions (most important ones are
emphasized):

* _Database management_
  * _[\*neo-db\*](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/*neo-db*)_ holds current database instance
  * _[\*exec-eng\*](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/*exec-eng*)_ holds instance of execution engine
  * _[start!](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/start!)_ establishes a connection to the database
  * _[stop!](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/stop!)_ closes a connection stored in \*neo-db\*
  * ___[with-db!](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/with-db!)_ establishes a connection to the database__
  * _[with-local-db!](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/with-local-db!)_ establishes a thread local connection to the database
  * ___[with-tx](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/with-tx)_ establishes a transaction__
  * _[get-path](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/get-path)_ gets path to where database is stored
  * _[read-only?](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/read-only?)_ returns true if database is read only
  * _[index](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/index)_ returns Index Manager
  * _[purge!](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/purge!)_ deletes all nodes and relationships
* _Property Containers (both Nodes and Relationships)_
  * _[prop?](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/prop?)_ returns true if node or relationship contains given property
  * _[prop](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/prop)_ returns specific property value for a given node or relationship
  * ___[props](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/props)_ returns map of properties for a given node or relationship__
  * _[set-prop!](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/set-prop!)_ sets or removes property in a given node or relationship
  * ___[set-props!](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/set-props!)_ sets (or removes) properties for a given node or relationships__
  * _[get-id](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/get-id)_ returns id of a given node or relationship
  * ___[delete!](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/delete!)_ deletes relationship or "free" node__
* _Relationships_
  * ___[rel-nodes](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/rel-nodes)_ returns the two nodes attached to the given relationship__
  * _[start-node](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/start-node)_ returns start node for given relationsip
  * _[end-node](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/end-node)_ returns end node for given relationsip
  * _[other-node](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/other-node)_ returns other node for given relationsip
  * ___[rel-type](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/rel-type)_ returns type of given relationship__
  * ___[create-rel!](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/create-rel!)_ creates relationship between two nodes__
  * _[all-rel-types](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/all-rel-types)_ returns lazy seq of all relationship types in database
* _Labels_
  * _[dynamic-label](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/dynamic-label)_ creates a label with the supplied name
  * _[label?](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/label?)_ returns true if the given node has a label with the supplied name
  * _[add-label!](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/addlabel!)_ adds the given label to the node
  * _[remove-label!](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/remove-label!)_ removes the supplied label from the node
  * _[labels](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/labels)_ lists all labels attached to this node
* _Nodes_
  * _[rel?](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/rel?)_ returns true if node has given relationship(s)
  * ___[rels](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/rels)_ returns relationships attached to given node__
  * _[single-rel](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/single-rel)_ returns single relationship for given node
  * _[create-labeled-node!](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/create-labeled-node!)_ creates new labeled node, not linked with any other nodes
  * _[create-node!](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/create-node!)_ creates new node, not linked with any other nodes. Labels may be provided as an optional arguments.
  * ___[create-child!](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/create-child!)_ creates a child node of a given parent__
  * ___[delete-node!](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/delete-node!)_ deletes node and all its relationships__
  * _[find-nodes](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/find-nodes)_ finds nodes with the supplied label and predicate.
* _Graph traversal protocols_
  * _[ReturnableEvaluator](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/ReturnableEvaluator)_ protocol for return evaluation. Used for graph traversing.
  * _[StopEvaluator](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/StopEvaluator)_ protocol for stop evaluation. Used for graph traversing.
* _Graph traversal_
  * _[all-nodes](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/all-nodes)_ returns lazy-seq of all nodes in database
  * _[all-nodes-with-label](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/all-nodes-with-label)_ returns lazy-seq of all nodes with the given label
  * _[node-by-id](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/node-by-id)_ returns node with a given id
  * _[rel-by-id](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/rel-by-id)_ returns relationship with a given id
  * ___[walk](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/walk)_ walks though the graph by following through given single relations__
  * ___[traverse](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/traverse)_ traverses the graph__
* _Cypher queries_
  * _[cypher](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/cypher)_ returns lazy-seq of results from a Cypher query

## Examples

Following examples show basic borneo functions. Code presented here is
not meant to be an idiomatic clojure code, e.g. you should wrap most
of your operations in separate functions, and use let instead of def
to store a reference to a node.

NOTE: Following examples are out of date (for version 0.4). Pull request for up to date example is welcomed.

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

Assuming I do not have any previous references to nodes.
    
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

Want to find Mr. Andersons node, assuming I don't have one:

    (def the-one (first (neo/traverse (neo/walk (neo/root) :humans)
                                      {:name "Thomas Anderson"}
                                      :human)))
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
         (neo/traverse the-one :1 nil :knows))
    ;; ("Trinity" "Morpheus")

Go one level deeper:

    (map #(neo/prop % :name)
         (neo/traverse the-one :2 nil :knows))
    ;; ("Trinity" "Morpheus" "Cypher")

Go all the way down:

    (map #(neo/prop % :name)
         (neo/traverse the-one nil nil :knows))
    ;; ("Trinity" "Morpheus" "Cypher" "Agent Smith" "Architect")

Return every human who does not have his age set. Create a
custom returnable evaluator function first:

    (defn age-not-present? [pos]
      (and
       (not (:start? pos))              ; eliminate start node
       (not (neo/prop (:node pos) :age))))

Now find every human without his age set:

    (map neo/props (neo/traverse (neo/walk (neo/root) :humans)
                                 age-not-present? :human))
    ;; ({:name "Cypher"})

Return anybody between specified age range. Create
custom return evaluator:

    (deftype AgeRangeEvaluator [from to]
      neo/ReturnableEvaluator
      (returnable-node? [this pos] (let [age (neo/prop (:node pos) :age)]
                                     (when age
                                       (and
                                        (>= age from)
                                        (<= age to))))))

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

Disclaimer: Forked from
[hgavin/clojure-neo4j](http://github.com/hgavin/clojure-neo4j) (no
longer available)

Disclaimer: Small amount of comments and docs are based on official
Neo4j javadocs. 

Copyright (C) 2011, 2014, Jozef Wagner and contributors. All rights reserved.

The use and distribution terms for this software are covered by the
Eclipse Public License 1.0 ([http://opensource.org/licenses/eclipse-1.0.php](http://opensource.org/licenses/eclipse-1.0.php))
which can be found in the file epl-v10.html at the root of this 
distribution.

By using this software in any fashion, you are agreeing to be bound by
the terms of this license.

You must not remove this notice, or any other, from this software.
