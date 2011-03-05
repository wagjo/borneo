# borneo

Wrapper for Neo4j, a graph database.

Because I was not happy with the current state (01/2011) of existing
Neo4j wrappers for clojure, I've decided to create my own.

Purpose of this library is to provide intiutive access to commonly used
Neo4j operations. It uses official Neo4j Java bindings. It does not
use Blueprints interface.

## Usage

Add the following dependency to your project.clj file:

    [borneo "0.1.0-SNAPSHOT"]

## Documentation

* API docs are at http://wagjo.github.com/borneo/

* use with-db! to establish a connection to the database

* all db operations must be inside with-db! body

* TODO

## Examples

    (ns foo.example
      (:require [borneo.core :as neo]))

    (neo/with-neo "/path/to/db"

      TODO

    )

## Contact

* http://github.com/wagjo

* http://www.google.com/profiles/jozef.wagner

## License

Disclaimer: Forked from hgavin/clojure-neo4j

Disclaimer: Small amount of comments and docs are based on official
Neo4j javadocs. 

Copyright (C) 2011, Jozef Wagner. All rights reserved.

The use and distribution terms for this software are covered by the
Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
which can be found in the file epl-v10.html at the root of this 
distribution.

By using this software in any fashion, you are agreeing to be bound by
the terms of this license.

You must not remove this notice, or any other, from this software.
