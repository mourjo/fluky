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



(deftest add-holes-to-range-test
  (is (= (sut/add-holes-to-range
          [5 10]
          [1 2])
         [[5 10]]))

  (is (= (sut/add-holes-to-range
          [5 10]
          [1 11])
         []))

  (is (= (sut/add-holes-to-range
          [5 10]
          [1 10])
         []))

  (is (= (sut/add-holes-to-range
          [5 10]
          [5 10])
         []))

  (is (= (sut/add-holes-to-range
          [5 10]
          [6 10])
         [[5 5]]))

  (is (= (sut/add-holes-to-range
          [5 10]
          [5 9])
         [[10 10]]))

  (is (= (sut/add-holes-to-range
          [5 10]
          [5 11])
         []))

  (is (= (sut/add-holes-to-range
          [5 10]
          [1 6])
         [[7 10]]))

  (is (= (sut/add-holes-to-range
          [5 10]
          [5 7])
         [[8 10]]))

  (is (= (sut/add-holes-to-range
          [5 10]
          [6 7])
         [[5 5] [8 10]]))

  (is (= (sut/add-holes-to-range
          [5 10]
          [7 11])
         [[5 6]]))

  (is (= (sut/add-holes-to-range
          [5 10]
          [11 100])
         [[5 10]]))

  (is (= (sut/add-holes-to-range
          [5 10]
          [10 100])
         [[5 9]]))

  (is (= (sut/add-holes-to-range
          [5 5]
          [6 10])
         [[5 5]]))

  (is (= (sut/add-holes-to-range
          [5 5]
          [4 10])
         []))

  (is (= (sut/add-holes-to-range
          [5 5]
          [4 5])
         []))

  (is (= (sut/add-holes-to-range
          [5 5]
          [8 10])
         [[5 5]]))

  (is (= (sut/add-holes-to-range
          [5 5]
          [1 2])
         [[5 5]]))

  (is (= (sut/add-holes-to-range
          [5 10]
          [1 1])
         [[5 10]]))

  (is (= (sut/add-holes-to-range
          [5 10]
          [5 5])
         [[6 10]]))

  (is (= (sut/add-holes-to-range
          [5 10]
          [6 6])
         [[5 5] [7 10]]))

  (is (= (sut/add-holes-to-range
          [5 10]
          [9 9])
         [[5 8] [10 10]]))

  (is (= (sut/add-holes-to-range
          [5 10]
          [10 10])
         [[5 9]]))

  (is (= (sut/add-holes-to-range
          [5 5]
          [5 5])
         []))

  (is (= (sut/add-holes-to-range
          [5 5]
          [5 5])
         []))

  (is (= (sut/add-holes-to-range
          [5 10]
          [3 5])
         [[6 10]]))

  (is (= (sut/add-holes-to-range [5 15] [6 10])
         [[5 5] [11 15]]))

  (is (= (sut/add-holes-to-range [5 15] [11 15])
         [[5 10]])))


(deftest add-holes-to-ranges-test
  (is (= (sut/add-holes-to-ranges [[1 10]] [[1 1] [2 2] [7 8]])
         [[3 6] [9 10]]))

  (is (= (sut/add-holes-to-ranges [[1 10]] [[1 1] [2 2]])
         [[3 10]]))

  (is (= (sut/add-holes-to-ranges [[5 10]] [[1 1] [2 2]])
         [[5 10]]))

  (is (= (sut/add-holes-to-ranges [[5 10]] [[1 20]])
         []))

  (is (= (sut/add-holes-to-ranges [[5 10]] [[1 1] [2 2] [3 3] [4 4] [5 5] [6 6] [7 7] [8 8] [9 9] [10 10]])
         []))

  (is (= (sut/add-holes-to-ranges [[5 10]] [[11 20]])
         [[5 10]]))

  (is (= (sut/add-holes-to-ranges [[5 10]] [[1 20]])
         []))

  (is (= (sut/add-holes-to-ranges [[5 10]] [[3 5] [2 7]])
         [[8 10]]))

  (is (= (sut/add-holes-to-ranges [[5 10]] [[3 5] [2 3]])
         [[6 10]]))

  (is (= (sut/add-holes-to-ranges [[5 10]] [[5 8] [6 9]])
         [[10 10]]))

  (is (= (sut/add-holes-to-ranges [[5 15]] [[6 10] [7 12]])
         [[5 5] [13 15]]))

  (is (= (sut/add-holes-to-ranges [[5 15]] [[6 10] [11 15]])
         [[5 5]]))

  (is (= (sut/add-holes-to-ranges [[5 15]] [[6 10] [10 15]])
         [[5 5]]))

  (is (= (sut/add-holes-to-ranges [[5 15]] [[6 9] [11 15]])
         [[5 5] [10 10]]))

  (is (= (sut/add-holes-to-ranges [[5 15]] [[1 9] [11 20]])
         [[10 10]]))

  (is (= (sut/add-holes-to-ranges [[5 15]] [[1 7] [11 20] [8 9]])
         [[10 10]]))

  (is (= (sut/add-holes-to-ranges [[5 15] [100 200]]
                                  [[1 7] [11 20] [8 9]])
         [[10 10] [100 200]]))

  (is (= (sut/add-holes-to-ranges [[5 15] [100 200]]
                                  [[1 7] [11 20000] [8 9]])
         [[10 10]]))

  (is (= (sut/add-holes-to-ranges [[5 15] [100 200]]
                                  [[1 7] [11 20] [8 9]])
         [[10 10] [100 200]]))

  (is (= (sut/add-holes-to-ranges [[5 15] [1 200]]
                                  [[1 7] [11 200] [8 9]])
         [[10 10]]))

  (is (= (sut/add-holes-to-ranges [[5 15] [1 201]]
                                  [[1 7] [11 200] [8 9]])
         [[10 10] [201 201]]))

  (is (= (sut/add-holes-to-ranges [[5 15] [1 205]]
                                  [[1 7] [11 200] [8 9]])
         [[10 10] [201 205]]))

  (is (= (sut/add-holes-to-ranges [[5 15] [1 205]]
                                  [[1 7] [11 200] [8 9]])
         [[10 10] [201 205]])))


(def rand-char-from-neg-range
  (comp sut/pos-range-to-rand-char sut/pos-range-from-neg-range))


(deftest negative-generation-test
  (compare-generation "0123456789abcdefghijklmnopqrstuvwxyz"
                      (rand-char-from-neg-range
                       [[:RANGE [:CHAR \A] [:CHAR \Z]]]))
  (compare-generation "0123456789"
                      (rand-char-from-neg-range
                       [[:RANGE [:CHAR \A] [:CHAR \Z]]
                        [:RANGE [:CHAR \a] [:CHAR \z]]]))
  (compare-generation "0123456789"
                      (rand-char-from-neg-range
                       [[:RANGE [:CHAR \a] [:CHAR \z]]
                        [:RANGE [:CHAR \A] [:CHAR \Z]]]))
  (compare-generation "{|}"
                      (rand-char-from-neg-range
                       [[:RANGE [:CHAR \a] [:CHAR \z]]
                        [:RANGE [:CHAR \0] [:CHAR \9]]
                        [:RANGE [:CHAR \A] [:CHAR \Z]]]))
  (compare-generation "012346789"
                      (rand-char-from-neg-range
                       [[:RANGE [:CHAR \a] [:CHAR \z]]
                        [:CHAR \5]
                        [:RANGE [:CHAR \A] [:CHAR \Z]]]))
  (compare-generation "01236789"
                      (rand-char-from-neg-range
                       [[:RANGE [:CHAR \a] [:CHAR \z]]
                        [:CHAR \4]
                        [:CHAR \5]
                        [:RANGE [:CHAR \A] [:CHAR \Z]]])))
