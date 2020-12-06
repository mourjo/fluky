(ns fluky.parser-test
  (:require [fluky.parser :as sut]
            [clojure.test :refer :all])
  (:import clojure.lang.ExceptionInfo
           [java.util.regex Pattern PatternSyntaxException]))


(defn parse-tree
  [s]
  (-> s
      sut/parse
      :processed-tokens))


(defn invalid-java-pattern?
  [s]
  (try (Pattern/compile s)
       false
       (catch PatternSyntaxException _
         true)
       (catch Exception _
         false)))


(defn invalid?
  ([t s]
   (try
     (parse-tree s)
     (catch ExceptionInfo e
       (and (invalid-java-pattern? s)
            (-> e
                ex-data
                :type
                (= t))))))
  ([t s opts]
   (if (:test-java-pattern? opts)
     (invalid? t s)
     (try
       (parse-tree s)
       (catch ExceptionInfo e
         (-> e
             ex-data
             :type
             (= t)))))))


(defn validate-and-parse
  [s]
  (when-not (invalid-java-pattern? s)
    (parse-tree s)))


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

  (is (= (parse-tree "[abc[]")
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
           [:RANGE [:CHAR \a] [:CHAR \a]]]]))

  (is (= (validate-and-parse "[a\\-z]")
         [[:SET
           [:CHAR \a]
           [:CHAR \-]
           [:CHAR \z]]]))

  (is (= (validate-and-parse "a\\-z")
         [[:CHAR \a]
          [:CHAR \-]
          [:CHAR \z]]))


  (is (= (validate-and-parse "a?")
         [[:QMARK_QUANTIFIER [:CHAR \a]]]))

  (is (= (validate-and-parse "abc?")
         [[:CHAR \a]
          [:CHAR \b]
          [:QMARK_QUANTIFIER [:CHAR \c]]]))

  (is (= (validate-and-parse "[a]?")
         [[:QMARK_QUANTIFIER [:SET [:CHAR \a]]]]))

  (is (= (validate-and-parse "[abc]?")
         [[:QMARK_QUANTIFIER
           [:SET
            [:CHAR \a]
            [:CHAR \b]
            [:CHAR \c]]]]))

  (is (= (validate-and-parse "[a-c]?")
         [[:QMARK_QUANTIFIER
           [:SET
            [:RANGE
             [:CHAR \a]
             [:CHAR \c]]]]]))

  (is (= (validate-and-parse "[a-cd-e]?")
         [[:QMARK_QUANTIFIER
           [:SET
            [:RANGE [:CHAR \a] [:CHAR \c]]
            [:RANGE [:CHAR \d] [:CHAR \e]]]]]))

  (is (= (validate-and-parse "[a-c\\-d-e]?")
         [[:QMARK_QUANTIFIER
           [:SET
            [:RANGE [:CHAR \a] [:CHAR \c]]
            [:CHAR \-]
            [:RANGE [:CHAR \d] [:CHAR \e]]]]]))

  (is (= (validate-and-parse "a*")
         [[:STAR_QUANTIFIER [:CHAR \a]]]))

  (is (= (validate-and-parse "abc*")
         [[:CHAR \a]
          [:CHAR \b]
          [:STAR_QUANTIFIER [:CHAR \c]]]))

  (is (= (validate-and-parse "[a]*")
         [[:STAR_QUANTIFIER [:SET [:CHAR \a]]]]))

  (is (= (validate-and-parse "[abc]*")
         [[:STAR_QUANTIFIER
           [:SET
            [:CHAR \a]
            [:CHAR \b]
            [:CHAR \c]]]]))

  (is (= (validate-and-parse "[a-c]*")
         [[:STAR_QUANTIFIER
           [:SET
            [:RANGE
             [:CHAR \a]
             [:CHAR \c]]]]]))

  (is (= (validate-and-parse "[a-cd-e]*")
         [[:STAR_QUANTIFIER
           [:SET
            [:RANGE [:CHAR \a] [:CHAR \c]]
            [:RANGE [:CHAR \d] [:CHAR \e]]]]]))

  (is (= (validate-and-parse "[a-c\\-d-e]*")
         [[:STAR_QUANTIFIER
           [:SET
            [:RANGE [:CHAR \a] [:CHAR \c]]
            [:CHAR \-]
            [:RANGE [:CHAR \d] [:CHAR \e]]]]]))

  (is (= (validate-and-parse "a+")
         [[:PLUS_QUANTIFIER [:CHAR \a]]]))

  (is (= (validate-and-parse "abc+")
         [[:CHAR \a]
          [:CHAR \b]
          [:PLUS_QUANTIFIER [:CHAR \c]]]))

  (is (= (validate-and-parse "[a]+")
         [[:PLUS_QUANTIFIER [:SET [:CHAR \a]]]]))

  (is (= (validate-and-parse "[abc]+")
         [[:PLUS_QUANTIFIER
           [:SET
            [:CHAR \a]
            [:CHAR \b]
            [:CHAR \c]]]]))

  (is (= (validate-and-parse "[a-c]+")
         [[:PLUS_QUANTIFIER
           [:SET
            [:RANGE
             [:CHAR \a]
             [:CHAR \c]]]]]))

  (is (= (validate-and-parse "[a-cd-e]+")
         [[:PLUS_QUANTIFIER
           [:SET
            [:RANGE [:CHAR \a] [:CHAR \c]]
            [:RANGE [:CHAR \d] [:CHAR \e]]]]]))

  (is (= (validate-and-parse "[a-c\\-d-e]+")
         [[:PLUS_QUANTIFIER
           [:SET
            [:RANGE [:CHAR \a] [:CHAR \c]]
            [:CHAR \-]
            [:RANGE [:CHAR \d] [:CHAR \e]]]]]))

  (is (= (validate-and-parse "a{1}")
         [[:EXACT_QUANTIFIER [1] [:CHAR \a]]]))

  (is (= (validate-and-parse "a{1,2}")
         [[:MIN_MAX_QUANTIFIER [1,2] [:CHAR \a]]]))



  (is (= (validate-and-parse "[a-z]{20,100}a")
         [[:MIN_MAX_QUANTIFIER
           [20 100]
           [:SET [:RANGE [:CHAR \a] [:CHAR \z]]]]
          [:CHAR \a]]))

  (is (= (validate-and-parse "[a-zx]{0,100}")
         [[:MIN_MAX_QUANTIFIER
           [0 100]
           [:SET
            [:RANGE [:CHAR \a] [:CHAR \z]]
            [:CHAR \x]]]]))

  (is (= (validate-and-parse "[a-zx]{100}")
         [[:EXACT_QUANTIFIER
           [100]
           [:SET
            [:RANGE [:CHAR \a] [:CHAR \z]]
            [:CHAR \x]]]]))

  (is (= (validate-and-parse "[a-zx]{0}")
         [[:EXACT_QUANTIFIER
           [0]
           [:SET
            [:RANGE [:CHAR \a] [:CHAR \z]]
            [:CHAR \x]]]]))

  (is (= (validate-and-parse "[a-zx]{0,1}")
         [[:MIN_MAX_QUANTIFIER
           [0,1]
           [:SET
            [:RANGE [:CHAR \a] [:CHAR \z]]
            [:CHAR \x]]]]))


  (is (= (validate-and-parse "[a-zx]0,1}")
         [[:SET
           [:RANGE [:CHAR \a] [:CHAR \z]]
           [:CHAR \x]]
          [:CHAR \0]
          [:CHAR \,]
          [:CHAR \1]
          [:CHAR \}]]))

  (is (= (validate-and-parse "a{25}")
         [[:EXACT_QUANTIFIER [25] [:CHAR \a]]]))

  (is (= (validate-and-parse "abc{25}")
         [[:CHAR \a]
          [:CHAR \b]
          [:EXACT_QUANTIFIER [25] [:CHAR \c]]]))

  (is (= (validate-and-parse "[a]{25}")
         [[:EXACT_QUANTIFIER [25] [:SET [:CHAR \a]]]]))

  (is (= (validate-and-parse "[abc]{25}")
         [[:EXACT_QUANTIFIER  [25]
           [:SET
            [:CHAR \a]
            [:CHAR \b]
            [:CHAR \c]]]]))

  (is (= (validate-and-parse "[a-c]{25}")
         [[:EXACT_QUANTIFIER [25]
           [:SET
            [:RANGE
             [:CHAR \a]
             [:CHAR \c]]]]]))

  (is (= (validate-and-parse "[a-cd-e]{25}")
         [[:EXACT_QUANTIFIER [25]
           [:SET
            [:RANGE [:CHAR \a] [:CHAR \c]]
            [:RANGE [:CHAR \d] [:CHAR \e]]]]]))

  (is (= (validate-and-parse "[a-c\\-d-e]{25}")
         [[:EXACT_QUANTIFIER [25]
           [:SET
            [:RANGE [:CHAR \a] [:CHAR \c]]
            [:CHAR \-]
            [:RANGE [:CHAR \d] [:CHAR \e]]]]]))

  (is (= (validate-and-parse "a{2,5}")
         [[:MIN_MAX_QUANTIFIER [2 5] [:CHAR \a]]]))

  (is (= (validate-and-parse "abc{2,5}")
         [[:CHAR \a]
          [:CHAR \b]
          [:MIN_MAX_QUANTIFIER [2 5] [:CHAR \c]]]))

  (is (= (validate-and-parse "[a]{2,5}")
         [[:MIN_MAX_QUANTIFIER [2 5] [:SET [:CHAR \a]]]]))

  (is (= (validate-and-parse "[abc]{2,5}")
         [[:MIN_MAX_QUANTIFIER [2 5]
           [:SET
            [:CHAR \a]
            [:CHAR \b]
            [:CHAR \c]]]]))

  (is (= (validate-and-parse "[a-c]{2,5}")
         [[:MIN_MAX_QUANTIFIER [2 5]
           [:SET
            [:RANGE
             [:CHAR \a]
             [:CHAR \c]]]]]))

  (is (= (validate-and-parse "[a-cd-e]{2,5}")
         [[:MIN_MAX_QUANTIFIER [2 5]
           [:SET
            [:RANGE [:CHAR \a] [:CHAR \c]]
            [:RANGE [:CHAR \d] [:CHAR \e]]]]]))

  (is (= (validate-and-parse "[a-c\\-d-e]{2,5}")
         [[:MIN_MAX_QUANTIFIER [2 5]
           [:SET
            [:RANGE [:CHAR \a] [:CHAR \c]]
            [:CHAR \-]
            [:RANGE [:CHAR \d] [:CHAR \e]]]]]))


  (is (= (validate-and-parse ".")
         [[:DOT]]))


  (is (= (validate-and-parse "a.")
         [[:CHAR \a] [:DOT]]))

  (is (= (validate-and-parse "[a.]")
         [[:SET [:CHAR \a] [:CHAR \.]]]))

  (is (= (validate-and-parse ".*")
         [[:STAR_QUANTIFIER [:DOT]]]))


  (is (= (validate-and-parse ".+")
         [[:PLUS_QUANTIFIER [:DOT]]]))


  (is (= (validate-and-parse ".?")
         [[:QMARK_QUANTIFIER [:DOT]]]))

  (is (= (validate-and-parse ".{1023}")
         [[:EXACT_QUANTIFIER [1023] [:DOT]]]))

  (is (= (validate-and-parse ".{10,23}")
         [[:MIN_MAX_QUANTIFIER [10 23] [:DOT]]]))

  (is (= (validate-and-parse "[a-z]{10,11}.{10,23}")
         [[:MIN_MAX_QUANTIFIER
           [10 11]
           [:SET [:RANGE [:CHAR \a] [:CHAR \z]]]]
          [:MIN_MAX_QUANTIFIER [10 23] [:DOT]]]))



  (is (= (validate-and-parse "[^abc]")
         [[:NEG_SET
           [:CHAR \a]
           [:CHAR \b]
           [:CHAR \c]]]))

  (is (= (parse-tree "[^abc[]")
         ;; this is interpreted differently by Java where Java considers this invalid:
         ;; Execution error (PatternSyntaxException) at java.util.regex.Pattern/error (Pattern.java:1955).
         ;; Unclosed character class near index 5
         ;; [abc[]
         ;;      ^
         ;; See https://regexr.com/
         [[:NEG_SET
           [:CHAR \a]
           [:CHAR \b]
           [:CHAR \c]
           [:CHAR \[]]]))

  (is (= (validate-and-parse "[^abc]]")
         [[:NEG_SET
           [:CHAR \a]
           [:CHAR \b]
           [:CHAR \c]]
          [:CHAR \]]]))

  (is (= (validate-and-parse "[^abc]d]")
         [[:NEG_SET
           [:CHAR \a]
           [:CHAR \b]
           [:CHAR \c]]
          [:CHAR \d]
          [:CHAR \]]]))

  (is (= (validate-and-parse "[^abc(d]")
         [[:NEG_SET
           [:CHAR \a]
           [:CHAR \b]
           [:CHAR \c]
           [:CHAR \(]
           [:CHAR \d]]]))

  (is (= (validate-and-parse "[^abc)d]")
         [[:NEG_SET
           [:CHAR \a]
           [:CHAR \b]
           [:CHAR \c]
           [:CHAR \)]
           [:CHAR \d]]]))

  (is (= (validate-and-parse "[^abc)+*-.d]")
         [[:NEG_SET
           [:CHAR \a]
           [:CHAR \b]
           [:CHAR \c]
           [:CHAR \)]
           [:CHAR \+]
           [:RANGE [:CHAR \*] [:CHAR \.]]
           [:CHAR \d]]]))

  (is (= (validate-and-parse "[^a-z]bcde")
         [[:NEG_SET [:RANGE [:CHAR \a] [:CHAR \z]]]
          [:CHAR \b]
          [:CHAR \c]
          [:CHAR \d]
          [:CHAR \e]]))

  (is (= (validate-and-parse "[^a-z0-9]")
         [[:NEG_SET
           [:RANGE [:CHAR \a] [:CHAR \z]]
           [:RANGE [:CHAR \0] [:CHAR \9]]]]))

  (is (= (validate-and-parse "[^a-z0-9x]")
         [[:NEG_SET
           [:RANGE [:CHAR \a] [:CHAR \z]]
           [:RANGE [:CHAR \0] [:CHAR \9]]
           [:CHAR \x]]]))

  (is (= (validate-and-parse "[^xa-zy0-9z]")
         [[:NEG_SET
           [:CHAR \x]
           [:RANGE [:CHAR \a] [:CHAR \z]]
           [:CHAR \y]
           [:RANGE [:CHAR \0] [:CHAR \9]]
           [:CHAR \z]]]))

  (is (= (validate-and-parse "[^-az]")
         [[:NEG_SET
           [:CHAR \-]
           [:CHAR \a]
           [:CHAR \z]]]))

  (is (= (validate-and-parse "[^-az-]")
         [[:NEG_SET
           [:CHAR \-]
           [:CHAR \a]
           [:CHAR \z]
           [:CHAR \-]]]))

  (is (= (validate-and-parse "[^-a-z-]")
         [[:NEG_SET
           [:CHAR \-]
           [:RANGE [:CHAR \a] [:CHAR \z]]
           [:CHAR \-]]]))

  (is (= (validate-and-parse "[^-----]")
         [[:NEG_SET
           [:RANGE [:CHAR \-] [:CHAR \-]]
           [:CHAR \-]
           [:CHAR \-]]]))

  (is (= (validate-and-parse "[^a-a]")
         [[:NEG_SET
           [:RANGE [:CHAR \a] [:CHAR \a]]]]))

  (is (= (validate-and-parse "[^a\\-z]")
         [[:NEG_SET
           [:CHAR \a]
           [:CHAR \-]
           [:CHAR \z]]]))

  (is (= (validate-and-parse "[^a]?")
         [[:QMARK_QUANTIFIER [:NEG_SET [:CHAR \a]]]]))

  (is (= (validate-and-parse "[^abc]?")
         [[:QMARK_QUANTIFIER
           [:NEG_SET
            [:CHAR \a]
            [:CHAR \b]
            [:CHAR \c]]]]))

  (is (= (validate-and-parse "[^a-c]?")
         [[:QMARK_QUANTIFIER
           [:NEG_SET
            [:RANGE
             [:CHAR \a]
             [:CHAR \c]]]]]))

  (is (= (validate-and-parse "[^a-cd-e]?")
         [[:QMARK_QUANTIFIER
           [:NEG_SET
            [:RANGE [:CHAR \a] [:CHAR \c]]
            [:RANGE [:CHAR \d] [:CHAR \e]]]]]))

  (is (= (validate-and-parse "[^a-c\\-d-e]?")
         [[:QMARK_QUANTIFIER
           [:NEG_SET
            [:RANGE [:CHAR \a] [:CHAR \c]]
            [:CHAR \-]
            [:RANGE [:CHAR \d] [:CHAR \e]]]]]))

  (is (= (validate-and-parse "[^a]*")
         [[:STAR_QUANTIFIER [:NEG_SET [:CHAR \a]]]]))

  (is (= (validate-and-parse "[^abc]*")
         [[:STAR_QUANTIFIER
           [:NEG_SET
            [:CHAR \a]
            [:CHAR \b]
            [:CHAR \c]]]]))

  (is (= (validate-and-parse "[^a-c]*")
         [[:STAR_QUANTIFIER
           [:NEG_SET
            [:RANGE
             [:CHAR \a]
             [:CHAR \c]]]]]))

  (is (= (validate-and-parse "[^a-cd-e]*")
         [[:STAR_QUANTIFIER
           [:NEG_SET
            [:RANGE [:CHAR \a] [:CHAR \c]]
            [:RANGE [:CHAR \d] [:CHAR \e]]]]]))

  (is (= (validate-and-parse "[^a-c\\-d-e]*")
         [[:STAR_QUANTIFIER
           [:NEG_SET
            [:RANGE [:CHAR \a] [:CHAR \c]]
            [:CHAR \-]
            [:RANGE [:CHAR \d] [:CHAR \e]]]]]))

  (is (= (validate-and-parse "[^a]+")
         [[:PLUS_QUANTIFIER [:NEG_SET [:CHAR \a]]]]))

  (is (= (validate-and-parse "[^abc]+")
         [[:PLUS_QUANTIFIER
           [:NEG_SET
            [:CHAR \a]
            [:CHAR \b]
            [:CHAR \c]]]]))

  (is (= (validate-and-parse "[^a-c]+")
         [[:PLUS_QUANTIFIER
           [:NEG_SET
            [:RANGE
             [:CHAR \a]
             [:CHAR \c]]]]]))

  (is (= (validate-and-parse "[^a-cd-e]+")
         [[:PLUS_QUANTIFIER
           [:NEG_SET
            [:RANGE [:CHAR \a] [:CHAR \c]]
            [:RANGE [:CHAR \d] [:CHAR \e]]]]]))

  (is (= (validate-and-parse "[^a-c\\-d-e]+")
         [[:PLUS_QUANTIFIER
           [:NEG_SET
            [:RANGE [:CHAR \a] [:CHAR \c]]
            [:CHAR \-]
            [:RANGE [:CHAR \d] [:CHAR \e]]]]]))

  (is (= (validate-and-parse "[^a-z]{20,100}a")
         [[:MIN_MAX_QUANTIFIER
           [20 100]
           [:NEG_SET [:RANGE [:CHAR \a] [:CHAR \z]]]]
          [:CHAR \a]]))

  (is (= (validate-and-parse "[^a-zx]{0,100}")
         [[:MIN_MAX_QUANTIFIER
           [0 100]
           [:NEG_SET
            [:RANGE [:CHAR \a] [:CHAR \z]]
            [:CHAR \x]]]]))

  (is (= (validate-and-parse "[^a-zx]{100}")
         [[:EXACT_QUANTIFIER
           [100]
           [:NEG_SET
            [:RANGE [:CHAR \a] [:CHAR \z]]
            [:CHAR \x]]]]))

  (is (= (validate-and-parse "[^a-zx]{0}")
         [[:EXACT_QUANTIFIER
           [0]
           [:NEG_SET
            [:RANGE [:CHAR \a] [:CHAR \z]]
            [:CHAR \x]]]]))

  (is (= (validate-and-parse "[^a-zx]{0,1}")
         [[:MIN_MAX_QUANTIFIER
           [0,1]
           [:NEG_SET
            [:RANGE [:CHAR \a] [:CHAR \z]]
            [:CHAR \x]]]]))

  (is (= (validate-and-parse "[^a-zx]0,1}")
         [[:NEG_SET
           [:RANGE [:CHAR \a] [:CHAR \z]]
           [:CHAR \x]]
          [:CHAR \0]
          [:CHAR \,]
          [:CHAR \1]
          [:CHAR \}]]))

  (is (= (validate-and-parse "[^a]{25}")
         [[:EXACT_QUANTIFIER [25] [:NEG_SET [:CHAR \a]]]]))

  (is (= (validate-and-parse "[^abc]{25}")
         [[:EXACT_QUANTIFIER  [25]
           [:NEG_SET
            [:CHAR \a]
            [:CHAR \b]
            [:CHAR \c]]]]))

  (is (= (validate-and-parse "[^a-c]{25}")
         [[:EXACT_QUANTIFIER [25]
           [:NEG_SET
            [:RANGE
             [:CHAR \a]
             [:CHAR \c]]]]]))

  (is (= (validate-and-parse "[^a-cd-e]{25}")
         [[:EXACT_QUANTIFIER [25]
           [:NEG_SET
            [:RANGE [:CHAR \a] [:CHAR \c]]
            [:RANGE [:CHAR \d] [:CHAR \e]]]]]))

  (is (= (validate-and-parse "[^a-c\\-d-e]{25}")
         [[:EXACT_QUANTIFIER [25]
           [:NEG_SET
            [:RANGE [:CHAR \a] [:CHAR \c]]
            [:CHAR \-]
            [:RANGE [:CHAR \d] [:CHAR \e]]]]]))

  (is (= (validate-and-parse "[^a]{2,5}")
         [[:MIN_MAX_QUANTIFIER [2 5] [:NEG_SET [:CHAR \a]]]]))

  (is (= (validate-and-parse "[^abc]{2,5}")
         [[:MIN_MAX_QUANTIFIER [2 5]
           [:NEG_SET
            [:CHAR \a]
            [:CHAR \b]
            [:CHAR \c]]]]))

  (is (= (validate-and-parse "[^a-c]{2,5}")
         [[:MIN_MAX_QUANTIFIER [2 5]
           [:NEG_SET
            [:RANGE
             [:CHAR \a]
             [:CHAR \c]]]]]))

  (is (= (validate-and-parse "[^a-cd-e]{2,5}")
         [[:MIN_MAX_QUANTIFIER [2 5]
           [:NEG_SET
            [:RANGE [:CHAR \a] [:CHAR \c]]
            [:RANGE [:CHAR \d] [:CHAR \e]]]]]))

  (is (= (validate-and-parse "[^a-c\\-d-e]{2,5}")
         [[:MIN_MAX_QUANTIFIER [2 5]
           [:NEG_SET
            [:RANGE [:CHAR \a] [:CHAR \c]]
            [:CHAR \-]
            [:RANGE [:CHAR \d] [:CHAR \e]]]]]))

  (is (= (validate-and-parse "[^a.]")
         [[:NEG_SET [:CHAR \a] [:CHAR \.]]]))

  (is (= (validate-and-parse "[^a-z]{10,11}.{10,23}")
         [[:MIN_MAX_QUANTIFIER
           [10 11]
           [:NEG_SET [:RANGE [:CHAR \a] [:CHAR \z]]]]
          [:MIN_MAX_QUANTIFIER [10 23] [:DOT]]])))





;; list of things not supported
;; a?? -->  laziness not supported

(deftest parse-invalid-test
  (is (invalid? :invalid-range "[z-a]bcde"))
  (is (invalid? :invalid-range "[]"))
  (is (invalid? :invalid-range "[]a"))
  (is (invalid? :invalid-range "a[]a"))
  (is (invalid? :invalid-range "a[]"))
  (is (invalid? :dangling-meta-character "?"))
  ;; laziness is valid regex but not implemented in our parser
  (is (invalid? :dangling-meta-character "a??"
               {:test-java-pattern? false}))
  (is (invalid? :dangling-meta-character "*"))
  (is (invalid? :dangling-meta-character "**"))
  (is (invalid? :dangling-meta-character "a**"))
  (is (invalid? :dangling-meta-character "+"))
  (is (invalid? :dangling-meta-character "++"))
  (is (invalid? :dangling-meta-character "a++"
                {:test-java-pattern? false}))
  (is (invalid? :dangling-meta-character "+*-"))
  (is (invalid? :dangling-meta-character "+-*"))
  (is (invalid? :dangling-meta-character "-+*"))
  (is (invalid? :dangling-meta-character "-+?*"))
  (is (invalid? :dangling-meta-character "-+?*?"))
  (is (invalid? :dangling-meta-character "?-"))

  (is (invalid? :invalid-quantifier "[a-zx]{10,1}"))
  ;; Java considers this valid as a lower bound only, but is not implemented in our parser
  (is (invalid? :invalid-quantifier "[a-zx]{10,}"
                {:test-java-pattern? false}))
  (is (invalid? :invalid-quantifier "[a-zx]{,,}"))
  (is (invalid? :invalid-quantifier "[a-zx]{,}"))
  (is (invalid? :invalid-quantifier "[a-zx]{,19}"))
  (is (invalid? :invalid-quantifier "[a-zx]{-19}"))
  (is (invalid? :invalid-quantifier "[a-zx]{s}"))
  (is (invalid? :invalid-range "[]{10,1}"))
  (is (invalid? :lexer-error ".{10, 23}")) ;; whitespace
  )
