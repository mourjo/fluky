(ns fluky.parse-utils
  (:require [clojure.string :as cs]))

(def quantifiable?
  #{:NEG_SET :SET :CHAR :DOT})


(defn read-int
  [char-digits]
  (reduce (fn [acc i] (+ (- (int i) (int \0)) (* 10 acc)))
          0
          char-digits))


(defn str-join
  [xs]
  (apply str (mapv (partial cs/join "") xs)))
