# Using Moonshot's Kimi K2 Model with Built In $web_search Tool Support

Moonshot AI (月之暗面, or "Dark Side of the Moon") is a prominent Chinese artificial intelligence startup founded in March 2023. The company quickly achieved a multi-billion dollar valuation, notably securing a $1 billion funding round led by Alibaba. Founded by a team with strong academic roots from institutions like Tsinghua University and Carnegie Mellon, Moonshot AI has established itself as one of China's leading "AI Tigers." The company's primary strategic focus is on developing large language models (LLMs) capable of handling exceptionally long context windows, aiming to push the boundaries of what AI can process and comprehend in a single prompt.

Their flagship model, Kimi K2, is a state-of-the-art, open-weight language model that has drawn significant attention. It is built on a Mixture-of-Experts (MoE) architecture with one trillion total parameters, of which 32 billion are active during inference, making it both powerful and efficient. Kimi K2 is specifically designed as an "agentic" AI, meaning it's optimized not just for conversation but for performing complex, multi-step tasks, using tools, and executing code autonomously. With a 128k token context window and benchmark performance that rivals or exceeds leading proprietary models in coding and reasoning tasks, Kimi K2 represents a major milestone for open-source AI.

Kimi K2 has built in support for web searching, as the follwoing code example shows. You will need a Moonshot API key from the Moonshot console [https://login.moonshot.ai](https://login.moonshot.ai) and the model documentation can be found here [https://platform.moonshot.ai/docs](https://platform.moonshot.ai/docs). If you don't want to use servers in China, the Kimi K2 model is open source and several US-based providers offer inference services.

## Clojure Library Moonshot.ai's Kimi K2 Model (Including Web Search Tool)

The following Clojure code defines a client for interacting with the Moonshot AI API, providing two primary functions for chat completions. The first, **completions**, performs a basic, single-turn request by sending a user prompt along with a predefined system message to the Kimi 2 model and returns the text content of the model's response. The second, more advanced function, **search**, orchestrates a multi-turn, agentic conversation that leverages the model's built-in web search tool; it initiates a loop that repeatedly calls a private chat helper function, checks if the model's response is a request to use a tool ("tool_calls"), and if so, constructs and appends the necessary tool-related messages to the conversation history before continuing the loop until the model provides a final content-based answer. The entire namespace relies on the clj-http.client and clojure.data.json libraries for making HTTP POST requests and handling JSON serialization, respectively, while retrieving the required **MOONSHOT_API_KEY** from system environment variables and including basic error handling for failed API calls.

```clojure
(ns moonshot-api.core
  (:require [clj-http.client :as client]
            [clojure.data.json :as json])
  (:gen-class))

;; Define the API key, base URL, and model for Moonshot AI
(def moonshot-api-key (System/getenv "MOONSHOT_API_KEY"))
(def moonshot-base-url "https://api.moonshot.ai/v1")
(def moonshot-model "kimi-k2-0711-preview")

(defn completions
  "Sends promp request to the Moonshot AI chat completions API."
  [prompt]
  (if-not moonshot-api-key
    (println "Error: MOONSHOT_API_KEY environment variable not set.")
    (try
      (let [url (str moonshot-base-url "/chat/completions")
            headers {"Authorization" (str "Bearer " moonshot-api-key)
                     "Content-Type" "application/json"}
            ;; Construct the request body to match the Python example
            body {:model moonshot-model
                  :messages [{:role "system" :content "You are Kimi, an AI assistant provided by Moonshot AI. You are proficient in English conversations. You provide users with safe, helpful, and accurate answers."}
                             {:role "user" :content prompt}]
                  :temperature 0.3}
            ;; Make the POST request
            response (client/post url {:headers headers
                                       :body    (json/write-str body)
                                       ;; Throw an exception for non-2xx response codes
                                       :throw-exceptions false})
            ;; Parse the JSON response body
            parsed-body (json/read-str (:body response) :key-fn keyword)]

        (if (= (:status response) 200)
          ;; Extract the content from the response using the -> (thread-first) macro
          ;; This is equivalent to: (get (get (first (get parsed-body :choices)) :message) :content)
          (-> parsed-body :choices first :message :content)
          ;; Handle potential errors from the API
          (str "Error: Received status " (:status response) ". Body: " (:body response))))
      (catch Exception e
        (str "An exception occurred: " (.getMessage e))))))

(defn- chat
  "Sends a chat request to the Moonshot AI chat completions API with tool support."
  [messages]
  (let [url (str moonshot-base-url "/chat/completions")
        headers {"Authorization" (str "Bearer " moonshot-api-key)
                 "Content-Type" "application/json"}
        body {:model moonshot-model
              :messages messages
              :temperature 0.3
              :tools [{:type "builtin_function"
                       :function {:name "$web_search"}}]}
        response (client/post url {:headers headers
                                   :body    (json/write-str body)
                                   :throw-exceptions false})
        parsed-body (json/read-str (:body response) :key-fn keyword)]
    (if (= (:status response) 200)
      (-> parsed-body :choices first)
      (throw (Exception. (str "Error: Received status " (:status response) ". Body: " (:body response)))))))

(defn search
  "Performs a Kimi 2 completion with web search."
  [user-question]
  (if-not moonshot-api-key
    (println "Error: MOONSHOT_API_KEY environment variable not set.")
    (try
      (loop [messages [{:role "system" :content "You are Kimi an AI assistant who returns all answers in English."}
                       {:role "user" :content user-question}]]
        (let [choice (chat messages)
              finish-reason (:finish_reason choice)]
          (if (= finish-reason "tool_calls")
            (let [assistant-message (:message choice)
                  tool-calls (-> assistant-message :tool_calls)
                  tool-messages (map (fn [tool-call]
                                       (let [tool-call-name (-> tool-call :function :name)]
                                         (if (= tool-call-name "$web_search")
                                           (let [tool-call-args (json/read-str (-> tool-call :function :arguments) :key-fn keyword)]
                                             {:role "tool"
                                              :tool_call_id (:id tool-call)
                                              :name tool-call-name
                                              :content (json/write-str tool-call-args)})
                                           (let [error-message (str "Error: unable to find tool by name '" tool-call-name "'")]
                                             {:role "tool"
                                              :tool_call_id (:id tool-call)
                                              :name tool-call-name
                                              :content (json/write-str error-message)}))))
                                     tool-calls)]
              (recur (concat messages [assistant-message] tool-messages)))
            (-> choice :message :content))))
      (catch Exception e
        (str "An exception occurred: " (.getMessage e))))))
```

## Test code for the Moonshot.ai Kimi K2 API Client Library

The test code for this library has two examples: a simple text completion augmented by the text completion with the web search tool. Here is the test code to simple text completion:

```clojure
(let [results
      (moonshot-api.core/completions "Write a story starting with the text: He walked to the river")]
  (println results)
```
 Here is a few lines of sample output:

 ```console
$ lein test

lein test moonshot-api.core-test
He walked to the river because the house behind him had become too small for the grief it carried.

The grass was still wet with dew, and each step left a darker footprint, as though the earth itself were taking notes. When he reached the bank, the water was the color of tarnished coins, sliding past without a sound. He knelt, cupped his hands, and let the river run through them. The cold stung, but it was the first thing all morning that felt real.

A dragonfly hovered, wings catching the early light like splinters of glass. It dipped once, skimming the surface, and he thought of his daughter’s laugh—bright, brief, skimming the surface of every room she had ever entered.

He had come to scatter her ashes, but the plastic urn in his coat pocket might as well have been a brick. His fingers closed around it, then opened again. Not yet.

Across the water, a willow leaned so low its branches combed the current. He imagined the tree drinking her in, leaf by leaf, until every green blade carried a memory. That seemed better than surrendering her to the anonymous pull of the river.

 ...
```

Here is example code for completion augmented with web search:

```clojure
(let [results
      (moonshot-api.core/search "Current weather in Flagstaff Arizona")]
  (println results)
```

Here is some sample output:

```console
Right now in Flagstaff, Arizona it is **76 °F (24 °C)** under **partly cloudy** skies.  
- **Wind:** Southwest at 15-16 mph  
- **Humidity:** 33 %  
- **UV index:** 7 of 11 (high)  
- **Rain chance:** 15 % with no measurable precipitation so far  

Today’s high will reach about **77-78 °F** and tonight’s low will drop to around **52 °F**. There is a slight chance of an isolated shower or thunderstorm later in the day.
```
