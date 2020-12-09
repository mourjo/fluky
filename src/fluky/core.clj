(ns fluky.core
  (:gen-class)
  (:require [fluky.parser :as fp]))

(defn random-regex
  "Generate a random string given a regex."
  [regex]
  (fp/random-from-regex regex))


(defn -main
  "Default entry point from the commandline"
  ([regex]
   (prn (random-regex regex)))
  ([n regex]
   (dotimes [_ (Integer/parseInt n)]
     (-main regex))))
