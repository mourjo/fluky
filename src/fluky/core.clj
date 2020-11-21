(ns fluky.core
  (:require [instaparse.core :as insta])
  (:gen-class))


(insta/defparser rgx
  "
REGEX = (ESCAPED | DOT | CHAR | POS_SET | NEG_SET | STAR_QUANTIFIER | PLUS_QUANTIFIER | QMARK_QUANTIFIER | MIN_MAX_QUANTIFIER)* ;

CHAR = 'a' | 'A'| 'b' | 'B' | 'c' | 'C' | 'd' | 'D' | 'e' | 'E' | 'f' | 'F' |
       'g' | 'G'| 'h' | 'H' | 'i' | 'I' | 'j' | 'J' | 'k' | 'K' | 'l' | 'L' |
       'm' | 'M'| 'n' | 'N' | 'o' | 'O' | 'p' | 'P' | 'q' | 'Q' | 'r' | 'R' |
       's' | 'S'| 't' | 'T' | 'u' | 'U' | 'v' | 'V' | 'w' | 'W' | 'x' | 'X' |
       'y' | 'Y'| 'z' | 'Z' | '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' |
       '8' | '9' ;

META_CHAR = '+' | '-' | '*' | '{' | '}' | '.';

BACK_SLASH = '\\\\' ;
ESCAPED = (BACK_SLASH '+') | (BACK_SLASH '-') | (BACK_SLASH '*') | (BACK_SLASH '?') | (BACK_SLASH '.') ;

DOT = '.';

RANGE = CHAR '-' CHAR ;
POS_SET = '[' (RANGE | ESCAPED | CHAR | META_CHAR)* ']' ;
NEG_SET = '[' '^' (RANGE | ESCAPED | CHAR | META_CHAR)* ']' ;

STAR_QUANTIFIER = (DOT | CHAR | POS_SET | NEG_SET) '*' ;
PLUS_QUANTIFIER = (DOT | CHAR | POS_SET | NEG_SET) '+' ;
QMARK_QUANTIFIER = (DOT | CHAR | POS_SET | NEG_SET) '?' ;

NUMBER = ('1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9')
         ('1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9' | '0')* ;

MIN_MAX_QUANTIFIER = (DOT | CHAR | POS_SET | NEG_SET) '{' NUMBER ',' NUMBER '}' ;
"
  )



(defn parse-regex
  [s]
  (let [res (rgx s)]
    (if (insta/failure? res)
      ::FAILURE
      res)))
