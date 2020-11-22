(ns fluky.grammar
  (:require [instaparse.core :as insta]))

(insta/defparser regex-grammar
  "
REGEX = REGEX_CLAUSE* ;
REGEX_CLAUSE =
           ESCAPED |
           DOT |
           CHAR |
           POS_SET |
           NEG_SET |
           STAR_QUANTIFIER |
           PLUS_QUANTIFIER |
           QMARK_QUANTIFIER |
           MIN_MAX_QUANTIFIER |
           EXACT_QUANTIFIER ;

CHAR = 'a' | 'A' | 'b' | 'B' | 'c' | 'C' | 'd' | 'D' | 'e' | 'E' | 'f' | 'F' |
       'g' | 'G' | 'h' | 'H' | 'i' | 'I' | 'j' | 'J' | 'k' | 'K' | 'l' | 'L' |
       'm' | 'M' | 'n' | 'N' | 'o' | 'O' | 'p' | 'P' | 'q' | 'Q' | 'r' | 'R' |
       's' | 'S' | 't' | 'T' | 'u' | 'U' | 'v' | 'V' | 'w' | 'W' | 'x' | 'X' |
       'y' | 'Y' | 'z' | 'Z' | '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' |
       '8' | '9' | '}' | ']' | ' ' | '-' ;

META_CHAR =  '*' | '{' | '+' | '.';

BACK_SLASH = '\\\\' ;
ESCAPED = (BACK_SLASH '+')    |
          (BACK_SLASH '*')    |
          (BACK_SLASH '-')    |
          (BACK_SLASH '\\\\') |
          (BACK_SLASH '?')    |
          (BACK_SLASH '{')    |
          (BACK_SLASH '}')    |
          (BACK_SLASH '[')    |
          (BACK_SLASH ']')    |
          (BACK_SLASH '(')    |
          (BACK_SLASH ')')    |
          (BACK_SLASH '.')    ;

DOT = '.';

RANGE = CHAR '-' CHAR ;

POS_SET = '[' (REGEX_CLAUSE | META_CHAR | RANGE)+ ']' ;
NEG_SET = '[' '^' (REGEX_CLAUSE | META_CHAR | RANGE)+ ']' ;

STAR_QUANTIFIER = (ESCAPED | DOT | CHAR | POS_SET | NEG_SET) '*' ;
PLUS_QUANTIFIER = (ESCAPED | DOT | CHAR | POS_SET | NEG_SET) '+' ;
QMARK_QUANTIFIER = (ESCAPED | DOT | CHAR | POS_SET | NEG_SET) '?' ;

NUMBER = ('1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9' | '0')+ ;

MIN_MAX_QUANTIFIER = (ESCAPED | DOT | CHAR | POS_SET | NEG_SET) '{' NUMBER ',' NUMBER '}' ;
EXACT_QUANTIFIER = (ESCAPED | DOT | CHAR | POS_SET | NEG_SET) '{' NUMBER '}' ;
"
  )


(defn ->parse-error
  "Generate a helpful error message if the input was not understood by
  the grammar."
  [result]
  (try
    (let [msg (if (:index result)
                (str "Invalid regex, near " (:index result))
                "Invalid regex")
          reason (map :expecting (:reason result []))]
      (ex-info msg
               {:expecting reason}))
    (catch Exception _
      (ex-info "Invalid regex" result))))


(defn regex->tree
  "Given a regex string, generate a tree based on the grammar above."
  [regex]
  (let [tree (regex-grammar regex)]
    (if (insta/failure? tree)
      (throw (->parse-error tree))
      tree)))
