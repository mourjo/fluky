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
            \6 \7 \8 \9 \. \{ \} \[ \] \  \- \* \+ \? \( \) ]))

(def ^:dynamic *context* :pos)

(defmulti rwalk first)

(defmethod rwalk :REGEX
  [[_ & clauses]]
  (reduce (fn [acc typed-f]
            (if (= :selector (:type typed-f))
              (str acc (first (filter (:fn typed-f) (shuffle ALPHABET))))
              (str acc (cstr/join "" ((:fn typed-f) (shuffle ALPHABET))))))
          ""
          (map rwalk clauses)))


(defmethod rwalk :REGEX_CLAUSE
  [[_ clause]]
  (rwalk clause))


(defmethod rwalk :ESCAPED
  [[_ _ ch]]
  {:type :selector
   :fn (fn [c]
         (= (first ch) c))})


(defmethod rwalk :PLUS_QUANTIFIER
  [[_ clause _]]
  {:type :collector
   :fn (fn [alpha]
         (let [n (inc (rand-int FACTOR))]
           (take n (filter (:fn (rwalk clause)) (cycle alpha)))))})


(defmethod rwalk :STAR_QUANTIFIER
  [[_ clause _]]
  {:type :collector
   :fn (fn [alpha]
         (let [n (rand-int FACTOR)]
           (take n (filter (:fn (rwalk clause)) (cycle alpha)))))})


(defmethod rwalk :QMARK_QUANTIFIER
  [[_ clause _]]
  {:type :collector
   :fn (fn [alpha]
         (let [n (rand-int 2)]
           (take n (filter (:fn (rwalk clause)) alpha))))})

(defn rand-range
  [low high]
  (let [r (rand-int (- (inc high) low))]
    (+ low r)))


(defn parse-number
  [[_ & digits]]
  (Long/parseLong (cstr/join "" digits)))


(defmethod rwalk :MIN_MAX_QUANTIFIER
  [[_ clause _ lower _ upper _]]
  {:type :collector
   :fn (fn [alpha]
         (let [n (rand-range (parse-number lower) (parse-number upper))]
           (take n (filter (:fn (rwalk clause)) (cycle alpha)))))})


(defmethod rwalk :DOT
  [_]
  {:type :selector
   :fn (constantly true)})


(defmethod rwalk :EXACT_QUANTIFIER
  [[_ clause _ n _]]
  {:type :collector
   :fn (fn [alpha]
         (take (parse-number n)
               (filter (:fn (rwalk clause)) (cycle alpha))))})


(defmethod rwalk :POS_SET
  [[_ _ & clauses]]

  (let [candidates (shuffle (butlast clauses))
        f (apply some-fn (mapv (comp :fn rwalk) candidates))]
    {:type :selector
     :fn f}))


(defmethod rwalk :NEG_SET
  [[_ _ _ & clauses]]
  (let [candidates (butlast clauses)
        f (apply some-fn (map (comp :fn rwalk) candidates))]
    {:type :selector
     :fn (complement f)}))


(defmethod rwalk :META_CHAR
  [[_ clause]]
  {:type :selector
   :fn (fn [c] (= (first clause) c))})


(defmethod rwalk :CHAR
  [[_ clause]]
  {:type :selector
   :fn (fn [c] (= (first clause) c))})

(defmethod rwalk :RANGE
  [[_ from _ to :as clauses]]

  (let [f (rwalk from)
        t (rwalk to)]
    {:type :selector
     :fn (fn [c]
           (let [starting-char (first (filter (:fn f) ALPHABET))
                 ending-char (first (filter (:fn t) ALPHABET))]
             (if (<= (int starting-char) (int ending-char))
               (<= (int starting-char) (int c) (int ending-char))
               (throw (ex-info "Invalid range" {:clauses clauses})))))}))


(defn random-regex
  [s]
  (let [tree (fg/regex->tree s)]
    ;;(clojure.pprint/pprint tree)
    (rwalk tree)))
