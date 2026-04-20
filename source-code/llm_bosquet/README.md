# Try the Clojure LLM library Bosquet

> **Note:** This example is a work-in-progress and is not yet part of *Practical Artificial Intelligence Programming With Clojure*. You can browse the book for free at [https://leanpub.com/clojureai/read](https://leanpub.com/clojureai/read).

Currently uses Bosquet's defaults:

- Model: mistral-small
- Local Ollama model hosting

## Example files

project-root/
├── deps.edn
├── src/
│   └── llm_bosquet/
│       └── core.clj
└── test/
    └── llm_bosquet/
        └── core_test.clj

## Running the example

Make sure you define a valif OpenAI API key (if using OpenAI):

    export OPENAI_API_KEY=sk-......

Run the tests:

   clj -X:test

# THIS EXAMPE IS WORK IN PROGRESS - NOT YET IN BOOK!

