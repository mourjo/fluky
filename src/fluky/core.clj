(ns fluky.core
  (:require [instaparse.core :as insta])
  (:gen-class))


(insta/defparser rgx
  "
REGEX = (SIMPLE | DOT | CHAR | POSSET | NEGSET | STARSELECTOR | PLUSSELECTOR)*

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

SIMPLE = CHAR | (\"\\\\\" \"+\") | (\"\\\\\" \"*\")

DOT = \".\"

RANGE = CHAR \"-\" CHAR
POSSET = \"[\" (RANGE | CHAR)* \"]\"
NEGSET = \"[\" \"^\" (RANGE | CHAR)* \"]\"

STARSELECTOR = (DOT | CHAR | POSSET | NEGSET) \"*\"
PLUSSELECTOR = (DOT | CHAR | POSSET | NEGSET) \"+\"

"
  )



(defn parse-regex
  [s]
  (let [res (rgx s)]
    (if (insta/failure? res)
      ::FAILURE
      res)))
