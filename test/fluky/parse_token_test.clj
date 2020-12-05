(ns fluky.parse-token-test
  (:require [fluky.parser :as sut]
            [clojure.test :refer :all])
  (:import clojure.lang.ExceptionInfo
           [java.util.regex Pattern PatternSyntaxException]))


(defn invalid-java-pattern?
  [s]
  (try (Pattern/compile s)
       false
       (catch PatternSyntaxException _
         true)
       (catch Exception _
         false)))


(defn invalid-range?
  [s]
  (try
    (sut/parse s)
    (catch ExceptionInfo e
      (and (invalid-java-pattern? s)
           (-> e
               ex-data
               :type
               (= :invalid-range))))))


(defn validate-and-parse
  [s]
  (when-not (invalid-java-pattern? s)
    (sut/parse s)))


(deftest parse-test
  (is (= (validate-and-parse "abc")
         [[:CHAR \a]
          [:CHAR \b]
          [:CHAR \c]]))

  (is (= (validate-and-parse "a-c")
         [[:CHAR \a]
          [:CHAR \-]
          [:CHAR \c]]))

  (is (= (validate-and-parse "[abc]")
         [[:SET
           [:CHAR \a]
           [:CHAR \b]
           [:CHAR \c]]]))

  (is (= (sut/parse "[abc[]")
         ;; this is interpreted differently by Java where Java considers this invalid:
         ;; Execution error (PatternSyntaxException) at java.util.regex.Pattern/error (Pattern.java:1955).
         ;; Unclosed character class near index 5
         ;; [abc[]
         ;;      ^
         ;; See https://regexr.com/
         [[:SET
           [:CHAR \a]
           [:CHAR \b]
           [:CHAR \c]
           [:CHAR \[]]]))

  (is (= (validate-and-parse "[abc]]")
         [[:SET
           [:CHAR \a]
           [:CHAR \b]
           [:CHAR \c]]
          [:CHAR \]]]))

  (is (= (validate-and-parse "[abc]d]")
         [[:SET
           [:CHAR \a]
           [:CHAR \b]
           [:CHAR \c]]
          [:CHAR \d]
          [:CHAR \]]]))

  (is (= (validate-and-parse "[abc(d]")
         [[:SET
           [:CHAR \a]
           [:CHAR \b]
           [:CHAR \c]
           [:CHAR \(]
           [:CHAR \d]]]))

  (is (= (validate-and-parse "[abc)d]")
         [[:SET
           [:CHAR \a]
           [:CHAR \b]
           [:CHAR \c]
           [:CHAR \)]
           [:CHAR \d]]]))

  (is (= (validate-and-parse "[abc)+*-.d]")
         [[:SET
           [:CHAR \a]
           [:CHAR \b]
           [:CHAR \c]
           [:CHAR \)]
           [:CHAR \+]
           [:RANGE [:CHAR \*] [:CHAR \.]]
           [:CHAR \d]]]))

  (is (= (validate-and-parse "[a-z]bcde")
         [[:SET [:RANGE [:CHAR \a] [:CHAR \z]]]
          [:CHAR \b]
          [:CHAR \c]
          [:CHAR \d]
          [:CHAR \e]]))

  (is (= (validate-and-parse "[a-z0-9]")
         [[:SET
           [:RANGE [:CHAR \a] [:CHAR \z]]
           [:RANGE [:CHAR \0] [:CHAR \9]]]]))

  (is (= (validate-and-parse "[a-z0-9x]")
         [[:SET
           [:RANGE [:CHAR \a] [:CHAR \z]]
           [:RANGE [:CHAR \0] [:CHAR \9]]
           [:CHAR \x]]]))

  (is (= (validate-and-parse "[xa-zy0-9z]")
         [[:SET
           [:CHAR \x]
           [:RANGE [:CHAR \a] [:CHAR \z]]
           [:CHAR \y]
           [:RANGE [:CHAR \0] [:CHAR \9]]
           [:CHAR \z]]]))

  (is (= (validate-and-parse "[-az]")
         [[:SET
           [:CHAR \-]
           [:CHAR \a]
           [:CHAR \z]]]))

  (is (= (validate-and-parse "[-az-]")
         [[:SET
           [:CHAR \-]
           [:CHAR \a]
           [:CHAR \z]
           [:CHAR \-]]]))

  (is (= (validate-and-parse "[-a-z-]")
         [[:SET
           [:CHAR \-]
           [:RANGE [:CHAR \a] [:CHAR \z]]
           [:CHAR \-]]]))

  (is (= (validate-and-parse "[-----]")
         [[:SET
           [:RANGE [:CHAR \-] [:CHAR \-]]
           [:CHAR \-]
           [:CHAR \-]]]))

  (is (= (validate-and-parse "[a-a]")
         [[:SET
           [:RANGE [:CHAR \a] [:CHAR \a]]]])))


(deftest parse-invalid-test
  (invalid-range? "[z-a]bcde")
  (invalid-range? "[]")
  (invalid-range? "[]a")
  (invalid-range? "a[]a")
  (invalid-range? "a[]"))
