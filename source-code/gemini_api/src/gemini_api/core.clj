(ns gemini-api.core
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]
            [clojure.tools.logging :as log]))

;;;; ── Configuration ──────────────────────────────────────────────────────────

(def model "gemini-2.5-flash")   ; default model

(def google-api-key (System/getenv "GOOGLE_API_KEY"))
(when (nil? google-api-key)
  (log/error "GOOGLE_API_KEY environment variable not set!"))

(def base-url "https://generativelanguage.googleapis.com/v1beta/models")

;;;; ── Helpers ─────────────────────────────────────────────────────────────────

(defn- api-url
  "Build a full API endpoint URL."
  ([endpoint] (api-url model endpoint))
  ([model-id endpoint]
   (str base-url "/" model-id ":" endpoint "?key=" google-api-key)))

(defn- post-json
  "POST body (a Clojure map) to url; return parsed JSON response as a map."
  [url body]
  (let [opts {:body            (json/write-str body)
              :content-type    :json
              :accept          :json
              :socket-timeout     30000
              :connection-timeout 10000}]
    (try
      (let [resp (client/post url opts)]
        (json/read-str (:body resp) :key-fn keyword))
      (catch Exception e
        (log/error "Request error:" (.getMessage e))
        (when-let [rb (-> e ex-data :body)]
          (log/error "Response body:" rb))
        nil))))

(defn- extract-text
  "Pull the main generated text out of a parsed API response."
  [parsed-response]
  (let [candidates (:candidates parsed-response)]
    (if (seq candidates)
      (let [text (get-in (first candidates) [:content :parts 0 :text])]
        (if text
          text
          (do (log/warn "No text in response:" parsed-response) nil)))
      (do (log/warn "No candidates in response:" parsed-response) nil))))

;;;; ── Basic generation ────────────────────────────────────────────────────────

(defn generate-content
  "Generate text from PROMPT.
   Optional keyword args:
     :model-id  – model to use (default: model)
     :system    – system instruction string"
  [prompt & {:keys [model-id system]
             :or   {model-id model}}]
  (let [body (cond-> {:contents [{:parts [{:text prompt}]}]}
               system (assoc :systemInstruction
                             {:parts [{:text system}]}))
        resp (post-json (api-url model-id "generateContent") body)]
    (extract-text resp)))

;; (generate-content "In one sentence, explain how AI works to a child.")
;; (generate-content "What is 2+2?" :system "You are a concise math tutor.")

(defn summarize [text]
  (generate-content (str "Summarize the following text:\n\n" text)))

;;;; ── Token counting ──────────────────────────────────────────────────────────

(defn count-tokens
  "Return the total token count for PROMPT using the Gemini countTokens API."
  [prompt & {:keys [model-id] :or {model-id model}}]
  (let [body {:contents [{:parts [{:text prompt}]}]}
        resp (post-json (api-url model-id "countTokens") body)]
    (if-let [tc (:totalTokens resp)]
      tc
      (do (log/warn "Could not retrieve token count:" resp) nil))))

;; (count-tokens "In one sentence, explain how AI works to a child.")

;;;; ── Google Search grounding ─────────────────────────────────────────────────

(defn generate-with-search
  "Like generate-content but enables the google_search grounding tool."
  [prompt & {:keys [model-id] :or {model-id model}}]
  (let [body {:contents [{:parts [{:text prompt}]}]
              :tools    [{:google_search {}}]}
        resp (post-json (api-url model-id "generateContent") body)]
    (extract-text resp)))

;; (generate-with-search "What sci-fi movies are playing at Harkins 16 in Flagstaff today?")

(defn generate-with-search-and-citations
  "Like generate-with-search but returns [text citations] where citations is a
   seq of {:title … :uri …} maps extracted from grounding metadata."
  [prompt & {:keys [model-id] :or {model-id model}}]
  (let [body {:contents [{:parts [{:text prompt}]}]
              :tools    [{:google_search {}}]}
        resp (post-json (api-url model-id "generateContent") body)]
    (let [text      (extract-text resp)
          candidate (first (:candidates resp))
          chunks    (get-in candidate [:groundingMetadata :groundingChunks] [])
          citations (keep (fn [chunk]
                            (when-let [web (:web chunk)]
                              {:title (:title web) :uri (:uri web)}))
                          chunks)]
      [text citations])))

;; (let [[answer sources] (generate-with-search-and-citations "Who won the Super Bowl in 2024?")]
;;   (println "Answer:" answer)
;;   (doseq [{:keys [title uri]} sources]
;;     (println " -" title uri)))

;;;; ── Function / Tool calling ─────────────────────────────────────────────────
;;
;; Tool definitions follow the Gemini function-declaration schema:
;;
;;   {:name        "get_weather"
;;    :description "Returns current weather for a location."
;;    :parameters  {:type       "OBJECT"
;;                  :properties {:location {:type        "STRING"
;;                                          :description "City name"}}
;;                  :required   ["location"]}}
;;
;; The dispatch-fn is a Clojure function (map of name→fn) that the caller
;; supplies to handle function-call requests from the model.
;;
;; generate-with-tools implements a single round-trip:
;;   1. Send the prompt + tool declarations.
;;   2. If the model returns a functionCall part, invoke the matching dispatch-fn
;;      and return {:function-call {:name … :args …} :result <return-value>}.
;;   3. Otherwise return the plain text.
;;
;; For a multi-turn agentic loop, wrap generate-with-tools yourself and keep
;; accumulating the conversation history (see the docstring example).

(defn generate-with-tools
  "Call the Gemini API with PROMPT and a seq of TOOL-DEFS.
   DISPATCH-FNS is a map of tool-name (string) → (fn [args-map] …).

   Returns a map:
     {:text \"…\"}               – if the model responded with text
     {:function-call {:name … :args …}
      :result        <dispatch-fn return value>}  – if a tool was called

   Optional kwargs: :model-id :system"
  [prompt tool-defs dispatch-fns
   & {:keys [model-id system]
      :or   {model-id model}}]
  (let [fn-decls (mapv (fn [td] {:functionDeclaration td}) tool-defs)
        body     (cond-> {:contents [{:role  "user"
                                      :parts [{:text prompt}]}]
                          :tools    [{:functionDeclarations (mapv :functionDeclaration fn-decls)}]}
                   system (assoc :systemInstruction {:parts [{:text system}]}))
        resp     (post-json (api-url model-id "generateContent") body)]
    (let [candidate (first (:candidates resp))
          parts     (get-in candidate [:content :parts] [])]
      (if-let [fc-part (first (filter :functionCall parts))]
        ;; Model wants to call a function
        (let [fc     (:functionCall fc-part)
              fname  (:name fc)
              fargs  (:args fc)
              f      (get dispatch-fns fname)]
          (if f
            {:function-call fc :result (f fargs)}
            (do (log/warn "No dispatch function for tool:" fname)
                {:function-call fc :result nil})))
        ;; Regular text response
        {:text (get-in (first parts) [:text])}))))

;; ── Example: single tool call ─────────────────────────────────────────────────
;;
;; (defn get-weather [{:keys [location]}]
;;   (str "It is 72°F and sunny in " location "."))
;;
;; (def weather-tool
;;   {:name        "get_weather"
;;    :description "Returns current weather for a city."
;;    :parameters  {:type       "OBJECT"
;;                  :properties {:location {:type        "STRING"
;;                                          :description "City name"}}
;;                  :required   ["location"]}})
;;
;; (generate-with-tools
;;   "What is the weather like in Paris?"
;;   [weather-tool]
;;   {"get_weather" get-weather})

;;;; ── Chat (stateful, in-process) ────────────────────────────────────────────

(defn make-chat-session
  "Return a new chat session atom. Holds a vector of {:role … :parts […]} turns."
  []
  (atom []))

(defn chat-turn
  "Send USER-MSG in the context of SESSION (an atom returned by make-chat-session).
   Appends both the user message and the model reply to the session history.
   Returns the model's reply text."
  [session user-msg & {:keys [model-id] :or {model-id model}}]
  (let [user-turn {:role "user" :parts [{:text user-msg}]}
        history   (conj @session user-turn)
        body      {:contents history}
        resp      (post-json (api-url model-id "generateContent") body)
        reply     (extract-text resp)
        model-turn {:role "model" :parts [{:text (or reply "")}]}]
    (swap! session conj user-turn model-turn)
    reply))

(defn chat-repl
  "Simple REPL-based chat session. Type 'quit' to exit."
  []
  (let [session (make-chat-session)]
    (println "Gemini Chat – type 'quit' to exit.")
    (loop []
      (print "You: ") (flush)
      (let [input (read-line)]
        (when (and input (not= (clojure.string/trim input) "quit"))
          (let [reply (chat-turn session input)]
            (println "Gemini:" reply))
          (recur))))))

;; (chat-repl)