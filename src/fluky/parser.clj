(ns fluky.parser
  (:require [fluky.lexer :as fl]
            [fluky.parse-utils :as fpu]
            [fluky.random :as fr])
  (:import java.util.Stack))


(defmulti parse-token
  (fn [_ token] (first token)))


(defn parse-set
  [chars]
  (when (empty? chars)
    (throw (ex-info "Empty range"
                    {:type :invalid-range})))
  (loop [[c & r] chars
         stack []]
    (cond
      (not c)
      stack

      (and (= c \-) (seq stack)
           (seq r) (= :CHAR (first (peek stack))))
      (let [tos (peek stack)]
        (when (not (<= (int (second tos)) (int (first r))))
          (throw (ex-info "Invalid range"
                          {:range [(second tos) (first r)]
                           :type :invalid-range})))
        (recur (rest r) (conj (pop stack) [:RANGE tos [:CHAR (first r)]])))


      (and (sequential? c)
           (= :escaped (first c)))
      (recur r (conj stack [:CHAR (second c)]))

      :else
      (recur r (conj stack [:CHAR c])))))


(defmethod parse-token :set
  [result [_ chars]]
  (let [r (parse-set chars)]
    (-> result
        (update :processed-tokens conj (into [:SET] r))
        (assoc :random-subs (fr/generate-set result r)))))


(defmethod parse-token :neg-set
  [result [_ chars]]
  (let [r (parse-set chars)]
    (-> result
        (update :processed-tokens conj (into [:NEG_SET] r))
        (assoc :random-subs (fr/generate-neg-set result r)))))


(defn parse-quantifier
  [{:keys [processed-tokens] :as result} token]
  (when (empty? processed-tokens)
    (throw (ex-info "Dangling meta character"
                    {:token token
                     :type :dangling-meta-character})))
  (let [previous (peek processed-tokens)
        qkey (case (first token)
               :qmark :QMARK_QUANTIFIER
               :star  :STAR_QUANTIFIER
               :plus  :PLUS_QUANTIFIER)]
    (when-not (fpu/quantifiable? (first previous))
      (throw (ex-info "Dangling meta character"
                      {:token token
                       :type :dangling-meta-character})))
    (update result :processed-tokens (fn [current-tokens]
                                       (conj (pop current-tokens)
                                             [qkey previous])))))


(defmethod parse-token :qmark
  [result token]
  (-> result
      (parse-quantifier token)
      (assoc :random-subs (fr/generate-qmark result))))


(defmethod parse-token :star
  [result token]
  (-> result
      (parse-quantifier token)
      (assoc :random-subs (fr/generate-star result))))


(defmethod parse-token :plus
  [result token]
  (-> result
      (parse-quantifier token)
      (assoc :random-subs (fr/generate-plus result))))


(defmethod parse-token :quantifier
  [{:keys [processed-tokens] :as result} [_ args]]
  (when (empty? processed-tokens)
    (throw (ex-info "Dangling meta character"
                    {:token args
                     :type :dangling-meta-character})))
  (let [[l u :as bounds] (fpu/read-quantifier-bounds args)
        quantifier-type (if (and l u) :MIN_MAX_QUANTIFIER :EXACT_QUANTIFIER)
        previous-token (peek processed-tokens)]
    (when (not (<= 1 (count bounds) 2))
      (throw (ex-info "Quantifier can have 1 or 2 bounds"
                      {:type :invalid-quantifier
                       :token args})))
    (when (and (= :MIN_MAX_QUANTIFIER quantifier-type)
               (< u l))
      (throw (ex-info "Invalid range in quantifier lower must be <= upper"
                      {:type :invalid-quantifier
                       :token args})))
    (when-not (fpu/quantifiable? (first previous-token))
      (throw (ex-info "Dangling meta character"
                      {:token args
                       :type :dangling-meta-character})))

    (-> result
        (update :processed-tokens
                (fn [current-processed]
                  (conj (pop current-processed)
                        [quantifier-type bounds previous-token])))
        (assoc :random-subs
               (fr/generate-bounded-quantifier result quantifier-type l u)))))


(defmethod parse-token :char
  [result [_ ch]]
  (-> result
      (update :processed-tokens conj [:CHAR ch])
      (assoc :random-subs (fr/generate-char result ch))))


(defmethod parse-token :escaped
  [result [_ ch]]
  (-> result
      (update :processed-tokens conj [:CHAR ch])
      (assoc :random-subs (fr/generate-char result ch))))


(defmethod parse-token :dot
  [result _]
  (-> result
      (update :processed-tokens conj [:DOT])
      (assoc :random-subs (fr/generate-dot result))))


(defn parse
  [s]
  (reduce parse-token
          {:processed-tokens []
           :random-subs []}
          (fl/lex s)))


(defn random-from-regex
  [s]
  (->> s
       parse
       :random-subs
       fpu/str-join))
