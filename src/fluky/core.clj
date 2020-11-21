(ns fluky.core
  (:require [fluky.grammar :as fg]
            [clojure.pprint :as pprint]
            [clojure.string :as cstr])
  (:gen-class))






(defmulti rwalk
  (fn [x]
    (first x)))

(defmethod rwalk :REGEX
  [[_ clause]]
  (rwalk clause))


(defmethod rwalk :REGEX_CLAUSE
  [[_ clause]]
  (rwalk clause))


(defmethod rwalk :PLUS_QUANTIFIER
  [[_ clause _]]
  (cstr/join ""
             (repeatedly (inc (rand-int 5))
                         (fn [] (rwalk clause)))))

(defmethod rwalk :POS_SET
  [clauses]
  (rwalk (nth clauses 2)))


(defmethod rwalk :CHAR
  [[_ clause]]
  clause)

(defmethod rwalk :RANGE
  [[_ from _ to :as clauses]]
  (let [f (int (first (rwalk from)))
        t (int (first (rwalk to )))]
    (if (<= f t)
      (str (char (+ f (rand-int (inc (- t f))))))
      (throw (ex-info "Invalid range" {:clauses clauses})))))


(defn random-regex
  [s]
  (let [tree (fg/regex->tree s)]
    (clojure.pprint/pprint tree)
    (rwalk tree)))
