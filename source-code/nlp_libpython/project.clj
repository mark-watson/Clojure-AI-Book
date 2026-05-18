(defproject python_interop_deeplearning "0.1.0-SNAPSHOT"
  :description "Example using libpython-clj with the Python spaCy NLP library"
  :url "https://github.com/gigasquid/libpython-clj-examples"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :jvm-opts ["-Djdk.attach.allowAttachSelf"
             "-XX:+UnlockDiagnosticVMOptions"
             "-XX:+DebugNonSafepoints"
             "-Dlibpython_clj.python_executable=.venv/bin/python"]

   :dependencies [[org.clojure/clojure "1.11.1"]
                  [clj-python/libpython-clj "2.026"]
                  [clj-http "3.10.3"]
                  [com.cemerick/url "0.1.1"]
                  [org.clojure/data.csv "1.0.0"]
                  [org.clojure/data.json "1.0.0"]]
  :main ^:skip-aot nlp-libpython-spacy.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
