(defproject vectordb_semantic_search "0.1.0-SNAPSHOT"
  :description "Semantic search example using libpython-clj and ChromaDB"
  :url "https://github.com/mark-watson/Clojure-AI-Book"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :jvm-opts ["-Djdk.attach.allowAttachSelf"
             "-XX:+UnlockDiagnosticVMOptions"
             "-XX:+DebugNonSafepoints"
             "-Dlibpython_clj.python_executable=.venv/bin/python"]
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [clj-python/libpython-clj "2.026"]]
  :main ^:skip-aot vectordb-semantic-search-python.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
