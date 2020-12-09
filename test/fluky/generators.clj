(ns fluky.generators
  (:require [clojure.string :as cstr]
            [clojure.test.check.generators :as gen]))

(def gchar
  (gen/fmap str gen/char-alphanumeric))


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


(def gsquare-chars
  ;; Generate a regex of the form:
  ;; [abcde]?
  ;; [abcde]*
  ;; [abcde]+
  ;; [abcde]{1,2}
  (gen/let [charseq (gen/vector gchar 1 5)]
    (cstr/join "" charseq)))


(def gsquare-range
  ;; Generate a regex of the form:
  ;; [a-e]?
  ;; [^a-b]*
  ;; [g-z]+
  ;; [0-9]{1,2}
  (gen/let [a gen/char-alpha-numeric
            b gen/char-alpha-numeric]
    (let [x (char (min (int a) (int b)))
          y (char (max (int a) (int b)))]
      (format "%s-%s" x y ))))


(def gsquare
  (gen/let [neg? gen/boolean
            args (gen/vector (gen/one-of [gsquare-range gsquare-chars])
                             1 5)
            q quantifier]
    (let [sargs (cstr/join "" args)]
      (if neg?
        (format "[^%s]%s" sargs q)
        (format "[%s]%s" sargs q)))))


(def gregex-clause
  (gen/one-of [gsquare
               gchar]))


(def gregex
  (gen/let [clauses (gen/vector gregex-clause)]
    (cstr/join "" clauses)))
