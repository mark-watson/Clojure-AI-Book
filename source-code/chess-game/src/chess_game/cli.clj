(ns chess-game.cli
  (:require [chess-game.engine :refer :all]
            [chess-game.ai :refer :all]
            [clojure.string :as str])
  (:gen-class))

;; Unicode chess pieces
(def PIECE-GLYPHS
  {(bit-or WHITE KING) "♔" (bit-or WHITE QUEEN) "♕" (bit-or WHITE ROOK) "♖"
   (bit-or WHITE BISHOP) "♗" (bit-or WHITE KNIGHT) "♘" (bit-or WHITE PAWN) "♙"
   (bit-or BLACK KING) "♚" (bit-or BLACK QUEEN) "♛" (bit-or BLACK ROOK) "♜"
   (bit-or BLACK BISHOP) "♝" (bit-or BLACK KNIGHT) "♞" (bit-or BLACK PAWN) "♟"})

(defn print-board [board]
  (let [board-vec (:board board)
        info (transient [])]
    (conj! info (str "\u001b[1;37mTurn: " (if (= (:turn board) WHITE) "White" "Black") "\u001b[0m"))
    (conj! info (str "Move: " (:fullmove-number board)))
    (conj! info (str "50-move: " (:halfmove-clock board)))
    (when (is-in-check? board)
      (conj! info "\u001b[1;31mIN CHECK!\u001b[0m"))
    (conj! info (str "EP: " (if (= (:en-passant-square board) -1) "-" (get SQUARE-NAMES (:en-passant-square board)))))
    (let [rights (:castling-rights board)
          castling (str (if (not= (bit-and rights WK) 0) "K" "")
                        (if (not= (bit-and rights WQ) 0) "Q" "")
                        (if (not= (bit-and rights BK) 0) "k" "")
                        (if (not= (bit-and rights BQ) 0) "q" ""))]
      (conj! info (str "Castling: " (if (empty? castling) "-" castling))))
    (conj! info (str "Eval: " (format "%.1f" (double (/ (evaluate board) 100)))))
    
    (let [p-info (persistent! info)]
      (println "")
      (doseq [rank (reverse (range 8))]
        (let [row (transient [(str "\u001b[90m" (inc rank) "\u001b[0m ")])]
          (doseq [file (range 8)]
            (let [sq (+ (* rank 8) file)
                  p (get board-vec sq)
                  bg (if (= (mod (+ rank file) 2) 0) "\u001b[48;5;237m" "\u001b[48;5;94m")]
              (if (= p EMPTY)
                (conj! row (str bg "  \u001b[0m"))
                (let [color (if (= (piece-color p) WHITE) "\u001b[1;37m" "\u001b[1;35m")]
                  (conj! row (str bg color (get PIECE-GLYPHS p) " \u001b[0m"))))))
          (let [extra (get p-info (- 7 rank))]
            (println (str (str/join "" (persistent! row)) (if extra (str "  " extra) ""))))))
      (println "\u001b[90m  a b c d e f g h\u001b[0m\n"))))

(defn parse-move [board input]
  (let [s (str/lower-case (str/trim input))]
    (when (and (>= (count s) 4) (<= (count s) 5))
      (let [from-name (subs s 0 2)
            to-name (subs s 2 4)
            from (get NAME-TO-SQUARE from-name)
            to (get NAME-TO-SQUARE to-name)]
        (when (and from to)
          (let [prom-char (if (= (count s) 5) (str (nth s 4)) nil)
                prom-type (if prom-char
                            (get {"q" QUEEN, "r" ROOK, "b" BISHOP, "n" KNIGHT} prom-char 0)
                            0)
                legal-moves (get-legal-moves board)]
            (some (fn [m]
                    (when (and (= (:from m) from) (= (:to m) to))
                      (cond
                        (and (not= prom-type 0) (= (:promotion m) prom-type)) m
                        (and (= prom-type 0) (or (= (:promotion m) EMPTY) (= (:promotion m) QUEEN))) m
                        :else nil)))
                  legal-moves)))))))

(defn ask [q]
  (print q)
  (flush)
  (read-line))

(defn play-game []
  (println "\n\u001b[1;36m=== Chess Game ===\u001b[0m\n")
  (println "1. Play as White")
  (println "2. Play as Black")
  (println "3. Bot vs Bot")
  
  (let [mode-str (ask "\nChoose mode (1-3): ")
        mode (try (Integer/parseInt (str/trim mode-str))
                  (catch Exception _ 1))
        human-color (cond
                      (= mode 1) WHITE
                      (= mode 2) BLACK
                      :else nil)
        depth-str (ask "Bot depth (1-6, default 3): ")
        depth (try (let [d (Integer/parseInt (str/trim depth-str))]
                     (max 1 (min 6 d)))
                   (catch Exception _ 3))]
    
    (loop [board (from-fen "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")]
      (print-board board)
      (let [legal-moves (get-legal-moves board)]
        (cond
          (empty? legal-moves)
          (if (is-in-check? board)
            (let [winner (if (= (:turn board) WHITE) "Black" "White")]
              (println (str "\u001b[1;33mCheckmate! " winner " wins.\u001b[0m")))
            (println "\u001b[1;33mStalemate! Draw.\u001b[0m"))
          
          (>= (:halfmove-clock board) 100)
          (println "\u001b[1;33m50-move rule! Draw.\u001b[0m")
          
          ;; Bot turn
          (or (nil? human-color) (not= (:turn board) human-color))
          (do
            (println (str "\u001b[90mBot is thinking (depth " depth ")...\u001b[0m"))
            (let [start-time (System/nanoTime)
                  [move score] (get-best-move board depth)
                  elapsed (double (/ (- (System/nanoTime) start-time) 1e9))
                  elapsed-str (format "%.1f" elapsed)]
              (println (str "Bot plays: " (move->uci move)
                            " | eval: " (format "%.1f" (double (/ score 100)))
                            " | nodes: " @nodes-visited
                            " | time: " elapsed-str "s"))
              (let [next-board (make-move board move)]
                (when (nil? human-color)
                  (Thread/sleep 500))
                (recur next-board))))
          
          ;; Human turn
          :else
          (let [input (ask "Your move (or help): ")]
            (cond
              (or (= input "exit") (= input "quit"))
              (println "Goodbye.")
              
              (= input "help")
              (do
                (println "Commands: <uci move> (e.g. e2e4), fen, setfen, legal, reset, help, exit")
                (recur board))
              
              (= input "fen")
              (do
                (println (to-fen board))
                (recur board))
              
              (= input "setfen")
              (let [fen (ask "Enter FEN: ")
                    next-board (from-fen (str/trim fen))]
                (recur next-board))
              
              (= input "legal")
              (do
                (println (str/join " " (map move->uci legal-moves)))
                (recur board))
              
              (= input "reset")
              (recur (from-fen "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"))
              
              :else
              (if-let [move (parse-move board input)]
                (recur (make-move board move))
                (do
                  (println "Invalid move. Try UCI format like e2e4, or 'help' for commands.")
                  (recur board))))))))))

(defn -main [& args]
  (play-game))
