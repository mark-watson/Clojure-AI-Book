# Using the OpenAI APIs

I have been working as an artificial intelligence practitioner since 1982 and the capability of the beta OpenAI APIs is the most impressive thing that I have seen (so far!) in my career. These APIs use the GPT-3 model.

While you should (eventually) read the [online documentation for the APIs](https://beta.openai.com/docs/introduction/key-concepts), let's start by jumping into the example code. As seen in the **src/openai_api/core.clj** file we use the **clj-http.client** and **‌clojure.data.json** libraries:

{lang="clojure",linenos=on}
~~~~~~~~
(ns openai-api.core
  (:require [clj-http.client :as client])
  (:require [clojure.data.json :as json]))
~~~~~~~~


The library that I wrote for this chapter supports three functions: for completing text, summarizing text, and answering general questions. The single OpenAI model that the beta OpenAI APIs use is fairly general purpose and can generate cooking directions when given an ingredient list, grammar correction, write an advertisement from a product description, generate spreadsheet data from data descriptions in English text, etc. 

Given the examples from [https://beta.openai.com](https://beta.openai.com) and the Clojure examples here, you should be able to modify my example code to use any of the functionality that OpenAI documents.

We will look closely at the function **completions** and then just look at the small differences to the other two example functions. The definitions for all three exported functions are kept in the file **src/openai_api/core.clj***. You need to request an API key (I had to wait a few weeks to relieve my key) and set the value of the environment variable **OPENAI_KEY** to your key. You can add a statement like:

{linenos=off}
~~~~~~~~
export OPENAI_KEY=sa-hdffds7&dhdhsdgffd
~~~~~~~~

to your **.profile** or other shell resource file.

While I sometimes use pure Common Lisp libraries to make HTTP requests, I prefer running the **curl** utility as a separate process for these reasons:

- No problems with system specific dependencies.
- Use the standard library UIOP to run a shell command and capture the output as a string.
- I use **curl** from the command line when experimenting with web services. After I get working **curl** options, it is very easy to translate this into Common Lisp code.

An example **curl** command line call to the beta OpenAPI APIs is:

{lang="bash",linenos=on}
~~~~~~~~
curl \
  https://api.openai.com/v1/engines/davinci/completions \
   -H "Content-Type: application/json"
   -H "Authorization: Bearer sa-hdffds7&dhdhsdgffd" \
   -d '{"prompt": "The President went to Congress", \
        "max_tokens": 22}'
~~~~~~~~

Here the API token "sa-hdffds7&dhdhsdgffd" on line 4 is made up - that is not my API token. All of the OpenAPIs expect JSON data with query parameters. To use the completion API, we set values for **prompt** and **max_tokens**. The value of **max_tokens** is the requested number of returns words or tokens. We will look at several examples later.

In the file **src/openai_api/core.clj** we start with a helper function **openai-helper** that takes a string with the OpenAI API call arguments encoded as a **curl** command, calls the service, and then extracts the results from the returned JSON data:

{lang="clojure",linenos=on}
~~~~~~~~
(defn- openai-helper [body]
  (let [json-results
        (client/post
          "https://api.openai.com/v1/engines/davinci/completions"
          {:accept :json
           :headers
               {"Content-Type"  "application/json"
                "Authorization" 
                (str "Bearer " 
                     (System/getenv "OPENAI_KEY"))
               }
           :body   body
           })]
    ((first ((json/read-str (json-results :body)) "choices")) "text")))
~~~~~~~~

I convert JSON data to a Lisp list and reach into the nested results list for the generated text string on line 14.

The three example functions all use this **openai-helper** function. The first example function **completions** sets the parameters to complete a text fragment. You have probably seen examples of the OpenAI GPT-3 model writing stories, given a starting sentence. We are using the same model and functionality here:

{lang="clojure",linenos=on}
~~~~~~~~
(defn completions
  "Use the OpenAI API for text completions"
  [prompt-text max-tokens]
  (let
    [body
     (str
       "{\"prompt\": \"" prompt-text "\", \"max_tokens\": "
       (str max-tokens) "}")]
    (openai-helper body)))
~~~~~~~~

Note that the OpenAPI models are stochastic. When generating output words (or tokens), the model assigns probabilities to possible words to generate and samples a word using these probabilities. As a simple example, suppose given prompt text "it fell and", then the model could only generate three words, with probabilities for each word based on this prompt text:

- the 0.9
- that 0.1
- a 0.1

The model would *emit* the word **the** 90% of the time, the word **that** 10% of the time, or the word **a** 10% of the time. As a result, the model can generate different completion text for the same text prompt. Let's look at some examples using the same prompt text. Notice the stochastic nature of the returned results:

{lang="clojure",linenos=on}
~~~~~~~~
$ lein repl
openai-api.core=> (openai-api.core/completions "He walked to the river" 24)
" and breathed in the new day, looking out to the lake where the Mire was displacing the Wold by its"
openai-api.core=> (openai-api.core/completions "He walked to the river" 24)
". He waded in, not caring about his expensive suit pants. He was going to do this right, even if"
openai-api.core=> (openai-api.core/completions "He walked to the river" 24)
" every day. The salty air puffed through their pores. He had enjoyed her company. Maybe he did need a companion"
~~~~~~~~

The function **summarize** is very similar to the function **completions** except the JSON data passed to the API has a few additional parameters that let the API know that we want a text summary:

- presence_penalty - penalize words found in the original text (we set this to zero)
- temperature - higher values the randomness used to select output tokens. If you set this to zero, then the same prompt text will always yield the same results (I never use a zero value).
- top_p - also affects randomness. All examples I have seen use a value of 1.
- frequency_penalty - penalize using the same words repeatedly (I usually set this to zero, but you should experiment with different values)

When summarizing text, try varying the number of generated tokens to get shorter or longer summaries; in the following examples we ask for 24, 90, and 150 output tokens:

{lang="clojure",linenos=on}
~~~~~~~~
openai-api.core=> (def some-text
             #_=>   "Jupiter is the fifth planet from the Sun and the largest in the Solar System. It is a gas giant with a mass one-thousandth that of the Sun, but two-and-a-half times that of all the other planets in the Solar System combined. Jupiter is one of the brightest objects visible to the naked eye in the night sky, and has been known to ancient civilizations since before recorded history. It is named after the Roman god Jupiter.[19] When viewed from Earth, Jupiter can be bright enough for its reflected light to cast visible shadows,[20] and is on average the third-brightest natural object in the night sky after the Moon and Venus.")
#'openai-api.core/some-text

openai-api.core=> (openai-api.core/summarize some-text 24)
" The planet is often referred to by its Latin name, Jupiter, or by its Greek name, Zeus.\n\nJ"
openai-api.core=> (openai-api.core/summarize some-text 24)
"\n\nJupiter is a gas giant with a radius 11 times that of Earth. It is a ball of mostly hydrogen"

openai-api.core=> (openai-api.core/summarize some-text openai-api.core=> (openai-api.core/summarize some-text 90)
"\n\nJupiter is classified as a gas giant along with Saturn, Uranus, and Neptune. Jupiter is composed primarily of gaseous and liquid matter.[21] It is the largest of the four giant planets in the Solar System and hence its largest planet. It has a diameter of 142,984 km at its equator, which is 0.11 times the diameter of Earth. Jupiter is a gas giant because the mass of the planet"

openai-api.core=> (openai-api.core/summarize some-text 150)
"\n\nJupiter is a gas giant, consisting mostly of hydrogen and helium. It is a giant because it is over two-and-a-half times as massive as the next-largest planet, Saturn. Jupiter is classified as a gas giant along with Saturn, Uranus, and Neptune. Jupiter is a planet because it is a body that has a solid core, a rocky mantle and a gaseous outer atmosphere. Jupiter is the largest planet in the Solar System, with a radius twice that of Earth and over 300 times that of the Earth's Moon. It is more massive than all the other planets in our Solar System combined.\n\nJupiter has a faint ring system, which was discovered in 1979 when the two Voyager spacecraft passed"
openai-api.core=> 
~~~~~~~~

The function **anser-question** is very similar to the function **summarize** except the JSON data passed to the API has one additional parameter that let the API know that we want a question answered:

- stop - The OpenAPI examples use the value: **[\n]**, which is what I use here.

We also need to prepend the string "nQ: " to the prompt text.

Additionally, the model returns a series of answers with the string "nQ:" acting as a delimiter between the answers. 

{lang="clojure",linenos=on}
~~~~~~~~
    (let [ind (clojure.string/index-of results "nQ:")]
      (if (nil? ind)
        results
        (subs results 0 ind))
~~~~~~~~

I strongly urge you to add a debug printout to the question answering code to print the full answer before we check for the delimiter string. For some questions, the OpenAI APIs generate a series of answers that increase in generality. In the example code we just take the most specific answer.

Let's look at a few question answering examples and we will discuss possible problems and workarounds. The first two examples ask the same question and get back different, but reasonable answers. The third example asks a general question. The GPT-3 model is trained using a massive amount of text from the web which is why it can generate reasonable answers:

{lang="clojure",linenos=on}
~~~~~~~~
openai-api.core=> (openai-api.core/answer-question "Where is the Valley of Kings?" 60)
" It's in Egypt."

openai-api.core=> (openai-api.core/answer-question "Where is the Valley of Kings?" 60)
" It is located in the Valley of the Kings, near Luxor."

openai-api.core=> (openai-api.core/answer-question "Who is Bill Clinton's wife?" 60)
" Hillary Clinton."

openai-api.core=> (openai-api.core/answer-question "What rivers are in Arizona?" 250)
" The Colorado, Verde, Salt, Gila, San Pedro, Little Colorado, and the San Francisco."
~~~~~~~~

In addition to reading the beta OpenAPI API documentation you might want to read general material on the use of OpenAI's GPT-3 model.
