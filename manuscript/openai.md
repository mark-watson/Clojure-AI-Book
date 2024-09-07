# Using the OpenAI APIs

I have been working as an artificial intelligence practitioner since 1982 and the capability of the beta OpenAI APIs is the most impressive thing that I have seen (so far!) in my career. These APIs use the GPT-4 model.

I recommend reading the [online documentation for the APIs](https://platform.openai.com/docs/introduction/key-concepts) to see all the capabilities of the OpenAI APIs. 

Let's start by jumping into the example code.

The library that I wrote for this chapter supports three functions: completing text, summarizing text, and answering general questions. The single OpenAI model that the OpenAI APIs use is fairly general purpose and can perform tasks like:

- Generate cooking directions when given an ingredient list.
- Grammar correction.
- Write an advertisement from a product description.
- Generate spreadsheet data from data descriptions in English text. 

Given the examples from [https://platform.openai.com](https://platform.openai.com) (will require you to login) and the Clojure examples here, you should be able to modify my example code to use any of the functionality that OpenAI documents.

We will look closely at the function **completions** and then just look at the small differences to the other two example functions. The definitions for all three exported functions are kept in the file **src/openai_api/core.clj***. You need to request an API key (I had to wait a few weeks to receive my key) and set the value of the environment variable **OPENAI_KEY** to your key. You can add a statement like:

{linenos=off}
~~~~~~~~
export OPENAI_API_KEY=sa-hdffds7&dhdhsdgffd
~~~~~~~~

to your **.profile** or other shell resource file. Here the API token "sa-hdffds7&dhdhsdgffd" is made up - that is not my API token.

When experimenting with OpenAI APIs it is often start by using the **curl** utility. An example **curl** command line call to the beta OpenAI APIs is (note: this CURL example uses an earlier API):

{lang="bash",linenos=on}
~~~~~~~~
curl https://api.openai.com/v1/chat/completions -H "Content-Type: application/json" -H "Authorization: Bearer $OPENAI_API_KEY"   -d '{
    "model": "gpt-3.5-turbo",
    "messages": [
      {
        "role": "system",
        "content": "You are an assistant, skilled in explaining complex programming and other technical problems."
      },
      {
        "role": "user",
        "content": "Write a Python function foo to add two argument"
      }
    ]
  }'
~~~~~~~~

Output might look like this:

```console
{
  "id": "chatcmpl-8nqUrlNsCPQgUkSIjW7ytvN5GlH3C",
  "object": "chat.completion",
  "created": 1706890561,
  "model": "gpt-3.5-turbo-0613",
  "choices": [
    {
      "index": 0,
      "message": {
        "role": "assistant",
        "content": "Certainly! Here is a Python function named `foo` that takes two arguments `a` and `b` and returns their sum:\n\n```python\ndef foo(a, b):\n    return a + b\n```\n\nTo use this function, simply call it and pass in two arguments:\n\n```python\nresult = foo(3, 5)\nprint(result)  # Output: 8\n```\n\nIn this example, `result` will store the sum of `3` and `5`, which is `8`. You can change the arguments `a` and `b` to any other numbers to get different results."
      },
      "logprobs": null,
      "finish_reason": "stop"
    }
  ],
  "usage": {
    "prompt_tokens": 35,
    "completion_tokens": 127,
    "total_tokens": 162
  },
  "system_fingerprint": null
}
```


 All of the OpenAI APIs expect JSON data with query parameters. To use the completion API, we set values for **prompt**. We will look at several examples later.

The file **src/openai_api/core.clj** contains the implementation of our wrapper library using Werner Kok's library:

{lang="clojure",linenos=on}
~~~~~~~~
(ns openai-api.core
  (:require
   [wkok.openai-clojure.api :as api])
  (:require [clj-http.client :as client])
  (:require [clojure.data.json :as json]))

(def model2 "gpt-4o-mini")

(def api-key (System/getenv "OPENAI_API_KEY"))

(defn completions [prompt]
  (let [url "https://api.openai.com/v1/chat/completions"
        headers {"Authorization" (str "Bearer " api-key)
                 "Content-Type" "application/json"}
        body {:model model2
              :messages [{:role "user" :content prompt}]}
        response (client/post url {:headers headers
                                   :body (json/write-str body)})]
    ;;(println (:body response))
    (get
     (get
      (first
       (get
        (json/read-str (:body response)  :key-fn keyword)
        :choices))
      :message)
     :content)))

(defn summarize [text]
  (completions (str "Summarize the following text:\n\n" text)))

(defn embeddings [text]
  (try
    (let* [body
           (str
            "{\"input\": \""
            (clojure.string/replace
             (clojure.string/replace text #"[\" \n :]" " ")
             #"\s+" " ")
            "\", \"model\": \"text-embedding-ada-002\"}")
           json-results
           (client/post
            "https://api.openai.com/v1/embeddings"
            {:accept :json
             :headers
             {"Content-Type"  "application/json"
              "Authorization" (str "Bearer " api-key)}
             :body   body})]
          ((first ((json/read-str (json-results :body)) "data")) "embedding"))
    (catch Exception e
      (println "Error:" (.getMessage e))
      "")))

(defn dot-product [a b]
  (reduce + (map * a b)))
~~~~~~~~

Note that the OpenAI models are stochastic. When generating output words (or tokens), the model assigns probabilities to possible words to generate and samples a word using these probabilities. As a simple example, suppose given prompt text "it fell and", then the model could only generate three words, with probabilities for each word based on this prompt text:

- the 0.9
- that 0.1
- a 0.1

The model would *emit* the word **the** 90% of the time, the word **that** 10% of the time, or the word **a** 10% of the time. As a result, the model can generate different completion text for the same text prompt. Let's look at some examples using the same prompt text. Notice the stochastic nature of the returned results:

{lang="clojure",linenos=on}
~~~~~~~~
$ lein repl
openai-api.core=> (openai-api.core/completions "He walked to the river")
" and breathed in the new day, looking out to the lake where the Mire was displacing the Wold by its"
openai-api.core=> (openai-api.core/completions "He walked to the river")
". He waded in, not caring about his expensive suit pants. He was going to do this right, even if"
openai-api.core=> (openai-api.core/completions "He walked to the river")
" every day. The salty air puffed through their pores. He had enjoyed her company. Maybe he did need a companion"
~~~~~~~~

The function **summarize** is very similar to the function **completions** except I changed the system prompt string. Here is some example output:

{lang="clojure",linenos=on}
~~~~~~~~
openai-api.core=> (def some-text
             #_=>   "Jupiter is the fifth planet from the Sun and the largest in the Solar System. It is a gas giant with a mass one-thousandth that of the Sun, but two-and-a-half times that of all the other planets in the Solar System combined. Jupiter is one of the brightest objects visible to the naked eye in the night sky, and has been known to ancient civilizations since before recorded history. It is named after the Roman god Jupiter.[19] When viewed from Earth, Jupiter can be bright enough for its reflected light to cast visible shadows,[20] and is on average the third-brightest natural object in the night sky after the Moon and Venus.")
#'openai-api.core/some-text

openai-api.core=> (openai-api.core/summarize some-text openai-api.core=> (openai-api.core/summarize some-text)
"Jupiter is classified as a gas giant along with Saturn, Uranus, and Neptune. Jupiter is composed primarily of gaseous and liquid matter.[21] It is the largest of the four giant planets in the Solar System and hence its largest planet. It has a diameter of 142,984 km at its equator, which is 0.11 times the diameter of Earth. Jupiter is a gas giant because the mass of the planet"
~~~~~~~~


In addition to reading the OpenAI API documentation you might want to read general material on the use of OpenAI's GPT-4 model.
