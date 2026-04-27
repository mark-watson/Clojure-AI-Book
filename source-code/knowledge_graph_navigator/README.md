# Knowledge Graph Navigator — Example for "Practical Artificial Intelligence Programming With Clojure"

> **Book Chapter:** [Knowledge Graph Navigator](https://leanpub.com/read/clojureai/kgn) — free to read online.

This project automates collecting and linking information from SPARQL endpoints such as DBPedia and Wikidata. Given entity names, it queries public Knowledge Graphs via SPARQL, retrieves descriptive text and relationships, and assembles the results. It demonstrates the power of combining semantic web technologies with Clojure's data-processing strengths.

## Prerequisites

| Tool | Version |
|------|---------|
| Java | 11+ |
| [Leiningen](https://leiningen.org) | 2.9+ |

No API keys are required — this example queries public SPARQL endpoints.

## Run

Run the tests:

    lein test

Run the main demo:

    lein run

## Book and License

Book URI: https://leanpub.com/clojureai — you can read the book for free online at https://leanpub.com/clojureai/read

Copyright © 2021-2026 Mark Watson. All rights reserved.

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.
