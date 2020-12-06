(ns fluky.parser
  (:require [fluky.lexer :as fl])
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
  [processed-tokens [_ chars]]
  (conj processed-tokens (into [:SET] (parse-set chars))))


(defn parse-quantifier
  [processed-tokens token]
  (when (empty? processed-tokens)
    (throw (ex-info "Dangling meta character"
                    {:token token
                     :type :dangling-meta-character})))
  (let [previous (peek processed-tokens)
        qkey (case (first token)
               :qmark :QMARK_QUANTIFIER
               :star :STAR_QUANTIFIER
               :plus :PLUS_QUANTIFIER
               :quantifier :EITHER_MIN_MAX_OR_EXACT)]
    (when-not (#{:SET :CHAR} (first previous))
      (throw (ex-info "Dangling meta character"
                      {:token token
                       :type :dangling-meta-character})))
    (conj (pop processed-tokens)
          [qkey previous])))


(defmethod parse-token :qmark
  [processed-tokens token]
  (parse-quantifier processed-tokens token))

(defmethod parse-token :star
  [processed-tokens token]
  (parse-quantifier processed-tokens token))

(defmethod parse-token :plus
  [processed-tokens token]
  (parse-quantifier processed-tokens token))


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
  [processed-tokens [_ args]]
  (when (empty? processed-tokens)
    (throw (ex-info "Dangling meta character"
                    {:token args
                     :type :dangling-meta-character})))
  (let [[l u :as bounds] (read-quantifier-bounds args)
        quantifier-type (if (and l u) :MIN_MAX_QUANTIFIER :EXACT_QUANTIFIER)]
    (when (not (<= 1 (count bounds) 2))
      (throw (ex-info "Quantifier can have 1 or 2 bounds"
                      {:type :invalid-quantifier
                       :token args})))
    (when (and (= :MIN_MAX_QUANTIFIER quantifier-type)
               (< u l))
      (throw (ex-info "Invalid range in quantifier lower must be <= upper"
                      {:type :invalid-quantifier
                       :token args})))

    (let [previous-token (peek processed-tokens)]
      (when-not (#{:SET :CHAR} (first previous-token))
        (throw (ex-info "Dangling meta character"
                        {:token args
                         :type :dangling-meta-character})))
      (conj (pop processed-tokens)
            [quantifier-type bounds previous-token]))))

(defmethod parse-token :char
  [processed-tokens token]
  (conj processed-tokens [:CHAR (second token)]))

(defmethod parse-token :neg-set
  [processed-tokens [_ chars]]
  (conj processed-tokens (into [:NEG_SET] (parse-set chars))))

(defmethod parse-token :escaped
  [processed-tokens [_ ch]]
  (conj processed-tokens [:CHAR ch]))


(defn parse
  [s]
  (reduce parse-token
          []
          (fl/lex s)))
