(ns fluky.random
  (:require [fluky.utils :as fu]))

;; Note. This generator is not uniformly random.

(def ^{:dynamic true} *enable-random-generation* true)


(def default-range
  [[:RANGE [:CHAR \a] [:CHAR \z]]
   [:RANGE [:CHAR \A] [:CHAR \Z]]
   [:RANGE [:CHAR \0] [:CHAR \9]]])


(defn char-range-to-ascii
  [[token-type & r]]
  (case token-type
    :RANGE (mapv (comp int second) r)
    :CHAR [(int (first r)) (int (first r))]))


(defn random-in-range
  [[t & range :as r]]
  (case t
    :RANGE (char (apply fu/rand-range (char-range-to-ascii r)))
    :CHAR (first range)))


(defn rand-char-from-range
  [r]
  (when (and *enable-random-generation* (seq r))
    (char (random-in-range (rand-nth r)))))


(defn any-rand-char
  []
  (when *enable-random-generation*
    (rand-char-from-range default-range)))


(defn add-holes-to-range
  [[l u] [nl nu]]
  (cond
    (< nu l) [[l u]]

    (< u nl) [[l u]]

    (and (= l nl)
         (< nu u)) [[(inc nu) u]]

    (and (= l nl)
         (= u nu)) []

    (and (= l nl)
         (< u nu)) []

    (and (< l nl u)
         (= u nu)) [[l (dec nl)]]

    (and (< l nl u)
         (< u nu)) [[l (dec nl)]]

    (= u nl) [[l (dec u)]]

    (and (< l nl u)
         (< l nu u)) [[l (dec nl)]
                      [(inc nu) u]]

    (and (< nl l)
         (< l nu u)) [[(inc nu) u]]

    (and (< nl l)
         (= nu u)) []

    (and (= nl l)
         (< u nu)) []

    (and (< nl l)
         (< u nu)) []

    (and (< nl l)
         (< l nu)
         (= nu l)) [[(inc l) u]]

    (= nu l) [[(inc l) u]]))


(defn add-holes-to-ranges
  [orig-ranges ranges]
  (distinct
   (reduce (fn [acc r]
             (apply concat
                    (for [x acc]
                      (add-holes-to-range x r))))
           orig-ranges
           ranges)))


(let [alpha-range (map char-range-to-ascii default-range)]
  (defn pos-range-from-neg-range
    [ranges]
    (when *enable-random-generation*
      (add-holes-to-ranges alpha-range
                           (map char-range-to-ascii ranges)))))


(defn ranges-to-rand-char
  [candidate-ranges]
  (if (seq candidate-ranges)
    (char (apply fu/rand-range (rand-nth candidate-ranges)))
    ;; for a tiebreaker when no range is possible in A-Z,a-z,0-9
    (rand-nth [\{ \| \}])))


(defn generate-neg-set
  [result parsed-token]
  (conj (:random-subs result)
        [(-> parsed-token
             pos-range-from-neg-range
             ranges-to-rand-char)]))


(defn negative?
  [token]
  (= :NEG_SET (first token)))


(defn dot?
  [token]
  (= :DOT (first token)))


(defn token->range
  [token]
  (case (first token)
    (:NEG_SET :SET) (rest token)
    :CHAR [token]
    nil))


(defn generate-set
  [result parsed-token]
  (conj (:random-subs result) [(rand-char-from-range parsed-token)]))


(defn generate-quantifier
  [{:keys [processed-tokens random-subs]} f]
  (let [previous-token (peek processed-tokens)]
    (f previous-token random-subs)))


(defn star-g
  [previous curr]
  (let [i (rand-int 5)]
    (cond
      (zero? i) (pop curr)

      (= 1 i) curr

      (dot? previous) (into (pop curr)
                            [(repeatedly i any-rand-char)])

      (negative? previous)
      (let [r (pos-range-from-neg-range (token->range previous))]
        (into (pop curr)
              [(repeatedly i
                           #(ranges-to-rand-char r))]))

      :else (into (pop curr)
                  [(repeatedly i #(rand-char-from-range
                                   (token->range previous)))]))))


(defn plus-g
  [previous curr]
  (let [i (inc (rand-int 5))]
    (cond
      (zero? i) (pop curr)

      (= 1 i) curr

      (dot? previous) (into (pop curr)
                            [(repeatedly i any-rand-char)])

      (negative? previous) (let [r (-> previous
                                       token->range
                                       pos-range-from-neg-range)]
                             (into (pop curr)
                                   [(repeatedly i #(ranges-to-rand-char r))]))

      :else (into (pop curr)
                  [(repeatedly i #(rand-char-from-range
                                   (token->range previous)))]))))


(defn qmark-g
  [_ curr]
  (if (rand-nth [true false])
    (pop curr)
    curr))


(defn generate-star
  [result]
  (generate-quantifier result
                       star-g))


(defn generate-plus
  [result]
  (generate-quantifier result
                       plus-g))


(defn generate-qmark
  [result]
  (generate-quantifier result
                       qmark-g))


(defn generate-bounded-quantifier
  [result quantifier-type l u]
  (let [previous-token (peek (:processed-tokens result))
        curr (:random-subs result)
        i (if (= quantifier-type :MIN_MAX_QUANTIFIER)
            (fu/rand-range l u)
            l)]
    (cond
      (zero? i) (pop curr)

      (= 1 i) curr

      (dot? previous-token) (into (pop curr)
                                  [(repeatedly i any-rand-char)])

      (negative? previous-token) (let [r (->> previous-token
                                              token->range
                                              pos-range-from-neg-range)]
                                   (into (pop curr)
                                         [(repeatedly i #(ranges-to-rand-char r))]))


      :else (into (pop curr)
                  [(repeatedly i #(rand-char-from-range
                                   (token->range previous-token)))]))))


(defn generate-char
  [result ch]
  (conj (:random-subs result) [ch]))


(defn generate-dot
  [result]
  (conj (:random-subs result) [(any-rand-char)]))
