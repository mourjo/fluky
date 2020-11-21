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


(def gsquare
  (gen/let [charseq (gen/vector gchar 1 5)
            quantifier (gen/one-of [(gen/fmap (fn [n] (format "{%d}" (Math/abs n))) gen/small-integer)
                                    (gen/return "*")
                                    (gen/return "+")
                                    (gen/return "?")])]
    (str "[" (cstr/join "" charseq) "]" quantifier)))


(comment
  (gen/generate gsquare 10))
