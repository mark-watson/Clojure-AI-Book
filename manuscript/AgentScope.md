# AgentScope Agent Oriented Framework.

AgentScope is an agent oriented programming framework for building LLM powered applications that has components for ReAct reasoning, tool calling, memory management, and multi agent collaboration.

Here we only write Clojure examples using a subset of the Java implementation of AgentScope. For reference this is the [home web page for Agentscope](https://agentscope.io).

We develop two parallel implementations of simple text generation and tool use examples in this chapter:

- Using the local model `nemotron-3-nano:4b` running with a local Ollama server. Source code: **Clojure-AI-Book/source-code/AgentScope_ollama**.
- Using the Google Gemini model `gemini-3-flash-preview`. Source code: **Clojure-AI-Book/source-code/AgentScope_gemini**.

These implementations are similar and could have been generalized into a single code base. To reduce the lines of code listings here, we will first look at the Gemini implementation of a simple text generation example and look at the local Ollama implementation of a multiple tool use example. You can read through the Gemini tool use and the Ollama simple generative text examples in the GitHub repository.

In case this is confusing, here are the parallel files:

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

AgentScope is a developer friendly and production ready framework for building LLM powered agent applications. While the original AgentScope SDK is written in Python, it also provides a Java implementation that we can call directly from Clojure via Java interop. The key abstractions in AgentScope are:

- **ReActAgent** — an agent that implements the ReAct (Reason + Act) loop. Given a user message, the agent reasons about what to do, optionally calls tools, observes the results, and continues reasoning until it can produce a final answer. This is the core agent type we use in both examples.

- **Model** — a pluggable chat model interface. AgentScope ships with built-in model implementations including the two we use here: `GeminiChatModel` (for Google Gemini) and `OllamaChatModel` (for any model served by a local Ollama instance). You construct a model using its builder, then hand it to an agent.

- **Msg** — the message abstraction. You build a `Msg` with a text content (and optionally images or other modalities), pass it to the agent via `.call()`, and receive a response `Msg` back. The `.block()` call unwraps the reactive (Project Reactor `Mono`) return value into a synchronous result.

- **AgentTool and Toolkit** — the tool-use subsystem. Each tool implements the `AgentTool` interface with four methods: `getName`, `getDescription`, `getParameters` (a JSON-schema map), and `callAsync` (which returns a Reactor `Mono<ToolResultBlock>`). Tools are registered into a `Toolkit`, which is then attached to a `ReActAgent`. When the LLM decides it needs a tool, the agent framework handles the function-call lifecycle automatically.

The beauty of this design is that tool definitions live entirely in Clojure — we use `reify` to implement the `AgentTool` interface inline, with no companion Java classes or annotation processing required. The LLM sees the tool names, descriptions, and parameter schemas; the agent runtime dispatches to our Clojure functions when the LLM requests a tool call.

For more information on AgentScope, see the [AgentScope documentation](https://doc.agentscope.io/) and the [AgentScope GitHub repository](https://github.com/agentscope-ai/agentscope).

## Generating Completions With AgentScope: Gemini Example

Our first example demonstrates the simplest use case: creating a `GeminiChatModel`, wrapping it in a `ReActAgent`, and sending a single prompt. This is the "Hello World" of AgentScope — no tools, just a straight question-and-response cycle.

The flow is straightforward:

1. Read the `GEMINI_API_KEY` from the environment and exit with an error if it is missing.
2. Build a `GeminiChatModel` using its builder, specifying the API key and the model name `gemini-2.5-flash`.
3. Build a `ReActAgent` with a name, a system prompt, and the model.
4. Construct a `Msg` with the user's text content, call `.call()` on the agent, and wait for the result with `.block()`.
5. Print the text content of the response `Msg`.

You will need a Google Gemini API key, which you can obtain from [Google AI Studio](https://aistudio.google.com/app/apikey). Set it as an environment variable before running:

    export GEMINI_API_KEY=your-key-here

The project depends on three Leiningen artifacts:

| Artifact | Version | Purpose |
|----------|---------|---------|
| `io.agentscope/agentscope` | 1.0.9 | AgentScope core (agents, messaging, tools) |
| `com.google.genai/google-genai` | 1.44.0 | Google GenAI SDK (Gemini models) |
| `org.slf4j/slf4j-simple` | 2.0.13 | Logging |

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
                                  (.textContent
								   "Hello. Fun fact about Java programming.")
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

Our second example is far more interesting: we give the agent five tools and let it decide which ones to call based on the user's question. This example uses the `OllamaChatModel` with the small local model `nemotron-3-nano:4b`, so no API key is needed — just a locally running Ollama server on `http://localhost:11434`.

Before running the example, start Ollama after pulling the model:

    ollama pull nemotron-3-nano:4b
    ollama serve
 
The project dependencies are simpler than the Gemini version since we do not need the Google GenAI SDK:

| Artifact | Version | Purpose |
|----------|---------|---------|
| `io.agentscope/agentscope` | 1.0.9 | AgentScope core (agents, messaging, tools) |
| `org.slf4j/slf4j-simple` | 2.0.13 | Logging |

We define five tools, each implemented as a Clojure function that returns a `reify` of the `AgentTool` interface:

1. **getWeather** — a stub that returns "Sunny, 25°C" for any city. In a real application you would call a weather API.
2. **list_dir** — lists files and subdirectories at a given path using `java.io.File`.
3. **read_file** — reads the contents of a file, with an optional `max_lines` parameter to limit output.
4. **recursive-file-search** — recursively searches for files whose names contain a search string.
5. **math-eval** — evaluates simple arithmetic expressions (integers with `+`, `*`, `/`) using a safe left-to-right evaluator.

Each tool follows the same pattern: implement `getName`, `getDescription`, `getParameters` (a JSON-schema object), and `callAsync` (which receives the parameters and returns a `Mono<ToolResultBlock>`). The `callAsync` method extracts the input parameters from the `param` object via `.getInput`, performs its logic in pure Clojure, and wraps the result string with `ToolResultBlock/text` inside `Mono/just`.

All five tools are registered into a `Toolkit`, which is then attached to the `ReActAgent` via its builder. When the agent receives a prompt, the ReAct loop inspects the available tools and their descriptions, decides which tools to call (if any), calls them, observes the results, and iterates until it can produce a final answer. The agent may call multiple tools in a single turn — for example, when asked about weather in two cities, it calls `getWeather` twice.

The `-main` function runs four example queries in sequence: a weather query for two cities, a request to list and read markdown files, a recursive file search, and a math evaluation.

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

``
 # AgentScope + Gemini — Clojure Edition
This directory contains Clojure examples for using the **AgentScope SDK** directly via Java interop.
> See [`README.md`](README.md) for background on AgentScope and the Gemini model.
``

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

Notice how the agent autonomously chains tool calls. For the markdown files query, it first calls `list_dir` to discover the files, then calls `read_file` with `max_lines=5` for each `.md` file it finds. For the weather query it calls `getWeather` twice (once for Tokyo, once for Paris). The ReAct loop handles all of this orchestration — your code simply registers the tools and sends the prompt.

## Summary

In this chapter we used the AgentScope Java SDK from Clojure to build two kinds of LLM-powered agents:

- A **simple completion agent** that wraps a Gemini model in a `ReActAgent` and sends a prompt with no tools.
- A **tool-using agent** that wraps an Ollama model, registers five tools written in Clojure via the `AgentTool` interface and `Toolkit`, and lets the ReAct loop decide which tools to call.

The key takeaway is that AgentScope's builder pattern and `reify` based tool definitions map naturally to Clojure idioms. You get the full power of the ReAct reasoning loop with automatic tool selection, multi-turn tool calling, and result synthesis without writing any orchestration code yourself. The same pattern scales to more tools, different models, or multi-agent workflows using AgentScope's `MsgHub` for agent-to-agent communication.
