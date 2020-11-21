(ns fluky.core-test
  (:require [clojure.test :refer :all]
            [fluky.core :as sut]
            [clojure.string :as cstr])
  (:import java.util.regex.Pattern))

(def regexes
  [["[x-x]+"]
   ["[a-x]+"]
   ["[a-x{}]+"]
   ["a-x"]
   ["[a-x+{}]+"]
   ["[{}]" (fn [all-generated-results]
             (= #{"{" "}"} (set all-generated-results)))]
   ["[a-x0-9]+" (fn [all-generated-results]
                  (some (fn [x]
                          (re-matches #"[a-x]+" x))
                        all-generated-results))
    (fn [all-generated-results]
      (some (fn [x]
              (re-matches #"[0-9]+" x))
            all-generated-results))]
   ["abcd" (fn [all-generated-results]
             (every? #{"abcd"} all-generated-results))]])


(defn valid?
  [regex-string result]
  (= result
     (re-matches (Pattern/compile regex-string)
                 result)))


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
              "All additional validations pass on the result")))
      (catch Throwable t
        (println "Erorr in " (pr-str regex))
        (throw t)))))
