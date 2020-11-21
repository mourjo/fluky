(ns fluky.core
  (:require [fluky.grammar :as fg]
            [clojure.pprint :as pprint]
            [clojure.string :as cstr])
  (:gen-class))

(defn rotate
  [xs]
  (drop (rand-int (count xs)) (cycle xs)))

(def FACTOR 5)
(def ALPHABET
  (sort-by int
           [\a \A \b \B \c \C \d \D \e \E \f \F \g \G \h \H \i \I \j \J \k \K \l \L \m \M \n \N \o
            \O \p \P \q \Q \r \R \s \S \t \T \u \U \v \V \w \W \x \X \y \Y \z \Z \0 \1 \2 \3 \4 \5
            \6 \7 \8 \9 \. \{\} \[\] \ \- \* \+ ]))

(def ^:dynamic *context* :pos)

(defmulti rwalk first)

(defmethod rwalk :REGEX
  [[_ & clauses]]
  (cstr/join "" (map rwalk clauses)))


(defmethod rwalk :REGEX_CLAUSE
  [[_ clause]]
  (rwalk clause))


(defmethod rwalk :PLUS_QUANTIFIER
  [[_ clause _]]
  (cstr/join ""
             (repeatedly (inc (rand-int FACTOR))
                         (fn [] (rwalk clause)))))

(defmethod rwalk :POS_SET
  [[_ _ & clauses]]
  (let [candidates (shuffle (butlast clauses))
        f (apply some-fn (map rwalk candidates))]
    (str (first (filter f
                        (rotate ALPHABET))))))


(defmethod rwalk :NEG_SET
  [[_ _ _ & clauses]]
  (let [candidates (butlast clauses)]
    (binding [*context* :neg]
      (let [f (apply some-fn (map rwalk candidates))]
        (str (first (remove f
                            (rotate ALPHABET))))))))




(defmethod rwalk :META_CHAR
  [[_ clause]]
  (if (= :neg *context*)
    (str (first (filter (fn [c] (not= (first clause) c)) (rotate ALPHABET))))
    clause))


(defmethod rwalk :CHAR
  [[_ clause]]
  (if (= :neg *context*)
    (str (first (filter (fn [c] (not= (first clause) c)) (rotate ALPHABET))))
    clause))

(defmethod rwalk :RANGE
  [[_ from _ to :as clauses]]

  (let [f (int (first (binding [*context* :pos] (rwalk from))))
        t (int (first (binding [*context* :pos] (rwalk to ))))]
    ;; (prn [f t])

    (if (<= f t)
      (fn [c] (<= (int f) (int c) (int t)))
      (throw (ex-info "Invalid range" {:clauses clauses})))

    ;; (if (<= f t)
    ;;   (if (= :neg *context*)
    ;;     (str (first (filter (fn [c] (not (<= (int f) (int c) (int t))))
    ;;                         (rotate ALPHABET))))
    ;;     (str (char (+ f (rand-int (inc (- t f)))))))
    ;;   (throw (ex-info "Invalid range" {:clauses clauses})))
    ))


(defn random-regex
  [s]
  (let [tree (fg/regex->tree s)]
    ;;(clojure.pprint/pprint tree)
    (rwalk tree)))
