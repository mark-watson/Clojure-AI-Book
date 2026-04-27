# Google Gemini REST API — Example for "Practical Artificial Intelligence Programming With Clojure"

> **Book Chapter:** [Using the Google Gemini APIs](https://leanpub.com/read/clojureai/leanpub-auto-using-the-google-gemini-apis) — free to read online.

This example calls the Google Gemini generative AI REST API from Clojure using `clj-http`. It provides functions for text generation (`generate-content`) and summarization (`summarize`). See also `../gemini_java_api` for an alternative approach using Google's Java SDK.

## Prerequisites

| Tool | Version |
|------|---------|
| Java | 11+ |
| [Leiningen](https://leiningen.org) | 2.9+ |
| `GOOGLE_API_KEY` | [Get one here](https://aistudio.google.com/app/apikey) |

Set your API key before running:

    export GOOGLE_API_KEY=your_key_here

## Run

    lein test

### REPL Usage

```clojure
(require '[gemini-api.core :as gemini])
(println (gemini/generate-content "Write a short poem about the ocean."))
(println (gemini/summarize "The quick brown fox jumps over the lazy dog."))
```

## Book and License

Book URI: https://leanpub.com/clojureai — you can read the book for free online at https://leanpub.com/clojureai/read

Copyright © 2021-2026 Mark Watson. All rights reserved.

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.
