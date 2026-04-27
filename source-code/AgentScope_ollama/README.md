# AgentScope + Ollama — Example for "Practical Artificial Intelligence Programming With Clojure"

> **Book Chapter:** [AgentScope Agent Oriented Framework](https://leanpub.com/read/clojureai/leanpub-auto-agentscope-agent-oriented-framework) — free to read online.

This directory contains Clojure examples for using the [AgentScope SDK](https://github.com/modelscope/agentscope) directly via Java interop with a **local Ollama** model as the backing LLM. No cloud API key is required — everything runs on your own hardware.

See also `../AgentScope_gemini` for the cloud-based Gemini variant.

## Prerequisites

| Tool | Version |
|------|---------|
| Java | 17+ |
| [Leiningen](https://leiningen.org) | 2.11+ |
| [Ollama](https://ollama.ai) | Latest |

Pull the model used by the example:

    ollama pull nemotron-3-nano:4b

Make sure Ollama is running:

    ollama serve

## Running the Examples

**Basic Agent Demo** — builds an `OllamaChatModel`, wraps it in a `ReActAgent`, sends a prompt, and prints the response:

    lein run

**Tool-Use Demo** — registers tools (weather lookup, file operations, math eval), attaches them to the agent, and lets the LLM invoke them automatically:

    lein run -m agentscope.tool-use

## How It Works

1. `OllamaChatModel` is constructed pointing at `http://localhost:11434`.
2. `ReActAgent` wraps the model with a system prompt and optional `Toolkit`.
3. Tools are defined in pure Clojure by implementing the `AgentTool` interface with `reify`.
4. A `Msg` is passed to `.call()` on the agent and the response is collected with `.block()`.

## Key Dependencies

| Artifact | Purpose |
|----------|---------|
| `io.agentscope/agentscope` | AgentScope core (agents, messaging, tools) |

## Book and License

Book URI: https://leanpub.com/clojureai — you can read the book for free online at https://leanpub.com/clojureai/read

Copyright © 2021-2026 Mark Watson. All rights reserved.

Licensed under the Apache License 2.0.
