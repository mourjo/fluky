(ns fluky.utils
  (:require [clojure.string :as cstr]))


(def BRANCH-FACTOR 5)

(def ALPHABET
  (sort-by int
           [\a \A \b \B \c \C \d \D \e \E \f \F \g \G \h \H \i \I \j \J \k \K \l \L \m \M \n \N \o
            \O \p \P \q \Q \r \R \s \S \t \T \u \U \v \V \w \W \x \X \y \Y \z \Z \0 \1 \2 \3 \4 \5
            \6 \7 \8 \9 \. \{ \} \[ \] \  \- \* \+ \? \( \) \\]))


(defn rand-range
  "Return a random number between bounds"
  [low high]
  (let [r (rand-int (- (inc high) low))]
    (+ low r)))


(defn parse-number
  "Convert a grammar leaf for numbers into a long."
  [[_ & digits]]
  (Long/parseLong (cstr/join "" digits)))


(defn ffilter
  "Find the first element that satisfies the predicate."
  [p xs]
  (first (filter p xs)))
