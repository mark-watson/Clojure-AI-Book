# Python/Clojure Interop with spaCy and Transformers using 'uv' for Python environment management

Example for *Practical Artificial Intelligence Programming With Clojure*

This project demonstrates calling Python NLP libraries from Clojure using [libpython-clj](https://github.com/clj-python/libpython-clj). It showcases two complementary Python AI libraries working together through a single Clojure application.

## Dual Example: spaCy + Hugging Face Transformers

This project intentionally pairs **two** Python NLP libraries to illustrate different AI capabilities accessed from Clojure via `libpython-clj`:

| Library | Role | What it does |
|---------|------|-------------|
| **spaCy** | Linguistic analysis | Tokenization, part-of-speech tagging, named-entity recognition (NER). Fast, rule-based + statistical pipeline for understanding text structure. |
| **Hugging Face Transformers** | Deep learning inference | Extractive question answering using a pre-trained BERT model (`NeuML/bert-small-cord19-squad2`). Finds answer spans within context text. |

The two libraries are combined in `spacy-qa-demo`: spaCy extracts named entities from a question, DBPedia SPARQL queries fetch context text about those entities, and the Transformer model answers the question using that context.

## Prerequisites

| Tool | Notes |
|------|-------|
| [uv](https://docs.astral.sh/uv/) | Manages the Python venv and dependencies |
| Java 11+ | |
| [Leiningen](https://leiningen.org) | 2.9+ |

## Setup (macOS & Linux)

[uv](https://docs.astral.sh/uv/) manages the Python virtual environment and dependencies via the included `pyproject.toml`. The same steps work on both macOS and Linux.

```bash
# Install uv if you don't have it
curl -LsSf https://astral.sh/uv/install.sh | sh

# Install Python deps and download the spaCy model
uv sync
uv run python -m spacy download en_core_web_sm
```

## Run

```bash
# Run the full demo (spaCy NER + Transformers QA + Knowledge Graph)
uv run lein run

# Or use the REPL interactively
uv run lein repl

# Run the Python QA script standalone
uv run python QA.py
```

## Architecture

![Generated image](architecture.png)

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