# Running LLMs Locally Using Ollama

In the last two chapters we used cloud services to run LLMs. For my personal research and experimentation I prefer running smaller models locally on my Mac Mini that has 32G of memory. We will use the Ollama system that runs on macOS and Linux (and Windows support is coming soon). Here we use the Mistral 7B LLM. If you don’t have at least 16G of memory on your system then you should consider substituting the model “mistral” in the following examples with Stable LM 2 1.6B that is a state-of-the-art 1.6 billion parameter small language model trained on multilingual data in English, Spanish, German, Italian, French, Portuguese, and Dutch. Use the model file name “stablelm2” in the following examples instead of “mistral”.

You need to install Ollama on your system: https://ollama.ai

You then need to install the Mistral model (this takes a while the first time, but the model file is cached so future startups are fast):

**ollama run mistral**


## Running the Example Tests

Before we look at the code, you might want to run the example tests first:

In one console, run the Ollama REST API service:

**ollama serve**

Then run the tests in another console:

**lein test**

## Clojure Client Library for the Ollama Service REST APIs

The following Clojure code (**ollama/src/ollama_api/core.clj**) defines a simple API client for interacting with the Ollama REST API text generation service. Here's a breakdown of its functionality:

Dependencies:

- (:require [clj-http.client :as client]) for HTTP requests.
- (:require [clojure.data.json :as json]) for JSON processing.

**ollama-helper** is a private function (indicated by - prefix) used to interact with the Ollama API. It takes body (a JSON string) as an argument and makes a POST request to http://localhost:11434/api/generate with the body, sets the request header to accept JSON and indicates that the content type is JSON, and finally extracts the response from the API and parses the JSON.

The **completions** function takes **prompt-text** as an argument, constructs a JSON string with the prompt and model details, and finally calls **ollama-helper** with this JSON string.

The **summarize** function uses the **completions** function to send a summarization request by creating a new prompt by concatenating the string "Summarize the following text: " with the original prompt text.

The **answer-question** function also uses the **completions** function to send a question answering request by creating a new prompt by concatenating the string "Answer the following question: " with the original prompt text.


```clojure
(ns ollama-api.core
  (:require [clj-http.client :as client])
  (:require [clojure.data.json :as json]))


(defn- ollama-helper [body]
  (let [json-results
        (client/post
          "http://localhost:11434/api/generate"
          {:accept :json
           :headers
           {"Content-Type" "application/json"}
           :body   body
           })]
    ((json/read-str (json-results :body)) "response")))

(defn completions
  "Use the Ollama API for text completions"
  [prompt-text]
  (let
    [body
     (str
       "{\"prompt\": \"" prompt-text
       "\", \"model\": \"mistral\""
       ", \"stream\": false }"
       )]
    (ollama-helper body)))

(defn summarize
  "Use the Ollama API for text summarization"
  [prompt-text]
  (completions
    (str "Summarize the following text: "
       prompt-text)))

(defn answer-question
  "Use the Ollama API for question answering"
  [prompt-text]
  (completions
    (str "Answer the following question: "
       prompt-text)))
```


Here are the unit tests:

```clojure
(ns ollama-api.core-test
  (:require [clojure.test :refer :all]
            [ollama-api.core :refer :all]))

(def some-text
  "Jupiter is the fifth planet from the Sun and the largest in the Solar System. It is a gas giant with a mass one-thousandth that of the Sun, but two-and-a-half times that of all the other planets in the Solar System combined. Jupiter is one of the brightest objects visible to the naked eye in the night sky, and has been known to ancient civilizations since before recorded history. It is named after the Roman god Jupiter.[19] When viewed from Earth, Jupiter can be bright enough for its reflected light to cast visible shadows,[20] and is on average the third-brightest natural object in the night sky after the Moon and Venus.")

(deftest completions-test
  (testing "ollama-Ollama completions API"
    (let [results
          (ollama-api.core/completions "He walked to the river")]
      (println results))))

(deftest summarize-test
  (testing "ollama-Ollama summarize API"
    (let [results
          (ollama-api.core/summarize
            some-text)]
      (println results))))


(deftest question-answering-test
  (testing "ollama-Ollama question-answering API"
    (let [results
          (ollama-api.core/answer-question
            "Where is the Valley of Kings?"
            )]
      (println results))))
```

The output will change each time you run the tests. The output from Large Language Models (LLMs) usually change each time you run the same tests or prompts. This variability stems from several key factors:

- Stochasticity: LLMs are complex neural networks trained on massive datasets. During training these networks develop internal representations and probabilistic weights that influence their outputs. When generating text LLMs sample from these weights, introducing an inherent element of randomness. This means even with identical inputs and identical LLM weights the same prompt used repeatedly can lead to slightly different outputs.
- Temperature: Some LLMs use a temperature parameter to control the "randomness" of their outputs. A higher temperature encourages exploration of less likely but potentially more creative responses, while a lower temperature leads to more deterministic and consistent outputs.
- Beam Search vs. Sampling: Different decoding strategies can also impact output variability. Beam search focuses on the most likely sequences, gradually refining them, while sampling explores a wider range of possibilities. Beam search typically leads to less variation compared to sampling, but even within beam search, there can be randomness in how ties are broken when choosing the next word.

I edited the following output shortening the output from the text completion test and by adding labels for each test:

 ```console
 $ lein test

COMPLETION TEST:

lein test ollama-api.core-test
He walked to the river, feeling the cool breeze on his skin and the gentle rustling of leaves in the trees. The sun was just beginning to set, casting a warm orange glow across the sky. As he approached the water's edge, he could see the ripples created by the gentle flow of the river. He stopped for a moment to take it all in, feeling a sense of peace and tranquility wash over him.

As he continued on his journey, he couldn't help but think about how much the world had changed in recent times. The pandemic had brought with it so much uncertainty and fear, but now, as he stood by the river, he felt grateful to be alive and able to enjoy such simple pleasures. He made a mental note to come back here more often, to take time for himself and to appreciate the beauty of nature....

SUMMARIZATION TEST:

Jupiter is a massive gas giant planet located in our solar system. It's the fifth planet from the sun and is known for being one of the brightest objects visible to the naked eye at night. With a mass one-thousandth that of the sun, it has a unique place in the cosmos.

QUESTION ANSWERING TEST:

The Valley of Kings is located in Egypt, specifically in the region of Giza. It is a valley on the west bank of the Nile River and is known for its abundance of royal tombs dating back to ancient Egypt's New Kingdom period.
 ```
 