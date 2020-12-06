(ns fluky.generators
  (:require [clojure.string :as cstr]
            [clojure.test.check.generators :as gen]))

(def gchar
  (gen/frequency [[10 (gen/fmap str gen/char-alphanumeric)]
                  [1  (gen/one-of [(gen/return "\\-")
                                   (gen/return "\\(")
                                   (gen/return "\\)")
                                   (gen/return "\\]")
                                   (gen/return "\\[")])]]))
(defn abs
  [s]
  (Math/abs s))


(def quantifier
  (gen/one-of [(gen/fmap (fn [n] (format "{%d}" (Math/abs n))) gen/small-integer)
               (gen/return "*")
               (gen/return "+")
               (gen/return ".")
               (gen/return "?")
               (gen/let [x (gen/fmap abs gen/small-integer)
                         y (gen/fmap abs gen/small-integer)]
                 (let [a (min x y)
                       b (max x y)]
                   (format "{%d,%d}" a b)))
               (gen/let [x (gen/fmap abs gen/small-integer)]
                 (format "{%d}" (max 1 x)))]))


(def gsquare-optional
  ;; Generate a regex of the form:
  ;; [abcde]?
  ;; [abcde]*
  ;; [abcde]+
  ;; [abcde]{1,2}
  (gen/let [charseq (gen/vector gchar 1 5)
            q quantifier
            neg? gen/boolean]
    (let [s (cstr/join "" charseq)]
      (if neg?
        (format "[^%s]%s" s q)
        (format "[%s]%s" s q)))))


(def gsquare-range
  ;; Generate a regex of the form:
  ;; [a-e]?
  ;; [^a-b]*
  ;; [g-z]+
  ;; [0-9]{1,2}
  (gen/let [a gen/char-alpha-numeric
            b gen/char-alpha-numeric
            q quantifier
            neg? gen/boolean]
    (let [x (char (min (int a) (int b)))
          y (char (max (int a) (int b)))]
      (if neg?
        (format "[^%s-%s]%s" x y q)
        (format "[%s-%s]%s" x y q)))))


(def gregex-clause
  (gen/one-of [gsquare-range
               gsquare-optional
               gchar]))


(def gregex
  (gen/let [clauses (gen/vector gregex-clause)]
    (cstr/join "" clauses)))
