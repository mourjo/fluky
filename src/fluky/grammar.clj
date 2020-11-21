(ns fluky.grammar
  (:require [instaparse.core :as insta]))

(insta/defparser regex-grammar
  "
REGEX = (
           ESCAPED |
           DOT |
           CHAR |
           POS_SET |
           NEG_SET |
           STAR_QUANTIFIER |
           PLUS_QUANTIFIER |
           QMARK_QUANTIFIER |
           MIN_MAX_QUANTIFIER |
           EXACT_QUANTIFIER
        )+ ;

CHAR = 'a' | 'A' | 'b' | 'B' | 'c' | 'C' | 'd' | 'D' | 'e' | 'E' | 'f' | 'F' |
       'g' | 'G' | 'h' | 'H' | 'i' | 'I' | 'j' | 'J' | 'k' | 'K' | 'l' | 'L' |
       'm' | 'M' | 'n' | 'N' | 'o' | 'O' | 'p' | 'P' | 'q' | 'Q' | 'r' | 'R' |
       's' | 'S' | 't' | 'T' | 'u' | 'U' | 'v' | 'V' | 'w' | 'W' | 'x' | 'X' |
       'y' | 'Y' | 'z' | 'Z' | '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' |
       '8' | '9' | '.' | '-' | '}' | ']' | ' ';

META_CHAR =  '*' | '{' | '+';

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

POS_SET = '[' (REGEX | META_CHAR)+ ']' ;
NEG_SET = '[' '^' (REGEX | META_CHAR)+ ']' ;

STAR_QUANTIFIER = (ESCAPED | DOT | CHAR | POS_SET | NEG_SET) '*' ;
PLUS_QUANTIFIER = (ESCAPED | DOT | CHAR | POS_SET | NEG_SET) '+' ;
QMARK_QUANTIFIER = (ESCAPED | DOT | CHAR | POS_SET | NEG_SET) '?' ;

NUMBER = ('1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9' | '0')+ ;

MIN_MAX_QUANTIFIER = (ESCAPED | DOT | CHAR | POS_SET | NEG_SET) '{' NUMBER ',' NUMBER '}' ;
EXACT_QUANTIFIER = (ESCAPED | DOT | CHAR | POS_SET | NEG_SET) '{' NUMBER '}' ;
"
  )



(defn parse-regex
  [s]
  (let [res (regex-grammar s)]
    (if (insta/failure? res)
      ::FAILURE
      res)))