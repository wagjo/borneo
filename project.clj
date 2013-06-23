(defproject borneo "0.4.0" 
  :description "Clojure wrapper for Neo4j, a graph database."
  :url "https://github.com/wagjo/borneo"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.neo4j/neo4j "1.9"]]
  :dev-dependencies [[org.clojars.rayne/autodoc "0.8.0-SNAPSHOT"]]
  :jvm-opts ["-Dswank.encoding=utf-8"]
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :autodoc {:copyright "Copyright (C) 2011, Jozef Wagner. All rights reserved."
            :web-src-dir "http://github.com/wagjo/borneo/blob/"
            :web-home "http://wagjo.github.com/borneo/"})

;;; you should install swank-clojure and lein-clojars locally:
;; lein plugin install swank-clojure 1.3.2
;; lein plugin install lein-clojars  0.7.0
