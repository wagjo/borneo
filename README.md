The clojure-neo4j project provides a more lispy interface to Neo4j, a graph-structured on-disk transactional database.

This library is under active development and there's plenty more work to do.

Wrapper for Neo4j. Because I was not happy with the current state
(01/2011) of existing Neo4j wrappers for clojure, I've decided to
create my own.

Purpose of this ns is to provide intiutive access to commonly used
Neo4j operations. It uses official Neo4j Java bindings. It does not
use Blueprints interface.

Disclaimer: I have forked hgavin/clojure-neo4j and modified it
heavily.

Disclaimer: Some comments and docs are taken from official Neo4j javadocs.

## Usage:

* use with-db! to establish a connection to the database

* all db operations must be inside with-db! body

## Code notes:

*   *neo-db* holds the current db instance, so that users do not have
    to supply db instance at each call to db operations. This
    approach has of course its drawbacks (e.g. only one connection at
    time), but I've found it suitable for my purposes.

Examples:

(ns foo.example
  (:require [neo4j.core :as neo]))

(neo/with-neo "/path/to/db"



)
