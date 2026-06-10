(ns chess-game.perft
  (:require [chess-game.engine :refer :all]))

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

(defn run-perft []
  (let [board (from-fen "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
        expected [20 400 8902]]
    (println "Perft tests from starting position:\n")
    (doseq [depth (range 1 4)]
      (let [start (System/nanoTime)
            nodes (perft board depth)
            elapsed (double (/ (- (System/nanoTime) start) 1e9))
            elapsed-str (format "%.2f" elapsed)
            expected-nodes (get expected (dec depth))
            status (if (= nodes expected-nodes) "PASS" "FAIL")
            nps (if (> elapsed 0.0) (long (Math/round (/ nodes elapsed))) 0)]
        (println (str "Depth " depth ": " nodes " nodes (expected " expected-nodes ") [" status "] "
                      elapsed-str "s (" (format "%,d" nps) " nps)"))))))

(defn -main [& args]
  (run-perft))
