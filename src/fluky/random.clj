(ns fluky.random
  (:require [fluky.utils :as fu]
            [clojure.set :as cset]))

;; Note. This generator is not uniformly random.

(def ^{:dynamic true} *enable-random-generation* true)


(def default-range
  [[:RANGE [:CHAR \a] [:CHAR \z]]
   [:RANGE [:CHAR \A] [:CHAR \Z]]
   [:RANGE [:CHAR \0] [:CHAR \9]]])


(defn random-in-range
  [[t & range]]
  (when *enable-random-generation*
    (case t
      :RANGE (char (apply fu/rand-range (mapv (comp int second) range)))
      :CHAR (first range))))


(defn rand-char-from-range
  [r]
  (when (and *enable-random-generation* (seq r))
    (char (random-in-range (rand-nth r)))))


(defn any-rand-char
  []
  (when *enable-random-generation* (rand-char-from-range default-range)))


(let [alpha (set (map char (range (dec (int \0)) (inc (int \z)))))]
  (defn rand-char-from-negative-range
    [neg-range]
    ;; Due to lack of time, using enumeration here, but it is possible to break
    ;; the positive default range into set of ranges with holes, eg:
    ;; [^bcdf] would convert the range a-z into these ranges :
    ;; a
    ;; e
    ;; g-z
    ;; At this point, the `rand-char-from-range` may be used

    (when *enable-random-generation*
      (let [reducer (fn [acc [t & r]]
                      (into acc
                            (case t
                              :RANGE (map char (apply fu/range-incl (mapv (comp int second) r)))
                              :CHAR r)))
            possibilities (->> neg-range
                               (reduce reducer #{})
                               (cset/difference alpha)
                               seq)]
        (if possibilities
          (rand-nth possibilities)
          (throw (ex-info "No possibilities in A-Za-z0-9"
                          {:range neg-range
                           :type :no-possibility})))))))


(defn prev-neg?
  [previous-token]
  (= :NEG_SET (first previous-token)))


(defn prev-dot?
  [previous-token]
  (= :DOT (first previous-token)))


(defn previous->range
  [previous-token]
  (case (first previous-token)
    (:NEG_SET :SET) (rest previous-token)
    :CHAR [previous-token]
    nil))


(defn generate-set
  [result parsed-token]
  (conj (:random-subs result) [(rand-char-from-range parsed-token)]))


(defn generate-neg-set
  [result parsed-token]
  (conj (:random-subs result)
        [(rand-char-from-negative-range parsed-token)]))


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

      (prev-dot? previous) (into (pop curr)
                                 [(repeatedly i any-rand-char)])

      (prev-neg? previous)
      (into (pop curr)
            [(repeatedly i #(rand-char-from-negative-range
                             (previous->range previous)))])

      :else (into (pop curr)
                  [(repeatedly i #(rand-char-from-range
                                   (previous->range previous)))]))))


(defn plus-g
  [previous curr]
  (let [i (inc (rand-int 5))]
    (cond
      (zero? i) (pop curr)

      (= 1 i) curr

      (prev-dot? previous) (into (pop curr)
                                 [(repeatedly i any-rand-char)])

      (prev-neg? previous) (into (pop curr)
                                 [(repeatedly i #(rand-char-from-negative-range
                                                  (previous->range previous)))])

      :else (into (pop curr)
                  [(repeatedly i #(rand-char-from-range
                                   (previous->range previous)))]))))


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

      (prev-dot? previous-token) (into (pop curr)
                                       [(repeatedly i any-rand-char)])

      (prev-neg? previous-token) (into (pop curr)
                                       [(repeatedly i #(rand-char-from-negative-range
                                                        (previous->range previous-token)))])

      :else (into (pop curr)
                  [(repeatedly i #(rand-char-from-range
                                   (previous->range previous-token)))]))))


(defn generate-char
  [result ch]
  (conj (:random-subs result) [ch]))


(defn generate-dot
  [result]
  (conj (:random-subs result) [(any-rand-char)]))
