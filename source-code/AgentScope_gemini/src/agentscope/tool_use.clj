(ns agentscope.tool-use
  "Demonstrates AgentScope tool use with five tools:
     - getWeather  – stub weather lookup
     - list_dir    – list files in a directory
     - read_file   – read a file's contents (with optional line limit)
     - recursive-file-search – recursively search for files matching a string
     - math-eval   – evaluate arithmetic expressions (+, *, /, integers)

   Tools are defined entirely in Clojure by implementing the AgentTool
   interface with reify.  No companion Java class is needed.

   AgentTool requires four methods:
     getName        – the function name the LLM will call
     getDescription – natural-language description for the LLM
     getParameters  – JSON-schema map describing the input parameters
     callAsync      – executes the tool, returns Mono<ToolResultBlock>

   Set the environment variable GEMINI_API_KEY before running:
     export GEMINI_API_KEY=your_key_here
     lein run -m agentscope.tool-use"
  (:import [io.agentscope.core ReActAgent]
           [io.agentscope.core.message Msg ToolResultBlock]
           [io.agentscope.core.model GeminiChatModel]
           [io.agentscope.core.tool AgentTool Toolkit]
           [reactor.core.publisher Mono]
           [java.io File])
  (:gen-class))

(defn- weather-tool
  "Returns an AgentTool implementation for a stub weather lookup."
  []
  (reify AgentTool
    (getName [_] "getWeather")
    (getDescription [_] "Get the current weather for a specified city")
    (getParameters [_]
      {"type"       "object"
       "properties" {"city" {"type"        "string"
                             "description" "The name of the city"}}
       "required"   ["city"]})
    (callAsync [_ param]
      (let [city (get (.getInput param) "city")]
        (Mono/just (ToolResultBlock/text (str city " weather: Sunny, 25°C")))))))

(defn- list-dir-tool
  "Returns an AgentTool that lists entries in a directory."
  []
  (reify AgentTool
    (getName [_] "list_dir")
    (getDescription [_] "List files and subdirectories at the given path. Defaults to the current working directory when no path is supplied.")
    (getParameters [_]
      {"type"       "object"
       "properties" {"path" {"type"        "string"
                             "description" "Directory path to list (defaults to current directory)"}}
       "required"   []})
    (callAsync [_ param]
      (let [path  (or (get (.getInput param) "path") ".")
            dir   (File. path)
            names (if (.isDirectory dir)
                    (->> (.listFiles dir)
                         (sort-by #(.getName %))
                         (map #(if (.isDirectory %) (str (.getName %) "/") (.getName %)))
                         (clojure.string/join "\n"))
                    (str "Error: not a directory: " path))]
        (Mono/just (ToolResultBlock/text names))))))

(defn- read-file-tool
  "Returns an AgentTool that reads a file, optionally limiting output to N lines."
  []
  (reify AgentTool
    (getName [_] "read_file")
    (getDescription [_] "Read the contents of a file. Optionally restrict output to the first max_lines lines.")
    (getParameters [_]
      {"type"       "object"
       "properties" {"path"      {"type"        "string"
                                  "description" "Path to the file to read"}
                     "max_lines" {"type"        "integer"
                                  "description" "Maximum number of lines to return (optional, returns all lines when omitted)"}}
       "required"   ["path"]})
    (callAsync [_ param]
      (let [input     (.getInput param)
            path      (get input "path")
            max-lines (get input "max_lines")
            content   (try
                        (let [lines (clojure.string/split-lines (slurp path))]
                          (clojure.string/join "\n" (if max-lines (take max-lines lines) lines)))
                        (catch Exception e
                          (str "Error reading file: " (.getMessage e))))]
        (Mono/just (ToolResultBlock/text content))))))

(defn- recursive-file-search-tool
  "Returns an AgentTool that recursively searches for files matching a search string."
  []
  (reify AgentTool
    (getName [_] "recursive-file-search")
    (getDescription [_] "Recursively search for files whose names contain the search string. Start search from the current working directory by default, or from an optional start_path.")
    (getParameters [_]
      {"type"       "object"
       "properties" {"search_string" {"type"        "string"
                                       "description" "String to search for in file names"}
                     "start_path"    {"type"        "string"
                                       "description" "Directory path to start the search from (defaults to current directory)"}}
       "required"   ["search_string"]})
    (callAsync [_ param]
      (let [input      (.getInput param)
            search-str (get input "search_string")
            start-path (or (get input "start_path") ".")
            matches    (fn matches [dir]
                         (when (.isDirectory dir)
                           (->> (.listFiles dir)
                                (mapcat (fn [f]
                                          (if (.isDirectory f)
                                            (matches f)
                                            (when (.contains (.getName f) search-str)
                                              [(.getPath f)])))))))]
        (try
          (let [result (matches (File. start-path))]
            (Mono/just (ToolResultBlock/text (if (empty? result)
                                               (str "No files matching '" search-str "' found.")
                                               (clojure.string/join "\n" result)))))
          (catch Exception e
            (Mono/just (ToolResultBlock/text (str "Error searching: " (.getMessage e))))))))))

(defn- math-eval-tool
  "Returns an AgentTool that evaluates a simple arithmetic expression."
  []
  (reify AgentTool
    (getName [_] "math-eval")
    (getDescription [_] "Evaluate a simple arithmetic expression consisting of integers and operators +, *, /. Example: \"2 + 3 * 4\"")
    (getParameters [_]
      {"type"       "object"
       "properties" {"expression" {"type"        "string"
                                    "description" "Arithmetic expression with integers and operators +, *, /"}}
       "required"   ["expression"]})
    (callAsync [_ param]
      (let [expr (get (.getInput param) "expression")
            result (try
                     (let [;; Tokenize: extract numbers and operators
                           tokens (re-seq #"\d+|[+*/]" expr)
                           ;; Validate: ensure only valid tokens
                           valid? (every? #(or (re-matches #"\d+" %) (re-matches #"[+*/]" %)) tokens)]
                       (if-not valid?
                         (str "Error: invalid expression '" expr "'")
                         (let [;; Parse and evaluate left-to-right (same precedence as clojure core math)
                               eval-expr (fn eval-expr [tokens]
                                           (loop [tok tokens
                                                  result (Long/parseLong (first tokens))
                                                  remaining (rest tokens)]
                                             (if (empty? remaining)
                                               result
                                               (let [op (first remaining)
                                                     next-val (Long/parseLong (second remaining))
                                                     new-result (case op
                                                                   "+" (+ result next-val)
                                                                   "*" (* result next-val)
                                                                   "/" (quot result next-val))]
                                                 (recur (drop 2 remaining) new-result (drop 2 remaining))))))]
                           (str (eval-expr tokens)))))
                     (catch Exception e
                       (str "Error evaluating expression: " (.getMessage e))))]
        (Mono/just (ToolResultBlock/text result))))))

(defn -main [& _args]
  (let [api-key (System/getenv "GEMINI_API_KEY")]
    (when (or (nil? api-key) (clojure.string/blank? api-key))
      (binding [*out* *err*]
        (println "ERROR: GEMINI_API_KEY environment variable is not set."))
      (System/exit 1))

    ;; Build the Gemini chat model
    (let [model (-> (GeminiChatModel/builder)
                    (.apiKey api-key)
                    (.modelName "gemini-2.5-flash")
                    (.build))

          ;; Register all five tools via the AgentTool interface —
          ;; no @Tool / @ToolParam annotations required.
          toolkit (doto (Toolkit.)
                    (.registerAgentTool (weather-tool))
                    (.registerAgentTool (list-dir-tool))
                    (.registerAgentTool (read-file-tool))
                    (.registerAgentTool (recursive-file-search-tool))
                    (.registerAgentTool (math-eval-tool)))

          ;; Build the ReActAgent with the toolkit attached
          agent (-> (ReActAgent/builder)
                    (.name "AssistantAgent")
                    (.sysPrompt "You are a helpful assistant with tools to look up weather, list directory contents, read files, search for files by name, and evaluate math expressions.")
                    (.model model)
                    (.toolkit toolkit)
                    (.build))

          ;; Example 1: weather – agent calls getWeather for each city
          weather-response (-> (.call agent
                                      (-> (Msg/builder)
                                          (.textContent "What is the weather like in Tokyo and Paris?")
                                          (.build)))
                               (.block))

          ;; Example 2: list the current directory, then show the first 5 lines
          ;;            of every .md file found there
          files-response   (-> (.call agent
                                      (-> (Msg/builder)
                                          (.textContent "List files in the current directory and for each .md markdown file, show me the first 5 lines.")
                                          (.build)))
                               (.block))

          ;; Example 3: recursive file search – find all .clj files
          search-response  (-> (.call agent
                                      (-> (Msg/builder)
                                          (.textContent "Search for all .clj files in the current directory and its subdirectories.")
                                          (.build)))
                               (.block))

          ;; Example 4: math evaluation
          math-response    (-> (.call agent
                                      (-> (Msg/builder)
                                          (.textContent "Calculate the value of:  15 + 27 * 3")
                                          (.build)))
                               (.block))]

      (println "=== Weather Query ===")
      (println (.getTextContent weather-response))
      (println)
      (println "=== Markdown Files Query ===")
      (println (.getTextContent files-response))
      (println)
      (println "=== File Search Query (.clj files) ===")
      (println (.getTextContent search-response))
      (println)
      (println "=== Math Eval Query ===")
      (println (.getTextContent math-response)))))
