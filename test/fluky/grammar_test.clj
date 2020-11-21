(ns fluky.grammar-test
  (:require [fluky.grammar :as sut]
            [instaparse.core :as insta]
            [clojure.test.check :as tcheck]
            [clojure.test.check.clojure-test :as ct]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test :as t])
  (:import java.util.regex.Pattern))


(def valid-regexes
  ["[*--]*"
   "[*--]*"
   "[--{]*"
   "[--a]*"
   "[---]*"
   "[--z]*"
   "[+-a]*"
   "[{]"
   "\\+"
   "\\+"
   "[*]?"
   "[-+]*"
   "[+-a]*"
   "[-]?"
   "[-]?"
   "[-]?"
   "[-]?[0-9]{1,16}[.][0-9]{1,6}"
   "[-+]?[0-9]{1,16}[.][0-9]{1,6}"
   "[1-10]{1,2}"
   "\\*[a-z10\\.]?"
   "\\*[a-z10.]?"
   "\\+"
   "\\*"
   "[^a-z01]"
   "[^a-z01]"
   "[^a-z01]"
   "[a-z0-9]*"
   "[a-z0-9]*"
   "[a-z0-9]*"
   "[a-z0-9]"
   "[a-z]"
   "[a-]"
   "[-a]"
   ;; "a-"
   ;; "[+-a]*+"
   ;; "\\{"
   ;; "-"
   ;; "a-z"
   ;; "\\**?"
   ;; "{1,2}"
   ;;"{112}"
   ;; "[ ---------z-]"
   ;; "123]"
   ;; "\\[123"

   ])


(def invalid-regexes
  ["[a-+]*+"
   "*"
   "[[]"
   "*"
   "+"
   "**"
   "[123"
   ;; "[ ----z]"
   ;; "[ ---------z--]"
   "???"])

(defn actually-valid?
  [s]
  (try (Pattern/compile s)
       true
       (catch Exception _
         false)))

(defn valid?
  [s]
  (let [res (sut/regex-grammar s)]
    (not (insta/failure? res))))


(t/deftest valid-samples
  (t/testing "Valid regex syntax"
    (doseq [x valid-regexes]
      (t/is (= (actually-valid? x)
               (valid? x)
               true)
            x)))
  (t/testing "Invalid regex syntax"
    (doseq [x invalid-regexes]
      (t/is (= (actually-valid? x)
               (valid? x)
               false)
            x))))





;; (ct/defspec abcd
;;   10)
