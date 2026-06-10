(ns chess-game.ai
  (:require [chess-game.engine :refer :all]))

;; Material values
(def PIECE-VALUES
  {PAWN 100, KNIGHT 320, BISHOP 330, ROOK 500, QUEEN 900, KING 20000})

;; Piece-square tables from White's perspective (sq 0=a1 .. 63=h8)
(def PAWN-PST
  [0,  0,  0,  0,  0,  0,  0,  0,
   50, 50, 50, 50, 50, 50, 50, 50,
   10, 10, 20, 30, 30, 20, 10, 10,
    5,  5, 10, 25, 25, 10,  5,  5,
    0,  0,  0, 20, 20,  0,  0,  0,
    5, -5,-10,  0,  0,-10, -5,  5,
    5, 10, 10,-20,-20, 10, 10,  5,
    0,  0,  0,  0,  0,  0,  0,  0])

(def KNIGHT-PST
  [-50,-40,-30,-30,-30,-30,-40,-50,
   -40,-20,  0,  0,  0,  0,-20,-40,
   -30,  0, 10, 15, 15, 10,  0,-30,
   -30,  5, 15, 20, 20, 15,  5,-30,
   -30,  0, 15, 20, 20, 15,  0,-30,
   -30,  5, 10, 15, 15, 10,  5,-30,
   -40,-20,  0,  5,  5,  0,-20,-40,
   -50,-40,-30,-30,-30,-30,-40,-50])

(def BISHOP-PST
  [-20,-10,-10,-10,-10,-10,-10,-20,
   -10,  0,  0,  0,  0,  0,  0,-10,
   -10,  0, 10, 10, 10, 10,  0,-10,
   -10,  5,  5, 10, 10,  5,  5,-10,
   -10,  0, 10, 10, 10, 10,  0,-10,
   -10, 10, 10, 10, 10, 10, 10,-10,
   -10,  5,  0,  0,  0,  0,  5,-10,
   -20,-10,-10,-10,-10,-10,-10,-20])

(def ROOK-PST
  [0,  0,  0,  0,  0,  0,  0,  0,
   5, 10, 10, 10, 10, 10, 10,  5,
  -5,  0,  0,  0,  0,  0,  0, -5,
  -5,  0,  0,  0,  0,  0,  0, -5,
  -5,  0,  0,  0,  0,  0,  0, -5,
  -5,  0,  0,  0,  0,  0,  0, -5,
  -5,  0,  0,  0,  0,  0,  0, -5,
   0,  0,  0,  5,  5,  0,  0,  0])

(def QUEEN-PST
  [-20,-10,-10, -5, -5,-10,-10,-20,
   -10,  0,  0,  0,  0,  0,  0,-10,
   -10,  0,  5,  5,  5,  5,  0,-10,
    -5,  0,  5,  5,  5,  5,  0, -5,
     0,  0,  5,  5,  5,  5,  0, -5,
   -10,  5,  5,  5,  5,  5,  0,-10,
   -10,  0,  5,  0,  0,  0,  0,-10,
   -20,-10,-10, -5, -5,-10,-10,-20])

(def KING-MIDDLE-PST
  [-30,-40,-40,-50,-50,-40,-40,-30,
   -30,-40,-40,-50,-50,-40,-40,-30,
   -30,-40,-40,-50,-50,-40,-40,-30,
   -30,-40,-40,-50,-50,-40,-40,-30,
   -20,-30,-30,-40,-40,-30,-30,-20,
   -10,-20,-20,-20,-20,-20,-20,-10,
    20, 20,  0,  0,  0,  0, 20, 20,
    20, 30, 10,  0,  0, 10, 30, 20])

(def KING-END-PST
  [-50,-40,-30,-20,-20,-30,-40,-50,
   -30,-20,-10,  0,  0,-10,-20,-30,
   -30,-10, 20, 30, 30, 20,-10,-30,
   -30,-10, 30, 40, 40, 30,-10,-30,
   -30,-10, 30, 40, 40, 30,-10,-30,
   -30,-10, 20, 30, 30, 20,-10,-30,
   -30,-30,  0,  0,  0,  0,-30,-30,
   -50,-30,-30,-30,-30,-30,-30,-50])

(def PST-TABLES
  {PAWN PAWN-PST, KNIGHT KNIGHT-PST, BISHOP BISHOP-PST,
   ROOK ROOK-PST, QUEEN QUEEN-PST})

(defn is-endgame? [board]
  (let [board-vec (:board board)
        material (reduce (fn [sum sq]
                           (let [pt (piece-type (get board-vec sq))]
                             (if (and (not= pt PAWN) (not= pt KING))
                               (+ sum (get PIECE-VALUES pt))
                               sum)))
                         0
                         (concat (get-in board [:pieces 0]) (get-in board [:pieces 1])))]
    (<= material 3000)))

(defn evaluate [board]
  (let [board-vec (:board board)
        white-pieces (get-in board [:pieces 0])
        black-pieces (get-in board [:pieces 1])
        
        ;; White piece values + PST
        white-score (reduce (fn [score sq]
                              (let [pt (piece-type (get board-vec sq))
                                    val (get PIECE-VALUES pt)]
                                (if (not= pt KING)
                                  (+ score val (get (get PST-TABLES pt) sq))
                                  (+ score val))))
                            0
                            white-pieces)
        
        ;; Black piece values + PST
        black-score (reduce (fn [score sq]
                              (let [pt (piece-type (get board-vec sq))
                                    val (get PIECE-VALUES pt)]
                                (if (not= pt KING)
                                  (+ score val (get (get PST-TABLES pt) (bit-xor sq 56)))
                                  (+ score val))))
                            0
                            black-pieces)
        
        ;; King PST
        endgame? (is-endgame? board)
        king-table (if endgame? KING-END-PST KING-MIDDLE-PST)
        
        white-king-sq (get-in board [:king-squares 0])
        black-king-sq (get-in board [:king-squares 1])
        
        white-king-bonus (get king-table white-king-sq)
        black-king-bonus (get king-table (bit-xor black-king-sq 56))
        
        score (+ (- white-score black-score) white-king-bonus (- black-king-bonus))]
    
    (if (= (:turn board) WHITE)
      score
      (- score))))

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

;; Transposition table
(def TT-EXACT 0)
(def TT-ALPHA 1)
(def TT-BETA 2)

(def transposition-table (atom {}))
(def nodes-visited (atom 0))
(def max-depth (atom 3))

(declare quiescence-search)

(defn search [board depth alpha beta]
  (swap! nodes-visited inc)
  (if (>= (:halfmove-clock board) 100)
    0
    (let [hash-val (:zobrist-hash board)
          tt-entry (get @transposition-table hash-val)
          original-alpha alpha
          
          ;; TT lookup and window adjustments
          [alpha beta tt-best-move tt-score-cutoff?]
          (if (and tt-entry (>= (:depth tt-entry) depth))
            (let [stored-score (:score tt-entry)
                  ;; Adjust mate score for distance from root
                  score (cond
                          (> stored-score 29000) (- stored-score (- @max-depth depth))
                          (< stored-score -29000) (+ stored-score (- @max-depth depth))
                          :else stored-score)]
              (cond
                (= (:flag tt-entry) TT-EXACT) [alpha beta (:best-move tt-entry) score]
                (and (= (:flag tt-entry) TT-ALPHA) (<= score alpha)) [alpha beta (:best-move tt-entry) score]
                (and (= (:flag tt-entry) TT-BETA) (>= score beta)) [alpha beta (:best-move tt-entry) score]
                :else
                (let [new-alpha (if (= (:flag tt-entry) TT-ALPHA) (max alpha score) alpha)
                      new-beta (if (= (:flag tt-entry) TT-BETA) (min beta score) beta)]
                  (if (>= new-alpha new-beta)
                    [new-alpha new-beta (:best-move tt-entry) score]
                    [new-alpha new-beta (:best-move tt-entry) nil]))))
            [alpha beta (and tt-entry (:best-move tt-entry)) nil])]
      
      (if tt-score-cutoff?
        tt-score-cutoff?
        (let [legal-moves (get-legal-moves board)]
          (cond
            (empty? legal-moves)
            (if (is-in-check? board)
              (+ -30000 (- @max-depth depth)) ; Checkmate
              0) ; Stalemate
            
            (= depth 0)
            (quiescence-search board alpha beta)
            
            :else
            (let [sorted-moves (sort-by (fn [m] (move-value board m tt-best-move)) > legal-moves)]
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
                        ;; Beta cutoff (stored as TT-ALPHA in TS terminology)
                        (do
                          (let [stored-score (cond
                                               (> new-best-score 29000) (+ new-best-score (- @max-depth depth))
                                               (< new-best-score -29000) (- new-best-score (- @max-depth depth))
                                               :else new-best-score)
                                existing (get @transposition-table hash-val)]
                            (when (or (nil? existing) (>= depth (:depth existing)))
                              (swap! transposition-table assoc hash-val
                                     {:depth depth :score stored-score :flag TT-ALPHA :best-move new-best-move})))
                          new-best-score)
                        (recur (rest moves) new-best-score new-best-move next-alpha))))
                  
                  ;; No more moves, store exact (TT-EXACT) or alpha-bound (TT-BETA in TS terminology)
                  (let [flag (if (<= best-score original-alpha) TT-BETA TT-EXACT)
                        stored-score (cond
                                       (> best-score 29000) (+ best-score (- @max-depth depth))
                                       (< best-score -29000) (- best-score (- @max-depth depth))
                                       :else best-score)
                        existing (get @transposition-table hash-val)]
                    (when (or (nil? existing) (>= depth (:depth existing)))
                      (swap! transposition-table assoc hash-val
                             {:depth depth :score stored-score :flag flag :best-move best-move}))
                    best-score))))))))))

(defn quiescence-search [board alpha beta]
  (swap! nodes-visited inc)
  (let [stand-pat (evaluate board)]
    (if (>= stand-pat beta)
      beta
      (let [curr-alpha (max alpha stand-pat)
            captures (filter (fn [m]
                               (let [new-board (make-move board m)]
                                 (not (square-attacked? new-board (get-in new-board [:king-squares (color-idx (piece-color (:piece-moved m)))]) (:turn new-board)))))
                             (filter (fn [m] (or (and (:piece-captured m) (not= (:piece-captured m) EMPTY))
                                                 (and (:promotion m) (not= (:promotion m) EMPTY))))
                                     (get-pseudo-legal-moves board)))
            sorted-captures (sort-by (fn [m] (move-value board m nil)) > captures)]
        (loop [moves sorted-captures
               c-alpha curr-alpha]
          (if-let [move (first moves)]
            (let [new-board (make-move board move)
                  score (- (quiescence-search new-board (- beta) (- c-alpha)))]
              (if (>= score beta)
                beta
                (recur (rest moves) (max c-alpha score))))
            c-alpha))))))

(defn get-best-move [board depth]
  (when (> (count @transposition-table) 500000)
    (reset! transposition-table {}))
  
  (let [best-move (atom nil)
        best-score (atom 0)]
    (doseq [d (range 1 (inc depth))]
      (reset! max-depth d)
      (reset! nodes-visited 0)
      
      (let [legal-moves (get-legal-moves board)
            tt-entry (get @transposition-table (:zobrist-hash board))
            tt-best (and tt-entry (:best-move tt-entry))
            sorted-moves (sort-by (fn [m] (move-value board m tt-best)) > legal-moves)]
        
        (loop [moves sorted-moves
               curr-best nil
               curr-score -1000000000
               alpha -1000000000
               beta 1000000000]
          (if-let [move (first moves)]
            (let [new-board (make-move board move)
                  score (- (search new-board (dec d) (- beta) (- alpha)))]
              (let [new-curr-score (max curr-score score)
                    new-curr-best (if (> score curr-score) move curr-best)
                    next-alpha (max alpha score)]
                (recur (rest moves) new-curr-best new-curr-score next-alpha beta)))
            
            (when curr-best
              (reset! best-move curr-best)
              (reset! best-score curr-score)
              (swap! transposition-table assoc (:zobrist-hash board)
                     {:depth d :score curr-score :flag TT-EXACT :best-move curr-best}))))))
    [@best-move @best-score]))
