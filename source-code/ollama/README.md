# Running LLMs Locally Using Ollama — Example for "Practical Artificial Intelligence Programming With Clojure"

> **Book Chapter:** [Running LLMs Locally Using Ollama](https://leanpub.com/read/clojureai/leanpub-auto-running-llms-locally-using-ollama) — free to read online.

This example shows how to call the [Ollama](https://ollama.ai) REST API from Clojure using `clj-http`. Ollama lets you run open-weight LLMs (Mistral, Llama, Qwen, etc.) entirely on your own hardware with no API key required. The library provides a simple `completions` function that sends a prompt and returns the model's response.

## Prerequisites

| Tool | Version |
|------|---------|
| Java | 11+ |
| [Leiningen](https://leiningen.org) | 2.9+ |
| [Ollama](https://ollama.ai) | Latest |

Install and pull a model (cached after first download):

    ollama pull mistral

## Run

Start the Ollama server in one terminal:

    ollama serve

Run the tests in another terminal:

    lein test

## Book and License

Book URI: https://leanpub.com/clojureai — you can read the book for free online at https://leanpub.com/clojureai/read

Copyright © 2021-2026 Mark Watson. All rights reserved.

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.
