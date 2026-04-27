# Simple RDF Datastore and SPARQL Query Processor — Example for "Practical Artificial Intelligence Programming With Clojure"

> **Book Chapter:** [Simple RDF Datastore and Partial SPARQL Query Processor](https://leanpub.com/read/clojureai/leanpub-auto-simple-rdf-datastore-and-partial-sparql-query-processor) — free to read online.

This project implements a lightweight, pure-Clojure RDF triple store with a partial SPARQL query processor. It stores subject–predicate–object triples in an atom, supports variable bindings (e.g. `?x`), and can execute simple `SELECT … WHERE` queries — all without external dependencies.

## Prerequisites

| Tool | Version |
|------|---------|
| Java | 11+ |
| [Leiningen](https://leiningen.org) | 2.9+ |

## Run

    lein run

Or start a REPL:

    lein repl

## Book and License

Book URI: https://leanpub.com/clojureai — you can read the book for free online at https://leanpub.com/clojureai/read

Copyright © 2021-2026 Mark Watson. All rights reserved.

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.
