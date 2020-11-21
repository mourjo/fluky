(ns fluky.core
  (:require [instaparse.core :as insta])
  (:gen-class))


(insta/defparser rgx
  "
REGEX = EXPR*
EXPR = SIMPLE | COMPLEX

CHAR =
\"a\"|\"A\"|
\"b\"|\"B\"|
\"c\"|\"C\"|
\"d\"|\"D\"|
\"e\"|\"E\"|
\"f\"|\"F\"|
\"g\"|\"G\"|
\"h\"|\"H\"|
\"i\"|\"I\"|
\"j\"|\"J\"|
\"k\"|\"K\"|
\"l\"|\"L\"|
\"m\"|\"M\"|
\"n\"|\"N\"|
\"o\"|\"O\"|
\"p\"|\"P\"|
\"q\"|\"Q\"|
\"r\"|\"R\"|
\"s\"|\"S\"|
\"t\"|\"T\"|
\"u\"|\"U\"|
\"v\"|\"V\"|
\"w\"|\"W\"|
\"x\"|\"X\"|
\"y\"|\"Y\"|
\"z\"|\"Z\"|
\"0\"|
\"1\"|
\"2\"|
\"3\"|
\"4\"|
\"5\"|
\"6\"|
\"7\"|
\"8\"|
\"9\"

SIMPLE = CHAR | (\"\\\\\" SPCL)

STAR = \"*\"
PLUS = \"+\"
DOT = \".\"
SPCL = PLUS | STAR

COLLECTION = COLLITEM*

COLLITEM = RANGE | CHAR

RANGE = CHAR \"-\" CHAR
POSSET = \"[\" COLLECTION \"]\"
NEGSET = \"[\" \"^\" COLLECTION \"]\"

STARSELECTOR = COMPLEX STAR
PLUSSELECTOR = COMPLEX PLUS

COMPLEX = DOT | CHAR | POSSET | NEGSET | STARSELECTOR | PLUSSELECTOR
"
  )



(defn parse-regex
  [s]
  (let [res (rgx s)]
    (if (insta/failure? res)
      ::FAILURE
      res)))
