# Moonshot Kimi — Example for "Practical Artificial Intelligence Programming With Clojure"

> **Book Chapter:** [Using Moonshot's Kimi Model](https://leanpub.com/read/clojureai/leanpub-auto-using-moonshots-kimi-k2-model-with-built-in-websearch-tool-support) — free to read online.

This example calls the [Moonshot AI](https://www.moonshot.ai/) Kimi K2 chat completions API from Clojure using `clj-http`. Kimi K2 is notable for its built-in web-search tool support — the model can autonomously search the web to ground its answers.

## Prerequisites

| Tool | Version |
|------|---------|
| Java | 11+ |
| [Leiningen](https://leiningen.org) | 2.9+ |
| `MOONSHOT_API_KEY` | [Get one here](https://platform.moonshot.ai/) |

Set your API key before running:

    export MOONSHOT_API_KEY=your_key_here

## Run

    lein test

## Book and License

Book URI: https://leanpub.com/clojureai — you can read the book for free online at https://leanpub.com/clojureai/read

Copyright © 2021-2026 Mark Watson. All rights reserved.

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.
