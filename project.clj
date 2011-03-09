(defproject borneo "0.2.0-SNAPSHOT" 
  :description "Clojure wrapper for Neo4j, a graph database."
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.neo4j/neo4j "1.2"]]
  :dev-dependencies [[swank-clojure "1.3.0-SNAPSHOT"]
                     [lein-clojars "0.6.0"]
                     [org.clojars.rayne/autodoc "0.8.0-SNAPSHOT"]]
  :jvm-opts ["-Dswank.encoding=utf-8"]
  :autodoc {:copyright "Copyright (C) 2011, Jozef Wagner. All rights reserved."
            :web-src-dir "http://github.com/wagjo/borneo/blob/"
            :web-home "http://wagjo.github.com/borneo/"})
