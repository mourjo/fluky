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


        :else
        (do (.push stack [:CHAR c])
            (recur r))))
    (vec stack)))


(defmethod parse-token :set
  [processed-tokens [_ chars]]
  (conj processed-tokens (into [:SET] (parse-set chars))))


(defmethod parse-token :qmark
  [processed-tokens token]
  (conj processed-tokens {:op (first token)}))


(defmethod parse-token :quantifier
  [processed-tokens token]
  (conj processed-tokens {:op (first token)}))


(defmethod parse-token :char
  [processed-tokens token]
  (conj processed-tokens [:CHAR (second token)]))


(defmethod parse-token :star
  [processed-tokens token]
  (conj processed-tokens {:op (first token)}))


(defmethod parse-token :neg-set
  [processed-tokens [_ chars]]
  (conj processed-tokens (into [:NEG_SET] (parse-set chars))))


(defmethod parse-token :plus
  [processed-tokens token]
  (conj processed-tokens {:op (first token)}))


(defmethod parse-token :escaped
  [processed-tokens token]
  (conj processed-tokens [:CHAR (second token)]))


(defn parse
  [s]
  (reduce parse-token
          []
          (fl/lex s)))
