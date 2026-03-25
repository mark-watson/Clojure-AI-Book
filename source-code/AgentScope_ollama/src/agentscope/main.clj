(ns agentscope.main
  "AgentScope ReActAgent demo using Ollama (qwen3-max).

   Ensure Ollama is running locally on http://localhost:11434 before running:
     lein run"
  (:import [io.agentscope.core ReActAgent]
           [io.agentscope.core.message Msg]
           [io.agentscope.core.model OllamaChatModel])
  (:gen-class))

(defn -main [& _args]
  ;; Build the Ollama chat model
  (let [model (-> (OllamaChatModel/builder)
                  (.modelName "nemotron-3-nano:4b")
                  (.baseUrl "http://localhost:11434")
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
    (println (.getTextContent response))))
