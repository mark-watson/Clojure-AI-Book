# Using the Google Gemini APIs

We used the OpenAI LLM APIs in the last chapter and now we provide a similar example using Google's **gemini-2.5-flash** model.

**Note: Added Tool Use and Search API support to the REST API code March 7, 2026.**

I recommend reading Google's [online documentation for the APIs](https://cloud.google.com/vertex-ai/generative-ai/docs/model-reference/inference) to see all the capabilities of the OpenAI APIs.

We first use a REST interface and write a Gemini access library from scratch using only low-level Clojure libraries. Later we use the Google Java Gemini SDK.

In all examples you may substitute the model **gemini-3.0-pro** for **gemini-2.5-flash**.


## Test Code for REST Interface and Sample Test Output

Before we look at the example code, let's look at an example code running it and later sample output:


```clojure
(ns gemini-api.core-test
  (:require [clojure.test :refer :all]
            [gemini-api.core :refer :all]))

(def some-text
  "Jupiter is the fifth planet from the Sun and the largest in the Solar System. It is a gas giant with a mass one-thousandth that of the Sun, but two-and-a-half times that of all the other planets in the Solar System combined. Jupiter is one of the brightest objects visible to the naked eye in the night sky, and has been known to ancient civilizations since before recorded history. It is named after the Roman god Jupiter.[19] When viewed from Earth, Jupiter can be bright enough for its reflected light to cast visible shadows,[20] and is on average the third-brightest natural object in the night sky after the Moon and Venus.")

(deftest completions-test
  (testing "gemini completions API"
    (let [results
          (gemini-api.core/generate-content "He walked to the river")]
      (println results)
      (is (= 0 0)))))

(deftest summarize-test
  (testing "gemini summarize API"
    (let [results
          (gemini-api.core/summarize
           some-text)]
      (println results)
      (is (= 0 0)))))

(deftest question-answering-test
  (testing "gemini question-answering API"
    (let [results
          (gemini-api.core/generate-content
            ;;"If it is not used for hair, a round brush is an example of what 1. hair brush 2. bathroom 3. art supplies 4. shower ?"
           "Where is the Valley of Kings?"
            ;"Where is San Francisco?"
           )]
      (println results)
      (is (= 0 0)))))

;; ── Google Search (grounding) ─────────────────────────────────────────────────

(deftest search-test
  (testing "gemini Google Search grounding API"
    (let [[text citations]
          (gemini-api.core/generate-with-search-and-citations
           "Who wrote the Clojure programming language?")]
      (println "Search result text:" text)
      (println "Citations:")
      (doseq [{:keys [title uri]} citations]
        (println " -" title uri))
      ;; text should be a non-empty string
      (is (string? text))
      (is (pos? (count text))))))

;; ── Tool / Function calling ───────────────────────────────────────────────────

(defn- mock-get-weather
  "Fake weather lookup – no real HTTP call; just returns a canned string."
  [{:keys [location]}]
  (str "It is 72°F and sunny in " location "."))

(def ^:private weather-tool-def
  {:name        "get_weather"
   :description "Returns current weather conditions for a given city."
   :parameters  {:type       "OBJECT"
                 :properties {:location {:type        "STRING"
                                         :description "The name of the city."}}
                 :required   ["location"]}})

(deftest tool-use-test
  (testing "gemini function/tool calling API"
    (let [result
          (gemini-api.core/generate-with-tools
           "What is the weather like in Paris right now?"
           [weather-tool-def]
           {"get_weather" mock-get-weather})]
      (println "Tool-use result:" result)
      ;; The model should have issued a functionCall; our dispatch fn returns
      ;; a string describing the weather.
      (is (map? result))
      (is (or (contains? result :text)
              (contains? result :function-call)))
      (when (contains? result :function-call)
        (is (string? (:result result)))
        (is (re-find #"Paris" (:result result)))))))
```

The output (edited for brevity) looks like this:

```text
 $ lein test

lein test gemini-api.core-test
Okay, "He walked to the river."

That's a simple and clear sentence! What would you like to do with it?

For example, I can:

1.  **Acknowledge it:** "Got it." or "I understand."
2.  **Ask for more information:** "Why did he walk to the river?" or "What happened next?"
3.  **Expand on it descriptively:** "The path was worn and led directly to the shimmering river."
4.  **Imagine the scene:** "I picture a man, perhaps with a thoughtful expression, making his way to the water's edge."
5.  **Use it in a story:** "He walked to the river, a place he always found peace, hoping to clear his mind."
6.  **Analyze the grammar:** "It's a simple past tense sentence, indicating a completed action."

Just let me know!
Jupiter is the fifth and largest planet in our Solar System, classified as a gas giant. It possesses a mass two-and-a-half times greater than all other planets combined. Extremely bright, it is visible to the naked eye and has been known since ancient times, capable of casting visible shadows. On average, it is the third-brightest natural object in the night sky after the Moon and Venus, and is named after the Roman god Jupiter.
The Valley of Kings is located in **Egypt**, specifically on the **west bank of the Nile River**, near the modern city of **Luxor**.

This area was part of ancient Thebes, and it served as the burial place for pharaohs and powerful nobles of the New Kingdom (18th to 20th Dynasties).
Search result text: Rich Hickey is the creator of the Clojure programming language. He developed Clojure in the mid-2000s, releasing it publicly in October 2007. Hickey wanted a modern Lisp that was functional, compatible with the Java platform, and designed for concurrency.

He continues to lead the development of the language. While much of Clojure's underpinnings and initial compiler were written in Java, Hickey expressed a desire to rewrite those parts in Clojure itself.
Citations:
 - clojure.org https://vertexaisearch.cloud.google.com/grounding-api-redirect/AUZIYQG7EkD0j4M6tohYe2mE4xsJ0UnD9mW_2k9kTfEC_KFjEjF3abclOhMEK_5lnWi9eEiYM-sLG5VLGmrz8MJr8GO78c7uuY-Y3tOYcbXERim9lE0ByuU=
 - wikipedia.org https://vertexaisearch.cloud.google.com/grounding-api-redirect/AUZIYQGUsFsuSAgQhO0xpLuPkmnCu0uH9avs3dSlsUqtRr3rwT1IJS0j_9Zidy9xJwXxsOU4GcA9MQ7K55g8RR8HsMXx_MTykOIRitXpLw0ykzvwWWmdBUIMbR3r6y33ZsDA
 - wikipedia.org https://vertexaisearch.cloud.google.com/grounding-api-redirect/AUZIYQHKDCXRr7I88rWWwvakVZ5cQKVXEVBaNVUL1U28qMXKJwRyB3wGCvm2t2N1a7d0oWcxXCP9ME-1x0toDWEtwjFIklLtoKWSJAmgE4wcVn1pGo7JvYdFqIp5GY-v62h92gELKQ==
 - clojure.org https://vertexaisearch.cloud.google.com/grounding-api-redirect/AUZIYQHv_3Lk5xWwXDXM1MLJiNKbW3M10klqM05QNHKmlgtqW1B88yaxN-e8W0XCQqCXp5XL3tkkyjY1PpF5GwaV1iMkc0t01YHU2l9GZuHi4Q==
 - nubank.com https://vertexaisearch.cloud.google.com/grounding-api-redirect/AUZIYQHydcJ7X1GBqp6Ix8TfWIdly167wONbnivgUq8LRk53ZtiMmX4_5RPIz3Ux1PEtfAhqePoRHPBF5SketLNaFZiHBYfIArg9CWWM2hdCVwUD28FpQE_YePas8xjZdUAw04ovQywWDvh1DyEm_bR9I4wAvxDW5K4es-iNwXbCHdg=
 - mcqueeney.tech https://vertexaisearch.cloud.google.com/grounding-api-redirect/AUZIYQFSqtbhsfbLZ0hKglZuYmMyHr5Me2p2G3S7c-XeRX_1V-8Iv9GtwpFI3cZa_fn8CzI8jemxFlh_nDckydR1RE17_FW-9EEMzZHkvrFp3p7RftRaYrQywASS2qV9LWwgP326Ia_4a-JXFXtkiiDdOblaP9HfgBV8IBQmnqKoEZV9pzja918uwCLLwL30PEFI6KaD_Tw898_3
 - github.com https://vertexaisearch.cloud.google.com/grounding-api-redirect/AUZIYQHrSB2btECWpuWNqk_iETlVTWfLqVOea9M3NocGXJhYqGSzeZwwlsWUiW4flthZXG2ZwKcziq50Gc7-WIAVkoomzrBXMYq5gR8NavqmAwEs4iz0E5dUDI31XO3giIJ9wKTcXGuKexkl6pwock4PwTRPfTxmS-OJCnBu
Tool-use result: {:function-call {:name get_weather, :args {:location Paris}}, :result It is 72°F and sunny in Paris.}

Ran 5 tests containing 9 assertions.
0 failures, 0 errors.
```

## Gemini API Library Implementation for REST Interface

Here is the library implementation, we will discuss the code after the listing:

```clojure
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
```

This Clojure code is designed to interact with Google's Gemini API to generate text content and specifically to summarize text. It sets up the necessary components to communicate with the API, including importing libraries for making HTTP requests and handling JSON data.  Crucially, it retrieves your Google API key from an environment variable, ensuring secure access. The code also defines configuration like the Gemini model to use and the base API endpoint URL.  It's structured within a Clojure namespace for organization and includes basic error handling and debug printing to aid in development and troubleshooting.

The core of the functionality lies in the **generate-content** function. This function takes a text prompt as input, constructs the API request URL with the chosen model and your API key, and then sends this request to Google's servers.  It handles the API response, parsing the JSON result to extract the generated text content. The code also checks for potential errors, both in the API request itself and in the structure of the response, providing informative error messages if something goes wrong.  Building on this, the function **summarize** offers a higher-level interface, taking text as input and using generate-content to send a "summarize" prompt to the API, effectively providing a convenient way to get text summaries using the Gemini models.

## (New) Gemini Client Library Using Google’s Java SDK for Gemini

The code for this section can be found in the directory ** Clojure-AI-Book-Code/gemini_java_api**.

Here we test code that is almost identical to that used earlier for the REST interface library so we don’t list the test code here.

Here is the library implementation:

```clojure
(ns gemini-java-api.core
  (:import (com.google.genai Client)
           (com.google.genai.types GenerateContentResponse)))

(def DEBUG false)

(def model "gemini-2.5-flash") ; or gemini-2.5-pro, etc.
(def google-api-key (System/getenv "GOOGLE_API_KEY")) ; Make sure to set this env variable

(defn generate-content
  "Sends a prompt to the Gemini API using the specified model and returns
   the text response."
  [prompt]
  (let [client (Client.)
        ^GenerateContentResponse resp
        (.generateContent (.models client)
                          model
                          prompt
                          nil)]
    (when DEBUG
      (println (.text resp))
      (when-let [headers
                 (some-> resp
                     .sdkHttpResponse (.orElse nil)
                     .headers        (.orElse nil))]
        (println "Response headers:" headers)))
    (.text resp)))

(defn summarize [text]
  (generate-content (str "Summarize the following text:\n\n" text)))
```

I used the previous REST interface library implementation for over one year but now I have switched to using this shorter implementation that uses interop with the Java Gemini SDK.


## Gemini APIs Wrap Up

The Gemini APIs also support a message-based API for optionally adding extra context data, configuration data, and AI safety settings. The example code using the REST interface provides a simple completion style of interacting with the Gemini models.

If you use my Java SDK example library you can clone it in your own projects and optionally use those features of the Java SDK that you might find useful. Reference: [https://github.com/googleapis/java-genai](https://github.com/googleapis/java-genai).