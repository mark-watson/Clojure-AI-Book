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
