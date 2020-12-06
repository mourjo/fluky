(ns fluky.random
  (:require [fluky.utils :as fu]
            [clojure.set :as cset]))

;; Note. This is not uniformly random.


(def ^{:dynamic true} *enable-random-generation* true)


(def default-range
  [[:RANGE [:CHAR \a] [:CHAR \z]]
   [:RANGE [:CHAR \A] [:CHAR \Z]]
   [:RANGE [:CHAR \0] [:CHAR \9]]])


(defn random-in-range
  [[t & range]]
  (when *enable-random-generation*
    (case t
      :RANGE (char (apply fu/rand-range (mapv (comp int second) range)))
      :CHAR (first range))))


(defn rand-char-from-range
  [range]
  (when (and *enable-random-generation* (seq range))
    (char (random-in-range (rand-nth range)))))


(defn any-rand-char
  []
  (when *enable-random-generation* (rand-char-from-range default-range)))


(let [alpha (set (seq "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0987654321"))]
  (defn rand-char-from-negative-range
    [neg-range]
    ;; Due to lack of time, using enumeration here, but it is possible to break
    ;; the positive default range into set of ranges with holes, eg:
    ;; [^bcdf] would convert the range a-z into these ranges :
    ;; a
    ;; e
    ;; g-z
    ;; At this point, the `rand-char-from-range` may be used

    (when *enable-random-generation*
          (let [reducer (fn [acc [t & r]]
                          (into acc
                                (case t
                                  :RANGE (map char (apply fu/range-incl (mapv (comp int second) r)))
                                  :CHAR r)))
                possibilities (->> neg-range
                                   (reduce reducer #{})
                                   (cset/difference alpha)
                                   seq)]
            (if possibilities
              (rand-nth possibilities)
              (throw (ex-info "No possibilities in A-Za-z0-9"
                              {:range neg-range
                               :type :no-possibility})))))))
