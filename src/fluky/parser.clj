(ns fluky.parser
  (:require [fluky.lexer :as fl]
            [fluky.random :as fr]
            [fluky.utils :as fu])
  (:import java.util.Stack))



(defmulti parse-token
  (fn [_ token] (first token)))


(defn parse-set
  [chars]
  (when (empty? chars)
    (throw (ex-info "Empty range"
                    {:type :invalid-range})))
  (let [stack (Stack.)]
    (loop [[c & r] chars]
      (cond
        (not c)
        ::no-op

        (and (= c \-) (not (.isEmpty stack))
             (seq r) (= :CHAR (first (.peek stack))))
        (let [tos (.pop stack)]
          (when (not (<= (int (second tos)) (int (first r))))
            (throw (ex-info "Invalid range"
                            {:range [(second tos) (first r)]
                             :type :invalid-range})))
          (.push stack [:RANGE tos [:CHAR (first r)]])
          (recur (rest r)))


        (and (sequential? c)
             (= :escaped (first c)))
        (do (.push stack [:CHAR (second c)])
            (recur r))

        :else
        (do (.push stack [:CHAR c])
            (recur r))))
    (vec stack)))


(defmethod parse-token :set
  [result [_ chars]]
  (let [r (parse-set chars)]
    (-> result
        (update :processed-tokens conj (into [:SET] r))
        (update :random-subs conj [(fr/rand-char-from-range r)]))))


(defmethod parse-token :neg-set
  [result [_ chars]]
  (let [r (parse-set chars)]
    (-> result
        (update :processed-tokens conj (into [:NEG_SET] r))
        (update :random-subs conj [(fr/rand-char-from-negative-range r)]))))


(def quantifiable?
  #{:NEG_SET :SET :CHAR :DOT})


(defn parse-quantifier
  [{:keys [processed-tokens] :as result} token]
  (when (empty? processed-tokens)
    (throw (ex-info "Dangling meta character"
                    {:token token
                     :type :dangling-meta-character})))
  (let [previous (peek processed-tokens)
        is-prev-neg? (= :NEG_SET (first previous))
        is-prev-dot? (= :DOT previous)
        dp (case (first previous)
             (:NEG_SET :SET) (rest previous)
             :CHAR previous
             nil)
        [qkey qf] (case (first token)
                    :qmark [:QMARK_QUANTIFIER (fn [curr]
                                                (if (rand-nth [true false])
                                                  (pop curr)
                                                  curr))]
                    :star  [:STAR_QUANTIFIER (fn [curr]
                                               (let [i (rand-int 5)]
                                                 (cond
                                                   (zero? i)
                                                   (pop curr)

                                                   (= 1 i) curr

                                                   is-prev-dot? (into (pop curr)
                                                                      [(repeatedly i fr/any-rand-char)])

                                                   is-prev-neg?
                                                   (into (pop curr)
                                                         [(repeatedly i #(fr/rand-char-from-negative-range dp))])

                                                   :else (into (pop curr)
                                                               [(repeatedly i #(fr/rand-char-from-range dp))]))))]
                    :plus  [:PLUS_QUANTIFIER (fn [curr]
                                               (let [i (inc (rand-int 5))]
                                                 (cond
                                                   (zero? i)
                                                   (pop curr)

                                                   (= 1 i) curr

                                                   is-prev-dot? (into (pop curr)
                                                                      [(repeatedly i fr/any-rand-char)])

                                                   is-prev-neg?
                                                   (into (pop curr)
                                                         [(repeatedly i #(fr/rand-char-from-negative-range dp ))])

                                                   :else (into (pop curr)
                                                               [(repeatedly i #(fr/rand-char-from-range dp))]))))])]
    (when-not (quantifiable? (first previous))
      (throw (ex-info "Dangling meta character"
                      {:token token
                       :type :dangling-meta-character})))
    (-> result
        (update :processed-tokens (fn [current-tokens]
                                    (conj (pop current-tokens)
                                          [qkey previous])))
        (update :random-subs qf))))


(defmethod parse-token :qmark
  [result token]
  (parse-quantifier result token))

(defmethod parse-token :star
  [result token]
  (parse-quantifier result token))

(defmethod parse-token :plus
  [result token]
  (parse-quantifier result token))


(defn read-int
  [char-digits]
  (reduce (fn [acc i] (+ (- (int i) (int \0)) (* 10 acc)))
          0
          char-digits))


(defn read-quantifier-bounds
  [chars]
  (:bounds
   (reduce (fn [{:keys [current bounds] :as acc} ch]
             (cond

               (and (= ch \,) (seq bounds))
               (throw (ex-info "Double commas not allowed in quantifier"
                               {:type :invalid-quantifier
                                :token chars}))

               (and (#{::EOF \,} ch) (empty? current))
               (throw (ex-info "Empty quantifier range"
                               {:type :invalid-quantifier
                                :token chars}))

               (#{::EOF \,} ch)
               (-> acc
                   (assoc :current [])
                   (update :bounds conj (read-int current)))

               (not (<= (int \0) (int ch) (int \9)))
               (throw (ex-info "Invalid range in quantifier"
                               {:type :invalid-quantifier
                                :token chars}))

               :else (update acc :current conj ch)))
           {:bounds [] :current []}
           (conj chars ::EOF))))


(defmethod parse-token :quantifier
  [{:keys [processed-tokens] :as result} [_ args]]
  (when (empty? processed-tokens)
    (throw (ex-info "Dangling meta character"
                    {:token args
                     :type :dangling-meta-character})))
  (let [[l u :as bounds] (read-quantifier-bounds args)
        quantifier-type (if (and l u) :MIN_MAX_QUANTIFIER :EXACT_QUANTIFIER)
        previous-token (peek processed-tokens)
        is-prev-neg? (= :NEG_SET (first previous-token))
        is-prev-dot? (= :DOT previous-token)
        dp (case (first previous-token)
             (:NEG_SET :SET) (rest previous-token)
             :CHAR previous-token
             nil)]
    (when (not (<= 1 (count bounds) 2))
      (throw (ex-info "Quantifier can have 1 or 2 bounds"
                      {:type :invalid-quantifier
                       :token args})))
    (when (and (= :MIN_MAX_QUANTIFIER quantifier-type)
               (< u l))
      (throw (ex-info "Invalid range in quantifier lower must be <= upper"
                      {:type :invalid-quantifier
                       :token args})))
    (when-not (quantifiable? (first previous-token))
      (throw (ex-info "Dangling meta character"
                      {:token args
                       :type :dangling-meta-character})))

    (-> result
        (update :processed-tokens
                (fn [current-processed]
                  (conj (pop current-processed)
                        [quantifier-type bounds previous-token])))
        (update :random-subs (fn [curr]
                               (let [i (if (= quantifier-type :MIN_MAX_QUANTIFIER)
                                         (fu/rand-range l u)
                                         l)]
                                 (cond
                                   (zero? i)
                                   (pop curr)

                                   (= 1 i) curr

                                   is-prev-dot? (into (pop curr)
                                                      [(repeatedly i fr/any-rand-char)])

                                   is-prev-neg?
                                   (into (pop curr)
                                         [(repeatedly i #(fr/rand-char-from-negative-range dp))])

                                   :else (into (pop curr)
                                               [(repeatedly i #(fr/rand-char-from-range dp))]))))))))


(defmethod parse-token :char
  [result [_ ch]]
  (-> result
      (update :processed-tokens conj [:CHAR ch])
      (update :random-subs conj [ch])))


(defmethod parse-token :escaped
  [result [_ ch]]
  (-> result
      (update :processed-tokens conj [:CHAR ch])
      (update :random-subs conj [ch])))


(defmethod parse-token :dot
  [result _]
  (-> result
      (update :processed-tokens conj [:DOT])
      (update :random-subs conj [(fr/any-rand-char)])))


(defn parse
  [s]
  (reduce parse-token
          {:processed-tokens []
           :random-subs []}
          (fl/lex s)))


(defn random-from-regex
  [s]
  (apply str (map (partial apply str) (:random-subs (parse s)))))
