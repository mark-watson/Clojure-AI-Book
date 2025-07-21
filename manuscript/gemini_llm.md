# Using the Google Gemini 2.0 Flash APIs

We used the OpenAI LLM APIs in the last chapter and now we provide a similar example using Google's **gemini-2.0-flash** model.

I recommend reading Google's [online documentation for the APIs](https://cloud.google.com/vertex-ai/generative-ai/docs/model-reference/inference) to see all the capabilities of the OpenAI APIs. 

## Test Code and Sample Test Output

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
      (println results))))

(deftest summarize-test
  (testing "gemini summarize API"
    (let [results
          (gemini-api.core/summarize
           some-text)]
      (println results))))

(deftest question-answering-test
  (testing "gemini question-answering API"
    (let [results
          (gemini-api.core/generate-content
           "Where is the Valley of Kings?"
           )]
      (println results))))
```

The output (edited for brevity) looks like this:

```text
$ lein test

**Adding detail:**

*   He walked slowly to the river.
*   He walked to the river, his head down.
*   He walked to the river, eager to cool off.
*   He walked to the river, his boots crunching on the gravel path.

**Adding context:**

*   Tired of the city, he walked to the river.
*   After the argument, he walked to the river.
*   He walked to the river, seeking solace in its gentle flow.

**Setting the scene:**

*   The sun beat down as he walked to the river.
*   A cool breeze rustled the leaves as he walked to the river.
*   The air smelled of damp earth as he walked to the river.

**Making it part of a longer narrative:**

*   He walked to the river and sat down on the bank, watching the water flow by.
*   He walked to the river, but when he arrived, he hesitated to go further.
*   He walked to the river, knowing it was the only place he could truly be alone.


Jupiter, the fifth planet from the Sun, is the largest planet in our solar system, a gas giant exceeding the mass of all other planets combined. Easily visible to the naked eye, it's been observed since ancient times and named after the Roman god Jupiter. Its brightness makes it a prominent object in the night sky, often third brightest after the Moon and Venus.


The Valley of the Kings is located on the west bank of the Nile River, near the city of Luxor (ancient Thebes) in Egypt.
```

## Gemini API Library Implementation

Here is the library implementation, we will discuss the code after the listing:

```clojure
(ns gemini-api.core
  (:require [clj-http.client :as client])
  (:require [clojure.data.json :as json]))

(def model "gemini-2.0-flash") ; or gemini-1.5-pro, etc.

(def google-api-key (System/getenv "GOOGLE_API_KEY"))

(def base-url "https://generativelanguage.googleapis.com/v1beta/models")

(defn generate-content [prompt]
  (let [url (str base-url "/" model ":generateContent?key=" google-api-key)
        headers {"Content-Type" "application/json"}
        body {:contents [{:parts [{:text prompt}]}]}]
    (try
      (let [response (client/post url {:headers headers
                                     :body (json/write-str body)
                                     :content-type :json
                                     :accept :json})
            parsed-response (json/read-str (:body response) :key-fn keyword)
            candidates (:candidates parsed-response)]
        (if (seq candidates)
          (let [text (get-in (first candidates) [:content :parts 0 :text])]
            (if text
              text
              (do
                (println "No text found in response structure:" parsed-response)
                nil)))
          (do
            (println "No candidates found in response:" parsed-response)
            nil)))
      (catch Exception e
           (println "Error making request:" (.getMessage e))
           (when-let [response-body (-> e ex-data :body)]
             (println "Error response body:" response-body))
           nil))))

(defn summarize [text]
  (generate-content (str "Summarize the following text:\n\n" text)))
```

This Clojure code is designed to interact with Google's Gemini API to generate text content and specifically to summarize text. It sets up the necessary components to communicate with the API, including importing libraries for making HTTP requests and handling JSON data.  Crucially, it retrieves your Google API key from an environment variable, ensuring secure access. The code also defines configuration like the Gemini model to use and the base API endpoint URL.  It's structured within a Clojure namespace for organization and includes basic error handling and debug printing to aid in development and troubleshooting.

The core of the functionality lies in the **generate-content** function. This function takes a text prompt as input, constructs the API request URL with the chosen model and your API key, and then sends this request to Google's servers.  It handles the API response, parsing the JSON result to extract the generated text content. The code also checks for potential errors, both in the API request itself and in the structure of the response, providing informative error messages if something goes wrong.  Building on this, the function **summarize** offers a higher-level interface, taking text as input and using generate-content to send a "summarize" prompt to the API, effectively providing a convenient way to get text summaries using the Gemini models.

## Gemini APIs Wrap Up

The Gemini APIs also support a message-based API for optionally adding extra context data, configuration data, and AI safety settings. The example code provides a simple completion style of interacting with the Gemini models.