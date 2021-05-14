# Python/Clojure Interoperation Using The libpython-clj Library

In the last chapter we used the Java OpenNLP library for natural language processing (NLP). Here we take an alternative approach of using the **libpython-clj** library to access the [spaCy](https://spacy.io) NLP library implemented in Python (and the embedded compiled code written in FORTRAN and C/C++). The **libpython-clj** library can also be used to tap into the wealth of deep learning and numerical computation libraries written in Python.

This example also uses the [Hugging Face Transformer models](https://huggingface.co/transformers/) for NLP question answering.

To get started using **libpython-clj** I want to point you towards two resources that you will want to familiarize yourself with:

- [libpython-clj GitHub repository](https://github.com/clj-python/libpython-clj)
- [Carin Meier's python-clj examples GitHub repository](https://github.com/gigasquid/libpython-clj-examples)

I suggest bookmarking the **libpython-clj** GitHub repository for reference and treat Carin Meier's **libpython-clj** examples as your main source for using a wide variety of Python libraries with **libpython-clj**.

## Using spaCy for Natural Language Processing

TBD

An example (code we will implement later):

{linenos=on}
~~~~~~~~
(text->entities test-text)
(text->tokens-and-pos test-text)
(text->pos test-text)
(text->tokens test-text)
~~~~~~~~

I reformatted the following to fit the page width:

{lang=plain,linenos=on}
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

Before looking at the code for this example, let's look at how it is used:

{lang=clojure",linenos=on}
~~~~~~~~
(def
  context
  "Since last year, Bill lives in Seattle. He likes to skateboard.")
(qa "where does Bill call home?" context)
(qa "what does Bill enjoy?" context)
~~~~~~~~
      
{lang=json,linenos=on}
~~~~~~~~
The generated output is:
{"score": 0.9626545906066895, "start": 31, "end": 38,
 "answer": "Seattle"}
{"score": 0.9084932804107666, "start": 52, "end": 62,
 "answer": "skateboard"}
~~~~~~~~

Nice results that show the power of using publicly available pre-trained deep learning models.

## Using libpython-clj With The spaCy Python NLP Library

I combined both examples we just saw in one project.


TBD


{lang="clojure",linenos=on}
~~~~~~~~
(defproject python_interop_deeplearning "0.1.0-SNAPSHOT"
  :description
  "Example using libpython-clj with spaCy"
  :url "http://markwatson.com"
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

TBD ^


TBD: discuss:

Formatted for page width:

{lang="clojure",linenos=on}
~~~~~~~~
(ns nlp-libpython-spacy.core
    (:require [libpython-clj.require :refer
                                     [require-python]]
              [libpython-clj.python :as py
                                    :refer
                                    [py. py.. py.-]]))

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

{lang="plain",linenos=on}
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

This example shows how to load the local Python file **QA.py** and call a function defined in the file. This is a good approach if you, for example, had existing Python code that uses TensorFlow or PyTorch, or was a complete application written in Python that you wanted to use from Clojure. While it is possible to do everything in Clojure calling directly into Python libraries it is simpler to write simple Python wrappers.

