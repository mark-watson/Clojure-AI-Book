# Python/Clojure Interop with spaCy and Transformers — Example for "Practical Artificial Intelligence Programming With Clojure"

> **Note:** This chapter was removed from the book due to complex Linux/Python setup requirements. The full chapter text is preserved below.

This project demonstrates calling Python NLP libraries from Clojure using [libpython-clj](https://github.com/clj-python/libpython-clj). It contains three demos:

1. **spaCy NLP** — tokenization, POS tagging, and named-entity recognition via the spaCy library
2. **Hugging Face Transformers** — question answering using a pre-trained BERT model
3. **Combined spaCy + Transformers + Knowledge Graph** — extracts entities with spaCy, fetches context text from DBPedia via SPARQL, and answers questions with the Transformer model

## Prerequisites

| Tool | Notes |
|------|-------|
| Linux / x86 | ARM Macs may have difficulties with libpython-clj |
| Python 3.8+ | With `spacy` and `transformers` installed |
| Java 11+ | |
| [Leiningen](https://leiningen.org) | 2.9+ |

See `INSTALL_MLW.txt` for detailed GCP VPS setup notes. A Docker container also works well.

Install the Python dependencies:

    pip install spacy transformers torch
    python -m spacy download en_core_web_sm

## Run

    lein run

Or use the REPL interactively:

    lein repl

## Book and License

Book URI: https://leanpub.com/clojureai — you can read the book for free online at https://leanpub.com/clojureai/read

Copyright © 2021-2026 Mark Watson. All rights reserved.

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

---

# Full Chapter Text (archived)

The following is the full chapter text that was removed from the book.

## Using spaCy for Natural Language Processing

**spaCy** is a great library that is likely all you need for processing text and NLP. The **libpython-clj** library gives us a way to access spaCy directly from Clojure.

Let's start by looking at example code in a REPL session:

```
$ lein repl

nlp-libpython-spacy.core=> (text->entities test-text)
(["John Smith" "PERSON"] ["IBM" "ORG"] ["Mexico" "GPE"]
 ["last year" "DATE"] ["$1 million" "MONEY"])

nlp-libpython-spacy.core=> (text->tokens-and-pos test-text)
(["John" "PROPN"] ["Smith" "PROPN"] ["worked" "VERB"]
 ["for" "ADP"] ["IBM" "PROPN"] ["in" "ADP"]
 ["Mexico" "PROPN"] ["last" "ADJ"] ["year" "NOUN"]
 ["and" "CCONJ"] ["earned" "VERB"] ["$" "SYM"]
 ["1" "NUM"] ["million" "NUM"] ["in" "ADP"]
 ["salary" "NOUN"] ["and" "CCONJ"] ["bonuses" "NOUN"]
 ["." "PUNCT"])
```

## Using Hugging Face Transformers for Question Answering

```clojure
nlp-libpython-spacy.core=> (qa "where does Bill call home?"
                               "Since last year, Bill lives in Seattle.")
{'score': 0.96, 'answer': 'Seattle'}

nlp-libpython-spacy.core=> (qa "what does Bill enjoy?"
                               "Since last year, Bill lives in Seattle. He likes to skateboard.")
{'score': 0.91, 'answer': 'skateboard'}
```

## Combined spaCy + Transformers + Knowledge Graph

```clojure
nlp-libpython-spacy.core=> (spacy-qa-demo "what is the population of Paris?")
{'score': 0.90, 'answer': '2,150,271'}

nlp-libpython-spacy.core=> (spacy-qa-demo "where does Bill Gates Work?")
{'score': 0.31, 'answer': 'Microsoft'}
```