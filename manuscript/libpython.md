# Python/Clojure Interoperation Using The libpython-clj Library

In the last chapter we used the Java OpenNLP library for natural language processing (NLP). Here we take an alternative approach of using the **libpython-clj** library to access the [spaCy](https://spacy.io) NLP library implemented in Python (and the embedded compiled code written in FORTRAN and C/C++). The **libpython-clj** library can also be used to tap into the wealth of deep learning and numerical computation libraries written in Python. See the file **INSTALL_MLW.txt** for project dependencies.

This example also uses the [Hugging Face Transformer models](https://huggingface.co/transformers/) for NLP question answering.

To get started using **libpython-clj** I want to point you towards two resources that you will want to familiarize yourself with:

- [libpython-clj GitHub repository](https://github.com/clj-python/libpython-clj)
- [Carin Meier's libpython-clj examples GitHub repository](https://github.com/gigasquid/libpython-clj-examples)

I suggest bookmarking the **libpython-clj** GitHub repository for reference and treat Carin Meier's **libpython-clj** examples as your main source for using a wide variety of Python libraries with **libpython-clj**.

## Using spaCy for Natural Language Processing

Let's start by looking at test code and output for this example (we will implement the code later):

{linenos=on}
~~~~~~~~
(text->entities test-text)
(text->tokens-and-pos test-text)
(text->pos test-text)
(text->tokens test-text)
~~~~~~~~

I reformatted the following output to fit the page width:

{lang=text,linenos=on}
~~~~~~~~
(PERSON ORG GPE DATE MONEY)
([John PROPN] [Smith PROPN] [worked VERB] [for ADP]
 [IBM PROPN] [in ADP] [Mexico PROPN]
 [last ADJ] [year NOUN] [and CCONJ] [earned VERB]
 [$ SYM] [1 NUM] [million NUM]
 [in ADP] [salary NOUN] [and CCONJ] [bonuses NOUN]
 [. PUNCT])
(PROPN PROPN VERB ADP PROPN ADP PROPN ADJ NOUN CCONJ
 VERB SYM NUM NUM ADP NOUN CCONJ NOUN PUNCT)
(John Smith worked for IBM in Mexico last year and
 earned $ 1 million in salary and bonuses .)
~~~~~~~~


  
## Using the Hugging Face Transformer Models For Question Answering

Deep learning NLP libraries like BERT and Transformers have changed the landscape for applications like translation and question answering. Here we use a Hugging Face Transformer Model to answer questions when provided with a block of text that contains the answer to the questions. Before looking at the code for this example, let's look at how it is used:

{lang=clojure",linenos=on}
~~~~~~~~
(def
  context
  "Since last year, Bill lives in Seattle. He likes to skateboard.")
(qa "where does Bill call home?" context)
(qa "what does Bill enjoy?" context)
~~~~~~~~

The generated output is:

{lang=json,linenos=on}
~~~~~~~~
{"score": 0.9626545906066895, "start": 31, "end": 38,
 "answer": "Seattle"}
{"score": 0.9084932804107666, "start": 52, "end": 62,
 "answer": "skateboard"}
~~~~~~~~

Nice results that show the power of using publicly available pre-trained deep learning models. Usually the context text block that contains the answers will be a few paragraphs of text. This is a very simple example.

## Using libpython-clj With The spaCy and Urging Face Transformer Python NLP Libraries

I combined both examples we just saw in one project. Let's start with the project file which is largely copied from Carin Meier's **libpython-clj** examples GitHub repository.


{lang="clojure",linenos=on}
~~~~~~~~
(defproject python_interop_deeplearning "0.1.0-SNAPSHOT"
  :description
  "Example using libpython-clj with spaCy"
  :url
  "https://github.com/gigasquid/libpython-clj-examples"
  :license
  {:name
   "EPL-2.0 OR GPL-2+ WITH Classpath-exception-2.0"
   :url "https://www.eclipse.org/legal/epl-2.0/"}
  :jvm-opts ["-Djdk.attach.allowAttachSelf"
             "-XX:+UnlockDiagnosticVMOptions"
             "-XX:+DebugNonSafepoints"]
    :plugins [[lein-tools-deps "0.4.5"]]
    :middleware
    [lein-tools-deps.plugin/resolve-dependencies-with-deps-edn]
    :lein-tools-deps/config {:config-files [:project]
                             :resolve-aliases []}

    :mvn/repos
    {"central" {:url "https://repo1.maven.org/maven2/"}
     "clojars" {:url "https://clojars.org/repo"}}

   :dependencies [[org.clojure/clojure "1.10.1"]
                 [clj-python/libpython-clj "1.37"]]
  :main ^:skip-aot nlp-libpython-spacy.core
  :target-path "target/%s"
  :profiles
  {:uberjar
    {:aot :all
          :jvm-opts
          ["-Dclojure.compiler.direct-linking=true"]}})
~~~~~~~~

In the following example we call directly into the **spaCy** library and we use a separate Python file **QA.py** to wrap the Hugging Face Transformer mode. This provides you, dear reader, with examples of both techniques I use (direct calls and using wrappers). We will list the file **QA.py** later.

In lines 1-8 of the example program we set up the Clojure namespace and define  accessor functions for interacting with Python. Before we jump into the example code listing, I want to show you a few things in a REPL:

{linenos=on}
~~~~~~~~
$ lein repl
nlp-libpython-spacy.core=> (nlp "The cat ran")
The cat ran
nlp-libpython-spacy.core=> (type (nlp "The cat ran"))
:pyobject
~~~~~~~~

The output on line 3 prints as a string but is really a Python object (a **spaCy** **Document**) returned as a value from the wrapped **nlp** function. The Python **dir** function prints all methods and attributes of a Python object. Here, I only show four out of the  eighty eight methods and attributes on a **spaCy** **Document** object:

{linenos=on}
~~~~~~~~
nlp-libpython-spacy.core=> (py/dir (nlp "The cat ran"))
["__iter__" "lang" "sentiment" "text" "to_json" ...]
~~~~~~~~

The method **__iter__** is a Python iterator and allows Clojure code using **libpython-clj** to iterate through a Python collection using the Clojure **map** function as we will see in the example program. The **text** method returns a string representation of a **spaCy** **Document** object and we will also use **text** to get the print-representation of **spaCy** **Token** objects.

Here we call two of the wrapper functions in our example:

{linenos=on}
~~~~~~~~
nlp-libpython-spacy.core=> (text->tokens "the cat ran")
("the" "cat" "ran")
nlp-libpython-spacy.core=> (text->tokens-and-pos "the cat ran")
(["the" "DET"] ["cat" "NOUN"] ["ran" "VERB"])
~~~~~~~~

Here is a listing of the example. The Python file **QA.py** loaded in line 9 will be seen later.

{lang="clojure",linenos=on}
~~~~~~~~
(ns nlp-libpython-spacy.core
    (:require [libpython-clj.require :refer
                                     [require-python]]
              [libpython-clj.python :as py
                                    :refer
                                    [py. py.-]]))

(require-python '[spacy :as sp])
(require-python '[QA :as qa]) ;; loads the file QA.py

(def nlp (sp/load "en_core_web_sm"))

(def test-text "John Smith worked for IBM in Mexico last year and earned $1 million in salary and bonuses.")

(defn text->tokens [text]
  (map (fn [token] (py.- token text))
       (nlp text)))

(defn text->pos [text]
  (map (fn [token] (py.- token pos_))
       (nlp text)))
  
(defn text->tokens-and-pos [text]
  (map (fn [token] [(py.- token text) (py.- token pos_)])
       (nlp text)))

(defn text->entities [text]
  (map (fn [entity] (py.- entity label_))
       (py.- (nlp text) ents)))

(defn qa
  "Use Transformer model for question answering"
  [question context-text]
  (qa/answer question context-text)) ;; prints to stdout and returns a map

(defn -main
  [& _]
  (println (text->entities test-text))
  (println (text->tokens-and-pos test-text))
  (println (text->pos test-text))
  (println (text->tokens test-text))
  (qa "where does Bill call home?"
      "Since last year, Bill lives in Seattle. He likes to skateboard.")
  (qa "what does Bill enjoy?"
      "Since last year, Bill lives in Seattle. He likes to skateboard."))
~~~~~~~~

If you **lein run** to run the test **-main** function in lines ZZZ-ZZZ in the last listing, you will see (with some output removed here for brevity and reformatted):

{lang="text",linenos=on}
~~~~~~~~
(PERSON ORG GPE DATE MONEY)
([John PROPN] [Smith PROPN] [worked VERB] [for ADP]
 [IBM PROPN] [in ADP] [Mexico PROPN] [last ADJ]
 [year NOUN] [and CCONJ] [earned VERB] [$ SYM]
 [1 NUM] [million NUM] [in ADP] [salary NOUN]
 [and CCONJ] [bonuses NOUN] [. PUNCT])
(PROPN PROPN VERB ADP PROPN ADP PROPN ADJ NOUN CCONJ
 VERB SYM NUM NUM ADP NOUN CCONJ NOUN PUNCT)
(John Smith worked for IBM in Mexico last year and earned
 $ 1 million in salary and bonuses .)
{'score': 0.9626545906066895, 'start': 31, 'end': 38,
 'answer': 'Seattle'}           
{'score': 0.9084932804107666, 'start': 52, 'end': 62,
 'answer': 'skateboard'}
~~~~~~~~

This example shows how to load the local Python file **QA.py** and call a function defined in the file:

{lang="python",linenos=on}
~~~~~~~~
from transformers import pipeline

qa = pipeline(
    "question-answering",
    model="NeuML/bert-small-cord19-squad2",
    tokenizer="NeuML/bert-small-cord19qa"
)

def answer (query_text,context_text):
  answer = qa({
                "question": query_text,
                "context": context_text
               })
  print(answer)
  return answer
~~~~~~~~

Lines 5-6 specify names for pre-trained model files that we use. In the example repository, the file **INSTALL_MLW.txt** shows how I installed the dependencies for this example on a Google Cloud Platform VPS. While I sometimes use Docker for projects with custom dependencies that I don't want to install on my laptop, I often prefer using a VPS that I can start and stop when I need it.

Writing a Python wrapper that is called from your Clojure code is a good approach if you, for example, had existing Python code that uses TensorFlow or PyTorch, or was a complete application written in Python that you wanted to use from Clojure. While it is possible to do everything in Clojure calling directly into Python libraries it is sometimes simpler to write Python wrappers that define top level functions that you need in your Clojure project.

