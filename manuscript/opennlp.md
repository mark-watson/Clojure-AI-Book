# Natural Language Processing Using OpenNLP {#opennlp}

Here we use the [Apache OpenNLP project](https://opennlp.apache.org). OpenNLP has pre-trained models for tokenization, sentence segmentation, part-of-speech tagging, named entity extraction, chunking, parsing, and coreference resolution. As we see later OpenNLP has tools to also build our own models. OpenNLP is written in Java

I have worked in the field of Natural Language Processing (NLP) since the early 1980s. Many more people are now interested in the field of NLP and the techniques have changed drastically in the last decade.

Currently, OpenNLP has support for Danish, German, English, Spanish, Portuguese, and Swedish. I include in the github repository some trained models for English in the directory **models**.


## Using the Clojure and Java Wrappers for OpenNLP

I won't list the simple Java wrapper code in the directory **src-java** here. You might want to open the files **NLP.java** and **Pair.java** for reference:

{linenos=off}
~~~~~~~~
src-java
└── main
    ├── java
    │   └── com
    │       └── markwatson
    │           └── opennlp
    │               ├── NLP.java
    │               └── Pair.java
    └── resources
        └── log4j.xml
~~~~~~~~


The **project.clj** file shows the setup for incorporating Java code into a Clojure project:

{lang="clojure",linenos=off}
~~~~~~~~
(defproject opennlp-clj "0.1.0-SNAPSHOT"
  :description "Example using OpenNLP with Clojure"
  :url "http://markwatson.com"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :source-paths      ["src"]
  :java-source-paths ["src-java"]
  :javac-options     ["-target" "1.8" "-source" "1.8"]

  :dependencies [[org.clojure/clojure "1.10.1"]
                 ;[com.markwatson/opennlp "1.0-SNAPSHOT"] ;;from my Java AI book
                 [opennlp/tools "1.5.0"]
                 ]
  :repl-options {:init-ns opennlp-clj.core})
  ~~~~~~~~


The model files, including the categorization model you will learn to build later in this chapter, are found in the subdirectory **models**.

TBD:

{lang="clojue",linenos=off}
~~~~~~~~
(ns opennlp-clj.core
  (:import (com.markwatson.opennlp NLP)))

(defn sentence-splitter
  "tokenize entire sentences"
  [string-input]
  (seq (NLP/sentenceSplitter string-input)))

(defn tokenize->seq
  "tokenize words to Clojure seq"
  [string-input]
  (seq (NLP/tokenize string-input)))

(defn tokenize->java
  "tokenize words to Java array"
  [string-input]
  (NLP/tokenize string-input))

;; Word analysis:

(defn POS
  "part of speech"
  [java-token-array]
  (seq (NLP/POS java-token-array)))

;; Entities:

(defn company-names
  [java-token-array]
  (seq (NLP/companyNames java-token-array)))

(defn location-names
  [java-token-array]
  (seq (NLP/locationNames java-token-array)))

(defn person-names
  [java-token-array]
  (seq (NLP/personNames java-token-array)))
~~~~~~~~

Here I tokenize text into a Java array that is used to call the Java OpenNLP code (in the directory **src-java**). The first operation that you will usually start with for processing natural language text is breaking input text into individual words and sentences.

The test code for this project shows how to use these APIs:

{lang="clojure",linenos=off}
~~~~~~~~
(ns opennlp-clj.core-test
  (:require [clojure.test :as test])
  (:require [opennlp-clj.core :as onlp]))

(def
  test-text
  "The cat chased the mouse around the tree while Mary Smith (who works at IBM in San Francisco) watched.")

(test/deftest pos-test
  (test/testing "parts of speech"
    (let [token-java-array (onlp/tokenize->java test-text)
          token-clojure-seq (onlp/tokenize->seq test-text)
          words-pos (onlp/POS token-java-array)
          companies (onlp/company-names token-java-array)
          places (onlp/location-names token-java-array)
          people (onlp/person-names token-java-array)]
      (println "Input text:\n" test-text)
      (println "Tokens as Java array:\n" token-java-array)
      (println "Tokens as Clojure seq:\n" token-clojure-seq)
      (println "Part of speech tokens:\n" words-pos)
      (println "Companies:\n" companies)
      (println "Places:\n" places)
      (println "People:\n" people)
      (test/is (= (first words-pos) "DT")))))
~~~~~~~~

Here is the test output:

{linenos=off}
~~~~~~~~
Input text:
 The cat chased the mouse around the tree while Mary Smith (who works at IBM in San Francisco) watched.
Tokens as Java array:
 #object[[Ljava.lang.String; 0x2f04105 [Ljava.lang.String;@2f04105]
Tokens as Clojure seq:
 (The cat chased the mouse around the tree while Mary Smith ( who works at IBM in San Francisco ) watched .)
Part of speech tokens:
 (DT NN VBD DT NN IN DT NN IN NNP NNP -LRB- WP VBZ IN NNP IN NNP NNP -RRB- VBD .)
Companies:
 (IBM)
Places:
 (San Francisco)
People:
 (Mary Smith)
 ~~~~~~~~

**Note:** my Java AI book covers OpenNLP in more depth, including how to train your own classification models.
