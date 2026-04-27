# Brave Search — Example for "Practical Artificial Intelligence Programming With Clojure"

> **Book Chapter:** [Using the Brave Search APIs](https://leanpub.com/read/clojureai/leanpub-auto-using-the-brave-search-apis) — free to read online.

This example shows how to call the [Brave Search API](https://brave.com/search/api/) from Clojure using `clj-http`. Given a query string, it returns a vector of `[title url description]` results. Brave Search is a privacy-focused alternative to Google for programmatic web search.

## Prerequisites

| Tool | Version |
|------|---------|
| Java | 11+ |
| [Leiningen](https://leiningen.org) | 2.9+ |
| `BRAVE_SEARCH_API_KEY` | [Get one here](https://brave.com/search/api/) |

Set your API key before running:

    export BRAVE_SEARCH_API_KEY=your_key_here

## Run

    lein test

## Book and License

Book URI: https://leanpub.com/clojureai — you can read the book for free online at https://leanpub.com/clojureai/read

Copyright © 2021-2026 Mark Watson. All rights reserved.

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.
