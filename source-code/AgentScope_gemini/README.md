# AgentScope + Gemini — Clojure Edition

This directory contains Clojure examples for using the **AgentScope SDK** directly via Java interop.

> See [`README.md`](README.md) for background on AgentScope and the Gemini model.

## Prerequisites

| Tool | Version |
|------|---------|
| Java | 17+ |
| [Leiningen](https://leiningen.org) | 2.11+ |
| `GEMINI_API_KEY` | [Get one here](https://aistudio.google.com/app/apikey) |

Set your API key before running the examples:
```bash
export GEMINI_API_KEY=your_key_here
```

## Project Structure

```
.
├── project.clj                          # Leiningen build file
└── src/
    └── main/
        └── clojure/agentscope/
            ├── main.clj                 # Basic ReActAgent demo
            └── tool_use.clj             # Tool-use demo
```

## Running the Examples

### Basic Agent Demo

Builds a `GeminiChatModel`, wraps it in a `ReActAgent`, sends a prompt, and prints the response.

```bash
# Basic agent demo
lein run
```

### Tool-Use Demo

Registers a weather tool, attaches it to the agent, and asks about the weather in Tokyo and Paris. The agent invokes the tool automatically.

```bash
# Tool-use demo
lein run -m agentscope.tool-use
```

### Build an Uberjar

```bash
lein uberjar
java -jar target/agentscope-gemini-standalone.jar
```

## Key Dependencies

| Artifact | Version | Purpose |
|----------|---------|---------|
| `io.agentscope/agentscope` | 1.0.9 | AgentScope core (agents, messaging, tools) |
| `com.google.genai/google-genai` | 1.44.0 | Google GenAI SDK (Gemini models) |
| `org.slf4j/slf4j-simple` | 2.0.13 | Logging |

## How It Works

1. **`GeminiChatModel`** is constructed with your API key.
2. **`ReActAgent`** wraps the model with a system prompt and optional `Toolkit`.
3. A **`Msg`** is passed to `.call()` on the agent.
4. The reactive response is collected with `.block()` and printed.
