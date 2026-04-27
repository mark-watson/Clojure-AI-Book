# OpenAI API — Example for "Practical Artificial Intelligence Programming With Clojure"

> **Book Chapter:** [Using the OpenAI APIs](https://leanpub.com/read/clojureai/leanpub-auto-using-the-openai-apis) — free to read online.

This project provides a Clojure client for the OpenAI APIs (GPT-4 / GPT-4o based). It wraps chat completions and embeddings endpoints using `clj-http` and `data.json`. Other examples in this repository (e.g. `docs_qa`) depend on this project as a local library.

## Prerequisites

| Tool | Version |
|------|---------|
| Java | 11+ |
| [Leiningen](https://leiningen.org) | 2.9+ |
| `OPENAI_API_KEY` | [Get one here](https://platform.openai.com/api-keys) |

Set your API key before running:

    export OPENAI_API_KEY=your_key_here

## Run

    lein test

To install as a local dependency (required by `docs_qa`):

    lein install

## Book and License

Book URI: https://leanpub.com/clojureai — you can read the book for free online at https://leanpub.com/clojureai/read

Copyright © 2021-2026 Mark Watson. All rights reserved.

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.
