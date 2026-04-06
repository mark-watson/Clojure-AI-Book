(ns ollama-api.core
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]))

(def ^:dynamic *base-url* "http://localhost:11434")
(def ^:dynamic *default-model* "mistral:v0.3")

(defn- ollama-helper [endpoint body]
  (let [response (client/post
                  (str *base-url* endpoint)
                  {:accept :json
                   :headers {"Content-Type" "application/json"}
                   :body (json/write-str body)
                   :throw-exceptions false})]
    (if (client/success? response)
      (json/read-str (:body response) :key-fn keyword)
      (throw (ex-info "Ollama API error"
                      {:status (:status response)
                       :body (:body response)})))))

(defn completions
  "Use the Ollama API for text completions.
   Options can include :model, :stream, etc.
   Throws ExceptionInfo on validation or API errors."
  ([prompt-text]
   (completions prompt-text {}))
  ([prompt-text opts]
   (when (or (nil? prompt-text) (not (string? prompt-text)))
     (throw (ex-info "Invalid prompt: must be a non-nil string"
                     {:prompt-text prompt-text})))
   (try
     (let [body (merge {:model *default-model*
                        :prompt prompt-text
                        :stream false}
                       opts)
           result (ollama-helper "/api/generate" body)]
       (if (contains? result :response)
         (:response result)
         (throw (ex-info "Unexpected response format from Ollama API"
                         {:result result}))))
     (catch clojure.lang.ExceptionInfo e
       (throw e))
     (catch Exception e
       (throw (ex-info "Failed to generate completions"
                       {:prompt-text prompt-text
                        :opts opts}
                       e))))))

(defn chat
  "Use the Ollama API for chat conversations.
   Messages should be a collection of maps like {:role \"user\" :content \"Hello\"}.
   Options can include :model, :stream, etc."
  ([messages]
   (chat messages {}))
  ([messages opts]
   (let [body (merge {:model *default-model*
                      :messages messages
                      :stream false}
                     opts)
         result (ollama-helper "/api/chat" body)]
     (get-in result [:message :content]))))

(defn summarize
  "Use the Ollama API for text summarization"
  [prompt-text]
  (completions (str "Summarize the following text: " prompt-text)))

(defn answer-question
  "Use the Ollama API for question answering"
  [prompt-text]
  (completions (str "Answer the following question: " prompt-text)))
