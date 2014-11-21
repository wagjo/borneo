(ns borneo.t-core
  (:use midje.sweet)
  (:require [borneo.core :as db])
  (:import (org.neo4j.graphdb GraphDatabaseService)))

(defn- db-path []
  (.getPath (java.io.File. (System/getProperty "java.io.tmpdir") "borneo")))

(facts "about Neo4j"
  (db/with-db! :test-db (db-path)

    (db/with-tx :test-db
      (db/purge!) ; purge the database for test consistency.

    (fact "(-> *neo-db* :test-db :db) holds the current database instance"
          (-> db/*neo-db* :test-db :db) =not=> falsey)

    (fact "the database should be ready for use"
      (.isAvailable (-> db/*neo-db* :test-db :db) 5) => true)

    (fact "(-> db/*neo-db* :test-db :exec-eng) holds an execution engine for Cypher queries"
      (-> db/*neo-db* :test-db :exec-eng) =not=> falsey)

    (fact "Cypher queries can be executed"
          (let [result (db/cypher :test-db "RETURN 1 AS one")]
          result =not=> falsey
          (get (first result) "one") => 1))

    (fact "Cypher queries can include parameters"
      (let [result (db/cypher :test-db "RETURN {two} AS two, {three} AS three"
                               {"two" 2 "three" "three"})]
          result =not=> falsey
          (get (first result) "two") => 2
          (get (first result) "three") => "three"))

    (fact "New nodes can be created"
      (doto (db/create-node! :test-db)
        =not=> falsey))

    (fact "Nodes can be created with labels"
      (let [neo (db/create-labeled-node! :test-db "Human" "TheOne")]
        neo =not=> falsey
        (db/label? neo "Human") => true
        (db/label? neo "TheOne") => true
        (db/label? neo "Program") => false

        (fact "Nodes can be assigned properties"
          (db/set-props! :test-db neo {:id 1 :name "Neo"})
          (db/with-tx :test-db
            (db/prop? neo :id) => true
            (db/prop? neo :name) => true
            (db/prop neo :id) => 1
            (db/prop neo :name) => "Neo"))

        (fact "Labels of a node can be listed"
          (sort (db/labels neo)) => ["Human" "TheOne"])

        (fact "Labels can be removed from a node"
          (db/remove-label! neo "TheOne")
          (db/label? neo "TheOne") => false
          (db/labels neo) => ["Human"])

        (fact "Labes can be added to a node"
          (db/add-label! neo "TheOne")
          (db/label? neo "TheOne") => true
          (sort (db/labels neo)) => ["Human" "TheOne"])))


    (fact "Nodes can be created with properties and labels"
      (let [morpheus (db/create-node! :test-db {:id 2 :name "Morpheus"} "Human")]
        morpheus =not=> falsey
        (db/label? morpheus "Human") => true
        (db/prop morpheus :id) => 2
        (db/prop morpheus :name) => "Morpheus"))


    (fact "All nodes of a label can be found"
      (db/with-tx :test-db
        (count (db/all-nodes-with-label :test-db "Human")) => 2))

    (fact "Nodes can be found by label and properties"
      (db/with-tx :test-db
        (let [results (db/find-nodes :test-db "Human" :id 1)]
          (count results) => 1
          (db/props (first results)) => {:id 1 :name "Neo"})))


    (fact "Relationships can be formed between nodes"
          (db/with-tx :test-db
            (let [results (db/all-nodes-with-label :test-db "Human")
                  neo (first results)
                  morpheus (second results)]
              (db/prop neo :name) => "Neo"
              (db/prop morpheus :name) => "Morpheus"
              (let [rel (db/create-rel! neo :KNOWS morpheus)]
                rel =not=> falsey
                (db/start-node rel) => neo
                (db/end-node rel) => morpheus)
              ))))))
