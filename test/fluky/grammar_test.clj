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
  [" ---------z-"
   " -z-"
   "-"
   "123]"
   "[ ---------z-]"
   "[ -z-]" ;; (re-matches #"[ -z-]" "-") (re-matches #"[ -z-]" "a")
   "[*--]*"
   "[*]?"
   "[+*]"
   "[+*{]"
   "[+*{}]"
   "[+*}{]"
   "[+-a]*"
   "[+]"
   "[-+]*"
   "[-+]?[0-9]{1,16}[.][0-9]{1,6}"
   "[---]*"
   "[--a]*"
   "[--z]*"
   "[--{]*"
   "[-]?"
   "[-]?[0-9]{1,16}[.][0-9]{1,6}"
   "[-a]"
   "[1-10]{1,2}"
   "[[[a-z]*]+]*"
   "[[a-z]*]+"
   "[[a-z]*]+"
   "[[a-z]*]{2,3}"
   "[^a-z01]"
   "[^a-z01]"
   "[^a-z01]"
   "[a-]"
   "[a-z0-9]"
   "[a-z0-9]*"
   "[a-z0-9]*"
   "[a-z0-9]*"
   "[a-z0-9]{01}"
   "[a-z0-9]{1}"
   "[a-z0-9]{201}"
   "[a-z]"
   "[{]"
   "[{]"
   "\\("
   "\\(\\(\\)"
   "\\)"
   "\\*"
   "\\*[a-z10.]?"
   "\\*[a-z10\\.]?"
   "\\+"
   "\\+"
   "\\+"
   "\\."
   "\\["
   "\\[123"
   "\\\\"
   "\\]"
   "\\{"
   "\\{"
   "\\}"
   "a-"
   "a-z"
   "a{001}" ;; (re-matches (Pattern/compile "a{001}") "a")
   "a{00}"  ;; (re-matches (Pattern/compile "a{00}") "")
   "a{1,2}"
   "a{1}"
   "a{200}"
   "[*--]*"])


;; I wont add support for
;; character classes like \a
;; Possessive quantifier like ++ and *+ *?
;; Java allows this but this is not a valid regex {1,2} {112}

(def invalid-regexes
  ["*"
   "**"
   "+"
   "???"
   "[+*{[]"
   "[123"
   "[[]"
   "[[]"
   "[]+"
   "[a-z]**"
   "[a-z]*****"
   "[a-z]*+*"
   "[a-z]*++"
   "\\c"])

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
