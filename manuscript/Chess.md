# A Complete Clojure Chess Engine and AI Bot

This chapter walks through a complete chess engine written in idiomatic Clojure. The code runs on the JVM with no external dependencies beyond Clojure itself. The project contains a high-performance move generator, a **negamax** AI with alpha-beta pruning, and an interactive terminal-based UI. You can play against the bot or watch it play against itself.

We will use the project file to manage both the main game entry point and a perft testing alias:

{lang="clojure",linenos=on}
~~~~~~~~
(defproject chess-game "0.1.0-SNAPSHOT"
  :description "Chess engine and AI bot in Clojure"
  :url "https://github.com/mark-watson/Clojure-AI-Book"
  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.11.1"]]
  :main ^:skip-aot chess-game.cli
  :aliases {"perft" ["run" "-m" "chess-game.perft"]}
  :profiles {:uberjar {:aot :all}})
~~~~~~~~

The project is organized into four source files:

{linenos=off}
~~~~~~~~
src/
└── chess_game/
    ├── engine.clj   ; board, moves, FEN, Zobrist hashing
    ├── ai.clj       ; evaluation, search, transposition table
    ├── cli.clj       ; terminal UI, game loop
    └── perft.clj     ; move generation verification
~~~~~~~~

## Board Representation and Piece Encoding

The engine represents pieces as bytes where the lower three bits encode the piece type and bits 4--5 encode the color. This compact encoding allows us to test piece properties with fast bitwise operations while keeping the board as a single flat vector of 64 integers.

{lang="clojure",linenos=on}
~~~~~~~~
;; Piece representation
(def EMPTY 0)
(def PAWN 1)
(def KNIGHT 2)
(def BISHOP 3)
(def ROOK 4)
(def QUEEN 5)
(def KING 6)
(def TYPE-MASK 7)

(def WHITE 8)
(def BLACK 16)
(def COLOR-MASK 24)

(defn piece-type [p] (bit-and p TYPE-MASK))
(defn piece-color [p] (bit-and p COLOR-MASK))
(defn opponent [c] (if (= c WHITE) BLACK WHITE))
(defn color-idx [c] (if (= c WHITE) 0 1))
~~~~~~~~

On lines 4--11 we define piece types as small integers. Lines 13--15 define color constants as bit flags. The masks on lines 12 and 16 let us extract type and color from the combined value. For example, a white knight is `(bit-or WHITE KNIGHT)` which evaluates to 10.

The game state is a Clojure map containing:

- A 64-element persistent vector for the board square contents
- A two-element vector of persistent sets tracking the locations of white and black pieces
- A two-element vector of king square indices
- Turn indicator (WHITE or BLACK)
- Castling rights bitmask
- En passant target square (or -1)
- Halfmove and fullmove clocks for the 50-move rule
- A precomputed Zobrist hash for fast position identification

## Square Indexing and Precomputed Move Tables

Squares are indexed 0--63, where 0 is a1 and 63 is h8, increasing first by file then by rank. Two helper maps convert between algebraic notation and integer indices:

{lang="clojure",linenos=on}
~~~~~~~~
(def SQUARE-NAMES
  (vec (for [row (range 8)
             col (range 8)]
         (str (get "abcdefgh" col) (get "12345678" row)))))

(def NAME-TO-SQUARE
  (into {} (map-indexed (fn [idx name] [name idx]) SQUARE-NAMES)))
~~~~~~~~

Rather than computing moves on the fly, we precompute lookup tables for knight moves, king moves, and sliding piece rays. This approach trades memory for speed---the tables are computed once at load time and then reused across all move generation.

{lang="clojure",linenos=on}
~~~~~~~~
(def KNIGHT-MOVES
  (vec (for [sq (range 64)]
         (let [r (quot sq 8)
               f (mod sq 8)
               offsets [[-2 -1] [-2 1] [-1 -2] [-1 2] [1 -2] [1 2] [2 -1] [2 1]]]
           (vec (for [[dr df] offsets
                      :let [nr (+ r dr)
                            nf (+ f df)]
                      :when (and (>= nr 0) (< nr 8) (>= nf 0) (< nf 8))]
                  (+ (* nr 8) nf)))))))
~~~~~~~~

Each of the 64 squares gets a vector of valid knight destinations computed with rank-and-file offset arithmetic. The `:when` guard on line 9 ensures we only include squares that stay on the board.

King moves follow the same pattern with the eight adjacent directions. For sliding pieces---rooks, bishops, and queens---we compute rays:

{lang="clojure",linenos=on}
~~~~~~~~
(defn- compute-rays [sq dirs]
  (let [r (quot sq 8)
        f (mod sq 8)]
    (vec (for [[dr df] dirs]
           (loop [nr (+ r dr)
                  nf (+ f df)
                  ray []]
             (if (and (>= nr 0) (< nr 8) (>= nf 0) (< nf 8))
               (recur (+ nr dr) (+ nf df) (conj ray (+ (* nr 8) nf)))
               ray))))))

(def ROOK-RAYS
  (vec (for [sq (range 64)]
         (compute-rays sq [[1 0] [-1 0] [0 1] [0 -1]]))))
~~~~~~~~

Each ray is a vector of squares extending outward from the source square in one direction. The function on line 1 uses `loop/recur` to walk outward until it hits the board edge. Rook rays compute the four cardinal directions, bishop rays use the four diagonals, and queen rays concatenate both.

## Zobrist Hashing

Zobrist hashing assigns a random 64-bit integer to each possible piece-on-square combination, plus an extra key for side-to-move, sixteen keys for castling rights, and sixty-four keys for en passant squares. The hash of a position is the XOR of all applicable keys. This allows us to incrementally update the hash during move execution by XOR-ing out old keys and XOR-ing in new ones, rather than recomputing from scratch.

{lang="clojure",linenos=on}
~~~~~~~~
(defn- create-rng [seed]
  (let [state (atom (bit-or (long seed) 1))]
    (fn []
      (swap! state (fn [^long curr]
                     (unchecked-add (unchecked-multiply curr 6364136223846793005)
                                    1442695040888963407))))))

(def rng (create-rng 1337))

(def ZOBRIST_PIECES
  (vec (for [_ (range 64)]
         (vec (for [_ (range 32)] (rng))))))

(def ZOBRIST_SIDE (rng))

(def ZOBRIST_CASTLING
  (vec (for [_ (range 16)] (rng))))

(def ZOBRIST_EP
  (vec (for [_ (range 64)] (rng))))
~~~~~~~~

We generate the random keys with a deterministic seeded PRNG (line 1) so that hashes are reproducible across runs. The piece table on line 12 is a 64-by-32 matrix: each of the 64 squares has its own random key for each of the 32 possible piece values (six types times two colors, with unused slots).

## Generating Pseudo-Legal Moves

The move generator iterates over every piece owned by the side to move and produces candidate moves without checking whether the resulting position leaves the king in check. That legality filter is applied later in `get-legal-moves`.

{lang="clojure",linenos=on}
~~~~~~~~
(defrecord Move [from to piece-moved piece-captured promotion
                 en-passant? castling? double-push?])
~~~~~~~~

Each move is a `defrecord` carrying the source and destination squares, the piece being moved, any captured piece, a promotion piece type if applicable, and three boolean flags for en passant, castling, and double pawn push. The boolean flags allow `make-move` to handle the side effects of these special moves without inspecting the board.

The move generator handles six piece types. Pawns account for the most logic: single pushes, double pushes from the starting rank, captures (including en passant), and promotions:

{lang="clojure",linenos=on}
~~~~~~~~
(= ptype PAWN)
(let [push-sq (+ sq forward)]
  (when (= (get board-vec push-sq) EMPTY)
    (if (= (quot push-sq 8) promo-rank)
      (doseq [pp promo-pieces]
        (conj! moves (->Move sq push-sq piece EMPTY pp false false false)))
      (do
        (conj! moves (->Move sq push-sq piece EMPTY EMPTY false false false))
        (when (= r start-rank)
          (let [double-sq (+ push-sq forward)]
            (when (= (get board-vec double-sq) EMPTY)
              (conj! moves (->Move sq double-sq piece EMPTY EMPTY
                                   false false true))))))))
  ;; Captures & EP
  (doseq [df [-1 1]
          :let [nf (+ f df)]
          :when (and (>= nf 0) (<= nf 7))
          :let [cap-sq (+ sq forward df)]]
    (let [target (get board-vec cap-sq)]
      (cond
        (and (not= target EMPTY) (= (piece-color target) opp))
        (if (= (quot cap-sq 8) promo-rank)
          (doseq [pp promo-pieces]
            (conj! moves (->Move sq cap-sq piece target pp
                                 false false false)))
          (conj! moves (->Move sq cap-sq piece target EMPTY false false false)))
        (= cap-sq (:en-passant-square board))
        (let [ep-captured (get board-vec (- cap-sq forward))]
          (conj! moves (->Move sq cap-sq piece ep-captured
                               EMPTY true false false)))))))
~~~~~~~~

When a pawn reaches the promotion rank (line 5) we generate four moves, one for each promotion piece (QUEEN, ROOK, BISHOP, KNIGHT). On line 12 the double pawn push sets the `double-push?` flag, which `make-move` uses to set the en passant target square for the opponent's next turn.

Knight and king moves are straightforward: each destination in the precomputed table becomes a move if the target square is empty or occupied by an opponent piece. Kings also generate castling moves when the castling rights are still set and the intermediate squares are empty and unattacked:

{lang="clojure",linenos=on}
~~~~~~~~
;; Castling
(if (= color WHITE)
  (when (= sq 4)
    (when (and (not= (bit-and (:castling-rights board) WK) 0)
               (= (get board-vec 5) EMPTY)
               (= (get board-vec 6) EMPTY))
      (when (not (or (square-attacked? board 4 opp)
                     (square-attacked? board 5 opp)
                     (square-attacked? board 6 opp)))
        (conj! moves (->Move 4 6 piece EMPTY EMPTY false true false))))
    ...))
  ;; Black castling
  ...)
~~~~~~~~

The castling rights bitmask (WK = 1, WQ = 2, BK = 4, BQ = 8) is checked on line 4 with `bit-and`. The three intermediate squares must be empty, and the king must not pass through or land on an attacked square. The `castling?` flag on line 9 tells `make-move` to also move the rook.

Sliding pieces follow rays outward, stopping at the first occupied square:

{lang="clojure",linenos=on}
~~~~~~~~
:else ; sliding pieces
(let [rays (condp = ptype
             BISHOP (get BISHOP-RAYS sq)
             ROOK   (get ROOK-RAYS sq)
             QUEEN  (get QUEEN-RAYS sq))]
  (doseq [ray rays]
    (loop [r-sqs ray]
      (when-let [t (first r-sqs)]
        (let [target (get board-vec t)]
          (cond
            (= target EMPTY)
            (do
              (conj! moves (->Move sq t piece EMPTY EMPTY
                                   false false false))
              (recur (rest r-sqs)))
            (= (piece-color target) opp)
            (conj! moves (->Move sq t piece target EMPTY
                                 false false false))
            :else nil))))))
~~~~~~~~

On line 15, when the ray encounters an empty square we add a move and continue along the ray. On line 18, when we encounter an enemy piece we add the capture move but stop. On line 20, a friendly piece blocks the ray with no move added. The `nil` on line 20 signals `recur` to terminate the loop because `when-let` will see `nil` from the next `first` call.

## Executing Moves

The `make-move` function takes a board map and a move record, and returns a new board map representing the position after the move. It uses mutable atoms internally for performance during assembly but returns an immutable persistent map:

{lang="clojure",linenos=on}
~~~~~~~~
(defn make-move [board move]
  (let [color (piece-color (:piece-moved move))
        opp (opponent color)
        ci (color-idx color)
        oi (color-idx opp)
        board-vec (:board board)

        new-board-vec (atom (assoc board-vec (:from move) EMPTY))
        new-pieces-color (atom (disj (get-in board [:pieces ci]) (:from move)))
        new-pieces-opp (atom (get-in board [:pieces oi]))
        new-king-squares (atom (:king-squares board))

        ep? (:en-passant? move)
        captured-sq (if ep?
                      (- (:to move) (if (= color WHITE) 8 -8))
                      (:to move))

        h-atom (atom (long (:zobrist-hash board)))]
    ...))
~~~~~~~~

The moving piece is first removed from its origin square on line 9. En passant captures on line 17 compute the actual square of the captured pawn, which is one rank behind the destination square. The Zobrist hash is updated incrementally at every step: we XOR out the old side-to-move key, old castling rights, and old en passant square, then XOR them back in with their new values.

After placing the piece at its destination, the function handles promotion (using the promoted piece type instead of the original pawn), castling (moving the rook from its corner to its post-castle position), and updates to castling rights if the moved piece is a king or rook:

{lang="clojure",linenos=on}
~~~~~~~~
;; Update castling rights
(let [old-rights (:castling-rights board)
      r1 (if (= (piece-type (:piece-moved move)) KING)
           (if (= color WHITE)
             (bit-and old-rights (bit-not (bit-or WK WQ)))
             (bit-and old-rights (bit-not (bit-or BK BQ))))
           old-rights)
      r2 (cond-> r1
           (or (= (:from move) 7) (= (:to move) 7)) (bit-and (bit-not WK))
           (or (= (:from move) 0) (= (:to move) 0)) (bit-and (bit-not WQ))
           (or (= (:from move) 63) (= (:to move) 63)) (bit-and (bit-not BK))
           (or (= (:from move) 56) (= (:to move) 56)) (bit-and (bit-not BQ)))
      new-rights r2

      new-ep (if (:double-push? move)
               (- (:to move) (if (= color WHITE) 8 -8))
               -1)

      pawn? (= (piece-type (:piece-moved move)) PAWN)
      cap? (and (:piece-captured move) (not= (:piece-captured move) EMPTY))
      new-halfmove (if (or pawn? cap?) 0 (inc (:halfmove-clock board)))
      new-fullmove (if (= color BLACK)
                     (inc (:fullmove-number board))
                     (:fullmove-number board))]
  ...)
~~~~~~~~

Lines 6--10 use Clojure's `cond->` threading macro to revoke castling rights when a rook moves from or is captured on its starting square. The halfmove clock resets to zero on pawn moves or captures (line 23) and increments otherwise, enabling the 50-move draw rule. The fullmove number increments after Black's move.

## Attack Detection and Legal Move Filtering

The `square-attacked?` function checks whether a given square is under attack by any piece of the specified color. It queries the precomputed move tables in reverse---instead of asking "where can this knight go?", it asks "is there an enemy knight that can reach this square?"

{lang="clojure",linenos=on}
~~~~~~~~
(defn square-attacked? [board sq attacker-color]
  ...)
~~~~~~~~

Pawn attacks are checked by looking at the two diagonal squares behind the target (relative to the attacker's forward direction). Knight and king attacks look up the precomputed destination tables for `sq` and check if an enemy knight or king occupies any of those squares. Sliding piece attacks iterate outward along rays---just like the move generator, but stopping at the first piece whether it's the attacker or a blocker.

Legal move filtering wraps `get-pseudo-legal-moves` with a check that the king is not left in check:

{lang="clojure",linenos=on}
~~~~~~~~
(defn get-legal-moves [board]
  (let [color (:turn board)
        ci (color-idx color)
        opp (opponent color)]
    (filter (fn [move]
              (let [new-board (make-move board move)]
                (not (square-attacked? new-board
                      (get-in new-board [:king-squares ci]) opp))))
            (get-pseudo-legal-moves board))))
~~~~~~~~

For each candidate move we apply the move and test whether the moving side's king is under attack in the resulting position. This catches moves that are illegal because the king would be exposed to check, which includes pinned pieces moving off a pinning line.

## FEN Parsing and Serialization

Forsyth-Edwards Notation (FEN) is the standard text format for describing chess positions. The `from-fen` parser splits the FEN string into its six components and rebuilds the full board state:

{lang="clojure",linenos=on}
~~~~~~~~
(defn from-fen [fen]
  (let [parts (str/split fen #" ")
        [placement active-color castling ep halfmove fullmove] parts
        ...]
    (let [turn (if (= active-color "w") WHITE BLACK)
          castling-rights (reduce (fn [rights ch]
                                    (condp = ch
                                      \K (bit-or rights WK)
                                      \Q (bit-or rights WQ)
                                      \k (bit-or rights BK)
                                      \q (bit-or rights BQ)
                                      rights))
                                  0 castling)
          ep-square (if (= ep "-") -1 (get NAME-TO-SQUARE ep))
          ...
          initial-board {:board @board-vec
                         :pieces [@pieces-w @pieces-b]
                         :king-squares [wk-sq bk-sq]
                         :turn turn
                         :castling-rights castling-rights
                         :en-passant-square ep-square
                         :halfmove-clock halfmove-clock
                         :fullmove-number fullmove-number}]
      (assoc initial-board :zobrist-hash
             (compute-zobrist-hash initial-board)))))
~~~~~~~~

The placement string is eight rank descriptions separated by slashes, processed from the eighth rank down to the first. Digits represent empty squares; letters map through `type-map` to piece constants. After building the board vector and piece sets, we find the king squares by linear search (line 20) and compute the initial Zobrist hash from scratch on line 30.

The `to-fen` function reverses this process by iterating over ranks from top to bottom, counting empty squares and emitting piece characters.

## AI: Position Evaluation

The AI evaluates positions using material scores plus Piece-Square Tables (PSTs). The PSTs encode positional knowledge: a knight on d4 is worth more than a knight on a3, a rook on an open seventh rank is worth more than a rook on its starting square, and so on.

{lang="clojure",linenos=on}
~~~~~~~~
(def PIECE-VALUES
  {PAWN 100, KNIGHT 320, BISHOP 330, ROOK 500, QUEEN 900, KING 20000})
~~~~~~~~

The king is given an extremely high value so that checkmate is always the worst possible outcome. Material is counted in centipawns.

The PST for knights illustrates the pattern:

{lang="clojure",linenos=on}
~~~~~~~~
(def KNIGHT-PST
  [-50,-40,-30,-30,-30,-30,-40,-50,
   -40,-20,  0,  0,  0,  0,-20,-40,
   -30,  0, 10, 15, 15, 10,  0,-30,
   -30,  5, 15, 20, 20, 15,  5,-30,
   -30,  0, 15, 20, 20, 15,  0,-30,
   -30,  5, 10, 15, 15, 10,  5,-30,
   -40,-20,  0,  5,  5,  0,-20,-40,
   -50,-40,-30,-30,-30,-30,-40,-50])
~~~~~~~~

The table is arranged from a1 (index 0) to h8 (index 63), from White's perspective. For Black pieces, we XOR the square index with 56 to mirror the board vertically. Central squares for knights are the most valuable (rows 3--4, columns 3--4), while edge squares are penalized.

The evaluation function also distinguishes between midgame and endgame using a material threshold:

{lang="clojure",linenos=on}
~~~~~~~~
(defn is-endgame? [board]
  (let [board-vec (:board board)
        material (reduce (fn [sum sq]
                           (let [pt (piece-type (get board-vec sq))]
                             (if (and (not= pt PAWN) (not= pt KING))
                               (+ sum (get PIECE-VALUES pt))
                               sum)))
                         0
                         (concat (get-in board [:pieces 0])
                                 (get-in board [:pieces 1])))]
    (<= material 3000)))
~~~~~~~~

When the total non-pawn, non-king material drops below 3000 centipawns (roughly three minor pieces), the engine switches from the midgame king PST to the endgame king PST. In the midgame, kings are safer near the corner behind pawns; in the endgame, kings should be active and centralized.

The top-level `evaluate` function sums White's material plus PST bonuses, subtracts Black's, adds the king PST for each side, and returns the score from the perspective of the side to move:

{lang="clojure",linenos=on}
~~~~~~~~
(defn evaluate [board]
  ...
  (if (= (:turn board) WHITE)
    score
    (- score)))
~~~~~~~~

If it's Black's turn, we negate the score so that the search can always maximize from the current side's perspective.

## AI: Negamax Search with Alpha-Beta Pruning

The search is a standard negamax formulation. At each node we generate all legal moves, order them by a heuristic that prioritizes captures, promotions, and TT best-moves, and recurse with negated alpha and beta bounds:

{lang="clojure",linenos=on}
~~~~~~~~
(defn search [board depth alpha beta]
  (swap! nodes-visited inc)
  (if (>= (:halfmove-clock board) 100)
    0
    (let [hash-val (:zobrist-hash board)
          tt-entry (get @transposition-table hash-val)
          ...]
      ...)))
~~~~~~~~

The 50-move rule check on line 4 returns a draw score of 0. The transposition table probe on line 7 looks up the current position by its Zobrist hash. If a stored entry has sufficient depth, we retrieve its score and best move---possibly enabling an immediate cutoff if the stored score is outside our alpha-beta window.

Move ordering is critical for alpha-beta efficiency:

{lang="clojure",linenos=on}
~~~~~~~~
(defn move-value [board move tt-move]
  (cond
    (and tt-move (move-equals? move tt-move)) 1000000
    (and (:piece-captured move) (not= (:piece-captured move) EMPTY))
    (+ 10000 (get PIECE-VALUES (piece-type (:piece-captured move)))
       (- (quot (get PIECE-VALUES (piece-type (:piece-moved move))) 100)))
    (and (:promotion move) (not= (:promotion move) EMPTY))
    (+ 8000 (get PIECE-VALUES (:promotion move)))
    (:castling? move) 1000
    :else
    (let [pt (piece-type (:piece-moved move))]
      (if (not= pt KING)
        (- (get (get PST-TABLES pt) (:to move))
           (get (get PST-TABLES pt) (:from move)))
        0))))
~~~~~~~~

The highest priority (1,000,000) goes to the TT best-move from a previous iteration. Captures are scored by victim value minus attacker value / 100---the MVV-LVA (Most Valuable Victim, Least Valuable Attacker) heuristic. Promotions are worth the promoted piece value plus a large bonus. Quiet moves get a small PST-change score, and king moves get zero.

The negamax loop itself is a classic implementation:

{lang="clojure",linenos=on}
~~~~~~~~
(let [legal-moves (get-legal-moves board)]
  (cond
    (empty? legal-moves)
    (if (is-in-check? board)
      (+ -30000 (- @max-depth depth)) ; Checkmate
      0) ; Stalemate

    (= depth 0)
    (quiescence-search board alpha beta)

    :else
    (let [sorted-moves (sort-by (fn [m] (move-value board m tt-best-move))
                                > legal-moves)]
      (loop [moves sorted-moves
             best-score -1000000000
             best-move nil
             curr-alpha alpha]
        (if-let [move (first moves)]
          (let [new-board (make-move board move)
                score (- (search new-board (dec depth) (- beta) (- curr-alpha)))]
            (let [new-best-score (max best-score score)
                  new-best-move (if (> score best-score) move best-move)
                  next-alpha (max curr-alpha score)]
              (if (>= next-alpha beta)
                new-best-score ; Beta cutoff
                (recur (rest moves) new-best-score new-best-move next-alpha))))
          ...))))))
~~~~~~~~

Mate scores on line 6 include the distance from the root so that the engine prefers checkmating in fewer moves. When a beta cutoff occurs on line 22, the move is stored in the TT with a lower-bound flag (TT-ALPHA in our naming) and we return immediately without examining the remaining moves.

## Quiescence Search

Quiescence search extends the search beyond the nominal depth on capture and promotion moves only, evaluating quiet positions with the static evaluation function. This prevents the engine from seeing a position where it captures a queen on the last ply but doesn't notice that the capture initiates a series of exchanges that loses material:

{lang="clojure",linenos=on}
~~~~~~~~
(defn quiescence-search [board alpha beta]
  (swap! nodes-visited inc)
  (let [stand-pat (evaluate board)]
    (if (>= stand-pat beta)
      beta
      (let [curr-alpha (max alpha stand-pat)
            captures (filter (fn [m] (or (and (:piece-captured m)
                                              (not= (:piece-captured m) EMPTY))
                                         (and (:promotion m)
                                              (not= (:promotion m) EMPTY))))
                             (get-pseudo-legal-moves board))
            sorted-captures (sort-by (fn [m] (move-value board m nil))
                                     > captures)]
        (loop [moves sorted-captures
               c-alpha curr-alpha]
          ...)))))
~~~~~~~~

The "stand pat" score on line 3 is the static evaluation of the current position. If it already exceeds beta, we return immediately---the opponent would not have allowed us to reach this position if they could avoid it. Otherwise we consider only capture and promotion moves, filtering pseudo-legal moves to those that don't leave our king in check on line 10.

## Iterative Deepening

The top-level `get-best-move` function performs iterative deepening: it searches to depth 1, then depth 2, and so on up to the requested maximum. Each iteration's best move seeds the move ordering for the next iteration:

{lang="clojure",linenos=on}
~~~~~~~~
(defn get-best-move [board depth]
  (when (> (count @transposition-table) 500000)
    (reset! transposition-table {}))

  (let [best-move (atom nil)
        best-score (atom 0)]
    (doseq [d (range 1 (inc depth))]
      (reset! max-depth d)
      (reset! nodes-visited 0)
      ...)))
~~~~~~~~

The TT is cleared when it exceeds 500,000 entries (line 2) to prevent unbounded memory growth. For each iteration depth we perform a root-level search over all legal moves, using the full alpha-beta window. After each iteration, the best move and score are stored in atoms. The final iteration's best move is the move the engine plays.

## Interactive CLI

The command-line interface renders the board using Unicode chess pieces on a colored checkerboard and accepts moves in UCI format:

{lang="clojure",linenos=on}
~~~~~~~~
(def PIECE-GLYPHS
  {(bit-or WHITE KING) "♔" (bit-or WHITE QUEEN) "♕" (bit-or WHITE ROOK) "♖"
   (bit-or WHITE BISHOP) "♗" (bit-or WHITE KNIGHT) "♘" (bit-or WHITE PAWN) "♙"
   (bit-or BLACK KING) "♚" (bit-or BLACK QUEEN) "♛" (bit-or BLACK ROOK) "♜"
   (bit-or BLACK BISHOP) "♝" (bit-or BLACK KNIGHT) "♞" (bit-or BLACK PAWN) "♟"})
~~~~~~~~

The board display alternates dark and light backgrounds using ANSI escape codes, with white pieces in bold white and black pieces in magenta. Each rank has a label on the left, and a file label line is printed at the bottom. Extra information---turn, move number, evaluation score, castling rights, check indicator, and en passant target---appears to the right of the board.

The game loop supports three modes: human as White, human as Black, or bot vs bot. When it's the bot's turn, `get-best-move` is called with the chosen depth and the resulting move is applied. Console commands (`help`, `fen`, `setfen`, `legal`, `reset`, `exit`) allow inspection and manipulation of the game state:

{lang="clojure",linenos=on}
~~~~~~~~
(loop [board (from-fen "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")]
  (print-board board)
  (let [legal-moves (get-legal-moves board)]
    (cond
      (empty? legal-moves)
      (if (is-in-check? board)
        (let [winner (if (= (:turn board) WHITE) "Black" "White")]
          (println (str "... Checkmate! " winner " wins.")))
        (println "... Stalemate! Draw."))

      (>= (:halfmove-clock board) 100)
      (println "... 50-move rule! Draw.")

      ;; Bot turn
      (or (nil? human-color) (not= (:turn board) human-color))
      (do ...)

      ;; Human turn
      :else
      (let [input (ask "Your move (or help): ")]
        ...))))
~~~~~~~~

The game loop checks for terminal conditions first: checkmate (no legal moves and the king is in check), stalemate (no legal moves but not in check), and the 50-move draw.

## Perft Testing

Perft (performance test) is a standard technique for verifying chess move generators. It recursively enumerates all legal moves to a given depth and counts the leaf nodes. The counts are compared against known correct values. Our perft module also validates the incremental Zobrist hash against a full recomputation after every move:

{lang="clojure",linenos=on}
~~~~~~~~
(defn perft [board depth]
  (if (= depth 0)
    1
    (reduce (fn [nodes move]
              (let [new-board (make-move board move)
                    recomputed (compute-zobrist-hash new-board)]
                (when (not= recomputed (:zobrist-hash new-board))
                  (println (str "Hash mismatch after " (move->uci move)
                                ": stored=" (:zobrist-hash new-board)
                                " recomputed=" recomputed))
                  (System/exit 1))
                (+ nodes (perft new-board (dec depth)))))
            0
            (get-legal-moves board))))
~~~~~~~~

On line 6 we recompute the full Zobrist hash from scratch and compare it against the incrementally updated hash stored in the board map. Any mismatch triggers an error and program exit.

The test harness runs depths 1 through 3 from the starting position:

{linenos=off}
~~~~~~~~
$ lein perft
Depth 1: 20 nodes (expected 20) [PASS] 0.00s (200,000 nps)
Depth 2: 400 nodes (expected 400) [PASS] 0.00s (800,000 nps)
Depth 3: 8902 nodes (expected 8902) [PASS] 0.02s (445,100 nps)
~~~~~~~~

All three depths must pass for confidence in move generation correctness. The perft test also serves as a rough performance benchmark, reporting nodes-per-second throughput.

## Running the Game

Start the game with Leiningen or the Clojure CLI tools:

{linenos=off}
~~~~~~~~
$ lein run

=== Chess Game ===

1. Play as White
2. Play as Black
3. Bot vs Bot

Choose mode (1-3):
~~~~~~~~

Select a mode and bot depth (1--6), then enter moves in UCI format (e.g., `e2e4`, `g1f3`, `e7e8q` for promotion). The bot's thinking output shows the move, evaluation score in centipawns, nodes searched, and elapsed time.

## Summary

This chess engine demonstrates how Clojure's persistent data structures and functional approach can be applied to a high-performance game engine. Key design decisions include:

- **Bit-packed piece encoding** for compact, fast piece property tests
- **Precomputed move tables** to avoid recomputing move geometry
- **Incremental Zobrist hashing** for O(1) position identification
- **Transposition table** caching to avoid redundant search of previously visited positions
- **Iterative deepening with alpha-beta** for efficient tree traversal with optimal move ordering
- **Quiescence search** to prevent horizon-effect blunders in tactical positions

The complete implementation is approximately 1,100 lines of Clojure, demonstrating that you don't need a large codebase to build a competent chess-playing program.
