# Bosquet LLM Library — Example for "Practical Artificial Intelligence Programming With Clojure"

> **Note:** This example is a work-in-progress and is not yet included in the book.

This directory experiments with [Bosquet](https://github.com/zmedelis/bosquet), a Clojure library for composing LLM prompts with templates and chains. Bosquet supports multiple backends including OpenAI and local Ollama models.

Currently configured to use Bosquet's defaults:

- **Model:** mistral-small
- **Hosting:** Local Ollama

## Prerequisites

| Tool | Version |
|------|---------|
| Java | 11+ |
| [Clojure CLI](https://clojure.org/guides/install_clojure) | Latest |
| [Ollama](https://ollama.ai) | Latest (or set `OPENAI_API_KEY` for OpenAI) |

## Run

    clj -X:test

## Book and License

Book URI: https://leanpub.com/clojureai — you can read the book for free online at https://leanpub.com/clojureai/read

Copyright © 2021-2026 Mark Watson. All rights reserved.

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.
