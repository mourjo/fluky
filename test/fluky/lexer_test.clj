(ns fluky.lexer-test
  (:require [fluky.lexer :as sut]
            [clojure.test :refer :all])
  (:import clojure.lang.ExceptionInfo))


(defn invalid?
  [s]
  (try (sut/lex s)
       false
       (catch ExceptionInfo e
         (-> e
             ex-data
             :type
             (= :lexer-error)))))


(deftest lex-test
  (is (= (sut/lex "[a-z]")
         [[:set [\a \- \z]]]))

  (is (= (sut/lex "\\\\")
         [[:escaped \\]]))

  (is (= (sut/lex "a+")
         [[:char \a] [:plus]]))

  (is (= (sut/lex "a?")
         [[:char \a] [:qmark]]))

  (is (= (sut/lex "[?*+-]")
         [[:set [\? \* \+ \-]]]))

  (is (= (sut/lex "[?*+-]*")
         [[:set [\? \* \+ \-]] [:star]]))

  (is (= (sut/lex "[-+]?[0-9]{1,16}abc*[.*][^0-9]{1,6}[a-z*]+a\\+")
         [[:set [\- \+]]
          [:qmark]
          [:set [\0 \- \9]]
          [:quantifier [\1 \, \1 \6]]
          [:char \a]
          [:char \b]
          [:char \c]
          [:star]
          [:set [\. \*]]
          [:neg-set [\0 \- \9]]
          [:quantifier [\1 \, \6]]
          [:set [\a \- \z \*]]
          [:plus]
          [:char \a]
          [:escaped \+]]))

  (is (= (sut/lex "[a\\-z]")
         [[:set [\a [:escaped \-] \z]]]))

  (is (= (sut/lex "[a\\-z\\\\]")
         [[:set [\a [:escaped \-] \z [:escaped \\]]]]))

  (is (= (sut/lex "[a\\-z]")
         [[:set [\a [:escaped \-] \z]]]))

  (is (= (sut/lex "a-z")
         [[:char \a] [:char \-] [:char \z]]))

  (is (= (sut/lex "a\\-z")
         [[:char \a] [:escaped \-] [:char \z]]))

  (is (= (sut/lex "[a\\-z]")
         [[:set [\a [:escaped \-] \z]]]))

  ;; this is invalid but the lexer does not know that:
  (is (= (sut/lex "[a-z.]+*")
         [[:set [\a \- \z \.]] [:plus] [:star]]))

  (is (invalid? "{"))
  (is (invalid? " "))
  (is (invalid? "["))
  (is (invalid? "\\8"))
  (is (invalid? "\\"))
  (is (invalid? "[a-z\\]"))
  (is (invalid? "[a-\\z]"))
  (is (invalid? "[-+]?[0-9]{1,16}abc*[.*][^0-9]{1,6}\\i[a-z*]+a\\+")))
