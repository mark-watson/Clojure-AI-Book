---
name: clojure-ai-dev
description: Clojure AI programming tutorial, idioms, and API reference for all examples in Mark Watson's Clojure book "Practical Artificial Intelligence Programming With Clojure". Use this skill for writing Clojure code that accesses LLMs (Gemini, OpenAI, Ollama, Moonshot), SPARQL queries, NLP, web scraping, document Q&A, anomaly detection, and more.
---

# Notes for Using AGENT Skills with Clojure Book Examples

This document helps readers set up coding agent skills so that AI assistants can reference the Clojure APIs and patterns from this book when generating code.

## Source code for Gemini, OpenAI, Ollama, Moonshot, SPARQL, NLP, web scraping, document Q&A example code

```bash
git clone https://github.com/mark-watson/Clojure-AI-Book.git
```

All the Clojure examples are in the `source-code/` directory. Look in `~/GITHUB/Clojure-AI-Book/source-code/` for code to reuse.

---

## Clojure Idioms and Patterns Used in This Book

Clojure is a functional Lisp hosted on the JVM with immutable data structures and seamless Java interop.

### Project Structure

Most examples use **Leiningen** (`project.clj`); a few use **deps.edn**:

```bash
# Leiningen
cd source-code/<example_name>
lein run
lein repl

# deps.edn
cd source-code/<example_name>
clj -M -m <main-ns>
```

### Core Patterns

```clojure
;; Namespace declaration with requires
(ns my-app.core
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]))

;; Define a function
(defn greet [name]
  (str "Hello, " name "!"))

;; HTTP POST with JSON
(defn post-json [url body]
  (let [resp (client/post url {:body         (json/write-str body)
                                :content-type :json
                                :accept       :json})]
    (json/read-str (:body resp) :key-fn keyword)))

;; Environment variables
(def api-key (System/getenv "MY_API_KEY"))

;; Threading macros
(-> response :choices first :message :content)   ; thread-first
(->> items (filter valid?) (map transform))       ; thread-last

;; Dynamic vars for configurable defaults
(def ^:dynamic *default-model* "gemini-2.5-flash")
```

### Java Interop

```clojure
;; Import Java classes
(:import (com.google.genai Client)
         (org.jsoup Jsoup))

;; Call static methods
(System/getenv "KEY")

;; Create instances and call methods
(let [client (Client.)]
  (.generateContent (.models client) model prompt nil))

;; Builder pattern with threading
(-> (GeminiChatModel/builder)
    (.apiKey api-key)
    (.modelName "gemini-2.5-flash")
    (.build))
```

---

# Clojure Book APIs — Quick Reference

Knowledge of public APIs and usage patterns for the Clojure examples in Mark Watson's book *Practical Artificial Intelligence Programming With Clojure*.

---

## gemini_api

**Directory:** `gemini_api/`
**Build:** Leiningen
**Deps:** `clj-http`, `org.clojure/data.json`, `clojure.tools.logging`
**Env var:** `GOOGLE_API_KEY`
**Model:** `gemini-2.5-flash`

### API (`gemini-api.core`)

- `(generate-content prompt & {:keys [model-id system]})` — Generate text from a prompt. Supports optional `:model-id` and `:system` instruction.
- `(summarize text)` — Summarize text using Gemini.
- `(count-tokens prompt & {:keys [model-id]})` — Count tokens for a prompt.
- `(generate-with-search prompt & {:keys [model-id]})` — Generate text with Google Search grounding.
- `(generate-with-search-and-citations prompt & {:keys [model-id]})` — Returns `[text citations]` where citations is a seq of `{:title … :uri …}` maps.
- `(generate-with-tools prompt tool-defs dispatch-fns & {:keys [model-id system]})` — Function/tool calling. Returns `{:text "…"}` or `{:function-call {:name … :args …} :result <value>}`.
- `(make-chat-session)` — Create a new chat session atom.
- `(chat-turn session user-msg & {:keys [model-id]})` — Send a message in a chat session, returns reply text.
- `(chat-repl)` — Interactive REPL-based chat.

### Examples

```clojure
(require '[gemini-api.core :as gemini])

;; Simple generation
(gemini/generate-content "Explain recursion briefly.")

;; With system instruction
(gemini/generate-content "What is 2+2?" :system "You are a concise math tutor.")

;; Web search grounding
(gemini/generate-with-search "What movies are playing today?")

;; Web search with citations
(let [[answer sources] (gemini/generate-with-search-and-citations "Who won the Super Bowl in 2024?")]
  (println "Answer:" answer)
  (doseq [{:keys [title uri]} sources]
    (println " -" title uri)))

;; Tool calling
(defn get-weather [{:keys [location]}]
  (str "72°F and sunny in " location "."))

(def weather-tool
  {:name        "get_weather"
   :description "Returns current weather for a city."
   :parameters  {:type       "OBJECT"
                 :properties {:location {:type "STRING" :description "City name"}}
                 :required   ["location"]}})

(gemini/generate-with-tools
  "What is the weather in Paris?"
  [weather-tool]
  {"get_weather" get-weather})

;; Multi-turn chat
(let [session (gemini/make-chat-session)]
  (println (gemini/chat-turn session "Hello!"))
  (println (gemini/chat-turn session "Tell me more.")))
```

---

## gemini_java_api

**Directory:** `gemini_java_api/`
**Build:** Leiningen
**Deps:** `clj-http`, `org.clojure/data.json`, `com.google.genai/google-genai`
**Env var:** `GOOGLE_API_KEY`
**Model:** `gemini-3-flash-preview`

### API (`gemini-java-api.core`)

- `(generate-content prompt)` — Send prompt to Gemini via the Java SDK. Returns text.
- `(generate-content-with-search prompt)` — Generate with Google Search tool enabled. Returns text.
- `(summarize text)` — Summarize text.

### Example

```clojure
(require '[gemini-java-api.core :as gj])

(gj/generate-content "What is Clojure?")
(gj/generate-content-with-search "Latest Clojure news?")
```

---

## AgentScope_gemini

**Directory:** `AgentScope_gemini/`
**Build:** Leiningen
**Deps:** AgentScope Java SDK, `clj-http`
**Env var:** `GEMINI_API_KEY`
**Model:** `gemini-2.5-flash`

### API

- **`main.clj`** — Basic ReActAgent demo. Builds a `GeminiChatModel`, creates a `ReActAgent`, and sends a message.
- **`tool_use.clj`** — Tool-calling with five tools implemented via `reify AgentTool`:
  - `getWeather` — Stub weather lookup.
  - `list_dir` — List files in a directory.
  - `read_file` — Read a file with optional line limit.
  - `recursive-file-search` — Recursively search for files by name.
  - `math-eval` — Evaluate simple arithmetic expressions.

### Example

```clojure
;; Tool registration with AgentScope
(let [toolkit (doto (Toolkit.)
                (.registerAgentTool (weather-tool))
                (.registerAgentTool (list-dir-tool)))
      agent (-> (ReActAgent/builder)
                (.name "Assistant")
                (.model model)
                (.toolkit toolkit)
                (.build))
      response (-> (.call agent (-> (Msg/builder)
                                    (.textContent "What is the weather in Tokyo?")
                                    (.build)))
                   (.block))]
  (println (.getTextContent response)))
```

---

## AgentScope_ollama

**Directory:** `AgentScope_ollama/`
**Build:** Leiningen
**Deps:** AgentScope Java SDK
**Server:** Requires Ollama running locally

### API

- **`main.clj`** — ReActAgent using Ollama as the model backend.
- **`tool_use.clj`** — Tool-calling with Ollama-backed agents.

---

## openai_api

**Directory:** `openai_api/`
**Build:** Leiningen
**Deps:** `clj-http`, `org.clojure/data.json`
**Env var:** `OPENAI_API_KEY`
**Model:** `gpt-5.4-nano`

### API (`openai-api.core`)

- `(completions prompt)` — Send a chat completion request. Returns response text.
- `(summarize text)` — Summarize text using OpenAI.
- `(answer-question text)` — Answer a question using OpenAI.
- `(embeddings text)` — Get text embeddings using `text-embedding-ada-002`. Returns a float vector.
- `(dot-product a b)` — Compute dot product of two vectors.

### Example

```clojure
(require '[openai-api.core :as openai])

(openai/completions "Explain recursion briefly.")
(openai/summarize "Long text here...")
(openai/embeddings "Some text to embed")
```

---

## ollama

**Directory:** `ollama/`
**Build:** Leiningen
**Deps:** `clj-http`, `org.clojure/data.json`
**Server:** Requires Ollama running locally
**Model:** `mistral:v0.3` (default, configurable via `*default-model*`)

### API (`ollama-api.core`)

- `(completions prompt)` / `(completions prompt opts)` — Text completion via Ollama `/api/generate`. Returns response text.
- `(chat messages)` / `(chat messages opts)` — Chat via Ollama `/api/chat`. Messages are `[{:role "user" :content "…"} …]`. Returns response text.
- `(summarize text)` — Summarize text.
- `(answer-question text)` — Answer a question.

Dynamic vars for configuration:
- `*base-url*` — default `"http://localhost:11434"`
- `*default-model*` — default `"mistral:v0.3"`

### Example

```clojure
(require '[ollama-api.core :as ollama])

;; Simple completion
(ollama/completions "What is Clojure?")

;; Chat
(ollama/chat [{:role "user" :content "Tell me about functional programming."}])

;; Override model
(binding [ollama-api.core/*default-model* "llama3:8b"]
  (ollama/completions "What is Clojure?"))
```

---

## moonshot

**Directory:** `moonshot/`
**Build:** Leiningen
**Deps:** `clj-http`, `org.clojure/data.json`
**Env var:** `MOONSHOT_API_KEY`
**Model:** `kimi-k2-0711-preview`

### API (`moonshot-api.core`)

- `(completions prompt)` — Chat completion via Moonshot AI. Returns response text.
- `(search user-question)` — Completion with built-in web search (`$web_search` tool). Implements a multi-turn tool-call loop. Returns text.

### Example

```clojure
(require '[moonshot-api.core :as moonshot])

(moonshot/completions "What is Clojure?")
(moonshot/search "What are the latest Clojure releases?")
```

---

## brave_search

**Directory:** `brave_search/`
**Build:** Leiningen
**Deps:** `clj-http`, `cheshire`
**Env var:** `BRAVE_SEARCH_API_KEY`

### API (`brave-search.core`)

- `(brave-search query)` — Search the web via Brave Search API. Returns a vector of `[title url description]` triples.

### Example

```clojure
(require '[brave-search.core :as brave])

(doseq [[title url desc] (brave/brave-search "Clojure programming")]
  (println title url))
```

---

## docs_qa

**Directory:** `docs_qa/`
**Build:** Leiningen
**Deps:** `clojure.java.jdbc`, `openai-api` (internal)
**Env var:** `OPENAI_API_KEY`

### API

- **`vectordb.clj`** (`docs-qa.vectordb`):
  - `(document-texts-from_dir dir-path)` — Read all files from a directory.
  - `(document-texts-to-chunks strings)` — Break texts into 200-char chunks.
  - `embeddings-with-chunk-texts` — Delay containing `[embedding chunk-text]` pairs.

- **`core.clj`** (`docs-qa.core`):
  - `(best-vector-matches query)` — Find best-matching text chunks using OpenAI embeddings (dot product > 0.79).
  - `(answer-prompt prompt)` — Answer a question via OpenAI.
  - `(-main)` — Interactive RAG loop: loads `./data/*.txt`, chunks, embeds, then prompts for queries.

### Example

```bash
cd docs_qa
lein run
# Enter queries interactively against the indexed documents
```

---

## knowledge_graph_navigator

**Directory:** `knowledge_graph_navigator/`
**Build:** Leiningen
**Deps:** `clj-http`, `cemerick/url`, `org.clojure/data.csv`, `org.clojure/data.json`, `math.combinatorics`, Apache Jena
**Server:** Uses DBpedia SPARQL endpoint

### Key Modules

- **`sparql.clj`** (`knowledge-graph-navigator-clj.sparql`):
  - `(dbpedia sparql-query)` — Execute SPARQL on DBpedia via CSV endpoint.
  - `(graphdb graph-name sparql-query)` — Query a local GraphDB instance.
  - `(sparql-endpoint sparql-query)` — Auto-selects DBpedia, GraphDB, or Jena-cached endpoint.

- **`entities_by_name.clj`** (`knowledge-graph-navigator-clj.entities-by-name`):
  - `(dbpedia-get-entities-by-name name dbpedia-type)` — Look up entities on DBpedia by name and type URI.

- **`relationships.clj`** (`knowledge-graph-navigator-clj.relationships`):
  - `(entity-results->relationship-links pair-of-uris)` — Discover relationships between two entity URIs.

- **`kgn.clj`** (`knowledge-graph-navigator-clj.kgn`):
  - `(kgn input-entity-map)` — Top-level function. Input: `{:People ["name" …] :Organization ["name" …] :Place ["name" …]}`. Returns `{:entity-summaries … :discovered-relationships …}`.

### Example

```clojure
(require '[knowledge-graph-navigator-clj.kgn :as kgn])

(kgn/kgn {:People       ["Bill Gates" "Steve Jobs"]
           :Organization ["Microsoft"]
           :Place        ["California"]})
```

---

## semantic_web_jena

**Directory:** `semantic_web_jena/`
**Build:** Leiningen
**Deps:** Apache Jena, Apache Derby
**Server:** Uses DBpedia and Wikidata SPARQL endpoints

### API (`semantic-web-jena-clj.core`)

- `(load-rdf-file fpath)` — Load an RDF file into the Jena model.
- `(query sparql-query)` — Execute a SPARQL query on the local Jena model.
- `(query-remote remote-service sparql-query)` — Query a remote SPARQL endpoint.
- `(query-dbpedia sparql-query)` — Query DBpedia.
- `(query-wikidata sparql-query)` — Query Wikidata.

### Example

```clojure
(require '[semantic-web-jena-clj.core :as jena])

(jena/query-dbpedia "select * { ?s ?p ?o } limit 5")
(jena/query-wikidata "select * { ?s ?p ?o } limit 5")
```

---

## simple_rdf_sparql

**Directory:** `simple_rdf_sparql/`
**Build:** Leiningen
**Deps:** `org.clojure/clojure` (only)

### API (`simple-rdf-sparql.core`)

- `(add-triple subject predicate object)` — Add an RDF triple to the in-memory store.
- `(remove-triple subject predicate object)` — Remove a triple.
- `(query-triples subject predicate object)` — Pattern match triples (use `nil` or `?var` for wildcards).
- `(execute-sparql-query query-string)` — Execute a SPARQL-like query string. Returns seq of binding maps.
- `(print-query-results query-string)` — Execute and pretty-print query results.

### Example

```clojure
(require '[simple-rdf-sparql.core :as rdf])

(rdf/add-triple "John" "likes" "pizza")
(rdf/add-triple "Mary" "likes" "sushi")
(rdf/print-query-results "select ?name ?food where { ?name likes ?food }")
```

---

## nlp_opennlp

**Directory:** `nlp_opennlp/`
**Build:** Leiningen
**Deps:** `opennlp/tools`, Java NLP wrapper

### API (`opennlp-clj.core`)

- `(sentence-splitter text)` — Split text into sentences.
- `(tokenize->seq text)` — Tokenize to Clojure seq.
- `(tokenize->java text)` — Tokenize to Java array.
- `(POS java-token-array)` — Part-of-speech tagging.
- `(company-names java-token-array)` — Extract company names.
- `(location-names java-token-array)` — Extract location names.
- `(person-names java-token-array)` — Extract person names.

### Example

```clojure
(require '[opennlp-clj.core :as nlp])

(let [tokens (nlp/tokenize->java "John Smith worked at IBM in Mexico.")]
  (println "POS:" (nlp/POS tokens))
  (println "People:" (nlp/person-names tokens))
  (println "Orgs:" (nlp/company-names tokens))
  (println "Places:" (nlp/location-names tokens)))
```

---

## nlp_libpython

**Directory:** `nlp_libpython/`
**Build:** Leiningen
**Deps:** `libpython-clj`, `clj-http`, `cemerick/url`, `data.csv`, `data.json`
**Requires:** Python with spaCy installed, `en_core_web_sm` model

### API (`nlp-libpython-spacy.core`)

- `(text->tokens text)` — Tokenize text via spaCy.
- `(text->pos text)` — Get part-of-speech tags.
- `(text->tokens-and-pos text)` — Get `[token pos]` pairs.
- `(text->entities text)` — Extract named entities as `[text label]` pairs.
- `(qa question context-text)` — Transformer-based question answering.
- `(spacy-qa-demo query)` — Combined NLP + KG + QA demo.

### Example

```clojure
(require '[nlp-libpython-spacy.core :as nlp])

(nlp/text->entities "John Smith worked for IBM in Mexico.")
;; => (["John Smith" "PERSON"] ["IBM" "ORG"] ["Mexico" "GPE"])
```

---

## webscraping

**Directory:** `webscraping/`
**Build:** Leiningen
**Deps:** `org.jsoup/jsoup`

### API (`webscraping.core`)

- `(fetch-web-page-data uri)` — Fetch a web page. Returns `{:page-text "…" :anchors [{:text "…" :uri "…"} …]}`.
- `(get-html-anchors jsoup-doc)` — Extract anchor `{:text :uri}` maps from a Jsoup document.

### Example

```clojure
(require '[webscraping.core :as ws])

(let [{:keys [page-text anchors]} (ws/fetch-web-page-data "https://markwatson.com")]
  (println "Page length:" (count page-text))
  (doseq [a (take 5 anchors)]
    (println (:text a) "->" (:uri a))))
```

---

## anomaly_detection

**Directory:** `anomaly_detection/`
**Build:** Leiningen
**Deps:** `incanter`, `data.csv`, Java anomaly detection library

### API (`anomaly-detection-clj.core`)

- `(data->gausian vector-of-numbers)` — Transform data toward Gaussian distribution for anomaly detection.
- `(testAD)` — Run anomaly detection on Wisconsin cancer dataset. Returns `{:malignant-result bool :benign-result bool}`.

### Example

```bash
cd anomaly_detection
lein run
```

---

## llm_bosquet

**Directory:** `llm_bosquet/`
**Build:** deps.edn
**Deps:** `bosquet`
**Server:** Requires Ollama running locally

### API (`llm-bosquet.core`)

- `(ollama-generate prompt)` — Generate text using Bosquet's prompt-chaining with Ollama. Uses `mistral-small` model.

### Example

```clojure
(require '[llm-bosquet.core :as bosquet])

(bosquet/ollama-generate "What is functional programming?")
```

---

## deeplearning_dl4j

**Directory:** `deeplearning_dl4j/`
**Build:** Leiningen
**Deps:** DL4J (Deeplearning4j)

Demonstrates deep learning with the Wisconsin cancer dataset using DL4J Java interop.

---

## datomic_local

**Directory:** `datomic_local/`

Demonstrates Datomic local database operations from Clojure.

---

## General Notes

- Most examples use **Leiningen** (`project.clj`); run with `lein run` or `lein repl`. A few use **deps.edn**; run with `clj`.
- Clojure has seamless Java interop — any Java library can be used via `(:import ...)`.
- Common HTTP library: `clj-http`. Common JSON library: `clojure.data.json` or `cheshire`.
- Environment variables: `GOOGLE_API_KEY`, `OPENAI_API_KEY`, `BRAVE_SEARCH_API_KEY`, `MOONSHOT_API_KEY`, `GEMINI_API_KEY` (for AgentScope).
- Use threading macros (`->`, `->>`) for readable data pipelines.
- Use `def ^:dynamic` for configurable defaults that can be overridden with `binding`.
- Use `defn-` for private helper functions.
