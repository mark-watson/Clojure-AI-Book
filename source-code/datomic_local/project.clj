(defproject datomic-local "0.1.0-SNAPSHOT"
  :description "Datomic Local wrapper and examples — Practical Artificial Intelligence Programming With Clojure"
  :url "https://leanpub.com/clojureai"
  :license {:name "Eclipse Public License 2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [com.datomic/local "1.0.291"]]
  :repl-options {:init-ns datomic-local.core}
  :main datomic-local.core)
