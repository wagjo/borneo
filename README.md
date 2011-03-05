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
  * ___[with-db!](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/with-db!)_ - establish a connection to the database__
  * _[with-local-db!](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/with-local-db!)_ - establish a thread local connection to the database
  * ___[with-tx](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/with-tx)_ - establish a transaction__
  * _[get-db](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/get-db)_ - get current database instance
  * _[get-path](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/get-path)_ - get path to where database is stored
  * _[read-only?](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/read-only?)_ - returns true if database is read only
  * _[index](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/index)_ - returns Index Manager
* _Property Containers (both Nodes and Relationships)_
  * _[prop?](http://wagjo.github.com/borneo/borneo.core-api.html#borneo.core/prop?)_ - returns true if node or relationship contains given property
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

    (ns foo.example
      (:require [borneo.core :as neo]))

    (neo/with-neo "/path/to/db"

      TODO

    )

## Contact

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
