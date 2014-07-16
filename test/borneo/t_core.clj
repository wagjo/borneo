(ns borneo.t-core
  (:use midje.sweet)
  (:require [borneo.core :as neo])
  (:import (org.neo4j.graphdb GraphDatabaseService)))

(facts "about Neo4j"
  (neo/with-db! "/Users/steve/embedded"

    (fact "*neo-db* holds the current database instance"
      neo/*neo-db* =not=> falsey)

    (fact "the database should be ready for use"
      (.isAvailable neo/*neo-db* 5) => true)

    (fact "*exec-eng* holds an execution engine for Cypher queries"
      neo/*exec-eng* =not=> falsey)

    (fact "Cypher queries can be executed"
        (let [result (neo/cypher "RETURN 1 AS one")]
          result =not=> falsey
          (get (first result) "one") => 1))

  ))
