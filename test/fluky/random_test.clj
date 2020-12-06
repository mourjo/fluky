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

  (is (thrown? AssertionError (sut/rand-char-from-range [[:RANGE [:CHAR \z] [:CHAR \a]]]))))
