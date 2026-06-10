(ns chess-game.engine
  (:require [clojure.string :as str]))

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

;; Castling rights
(def WK 1)
(def WQ 2)
(def BK 4)
(def BQ 8)

;; Piece characters for FEN
(def PIECE-CHARS
  {(bit-or WHITE PAWN) "P", (bit-or WHITE KNIGHT) "N", (bit-or WHITE BISHOP) "B"
   (bit-or WHITE ROOK) "R", (bit-or WHITE QUEEN) "Q", (bit-or WHITE KING) "K"
   (bit-or BLACK PAWN) "p", (bit-or BLACK KNIGHT) "n", (bit-or BLACK BISHOP) "b"
   (bit-or BLACK ROOK) "r", (bit-or BLACK QUEEN) "q", (bit-or BLACK KING) "k"})

;; Square mapping: index 0-63 maps to algebraic notation a1..h8
(def SQUARE-NAMES
  (vec (for [row (range 8)
             col (range 8)]
         (str (get "abcdefgh" col) (get "12345678" row)))))

(def NAME-TO-SQUARE
  (into {} (map-indexed (fn [idx name] [name idx]) SQUARE-NAMES)))

(defn file-of [sq] (bit-and sq 7))
(defn rank-of [sq] (unsigned-bit-shift-right sq 3))

;; Precomputed move tables
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

(def KING-MOVES
  (vec (for [sq (range 64)]
         (let [r (quot sq 8)
               f (mod sq 8)
               offsets [[-1 -1] [-1 0] [-1 1] [0 -1] [0 1] [1 -1] [1 0] [1 1]]]
           (vec (for [[dr df] offsets
                      :let [nr (+ r dr)
                            nf (+ f df)]
                      :when (and (>= nr 0) (< nr 8) (>= nf 0) (< nf 8))]
                  (+ (* nr 8) nf)))))))

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

(def BISHOP-RAYS
  (vec (for [sq (range 64)]
         (compute-rays sq [[1 1] [1 -1] [-1 1] [-1 -1]]))))

(def QUEEN-RAYS
  (vec (for [sq (range 64)]
         (vec (concat (get ROOK-RAYS sq) (get BISHOP-RAYS sq))))))

;; Zobrist hashing — deterministic seeded 64-bit PRNG
(defn- create-rng [seed]
  (let [state (atom (bit-or (long seed) 1))]
    (fn []
      (swap! state (fn [^long curr]
                     (unchecked-add (unchecked-multiply curr 6364136223846793005) 1442695040888963407))))))

(def rng (create-rng 1337))

(def ZOBRIST_PIECES
  (vec (for [_ (range 64)]
         (vec (for [_ (range 32)] (rng))))))

(def ZOBRIST_SIDE (rng))

(def ZOBRIST_CASTLING
  (vec (for [_ (range 16)] (rng))))

(def ZOBRIST_EP
  (vec (for [_ (range 64)] (rng))))

;; Move record representation
(defrecord Move [from to piece-moved piece-captured promotion en-passant? castling? double-push?])

(defn move->uci [move]
  (let [from (:from move)
        to (:to move)
        promo (:promotion move)
        prom-chars {QUEEN "q", ROOK "r", BISHOP "b", KNIGHT "n"}
        base (str (get SQUARE-NAMES from) (get SQUARE-NAMES to))]
    (if (and promo (not= promo EMPTY))
      (str base (get prom-chars promo))
      base)))

(defn move-equals? [a b]
  (and a b
       (= (:from a) (:from b))
       (= (:to a) (:to b))
       (= (:promotion a) (:promotion b))))

;; Compute full Zobrist hash from scratch
(defn compute-zobrist-hash [board]
  (let [board-vec (:board board)
        h (atom 0)]
    (doseq [sq (range 64)
            :let [p (get board-vec sq)]
            :when (not= p EMPTY)]
      (swap! h bit-xor (get-in ZOBRIST_PIECES [sq p])))
    (when (= (:turn board) BLACK)
      (swap! h bit-xor ZOBRIST_SIDE))
    (swap! h bit-xor (get ZOBRIST_CASTLING (:castling-rights board)))
    (when (not= (:en-passant-square board) -1)
      (swap! h bit-xor (get ZOBRIST_EP (:en-passant-square board))))
    (long @h)))

;; Square attacks detection
(defn square-attacked? [board sq attacker-color]
  (let [r (quot sq 8)
        f (mod sq 8)
        board-vec (:board board)
        
        ;; Pawn attacks
        pawn-dir (if (= attacker-color WHITE) -8 8)
        pawn-attack? (some (fn [df]
                             (let [nf (+ f df)]
                               (and (>= nf 0) (<= nf 7)
                                    (let [pawn-sq (+ sq pawn-dir df)]
                                      (and (>= pawn-sq 0) (< pawn-sq 64)
                                           (= (get board-vec pawn-sq) (bit-or attacker-color PAWN)))))))
                           [-1 1])
        
        ;; Knight attacks
        knight-attack? (some (fn [t]
                               (= (get board-vec t) (bit-or attacker-color KNIGHT)))
                             (get KNIGHT-MOVES sq))
        
        ;; King attacks
        king-attack? (some (fn [t]
                             (= (get board-vec t) (bit-or attacker-color KING)))
                           (get KING-MOVES sq))
        
        ;; Rook/Queen rays
        rook-queen-attack? (some (fn [ray]
                                   (loop [ray ray]
                                     (if-let [t (first ray)]
                                       (let [p (get board-vec t)]
                                         (if (not= p EMPTY)
                                           (and (= (piece-color p) attacker-color)
                                                (or (= (piece-type p) ROOK)
                                                    (= (piece-type p) QUEEN)))
                                           (recur (rest ray))))
                                       false)))
                                 (get ROOK-RAYS sq))
        
        ;; Bishop/Queen rays
        bishop-queen-attack? (some (fn [ray]
                                     (loop [ray ray]
                                       (if-let [t (first ray)]
                                         (let [p (get board-vec t)]
                                           (if (not= p EMPTY)
                                             (and (= (piece-color p) attacker-color)
                                                  (or (= (piece-type p) BISHOP)
                                                      (= (piece-type p) QUEEN)))
                                             (recur (rest ray))))
                                         false)))
                                   (get BISHOP-RAYS sq))]
    (boolean (or pawn-attack? knight-attack? king-attack? rook-queen-attack? bishop-queen-attack?))))

(defn is-in-check?
  ([board] (is-in-check? board (:turn board)))
  ([board color]
   (square-attacked? board (get-in board [:king-squares (color-idx color)]) (opponent color))))

;; Pseudo-legal move generator
(defn get-pseudo-legal-moves [board]
  (let [color (:turn board)
        opp (opponent color)
        board-vec (:board board)
        pieces (get-in board [:pieces (color-idx color)])
        forward (if (= color WHITE) 8 -8)
        start-rank (if (= color WHITE) 1 6)
        promo-rank (if (= color WHITE) 7 0)
        promo-pieces [QUEEN ROOK BISHOP KNIGHT]
        moves (transient [])]
    (doseq [sq pieces]
      (let [piece (get board-vec sq)
            ptype (piece-type piece)
            r (quot sq 8)
            f (mod sq 8)]
        (cond
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
                        (conj! moves (->Move sq double-sq piece EMPTY EMPTY false false true))))))))
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
                      (conj! moves (->Move sq cap-sq piece target pp false false false)))
                    (conj! moves (->Move sq cap-sq piece target EMPTY false false false)))
                  
                  (= cap-sq (:en-passant-square board))
                  (let [ep-captured (get board-vec (- cap-sq forward))]
                    (conj! moves (->Move sq cap-sq piece ep-captured EMPTY true false false)))))))
          
          (= ptype KNIGHT)
          (doseq [t (get KNIGHT-MOVES sq)]
            (let [target (get board-vec t)]
              (cond
                (= target EMPTY)
                (conj! moves (->Move sq t piece EMPTY EMPTY false false false))
                (= (piece-color target) opp)
                (conj! moves (->Move sq t piece target EMPTY false false false)))))
          
          (= ptype KING)
          (do
            (doseq [t (get KING-MOVES sq)]
              (let [target (get board-vec t)]
                (cond
                  (= target EMPTY)
                  (conj! moves (->Move sq t piece EMPTY EMPTY false false false))
                  (= (piece-color target) opp)
                  (conj! moves (->Move sq t piece target EMPTY false false false)))))
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
                (when (and (not= (bit-and (:castling-rights board) WQ) 0)
                           (= (get board-vec 3) EMPTY)
                           (= (get board-vec 2) EMPTY)
                           (= (get board-vec 1) EMPTY))
                  (when (not (or (square-attacked? board 4 opp)
                                 (square-attacked? board 3 opp)
                                 (square-attacked? board 2 opp)))
                    (conj! moves (->Move 4 2 piece EMPTY EMPTY false true false)))))
              ;; Black castling
              (when (= sq 60)
                (when (and (not= (bit-and (:castling-rights board) BK) 0)
                           (= (get board-vec 61) EMPTY)
                           (= (get board-vec 62) EMPTY))
                  (when (not (or (square-attacked? board 60 opp)
                                 (square-attacked? board 61 opp)
                                 (square-attacked? board 62 opp)))
                    (conj! moves (->Move 60 62 piece EMPTY EMPTY false true false))))
                (when (and (not= (bit-and (:castling-rights board) BQ) 0)
                           (= (get board-vec 59) EMPTY)
                           (= (get board-vec 58) EMPTY)
                           (= (get board-vec 57) EMPTY))
                  (when (not (or (square-attacked? board 60 opp)
                                 (square-attacked? board 59 opp)
                                 (square-attacked? board 58 opp)))
                    (conj! moves (->Move 60 58 piece EMPTY EMPTY false true false)))))))
          
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
                        (conj! moves (->Move sq t piece EMPTY EMPTY false false false))
                        (recur (rest r-sqs)))
                      (= (piece-color target) opp)
                      (conj! moves (->Move sq t piece target EMPTY false false false))
                      :else nil)))))))))
    (persistent! moves)))

;; Execute move, producing a brand-new board state
(defn make-move [board move]
  (let [color (piece-color (:piece-moved move))
        opp (opponent color)
        ci (color-idx color)
        oi (color-idx opp)
        board-vec (:board board)
        
        ;; Start building new collections
        new-board-vec (atom (assoc board-vec (:from move) EMPTY))
        new-pieces-color (atom (disj (get-in board [:pieces ci]) (:from move)))
        new-pieces-opp (atom (get-in board [:pieces oi]))
        new-king-squares (atom (:king-squares board))
        
        ;; En passant capture sq and piece captured
        ep? (:en-passant? move)
        captured-sq (if ep?
                      (- (:to move) (if (= color WHITE) 8 -8))
                      (:to move))
        
        ;; incremental zobrist updates
        h-atom (atom (long (:zobrist-hash board)))]
    
    ;; 1. XOR out old turn, castling rights, and old en passant
    (swap! h-atom bit-xor ZOBRIST_SIDE)
    (swap! h-atom bit-xor (get ZOBRIST_CASTLING (:castling-rights board)))
    (when (not= (:en-passant-square board) -1)
      (swap! h-atom bit-xor (get ZOBRIST_EP (:en-passant-square board))))
    
    ;; XOR out moving piece from its origin
    (swap! h-atom bit-xor (get-in ZOBRIST_PIECES [(:from move) (:piece-moved move)]))
    
    ;; Handle capture
    (cond
      ep?
      (let [captured-piece (get board-vec captured-sq)]
        (swap! new-board-vec assoc captured-sq EMPTY)
        (swap! new-pieces-opp disj captured-sq)
        (swap! h-atom bit-xor (get-in ZOBRIST_PIECES [captured-sq captured-piece])))
      
      (and (:piece-captured move) (not= (:piece-captured move) EMPTY))
      (do
        (swap! new-pieces-opp disj (:to move))
        (swap! h-atom bit-xor (get-in ZOBRIST_PIECES [(:to move) (:piece-captured move)]))))
    
    ;; Place the piece
    (let [final-piece (if (and (:promotion move) (not= (:promotion move) EMPTY))
                        (bit-or color (:promotion move))
                        (:piece-moved move))]
      (swap! new-board-vec assoc (:to move) final-piece)
      (swap! new-pieces-color conj (:to move))
      (swap! h-atom bit-xor (get-in ZOBRIST_PIECES [(:to move) final-piece]))
      
      ;; King square update
      (when (= (piece-type (:piece-moved move)) KING)
        (swap! new-king-squares assoc ci (:to move)))
      
      ;; Castling rook
      (when (:castling? move)
        (let [[rook-from rook-to] (condp = (:to move)
                                    6 [7 5]
                                    2 [0 3]
                                    62 [63 61]
                                    58 [56 59])
              rook (bit-or color ROOK)]
          (swap! new-board-vec assoc rook-from EMPTY rook-to rook)
          (swap! new-pieces-color disj rook-from)
          (swap! new-pieces-color conj rook-to)
          (swap! h-atom bit-xor (get-in ZOBRIST_PIECES [rook-from rook]))
          (swap! h-atom bit-xor (get-in ZOBRIST_PIECES [rook-to rook])))))
    
    ;; Update castling rights
    (let [old-rights (:castling-rights board)
          r1 (if (= (piece-type (:piece-moved move)) KING)
               (if (= color WHITE)
                 (bit-and old-rights (bit-not (bit-or WK WQ)))
                 (bit-and old-rights (bit-not (bit-or BK BQ))))
               old-rights)
          ;; Rook moves or captures affecting castling
          r2 (cond-> r1
               (or (= (:from move) 7) (= (:to move) 7)) (bit-and (bit-not WK))
               (or (= (:from move) 0) (= (:to move) 0)) (bit-and (bit-not WQ))
               (or (= (:from move) 63) (= (:to move) 63)) (bit-and (bit-not BK))
               (or (= (:from move) 56) (= (:to move) 56)) (bit-and (bit-not BQ)))
          new-rights r2
          
          ;; En passant
          new-ep (if (:double-push? move)
                   (- (:to move) (if (= color WHITE) 8 -8))
                   -1)
          
          ;; Clocks
          pawn? (= (piece-type (:piece-moved move)) PAWN)
          cap? (and (:piece-captured move) (not= (:piece-captured move) EMPTY))
          new-halfmove (if (or pawn? cap?) 0 (inc (:halfmove-clock board)))
          new-fullmove (if (= color BLACK) (inc (:fullmove-number board)) (:fullmove-number board))]
      
      ;; XOR in new castling rights, new EP
      (swap! h-atom bit-xor (get ZOBRIST_CASTLING new-rights))
      (when (not= new-ep -1)
        (swap! h-atom bit-xor (get ZOBRIST_EP new-ep)))
      
      {:board @new-board-vec
       :pieces (if (= color WHITE)
                 [@new-pieces-color @new-pieces-opp]
                 [@new-pieces-opp @new-pieces-color])
       :king-squares @new-king-squares
       :turn opp
       :castling-rights new-rights
       :en-passant-square new-ep
       :halfmove-clock new-halfmove
       :fullmove-number new-fullmove
       :zobrist-hash (long @h-atom)})))

;; Get fully legal moves
(defn get-legal-moves [board]
  (let [color (:turn board)
        ci (color-idx color)
        opp (opponent color)]
    (filter (fn [move]
              (let [new-board (make-move board move)]
                (not (square-attacked? new-board (get-in new-board [:king-squares ci]) opp))))
            (get-pseudo-legal-moves board))))

;; FEN Parsers and Writers
(defn from-fen [fen]
  (let [parts (str/split fen #" ")
        [placement active-color castling ep halfmove fullmove] parts
        
        board-vec (atom (vec (repeat 64 EMPTY)))
        pieces-w (atom #{})
        pieces-b (atom #{})
        
        type-map {"p" PAWN, "n" KNIGHT, "b" BISHOP, "r" ROOK, "q" QUEEN, "k" KING}
        
        ;; Parse piece placement
        ranks (str/split placement #"/")]
    (doseq [rank-idx (range 8)
            :let [rank-str (get ranks (- 7 rank-idx))
                  file-atom (atom 0)]]
      (doseq [ch (map str rank-str)]
        (if (re-matches #"[1-8]" ch)
          (swap! file-atom + (Integer/parseInt ch))
          (let [color (if (= ch (str/upper-case ch)) WHITE BLACK)
                ptype (get type-map (str/lower-case ch))
                piece (bit-or color ptype)
                sq (+ (* rank-idx 8) @file-atom)]
            (swap! board-vec assoc sq piece)
            (if (= color WHITE)
              (swap! pieces-w conj sq)
              (swap! pieces-b conj sq))
            (swap! file-atom inc)))))
    
    (let [turn (if (= active-color "w") WHITE BLACK)
          castling-rights (reduce (fn [rights ch]
                                    (condp = ch
                                      \K (bit-or rights WK)
                                      \Q (bit-or rights WQ)
                                      \k (bit-or rights BK)
                                      \q (bit-or rights BQ)
                                      rights))
                                  0
                                  castling)
          ep-square (if (= ep "-") -1 (get NAME-TO-SQUARE ep))
          halfmove-clock (Integer/parseInt halfmove)
          fullmove-number (Integer/parseInt fullmove)
          
          ;; Find king squares
          find-king (fn [board color]
                      (some (fn [sq]
                              (when (= (get board sq) (bit-or color KING))
                                sq))
                            (range 64)))
          wk-sq (find-king @board-vec WHITE)
          bk-sq (find-king @board-vec BLACK)
          
          initial-board {:board @board-vec
                         :pieces [@pieces-w @pieces-b]
                         :king-squares [wk-sq bk-sq]
                         :turn turn
                         :castling-rights castling-rights
                         :en-passant-square ep-square
                         :halfmove-clock halfmove-clock
                         :fullmove-number fullmove-number}]
      (assoc initial-board :zobrist-hash (compute-zobrist-hash initial-board)))))

(defn to-fen [board]
  (let [board-vec (:board board)
        ranks (for [rank (reverse (range 8))]
                (let [row-pieces (for [file (range 8)]
                                   (get board-vec (+ (* rank 8) file)))]
                  (loop [pieces row-pieces
                         empty-count 0
                         acc ""]
                    (if-let [p (first pieces)]
                      (if (= p EMPTY)
                        (recur (rest pieces) (inc empty-count) acc)
                        (let [ch (get PIECE-CHARS p)
                              new-acc (str acc (if (> empty-count 0) (str empty-count) "") ch)]
                          (recur (rest pieces) 0 new-acc)))
                      (str acc (if (> empty-count 0) (str empty-count) ""))))))
        placement (str/join "/" ranks)
        active-color (if (= (:turn board) WHITE) "w" "b")
        castling (str (if (not= (bit-and (:castling-rights board) WK) 0) "K" "")
                      (if (not= (bit-and (:castling-rights board) WQ) 0) "Q" "")
                      (if (not= (bit-and (:castling-rights board) BK) 0) "k" "")
                      (if (not= (bit-and (:castling-rights board) BQ) 0) "q" ""))
        castling (if (empty? castling) "-" castling)
        ep (if (= (:en-passant-square board) -1)
             "-"
             (get SQUARE-NAMES (:en-passant-square board)))]
    (str/join " " [placement active-color castling ep (:halfmove-clock board) (:fullmove-number board)])))
