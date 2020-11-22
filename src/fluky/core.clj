(ns fluky.core
  (:gen-class)
  (:require [clojure.string :as cstr]
            [fluky.grammar :as fg]
            [fluky.utils :as utils]))

;; walk the tree generated by the grammar to produce random strings
(defmulti rwalk first)

(defmethod rwalk :REGEX
  [[_ & clauses]]

  ;; REGEX = REGEX_CLAUSE*

  (reduce (fn [acc clause]
            (let [typed-f (rwalk clause)]
              (if (= :selector (:type typed-f))
                (str acc (utils/ffilter (:fn typed-f) (shuffle utils/ALPHABET)))
                (str acc (cstr/join "" ((:fn typed-f) (shuffle utils/ALPHABET)))))))
          ""
          clauses))


(defmethod rwalk :REGEX_CLAUSE
  [[_ clause]]

  ;; REGEX_CLAUSE = ESCAPED |
  ;;                DOT |
  ;;                CHAR |
  ;;                POS_SET |
  ;;                NEG_SET |
  ;;                STAR_QUANTIFIER |
  ;;                PLUS_QUANTIFIER |
  ;;                QMARK_QUANTIFIER |
  ;;                MIN_MAX_QUANTIFIER |
  ;;                EXACT_QUANTIFIER ;

  (rwalk clause))


(defmethod rwalk :ESCAPED
  [[_ _ ch]]

  ;; ESCAPED = (BACK_SLASH '+')    |
  ;;           (BACK_SLASH '*')    |
  ;;           (BACK_SLASH '-')    |
  ;;           (BACK_SLASH '\\\\') |
  ;;           (BACK_SLASH '?')    |
  ;;           (BACK_SLASH '{')    |
  ;;           (BACK_SLASH '}')    |
  ;;           (BACK_SLASH '[')    |
  ;;           (BACK_SLASH ']')    |
  ;;           (BACK_SLASH '(')    |
  ;;           (BACK_SLASH ')')    |
  ;;           (BACK_SLASH '.')    ;

  ;; Sample tree:
  ;; [:ESCAPED [:BACK_SLASH "\\"] "{"]

  {:type :selector
   :fn (fn [c]
         (= (first ch) c))})


(defmethod rwalk :PLUS_QUANTIFIER
  [[_ clause _]]

  ;; PLUS_QUANTIFIER = (ESCAPED | DOT | CHAR | POS_SET | NEG_SET) '+' ;
  ;; Sample tree for "a+" :
  ;; [:PLUS_QUANTIFIER [:CHAR "a"] "+"]

  {:type :collector
   :fn (fn [alpha]
         (let [n (inc (rand-int utils/BRANCH-FACTOR))]
           (take n (filter (:fn (rwalk clause)) (cycle alpha)))))})


(defmethod rwalk :STAR_QUANTIFIER
  [[_ clause _]]

  ;; STAR_QUANTIFIER = (ESCAPED | DOT | CHAR | POS_SET | NEG_SET) '*' ;
  ;; Sample tree for "a*":
  ;; [:STAR_QUANTIFIER [:CHAR "a"] "*"]

  {:type :collector
   :fn (fn [alpha]
         (let [n (rand-int utils/BRANCH-FACTOR)]
           (take n (filter (:fn (rwalk clause)) (cycle alpha)))))})


(defmethod rwalk :QMARK_QUANTIFIER
  [[_ clause _]]

  ;; QMARK_QUANTIFIER = (ESCAPED | DOT | CHAR | POS_SET | NEG_SET) '?' ;

  ;; Sample tree for "[a-z]":
  ;; [:POS_SET "[" [:RANGE [:CHAR "a"] "-" [:CHAR "z"]] "]"]

  ;; Sample tree for "[abcd]":
  ;; [:POS_SET
  ;; "["
  ;;    [:REGEX_CLAUSE [:CHAR "a"]]
  ;;    [:REGEX_CLAUSE [:CHAR "b"]]
  ;;    [:REGEX_CLAUSE [:CHAR "c"]]
  ;;    [:REGEX_CLAUSE [:CHAR "d"]]
  ;; "]"
  ;; ]

  {:type :collector
   :fn (fn [alpha]
         (let [n (rand-int 2)]
           (take n (filter (:fn (rwalk clause)) alpha))))})


(defmethod rwalk :MIN_MAX_QUANTIFIER
  [[_ clause _ lower _ upper _]]

  ;; MIN_MAX_QUANTIFIER = (ESCAPED | DOT | CHAR | POS_SET | NEG_SET) '{' NUMBER ',' NUMBER '}' ;
  ;; Sample tree for "a{1,5}":
  ;; [:MIN_MAX_QUANTIFIER [:CHAR "a"] "{" [:NUMBER "1"] "," [:NUMBER "5"] "}"]

  {:type :collector
   :fn (fn [alpha]
         (let [n (utils/rand-range (utils/parse-number lower)
                                   (utils/parse-number upper))]
           (take n (filter (:fn (rwalk clause)) (cycle alpha)))))})


(defmethod rwalk :EXACT_QUANTIFIER
  [[_ clause _ n _]]

  ;; EXACT_QUANTIFIER = (ESCAPED | DOT | CHAR | POS_SET | NEG_SET) '{' NUMBER '}' ;
  ;; Sample tree for a{5}:
  ;; [:EXACT_QUANTIFIER [:CHAR "a"] "{" [:NUMBER "5"] "}"]

  {:type :collector
   :fn (fn [alpha]
         (take (utils/parse-number n)
               (filter (:fn (rwalk clause)) (cycle alpha))))})


(defmethod rwalk :POS_SET
  [[_ _ & clauses]]

  ;; POS_SET = '[' (ESCAPED | DOT | CHAR | META_CHAR | RANGE | POS_SET | NEG_SET)+ ']' ;
  ;; Sample tree for "[a-z]":
  ;; [:POS_SET "[" [:RANGE [:CHAR "a"] "-" [:CHAR "z"]] "]"]

  (let [candidates (shuffle (butlast clauses))
        f (apply some-fn (mapv (comp :fn rwalk) candidates))]
    {:type :selector
     :fn f}))


(defmethod rwalk :NEG_SET
  [[_ _ _ & clauses]]

  ;; NEG_SET = '[' '^' (ESCAPED | DOT | CHAR | META_CHAR | RANGE | POS_SET | NEG_SET)+ ']' ;
  ;; Sample tree for "[^a-z]":
  ;; [:NEG_SET "[" "^" [:RANGE [:CHAR "a"] "-" [:CHAR "z"]] "]"]

  (let [candidates (butlast clauses)
        f (apply some-fn (map (comp :fn rwalk) candidates))]
    {:type :selector
     :fn (complement f)}))


(defmethod rwalk :RANGE
  [[_ from _ to :as clauses]]

  ;; RANGE = CHAR '-' CHAR ;

  (let [f (rwalk from)
        t (rwalk to)]
    {:type :selector
     :fn (fn [c]
           (let [starting-char (utils/ffilter (:fn f) utils/ALPHABET)
                 ending-char (utils/ffilter (:fn t) utils/ALPHABET)]
             (if (<= (int starting-char) (int ending-char))
               (<= (int starting-char) (int c) (int ending-char))
               (throw (ex-info "Invalid range" {:clauses clauses})))))}))


(defmethod rwalk :META_CHAR
  [[_ clause]]

  ;; META_CHAR =  '*' | '{' | '+' | '.';
  ;; Sample tree:
  ;; [:META_CHAR "{"]

  {:type :selector
   :fn (fn [c] (= (first clause) c))})


(defmethod rwalk :CHAR
  [[_ clause]]

  ;; CHAR = 'a' | 'A' | 'b' | 'B' | 'c' | 'C' | 'd' | 'D' | 'e' | 'E' | 'f' | 'F' |
  ;;        'g' | 'G' | 'h' | 'H' | 'i' | 'I' | 'j' | 'J' | 'k' | 'K' | 'l' | 'L' |
  ;;        'm' | 'M' | 'n' | 'N' | 'o' | 'O' | 'p' | 'P' | 'q' | 'Q' | 'r' | 'R' |
  ;;        's' | 'S' | 't' | 'T' | 'u' | 'U' | 'v' | 'V' | 'w' | 'W' | 'x' | 'X' |
  ;;        'y' | 'Y' | 'z' | 'Z' | '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' |
  ;;        '8' | '9' | '}' | ']' | ' ' | '-' ;

  {:type :selector
   :fn (fn [c] (= (first clause) c))})


(defmethod rwalk :DOT
  [_]

  ;; DOT = '.';

  {:type :selector
   :fn (constantly true)})


(defn random-regex
  "Generate a random string given a regex."
  [regex]
  (let [tree (fg/regex->tree regex)]
    (rwalk tree)))
