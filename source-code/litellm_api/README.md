# LiteLLM — Example for "Practical Artificial Intelligence Programming With Clojure"

> **Book Chapter:** [Using LiteLLM with Multiple LLM Providers](https://leanpub.com/read/clojureai/leanpub-auto-using-the-openai-apis) — free to read online.

This example uses the [litellm-clj](https://github.com/unravel-team/litellm-clj) library by the Unravel team to call multiple LLM providers (OpenAI, Gemini, Ollama, Anthropic, and others) through a single unified router API. You register named model configs and then call `router/completion` — the library handles provider-specific HTTP details.

## Prerequisites

| Tool | Version |
|------|---------|
| Java | 11+ |
| [Leiningen](https://leiningen.org) | 2.9+ |
| `OPENAI_API_KEY` | For OpenAI tests |
| `GOOGLE_API_KEY` | For Gemini tests |

Set your API keys before running:

    export OPENAI_API_KEY=your_key_here
    export GOOGLE_API_KEY=your_key_here

## Run

    lein test

## Book and License

Book URI: https://leanpub.com/clojureai — you can read the book for free online at https://leanpub.com/clojureai/read

Copyright © 2021-2026 Mark Watson. All rights reserved.

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.
