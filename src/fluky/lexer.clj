(ns fluky.lexer
  (:import java.util.regex.Pattern))


;;; ---------------

;;         . Match any character except newline
;;         [ Start character class definition
;;         ] End character class definition
;;         ? 0 or 1 quantifier
;;         * 0 or more quantifiers
;;         + 1 or more quantifier
;;         { Start min/max quantifier
;;         } End min/max quantifier


;; (def sample "[abc]")
;; {:ctx [:pos-set] :args ["a" "b" "c"]}


;; (def sample "[abc]*")
;; {:ctx [:star] :args [{:ctx [:pos-set] :args ["a" "b" "c"]}]}


;; "[-+]?[0-9]{1,16}[.][0-9]{1,6}"




(def escapeable?
  #{\+ \* \- \\ \? \{ \} \[ \] \( \) \.})


(defn copy-semi-as-tokens
  [{:keys [tokens semi ctx] :as acc}]
  (-> acc
      (update :tokens into (mapv (fn [ch] [:char ch]) semi))
      (update :semi empty)))

(defn tokenize
  [{:keys [tokens semi ctx] :as acc} c]
  (let [result
        (cond
          (= c \\) (assoc acc :ctx :escaped)
          (and (not ctx) (= c \?)) (-> acc
                                       copy-semi-as-tokens
                                       (update :tokens conj [:qmark]))
          (and (not ctx) (= c \*)) (-> acc
                                       copy-semi-as-tokens
                                       (update :tokens conj [:star]))
          (and (not ctx) (= c \+)) (-> acc
                                       copy-semi-as-tokens
                                       (update :tokens conj [:plus]))
          (and (not ctx) (= c \.)) (-> acc
                                       copy-semi-as-tokens
                                       (update :tokens conj [:dot]))
          (and (not ctx)
               (= c \{)) (-> acc
               copy-semi-as-tokens
               (assoc :ctx :quantifier))
          (and (= ctx :quantifier)
               (= c \})) (-> acc
               (update :tokens conj [:quantifier semi])
               (update :semi empty)
               (dissoc :ctx))

          (and (not ctx) (= c \[)) (-> acc
                                       copy-semi-as-tokens
                                       (assoc :ctx :set ))
          (and (#{:set :neg-set} ctx)
               (= c \])) (-> acc
               (update :tokens conj [ctx semi])
               (update :semi empty)
               (dissoc :ctx))

          (and (= ctx :set)
               (= c \^)
               (empty? semi)) (-> acc
               (assoc :ctx :neg-set))

          (= :escaped ctx) (if (escapeable? c)
                             (-> acc
                                 (update :semi conj c)
                                 (dissoc :ctx))
                             (throw (ex-info "Unsupported escape sequence"
                                             {:char c})))

          :else (-> acc
                    (update :semi conj c)))]
    ;; (println c "   ->   " (dissoc result :java-pattern-valid?))
    result)
  )


(defn lex
  [s]
  (println "\n")
  (let [result (reduce tokenize
                       (sorted-map :tokens []
                                   :java-pattern-valid? (try (Pattern/compile s) true (catch Exception e false))
                                   :semi [])
                       s)]
    (if (:ctx result)
      (throw (ex-info "Invalid regex" {:s s}))
      (:tokens
       (copy-semi-as-tokens result)))))
