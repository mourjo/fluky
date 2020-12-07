(ns fluky.parse-utils
  (:require [clojure.string :as cs]))

(def quantifiable?
  #{:NEG_SET :SET :CHAR :DOT})


(defn read-int
  [char-digits]
  (reduce (fn [acc i] (+ (- (int i) (int \0)) (* 10 acc)))
          0
          char-digits))


(defn str-join
  [xs]
  (apply str (mapv (partial cs/join "") xs)))


(defn read-quantifier-bounds
  [chars]
  (:bounds
   (reduce (fn [{:keys [current bounds] :as acc} ch]
             (cond

               (and (= ch \,) (seq bounds))
               (throw (ex-info "Double commas not allowed in quantifier"
                               {:type :invalid-quantifier
                                :token chars}))

               (and (#{::EOF \,} ch) (empty? current))
               (throw (ex-info "Empty quantifier range"
                               {:type :invalid-quantifier
                                :token chars}))

               (#{::EOF \,} ch)
               (-> acc
                   (assoc :current [])
                   (update :bounds conj (read-int current)))

               (not (<= (int \0) (int ch) (int \9)))
               (throw (ex-info "Invalid range in quantifier"
                               {:type :invalid-quantifier
                                :token chars}))

               :else (update acc :current conj ch)))
           {:bounds [] :current []}
           (conj chars ::EOF))))
