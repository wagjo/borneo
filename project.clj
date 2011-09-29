(defproject borneo "0.3.0" 
  :description "Clojure wrapper for Neo4j, a graph database."
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.neo4j/neo4j "1.4"]]
  :dev-dependencies [[org.clojars.rayne/autodoc "0.8.0-SNAPSHOT"]]
  :jvm-opts ["-Dswank.encoding=utf-8"]
  :autodoc {:copyright "Copyright (C) 2011, Jozef Wagner. All rights reserved."
            :web-src-dir "http://github.com/wagjo/borneo/blob/"
            :web-home "http://wagjo.github.com/borneo/"})

;;; you should install swank-clojure and lein-clojars locally:
;; lein plugin install swank-clojure 1.3.2
;; lein plugin install lein-clojars  0.7.0
