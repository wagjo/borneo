(defproject borneo "0.1.0-SNAPSHOT" 
  :description "The borneo project provides a more lispy interface to Neo4j, a graph-structured on-disk transactional database." 
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.neo4j/neo4j "1.2"]]
  :dev-dependencies [[swank-clojure "1.3.0-SNAPSHOT"]
                     [lein-clojars "0.6.0"]]
  :jvm-opts ["-Dswank.encoding=utf-8"])
