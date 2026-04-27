# Google Gemini Java SDK — Example for "Practical Artificial Intelligence Programming With Clojure"

> **Book Chapter:** [Using the Google Gemini APIs](https://leanpub.com/read/clojureai/leanpub-auto-using-the-google-gemini-apis) — free to read online.

This example uses Google's official Java Gemini SDK (`com.google.genai`) from Clojure via Java interop. It demonstrates text generation and summarization. See also `../gemini_api` for an alternative approach using the REST API directly with `clj-http`.

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

## Book and License

Book URI: https://leanpub.com/clojureai — you can read the book for free online at https://leanpub.com/clojureai/read

Copyright © 2021-2026 Mark Watson. All rights reserved.

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.
