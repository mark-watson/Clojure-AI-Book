# Natural Language Processing Using OpenNLP — Example for "Practical Artificial Intelligence Programming With Clojure"

> **Book Chapter:** [Natural Language Processing Using OpenNLP](https://leanpub.com/read/clojureai/opennlp) — free to read online.

This example wraps the [Apache OpenNLP](https://opennlp.apache.org/) Java library for tokenization, part-of-speech tagging, and named-entity recognition from Clojure. OpenNLP uses pre-trained maximum-entropy models and runs entirely on-device with no API keys required.

## Prerequisites

| Tool | Version |
|------|---------|
| Java | 11+ |
| [Leiningen](https://leiningen.org) | 2.9+ |

## Run

    lein test

## Part-of-Speech Tag Reference

| Tag | Meaning |
|-----|---------|
| CC | Coordinating conjunction |
| CD | Cardinal number |
| DT | Determiner |
| EX | Existential *there* |
| FW | Foreign word |
| IN | Preposition / subordinating conjunction |
| JJ | Adjective |
| JJR | Adjective, comparative |
| JJS | Adjective, superlative |
| MD | Modal |
| NN | Noun, singular or mass |
| NNS | Noun, plural |
| NNP | Proper noun, singular |
| NNPS | Proper noun, plural |
| PRP | Personal pronoun |
| PRP$ | Possessive pronoun |
| RB | Adverb |
| RBR | Adverb, comparative |
| RBS | Adverb, superlative |
| VB | Verb, base form |
| VBD | Verb, past tense |
| VBG | Verb, gerund / present participle |
| VBN | Verb, past participle |
| VBP | Verb, non-3rd person singular present |
| VBZ | Verb, 3rd person singular present |

## Book and License

Book URI: https://leanpub.com/clojureai — you can read the book for free online at https://leanpub.com/clojureai/read

Copyright © 2021-2026 Mark Watson. All rights reserved.

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.
