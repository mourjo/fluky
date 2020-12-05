(ns fluky.lexer
  (:import java.util.regex.Pattern))


(def escapeable?
  #{\+ \* \- \\ \? \{ \} \[ \] \( \) \.})


(defn tokenify
  [ch]
  (if (sequential? ch)
    ch
    [:char ch]))


(defn build-tokens
  [{:keys [semi] :as acc}]
  (-> acc
      (update :tokens into (mapv tokenify semi))
      (update :semi empty)))


(defn add-type
  [acc t]
  (if (:ctx acc)
    (assoc-in acc [:ctx :type] t)
    (assoc acc :ctx {:type t})))


(defn tokenize
  [{:keys [semi ctx] :as acc} c]
  (cond

    (:escaped? ctx)
    (if (escapeable? c)
      (-> acc
          (update :semi conj [:escaped c])
          (assoc-in [:ctx :escaped?] false))
      (throw (ex-info "Unsupported escape sequence"
                      {:char c
                       :type :lexer-error})))


    (and (not (:escaped? ctx)) (= c \\))
    (assoc-in acc [:ctx :escaped?] true)


    (and (not (:type ctx)) (= c \?))
    (-> acc
        build-tokens
        (update :tokens conj [:qmark]))


    (and (not (:type ctx)) (= c \*))
    (-> acc
        build-tokens
        (update :tokens conj [:star]))


    (and (not (:type ctx)) (= c \+))
    (-> acc
        build-tokens
        (update :tokens conj [:plus]))


    (and (not (:type ctx)) (= c \.))
    (-> acc
        build-tokens
        (update :tokens conj [:dot]))


    (and (not (:type ctx))
         (= c \{))
    (-> acc
        build-tokens
        (add-type :quantifier))


    (and (= (:type ctx) :quantifier) (= c \}))
    (-> acc
        (update :tokens conj [:quantifier semi])
        (update :semi empty)
        (update :ctx dissoc :type))


    (and (not (:type ctx)) (= c \[))
    (-> acc
        build-tokens
        (add-type :set))


    (and (#{:set :neg-set} (:type ctx)) (= c \]))
    (-> acc
        (update :tokens conj [(:type ctx) semi])
        (update :semi empty)
        (update :ctx dissoc :type))


    (and (= (:type ctx) :set) (= c \^) (empty? semi))
    (add-type acc :neg-set)


    :else
    (update acc :semi conj c)))


(defn lex
  [s]
  (let [result (reduce tokenize
                       (sorted-map :tokens [] :semi [])
                       s)]
    (if (or (:escaped? (:ctx result))
            (:type (:ctx result))
            (seq (remove #{:escaped? :type} (keys (:ctx result)))))
      (throw (ex-info "Invalid regex" {:s s :type :lexer-error}))
      (-> result
          build-tokens
          :tokens))))
