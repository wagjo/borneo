(ns borneo.t-core
  (:use midje.sweet)
  (:require [borneo.core :as db])
  (:import (org.neo4j.graphdb GraphDatabaseService)))


(facts "about Neo4j"
  (db/with-db! "/Users/steve/embedded"

    (fact "*neo-db* holds the current database instance"
      db/*neo-db* =not=> falsey)

    (fact "the database should be ready for use"
      (.isAvailable db/*neo-db* 5) => true)

    (fact "*exec-eng* holds an execution engine for Cypher queries"
      db/*exec-eng* =not=> falsey)

    (fact "Cypher queries can be executed"
        (let [result (db/cypher "RETURN 1 AS one")]
          result =not=> falsey
          (get (first result) "one") => 1))

    (fact "Cypher queries can include parameters"
      (let [result (db/cypher "RETURN {two} AS two, {three} AS three"
                               {"two" 2 "three" "three"})]
          result =not=> falsey
          (get (first result) "two") => 2
          (get (first result) "three") => "three"))

    (fact "New nodes can be created"
      (doto (db/create-node!)
        =not=> falsey))

    (fact "Nodes can be created with labels"
      (let [neo (db/create-node! "Human")]
        neo =not=> falsey
        (db/label? neo "Human") => true
        (db/label? neo "Program") => false))))
