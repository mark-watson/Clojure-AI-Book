# Preface


The latest edition of this book is always available for purchase at [https://leanpub.com/clojueai](https://leanpub.com/clojureai).  You can also download a free copy from [my website](https://markwatson.com/books). You are very welcome both to taking a free copy and also sharing! I offer the purchase option for readers who wish to directly support my work.

I have been developing commercial Artificial Intelligence (AI) tools and applications since the 1980s.

![Mark Watson](images/Mark.png "Mark Watson")

I wrote this book for both professional programmers and home hobbyists who already know how to program in Clojure and who want to learn practical AI programming and information processing techniques. I have tried to make this an enjoyable book to work through. In the style of a “cook book,” the chapters can be studied in any order. When an example depends on a library developed in a previous chapter this is stated clearly. For example, the chapter [Knowledge Graph Navigator](#kgn) develops a library with tests for exploring public Knowledge Graphs like DBPedia and WikiData. The following chapter [Knowledge Graph Navigator Web Application](#kgnui) uses the library in a Pedestal web application. Most chapters follow the same pattern: a motivation for learning a technique, some theory for the technique, and a Clojure example program that you can experiment with.

This book is not self contained since a few of the examples use Java libraries created in my Java AI book that is also available for purchase at [Leanpub.com](https://leanpub.com/clojureai) and as a free download from my personal web site.

The code for the example programs is available on **github**:

  [https://github.com/mark-watson/Clojure-AI-Book-Code](https://github.com/mark-watson/Clojure-AI-Book-Code)

My Clojure code in this book can be used under either or both the LGPL3 and Apache 2 licenses - choose whichever of these two licenses that works best for you. Git pull requests with code improvements will be appreciated by me and the readers of this book.

## Clojure, With Some Java

My goal is to introduce you to common AI techniques and to provide you with Clojure source code to save you some time and effort. Using the Java ecosystem is also an important aspect of Clojure development and in the few cases where I use Java libraries from my Java AI book, I will take some care in explaining how to effectively converting Clojure *seq* data to Java arrays, handling returned Java data values, etc.

I use two Java libraries: the Anomaly Detection library from my book [Practical Artificial Intelligence Programming With Java](https://leanpub.com/javaai) and the Apache Jena library for handling RDF data and making SPARQL queries. I wrote a Java Wrapper for Jena my Java AI book and we use that here also. I also use the Java DeepLearning4J library. The remaining examples in this book are all Clojure.

## My Background and My Specific Areas of AI Research

Even though I have worked almost exclusively in the fields of deep learning (at Capital One) and Knowledge Graphs (at Google and Olive AI) in the last nine years, I urge you, dear reader, to look at the field of AI as being far broader than machine learning and deep learning in particular. I will pay particular attention to Knowledge Graphs and underlying semantic web and linked data technology as an effective and modern approach to Knowledge Representation.

## Personal Artificial Intelligence Journey: or, Life as a Lisp Developer

I have been interested in AI since reading Bertram Raphael’s excellent book *Thinking Computer: Mind Inside Matter* in the early 1980s. I have also had the good fortune to work on many interesting AI projects including the development of commercial expert system tools for the Xerox LISP machines and the Apple Macintosh, development of commercial neural network tools, application of natural language and expert systems technology, medical information systems, application of AI technologies to Nintendo and PC video games, and the application of AI technologies to the financial markets. I have also applied statistical natural language processing techniques to analyzing social media data from Twitter and Facebook. I worked at Google on their Knowledge Graph and I managed a deep learning team at Capital One.

I enjoy AI programming, and hopefully this enthusiasm will also infect you, the reader.


## Acknowledgements

I process the manuscript for this book using the [leanpub.com](http://leanpub.com) publishing system and I recommend leanpub.com to other authors.


**Book Editor: Carol Watson**

Thanks to the following people who found typos in this and earlier book editions: none so far!
