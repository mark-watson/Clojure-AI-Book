# Chess Engine & AI Bot (Clojure)

An educational, high-performance chess engine and AI bot written in idiomatic Clojure. Runs on the Java Virtual Machine (JVM) with zero external runtime dependencies.

This project implements a complete chess board representation, legal move generator, evaluation system, and interactive command-line interface (CLI) to play against an AI player or watch the bot play against itself. It is a direct port of the reference implementation from the *TypeScript AI Book*, adapted to utilize Clojure's immutable states and persistent data structures.

---

## Features

### 1. Core Chess Engine (`src/chess_game/engine.clj`)

- **Board Representation**: Clojure map containing a 64-element persistent vector for the board array, persistent sets tracking piece locations per color, active turn, castling rights mask, en passant square index, halfmove/fullmove clocks, and the Zobrist hash value.
- **Pure Functional State Transitions**: Incremental move execution produces a new, independent `board` state. No board mutation or undo rollback stack is necessary.
- **Zobrist Hashing**: 64-bit deterministic Zobrist keys computed incrementally via bitwise XOR operations on primitive JVM `long` values.
- **Fully Legal Move Generator**: Standard castling rights tracking, en passant targets, pawn double pushes, and pawn promotions (defaulting to Queen for user moves if unspecified).
- **FEN Parser & Writer**: Complete Forsythe-Edwards Notation serialization support.

### 2. Chess AI Bot (`src/chess_game/ai.clj`)

- **Negamax Search with Alpha-Beta Pruning**: Exploits the zero-sum nature of chess values to prune irrelevant search tree branches.
- **Transposition Table (TT)**: Caches searched positions using Zobrist hash values in a thread-safe global `transposition-table` atom to eliminate redundant subtree checks.
- **Iterative Deepening**: Progressively searches from depth 1 to the target depth to seed move ordering for subsequent iterations, maximizing cutoffs.
- **Quiescence Search**: Extends search on capture and promotion moves to resolve the horizon effect.
- **PSTs & Evaluation**: Utilizes material scores and Piece-Square Tables (PST) matching the reference implementation, adapting King evaluation depending on the midgame/endgame phase.

### 3. Interactive CLI (`src/chess_game/cli.clj`)

- Play modes: Human as White, Human as Black, and Bot vs Bot.
- Rendered ANSI checkers board using unicode glyphs (♔♕♖♗♘♙ / ♚♛♜♝♞♟).
- Supports UCI moves (e.g. `e2e4`, `g1f3`, `e7e8q` for promotion) and console commands (`legal`, `fen`, `setfen <fen>`, `reset`, `help`, `exit`).

### 4. Perft Test Suite (`src/chess_game/perft.clj`)

- Traverses the legal move tree to check node counts at depths 1–3 and verify move generator correctness.
- Validates that incremental Zobrist hashes match fresh recalculations after every single move.

---

## Installation & Running

### Prerequisites

- [Leiningen](https://leiningen.org/) or [Clojure CLI Tools](https://clojure.org/guides/install_clojure_cli) installed.
- Java JDK 11 or higher.

### Play the Game

#### Using Leiningen:
```bash
lein run
```

#### Using Clojure CLI:
```bash
clj -M:run
```

Choose your mode (1-3), set the bot depth, and enter moves in UCI format (e.g., `e2e4`, `d7d8q`).

### Run Move Generation (Perft) Tests

#### Using Leiningen:
```bash
lein perft
```

#### Using Clojure CLI:
```bash
clj -M:perft
```

This will run benchmarks from the standard starting position and output node counts, execution times, and NPS.
