(ns fluky.core-test
  (:require [clojure.test :refer :all]
            [fluky.core :as sut]
            [clojure.set :as cset]
            [clojure.string :as cstr]
            [clojure.test.check.properties :as prop]
            [fluky.generators :as fgen]
            [clojure.test.check.clojure-test :as ct])
  (:import java.util.regex.Pattern))

(def regexes
  [["[x-x]+"]

   ["[a-x]+"]

   ["[a-x{}]+"]

   ["a-x"]

   ["[a-x+{}]+"]

   ["[{}]"
    (fn [all-generated-results]
      (= #{"{" "}"} (set all-generated-results)))]

   ["[a-x0-9]+"
    (fn [all-generated-results]
      (some (fn [x]
              (re-matches #"[a-x]+" x))
            all-generated-results))
    (fn [all-generated-results]
      (some (fn [x]
              (re-matches #"[0-9]+" x))
            all-generated-results))]

   ["abcd" (fn [all-generated-results]
             (every? #{"abcd"} all-generated-results))]

   ["[^x-x]+"]

   ["[^a-x]+"]

   ["[^a-x{}]+"]

   ["[^a-x+{}]+"]

   ["[^{}]"
    (fn [all-generated-results]
      (empty? (cset/intersection #{"{" "}"}
                                 (set all-generated-results))))]

   ["[^a-x0-9]+" (fn [all-generated-results]
                   (not-any? (fn [x] (re-matches #"[a-x]+" x))
                             all-generated-results))
    (fn [all-generated-results]
      (not-any? (fn [x] (re-matches #"[0-9]+" x))
                all-generated-results))]

   ["[^a]+"]

   ["[a]{4}"
    (fn [all-generated-results]
      (every? #{"aaaa"}
              all-generated-results))]

   ["a{4}"
    (fn [all-generated-results]
      (every? #{"aaaa"}
              all-generated-results))]

   ["[^a]{4}"
    (fn [all-generated-results]
      (not-any? #{"aaaa"}
                all-generated-results)
      (every? (fn [x] (= 4 (count x)))
              all-generated-results))]

   ["[a]{1,4}"
    (fn [all-generated-results]
      (= #{"a" "aa" "aaa" "aaaa"}
         (set all-generated-results)))]

   ["a{1,4}"
    (fn [all-generated-results]
      (= #{"a" "aa" "aaa" "aaaa"}
         (set all-generated-results)))]

   ["[^a]{1,4}"
    (fn [all-generated-results]
      (every? (fn [x] (<= 1 (count x) 4))
              all-generated-results))]

   ["[a]*"
    (fn [all-generated-results]
      (every? (fn [x]
                (or (= #{\a} (set x))
                    (empty? x)))
              all-generated-results))]

   ["a*"
    (fn [all-generated-results]
      (every? (fn [x]
                (or (= #{\a} (set x))
                    (empty? x)))
              all-generated-results))]

   ["[a]?"
    (fn [all-generated-results]
      (every? (some-fn empty? #{"a"})
              all-generated-results))]

   ["a?"
    (fn [all-generated-results]
      (every? (some-fn empty? #{"a"})
              all-generated-results))]

   ["[a-e0-2]*"
    (fn [all-generated-results]
      (every? (fn [x]
                (every? #{\a \b \c \d \e \0 \1 \2} x))
              all-generated-results))]

   ["[a-e0-2]+"
    (fn [all-generated-results]
      (every? (fn [x]
                (and (seq x)
                     (every? #{\a \b \c \d \e \0 \1 \2} x)))
              all-generated-results))]

   ["[^a-e0-2]+"
    (fn [all-generated-results]
      (every? (fn [x]
                (and (seq x)
                     (not-any? #{\a \b \c \d \e \0 \1 \2} x)))
              all-generated-results))]

   ["[^a-e0-2]{5,5}"
    (fn [all-generated-results]
      (every? (fn [x]
                (and (= 5 (count x))
                     (not-any? #{\a \b \c \d \e \0 \1 \2} x)))
              all-generated-results))]

   ["[a-e0-2]{5}"
    (fn [all-generated-results]
      (every? (fn [x]
                (and (= 5 (count x))
                     (every? #{\a \b \c \d \e \0 \1 \2} x)))
              all-generated-results))]

   ["[a-e0-2]?"
    (fn [all-generated-results]
      (every? (fn [x]
                (and (<= 0 (count x) 1)
                     (every? #{\a \b \c \d \e \0 \1 \2} x)))
              all-generated-results))]

   ["[a-e0-2.]?"
    (fn [all-generated-results]
      (every? (fn [x] (<= 0 (count x) 1))
              all-generated-results))
    (fn [all-generated-results]
      (some (fn [x]
              (not-any? #{\a \b \c \d \e \0 \1 \2} x))
            all-generated-results))]
   [".*"]

   ["[.]*"
    (fn [all-generated-results]
      (some (fn [x] (every? #{\.} x))
            all-generated-results))]

   [".{5,6}"
    (fn [all-generated-results]
      (every? (fn [x] (<= 5 (count x) 6))
              all-generated-results))]])


(defn valid?
  [regex-string result]
  (= result
     (re-matches (Pattern/compile regex-string)
                 result)))

(defn valid*
  [regex-string result]
  [(Pattern/compile regex-string)
   (re-matches (Pattern/compile regex-string)
               result)])


(deftest random-regex-validation-test
  (doseq [[regex & additional-validations] regexes]
    (try
      (let [results (atom [])]
        (dotimes [_ 1000]
          (let [result (sut/random-regex regex)]
            (is (valid? regex result))
            (swap! results conj result)))
        (when (seq additional-validations)
          (is ((apply every-pred additional-validations)
               @results)
              (str "All additional validations pass on the result " (pr-str regex)))))
      (catch Throwable t
        (println "Erorr in " (pr-str regex))
        (throw t)))))


(ct/defspec generative-regex-generation
  1000
  (prop/for-all [regex-str fgen/gregex]
                (let [result (sut/random-regex regex-str)]
                  (is (valid? regex-str result)))))
