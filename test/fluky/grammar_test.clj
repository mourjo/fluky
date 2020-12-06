(ns fluky.grammar-test
  (:require [clojure.test :refer :all]
            [clojure.test.check.clojure-test :as ct]
            [clojure.test.check.properties :as prop]
            [fluky.generators :as fgen]
            [fluky.grammar :as sut]
            [fluky.parser :as fp]
            [instaparse.core :as insta])
  (:import java.util.regex.Pattern
           clojure.lang.ExceptionInfo))

(def valid-regexes
  ["---------z-"
   "-z-"
   "-"
   "123]"
   "[---------z-]"
   "[-z-]"
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


(def invalid-regexes
  ["*"
   "**"
   "+"
   "???"
   ;; "[+*{[]" ;; ambiguous
   "[123"
   ;; "[[]"    ;; ambiguous
   "[]+"
   "[a-z]**"
   "[a-z]*****"
   "[a-z]*+*"
   "[a-z]*++"
   "\\c"
   "{}"
   "a-x{}"])


(defn valid-java-pattern?
  [s]
  (try (Pattern/compile s)
       true
       (catch Exception _
         false)))


(defn valid-by-grammar?
  [s]
  (let [res (sut/regex-grammar s)]
    (not (insta/failure? res))))


(defn valid-by-parser?
  [s]
  (try (fp/parse s)
       true
       (catch ExceptionInfo _ false)))


(defn agreeable-regex?
  [regex-str exp]
  (= (valid-java-pattern? regex-str)
     (valid-by-grammar? regex-str)
     (valid-by-parser? regex-str)
     exp))


(deftest valid-samples
  (testing "Valid regex syntax"
    (doseq [regex-str valid-regexes]
      (is (agreeable-regex? regex-str true)
          (str "Expected to be valid but is not: " regex-str))))

  (testing "Invalid regex syntax"
    (doseq [regex-str invalid-regexes]
      (is (agreeable-regex? regex-str false)
          (str "Expected to be invalid but is: " regex-str)))))


(ct/defspec generative-syntax-validation
  1000
  (prop/for-all [regex-str fgen/gregex]
                (is (agreeable-regex? regex-str true)
                    (str "Expected to be valid but is not: " regex-str))))
