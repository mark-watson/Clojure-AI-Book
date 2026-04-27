# Document Question Answering — Example for "Practical Artificial Intelligence Programming With Clojure"

> **Book Chapter:** [Question Answering Using OpenAI APIs and a Local Embeddings Vector Database](https://leanpub.com/read/clojureai/leanpub-auto-question-answering-using-openai-apis-and-a-local-embeddings-vector-database) — free to read online.

This project implements a Retrieval-Augmented Generation (RAG) pipeline in Clojure. It reads text files from a `data/` directory, splits them into chunks, generates OpenAI embeddings for each chunk, and stores them in a local vector database (SQLite). At query time it finds the most relevant chunks via dot-product similarity and sends them as context to the OpenAI completions API to answer your question.

Inspired by the Python LangChain and LlamaIndex ecosystems, but written from scratch in Clojure.

## Prerequisites

| Tool | Version |
|------|---------|
| Java | 11+ |
| [Leiningen](https://leiningen.org) | 2.9+ |
| `OPENAI_API_KEY` | [Get one here](https://platform.openai.com/api-keys) |

You must first install the local `openai_api` dependency:

    cd ../openai_api && lein install

## Run

    lein test

## Book and License

Book URI: https://leanpub.com/clojureai — you can read the book for free online at https://leanpub.com/clojureai/read

Copyright © 2023-2026 Mark Watson. All rights reserved.

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.
