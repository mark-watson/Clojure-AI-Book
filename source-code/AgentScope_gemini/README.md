# AgentScope + Gemini — Example for "Practical Artificial Intelligence Programming With Clojure"

> **Book Chapter:** [AgentScope Agent Oriented Framework](https://leanpub.com/read/clojureai/leanpub-auto-agentscope-agent-oriented-framework) — free to read online.

This directory contains Clojure examples for using the [AgentScope SDK](https://github.com/modelscope/agentscope) directly via Java interop with **Google Gemini** as the backing LLM.

## Prerequisites

| Tool | Version |
|------|---------|
| Java | 17+ |
| [Leiningen](https://leiningen.org) | 2.11+ |
| `GEMINI_API_KEY` | [Get one here](https://aistudio.google.com/app/apikey) |

Set your API key before running:

    export GEMINI_API_KEY=your_key_here

## Running the Examples

**Basic Agent Demo** — builds a `GeminiChatModel`, wraps it in a `ReActAgent`, sends a prompt, and prints the response:

    lein run

**Tool-Use Demo** — registers a weather tool, attaches it to the agent, and asks about the weather. The agent invokes the tool automatically:

    lein run -m agentscope.tool-use

## How It Works

1. `GeminiChatModel` is constructed with your API key.
2. `ReActAgent` wraps the model with a system prompt and optional `Toolkit`.
3. A `Msg` is passed to `.call()` on the agent.
4. The reactive response is collected with `.block()` and printed.

## Key Dependencies

| Artifact | Purpose |
|----------|---------|
| `io.agentscope/agentscope` | AgentScope core (agents, messaging, tools) |
| `com.google.genai/google-genai` | Google GenAI SDK (Gemini models) |

## Book and License

Book URI: https://leanpub.com/clojureai — you can read the book for free online at https://leanpub.com/clojureai/read

Copyright © 2021-2026 Mark Watson. All rights reserved.

Licensed under the Apache License 2.0.
