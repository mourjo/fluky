(ns fluky.generators
  (:require [instaparse.core :as insta]
            [clojure.test.check :as tcheck]
            [clojure.test.check.clojure-test :as ct]
            [clojure.test.check.generators :as gen]
            [clojure.string :as cstr]
            [clojure.test.check.properties :as prop]
            [clojure.test :as t]))



(def gchar
  (gen/frequency [[10 (gen/fmap str gen/char-alphanumeric)]
                  [1  (gen/return " ")]
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
               (gen/return "?")
               (gen/let [x (gen/fmap abs gen/small-integer)
                         y (gen/fmap abs gen/small-integer)]
                 (let [a (min x y)
                       b (max x y)]
                   (format "{%d,%d}" a b)))
               (gen/let [x (gen/fmap abs gen/small-integer)]
                 (format "{%d}" x))]))


(def gsquare-optional
  ;; Generate a regex of the form:
  ;; [abcde]?
  ;; [abcde]*
  ;; [abcde]+
  ;; [abcde]{1,2}
  (gen/let [charseq (gen/vector gchar 1 5)
            q quantifier]
    (str "[" (cstr/join "" charseq) "]" q)))


(def gsquare-range
  ;; Generate a regex of the form:
  ;; [a-e]?
  ;; [a-b]*
  ;; [g-z]+
  ;; [0-9]{1,2}
  (gen/let [a gen/char-alpha-numeric
            b gen/char-alpha-numeric
            q quantifier]
    (let [x (char (min (int a) (int b)))
          y (char (max (int a) (int b)))]
      (str "[" x "-" y "]" q))))


(def gregex
  (gen/one-of [gsquare-range gsquare-optional gchar]))
