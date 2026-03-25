(defproject agentscope-gemini "1.0.0-SNAPSHOT"
  :description "AgentScope ReActAgent with Gemini model – Clojure edition"
  :url "https://github.com/mark-watson/Java-AI-Book"
  :license {:name "Apache License 2.0"
            :url  "https://www.apache.org/licenses/LICENSE-2.0"}

  ;; -------------------------------------------------------------------------
  ;; Dependencies – mirrors the Maven pom.xml
  ;; -------------------------------------------------------------------------
  :dependencies
  [[org.clojure/clojure "1.12.0"]
   ;; AgentScope all-in-one (includes DashScope SDK + core framework)
   [io.agentscope/agentscope "1.0.9"]
   ;; Google GenAI SDK for Gemini model support
   [com.google.genai/google-genai "1.44.0"]
   ;; SLF4J simple logging
   [org.slf4j/slf4j-simple "2.0.13"]]

  ;; -------------------------------------------------------------------------
  ;; Source paths
  ;; -------------------------------------------------------------------------
  :source-paths ["src"]

  ;; -------------------------------------------------------------------------
  ;; Build
  ;; -------------------------------------------------------------------------
  :aot :all   ; Ahead-of-time compile all namespaces (needed for :gen-class)

  ;; -------------------------------------------------------------------------
  ;; Entry points
  ;; -------------------------------------------------------------------------
  :main agentscope.main

  :profiles
  {:uberjar {:aot :all
             :uberjar-name "agentscope-gemini-standalone.jar"}}

  ;; -------------------------------------------------------------------------
  ;; Aliases for convenience
  ;; -------------------------------------------------------------------------
  :aliases
  {"run-main"     ["run" "-m" "agentscope.main"]
   "run-tool-use" ["run" "-m" "agentscope.tool-use"]})
