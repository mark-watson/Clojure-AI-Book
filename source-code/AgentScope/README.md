export GEMINI_API_KEY=your_key_here

# Basic agent demo
lein run

# Tool-use demo
lein run -m agentscope.tool-use

# AgentScope + Gemini — Clojure Edition

This directory contains a Clojure port of the AgentScope Java examples.
The Clojure code runs on the JVM and calls the **AgentScope Java SDK** directly via Clojure's Java interop, so no Clojure-native wrapper library is needed.

> See [`README.md`](README.md) for background on AgentScope and the Gemini model.

---

## Prerequisites

| Tool | Version |
|------|---------|
| Java | 17+ |
| [Leiningen](https://leiningen.org) | 2.11+ |
| `GEMINI_API_KEY` | [Get one here](https://aistudio.google.com/app/apikey) |

```bash
export GEMINI_API_KEY=your_key_here
```

---

## Project Structure

```
.
├── project.clj                          # Leiningen build file
└── src/
    └── main/
        ├── clojure/agentscope/
        │   ├── main.clj                 # Basic ReActAgent demo
        │   └── tool_use.clj             # Tool-use demo
        └── java/com/markwatson/agentscope/
            └── WeatherService.java      # @Tool-annotated stub (see note below)
```

---

## Running the Examples

### Basic agent demo (`main.clj`)

Builds a `GeminiChatModel`, wraps it in a `ReActAgent`, sends a prompt, and prints the response.

```bash
lein run
# or explicitly:
lein run-main
```

### Tool-use demo (`tool_use.clj`)

Registers a stub `getWeather` tool with a `Toolkit`, attaches it to the agent, and asks about the weather in Tokyo and Paris. The agent invokes the tool automatically during its ReAct loop.

```bash
lein run -m agentscope.tool-use
# or via the alias:
lein run-tool-use
```

### Build an uberjar

```bash
lein uberjar
java -jar target/agentscope-gemini-standalone.jar
```

---

## Key Dependencies (`project.clj`)

| Artifact | Version | Purpose |
|----------|---------|---------|
| `io.agentscope/agentscope` | 1.0.9 | AgentScope core (agents, messaging, tools) |
| `com.google.genai/google-genai` | 1.44.0 | Google GenAI SDK (Gemini models) |
| `org.slf4j/slf4j-simple` | 2.0.13 | SLF4J logging |

---

## How It Works

1. **`GeminiChatModel`** is constructed with the API key and model name (`gemini-2.5-flash`).
2. **`ReActAgent`** wraps the model with a system prompt (and optionally a `Toolkit`).
3. A **`Msg`** with text content is passed to `.call()` on the agent.
4. The reactive response is collected with `.block()` and the text content is printed.

### Why `WeatherService.java`?

AgentScope's `Toolkit#registerTool(Object)` discovers tools via Java reflection by scanning
`@Tool` and `@ToolParam` annotations on methods. Clojure functions cannot carry Java
annotations at runtime, so a minimal Java companion class (`WeatherService.java`) holds the
annotations. Leiningen compiles it automatically via `:java-source-paths` before AOT-compiling
the Clojure namespaces.

---

## How It Differs from the Java Version

| Aspect | Java | Clojure |
|--------|------|---------|
| Build tool | Maven + `pom.xml` | Leiningen + `project.clj` |
| Entry point | `public static void main` | `(-main)` + `:gen-class` |
| Builder pattern | `.builder()…​.build()` | `(-> (Foo/builder) (.method val) (.build))` |
| `doto` for side-effects | `toolkit.registerTool(…)` | `(doto (Toolkit.) (.registerTool …))` |
| Tool annotations | Inner class with `@Tool` | Companion `WeatherService.java` |
