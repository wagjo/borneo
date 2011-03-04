(defproject com.wagjo/neo "0.3.0-SNAPSHOT" 
  :description "The clojure-neo4j project provides a more lispy interface to Neo4j, a graph-structured on-disk transactional database." 

  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [org.neo4j/neo4j "1.2"]]
  :dev-dependencies [[swank-clojure "1.3.0-SNAPSHOT"]]
  :jvm-opts ["-Dswank.encoding=utf-8"])
