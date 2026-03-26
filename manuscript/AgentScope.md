# AgentScope Agent Oriented Framework.

AgentScope is an agent oriented programming framework for building LLM powered applications that has components for ReAct reasoning, tool calling, memory management, and multi agent collaboration.

Here we only write Clojure examples using a subset of the Java implementation of AgentScope. For reference this is the [home web page for Agentscope](https://agentscope.io).

We develop two parallel implementations of  simple text generation and tool use examples in this chapter:

- Using the local model `nemotron-3-nano:4b` running with a local Ollama server. Source code: **Clojure-AI-Book/source-code/AgentScope_ollama**.
- Using the Google Gemini model `gemini-3-flash-preview`. Source code: **Clojure-AI-Book/source-code/AgentScope_gemini**.

These implementations are similar and could have been generalized into a single code base. To reduce the lines of code listings here, we will first look at the Gemini implementation of a simple text generation example and look at the local Ollama implementation of a multiple tool use example. You can read through the Gemini tool use and the Ollama simple generative text examples in the GitHub repository.

In case his is confusing, here are the parallel files:

```
source-code $ tree AgentScope_gemini/src AgentScope_ollama/src
AgentScope_gemini/src
└── agentscope
    ├── main.clj
    └── tool_use.clj
AgentScope_ollama/src
└── agentscope
    ├── main.clj
    └── tool_use.clj

4 directories, 4 files
```

## Overview of AgentScope

TBD

## Generating Completions With AgentScope: Gemini Example

TBD

Here is a listing of **Clojure-AI-Book/source-code/AgentScope_gemini/src/agentscope/main.clj**:

```clojure
(ns agentscope.main
  "AgentScope ReActAgent demo using Google Gemini (gemini-2.5-flash).

   Set the environment variable GEMINI_API_KEY before running:
     export GEMINI_API_KEY=your_key_here
     lein run"
  (:import [io.agentscope.core ReActAgent]
           [io.agentscope.core.message Msg]
           [io.agentscope.core.model GeminiChatModel])
  (:gen-class))

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

          ;; Build the ReActAgent
          agent (-> (ReActAgent/builder)
                    (.name "Assistant")
                    (.sysPrompt "You are a helpful AI assistant.")
                    (.model model)
                    (.build))

          ;; Send a message and block for the response
          response (-> (.call agent
                              (-> (Msg/builder)
                                  (.textContent "Hello! Tell me a fun fact about Java programming.")
                                  (.build)))
                       (.block))]

      (println "Agent response:")
      (println (.getTextContent response)))))
```

Sample output looks like:

```
$ make hello
lein run
Compiling agentscope.main
Compiling agentscope.tool-use
Mar 26, 2026 1:24:04 PM com.google.genai.ApiClient getApiKeyFromEnv
WARNING: Both GOOGLE_API_KEY and GEMINI_API_KEY are set. Using GOOGLE_API_KEY.
Agent response:
Hello there!

Here's a fun fact about Java:

The programming language wasn't originally named Java! It was initially called **Oak**, after an oak tree outside James Gosling's office. However, due to a trademark conflict, they had to change it.

The team then renamed it **Java**, inspired by Java coffee, which was a favorite beverage of the developers (and the name of the Indonesian island where the coffee originates). This is why the Java logo is a steaming cup of coffee! ☕
```



## Multiple Tool Use with AgentScope: Ollama Example


Here is a listing of **Clojure-AI-Book/source-code/AgentScope_ollama/src/agentscope/tool_use.clj**:

```clojure
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

   Ensure Ollama is running locally on http://localhost:11434 before running:
     lein run -m agentscope.tool-use"
  (:import [io.agentscope.core ReActAgent]
           [io.agentscope.core.message Msg ToolResultBlock]
           [io.agentscope.core.model OllamaChatModel]
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
  ;; Build the Ollama chat model
  (let [model (-> (OllamaChatModel/builder)
                  (.modelName "nemotron-3-nano:4b")
                  (.baseUrl "http://localhost:11434")
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
      (println (.getTextContent math-response))))
```



Sample output looks like:

```
$ make run-tools
lein run -m agentscope.tool-use
Compiling agentscope.tool-use
[main] INFO io.agentscope.core.tool.Toolkit - Registered tool 'getWeather' in group 'ungrouped'
[main] INFO io.agentscope.core.tool.Toolkit - Registered tool 'list_dir' in group 'ungrouped'
[main] INFO io.agentscope.core.tool.Toolkit - Registered tool 'read_file' in group 'ungrouped'
[main] INFO io.agentscope.core.tool.Toolkit - Registered tool 'recursive-file-search' in group 'ungrouped'
[main] INFO io.agentscope.core.tool.Toolkit - Registered tool 'math-eval' in group 'ungrouped'
=== Weather Query ===
The weather in Tokyo is sunny with a temperature of 25°C. The weather in Paris is also sunny with a temperature of 25°C.

=== Markdown Files Query ===
I found 1 markdown file in the current directory: **README.md**

Here are the first 5 lines of README.md:

```
# AgentScope + Gemini — Clojure Edition
This directory contains Clojure examples for using the **AgentScope SDK** directly via Java interop.
> See [`README.md`](README.md) for background on AgentScope and the Gemini model.
```

Other files and directories in the current directory are: `.DS_Store`, `Makefile`, `project.clj`, `src/`, and `target/`.

=== File Search Query (.clj files) ===
I found a total of **3 .clj files** in the current directory and its subdirectories:

1. `./project.clj`
2. `./src/agentscope/main.clj`
3. `./src/agentscope/tool_use.clj`

These appear to be Clojure configuration, project management, and module file extensions.

=== Math Eval Query ===
The value of `15 + 27 * 3` is **126**. (Multiplication takes precedence over addition.)
[HttpTransportFactory-ShutdownHook] INFO io.agentscope.core.model.transport.HttpTransportFactory - Shutting down 1 managed HttpTransport(s)
```
