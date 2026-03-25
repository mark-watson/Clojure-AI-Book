(ns agentscope.tool-use
  "Demonstrates AgentScope tool use with a stub 'get weather' tool.

   The tool is defined entirely in Clojure by implementing the AgentTool
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
           [reactor.core.publisher Mono])
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

          ;; Register the weather tool via the AgentTool interface —
          ;; no @Tool / @ToolParam annotations required.
          toolkit (doto (Toolkit.)
                    (.registerAgentTool (weather-tool)))

          ;; Build the ReActAgent with the toolkit attached
          agent (-> (ReActAgent/builder)
                    (.name "WeatherAssistant")
                    (.sysPrompt "You are a helpful weather assistant. Use the getWeather tool to look up weather information.")
                    (.model model)
                    (.toolkit toolkit)
                    (.build))

          ;; Ask about the weather – the agent will invoke the tool automatically
          response (-> (.call agent
                              (-> (Msg/builder)
                                  (.textContent "What is the weather like in Tokyo and Paris?")
                                  (.build)))
                       (.block))]

      (println "Agent response:")
      (println (.getTextContent response)))))
