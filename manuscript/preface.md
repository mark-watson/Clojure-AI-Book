# Cover Material, Copyright, and License

Copyright 2020-2025 Mark Watson. All rights reserved. This book may be shared using the Creative Commons "share and share alike, no modifications, no commercial reuse" license.

This eBook will be updated occasionally so please check the [leanpub.com web page for this book](https://leanpub.com/clojureai) periodically for updates.

Please visit the [author's website](http://markwatson.com).

# Preface

I have been developing commercial Artificial Intelligence (AI) tools and applications since the 1980s and I usually use the Lisp languages Common Lisp, Clojure, Racket Scheme, and Gambit Scheme. The exception to my Lisp language preferences is that I use Python for my deep learning work. This book contains code that I wrote for myself and I am wrapping it in a book in the hopes that my code and this book will also be useful to you, dear reader.

If you read my eBooks free online then please consider tipping me [https://markwatson.com/#tip](https://markwatson.com/#tip).

The latest updates to this book (August and December 2025) features more Large Language Models (LLMs) examples for Gemini, Gemini via the Gogle Java Gemini SDK, and the universal LLM interface library LiteLLM for accessing many models from many providers.

I removed the chapter on Clojure/Python interoperation because of reported difficulties in configuring a Linux system to get the examples working. I copied the entire text for this deleted chapter to the README file [https://github.com/mark-watson/Clojure-AI-Book-Code/nlp_libpython](https://github.com/mark-watson/Clojure-AI-Book-Code/tree/main/nlp_libpython) if you would like to use this material.

{width: "60%"}
![Mark Watson](images/Mark.png)

I wrote this book for both professional programmers and home hobbyists who already know how to program in Clojure and who want to learn practical AI programming and information processing techniques. I have tried to make this an enjoyable book to work through. In the style of a “cook book,” the chapters can be studied in any order. 

This book uses two of the examples in my Java AI book that is also available to read free online or for purchase at [Leanpub.com](https://leanpub.com/javaai). I replicate these two bits of Java code in the GitHub repository:

[https://github.com/mark-watson/Clojure-AI-Book-Code](https://github.com/mark-watson/Clojure-AI-Book-Code)

Git pull requests with code improvements will be appreciated by me and the readers of this book.

## Requests from the Author

This book will always be available to read free online at [https://leanpub.com/clojureai/read](https://leanpub.com/clojureai/read).

That said, I appreciate it when readers purchase my books because the income enables me to spend more time writing.

### Hire the Author as a Consultant

I am available for short consulting projects. Please see [https://markwatson.com](https://markwatson.com).


## Clojure, With Some Java and Python

I like Common Lisp slightly more than Clojure, even though Clojure is a beautifully designed modern language and Common Lisp is ancient and has defects. Then why do I use Clojure? The Java ecosystem is huge and Clojure takes full advantage of Java interoperability, its elegant collections data types, and inline JSON literals. Just as I sometimes need access to the rich Java ecosystem I also need Python libraries for some of my projects. Here we will use the **libpython-clj** library for that. I also like the language **Hy** that has a Clojure-like syntax and wraps the Python language. If you use Python then my book [A Lisp Programmer Living in Python-Land: The Hy Programming Language](https://leanpub.com/hy-lisp-python) might be of interest.

Using the Java ecosystem is an important aspect of Clojure development and in the few cases where I use Java libraries from my Java AI book, my Clojure examples illustrate how to convert Clojure **seq** data to Java arrays, handle returned Java data values, etc.

## Personal Artificial Intelligence Journey: or, Life as a Lisp Developer

I have been interested in AI since reading Bertram Raphael’s excellent book *Thinking Computer: Mind Inside Matter* in the early 1980s. I have also had the good fortune to work on many interesting AI projects including the development of commercial expert system tools for the Xerox LISP machines and the Apple Macintosh, development of commercial neural network tools, application of natural language and expert systems technology, medical information systems, application of AI technologies to Nintendo and PC video games, and the application of AI technologies to the financial markets. I have also applied statistical natural language processing techniques to analyzing social media data from Twitter and Facebook. I worked at Google on their Knowledge Graph and I managed a deep learning team at Capital One where I was awarded 55 US patents. In recent years most of my work has been centered around creating deep learning models for specific applications and the use of Large Language Models for Natural Language Processing (NLP) and extracting semantic information from text.

I enjoy AI programming, and hopefully this enthusiasm will also infect you, dear reader.

## Notes for Setting Up Clojure, Emacs, and Cider

Assuming that you have Java JDK version 17 or greater installer install the Clojure toolchain on macOS using:

```
brew install clojure/tools/clojure
```

### Emacs packages

Install from MELPA:
- cider (nREPL client, jack-in, debugger)
- clojure-mode
- Optional: flycheck + flycheck-clj-kondo (linting), or use clojure-lsp if you prefer LSP. 
	
	Here is a snipper of Elisp code you can add to your ~/.emacs file:
	
```
(use-package clojure-mode :ensure t)
(use-package cider :ensure t :hook (clojure-mode . cider-mode))
(use-package flycheck :ensure t :init (global-flycheck-mode))
(use-package flycheck-clj-kondo :ensure t)
```

If you are using Emacs to edit any example program for this book you can start Cider using **M-x cider-jack-in** to start a nREPL.

Commonly used Cider commands:

- Eval defun: C-M-x
- Eval region: C-c C-r
- Jump to def / back: M-. / M-,
- Docs / source: C-c C-d C-d, C-c M-.


## Acknowledgements

I produced the manuscript for this book using the [leanpub.com](http://leanpub.com) publishing system and I recommend leanpub.com to other authors.


**Editor: Carol Watson**

Thanks to Alex Ott who rewrote a few of the example programs with a better Clojure style.

Thanks to the following people who found typos in this and earlier book editions: Roger Erens

