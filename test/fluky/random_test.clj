(ns fluky.random-test
  (:require [fluky.random :as sut]
            [clojure.test :refer :all])
  (:import clojure.lang.ExceptionInfo))




(defmacro compare-generation
  [alpha & forms]
  `(is (= (set (seq ~alpha))
          (set (repeatedly
                10000
                (fn [] ~@forms))))))



(def everything
  "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0987654321")


(deftest range-test
  (compare-generation everything
                      (sut/any-rand-char))

  (compare-generation "0987654321"
                      (sut/rand-char-from-range [[:RANGE [:CHAR \0] [:CHAR \9]]]))

  (compare-generation "abc"
                      (sut/rand-char-from-range [[:CHAR \a] [:CHAR \b] [:CHAR \c]]))

  (compare-generation "abcxyz"
                      (sut/rand-char-from-range [[:CHAR \a]
                                                 [:CHAR \b]
                                                 [:CHAR \c]
                                                 [:RANGE [:CHAR \x] [:CHAR \z]]]))

  (compare-generation everything
                      (sut/rand-char-from-range [[:CHAR \a]
                                                 [:CHAR \b]
                                                 [:CHAR \c]
                                                 [:RANGE [:CHAR \a] [:CHAR \z]]
                                                 [:RANGE [:CHAR \A] [:CHAR \A]]
                                                 [:RANGE [:CHAR \A] [:CHAR \Z]]
                                                 [:RANGE [:CHAR \0] [:CHAR \9]]]))

  (is (nil? (sut/rand-char-from-range [])))

  (is (thrown? AssertionError (sut/rand-char-from-range [[:RANGE [:CHAR \z] [:CHAR \a]]])))

  (compare-generation "abcxyz"
                      (sut/rand-char-from-negative-range
                       [[:RANGE [:CHAR \0] [:CHAR \9]]
                        [:RANGE [:CHAR \d] [:CHAR \w]]
                        [:RANGE [:CHAR \A] [:CHAR \Z]]]))

  (compare-generation "acxyz"
                      (sut/rand-char-from-negative-range
                       [[:RANGE [:CHAR \0] [:CHAR \9]]
                        [:RANGE [:CHAR \d] [:CHAR \w]]
                        [:CHAR \b]
                        [:RANGE [:CHAR \A] [:CHAR \Z]]]))

  (compare-generation "acxyz"
                      (sut/rand-char-from-negative-range
                       [[:RANGE [:CHAR \0] [:CHAR \9]]
                        [:RANGE [:CHAR \d] [:CHAR \w]]
                        [:CHAR \b]
                        [:RANGE [:CHAR \A] [:CHAR \Z]]
                        [:CHAR \b]]))

  (compare-generation "axyz"
                      (sut/rand-char-from-negative-range
                       [[:RANGE [:CHAR \0] [:CHAR \9]]
                        [:RANGE [:CHAR \b] [:CHAR \w]]
                        [:RANGE [:CHAR \A] [:CHAR \Z]]
                        [:CHAR \b]]))

  (compare-generation "axyzZ"
                      (sut/rand-char-from-negative-range
                       [[:RANGE [:CHAR \0] [:CHAR \9]]
                        [:RANGE [:CHAR \b] [:CHAR \w]]
                        [:RANGE [:CHAR \A] [:CHAR \Y]]
                        [:CHAR \b]]))

  (compare-generation "axyzZ"
                      (sut/rand-char-from-negative-range
                       [[:RANGE [:CHAR \0] [:CHAR \9]]
                        [:RANGE [:CHAR \b] [:CHAR \w]]
                        [:RANGE [:CHAR \A] [:CHAR \Y]]
                        [:CHAR \b]]))

  (is (thrown-with-msg? ExceptionInfo
                        #"No possibilities"
                        (sut/rand-char-from-negative-range
                         [[:RANGE [:CHAR \A] [:CHAR \Z]]
                          [:RANGE [:CHAR \a] [:CHAR \z]]
                          [:RANGE [:CHAR \0] [:CHAR \9]]
                          [:CHAR \b]]))))
