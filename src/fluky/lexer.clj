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
      (update :tokens into (mapv (fn [ch]
                                   (if (sequential? ch)
                                     ch
                                     [:char ch]))
                                 semi))
      (update :semi empty)))


(defn add-type
  [acc t]
  (if (:ctx acc)
    (assoc-in acc [:ctx :type] t)
    (assoc acc :ctx {:type t})))

(defn tokenize
  [{:keys [tokens semi ctx] :as acc} c]
  (let [result
        (cond
          (:escaped? ctx) (if (escapeable? c)
                            (-> acc
                                (update :semi conj [:escaped c])
                                (assoc-in [:ctx :escaped?] false))
                            (throw (ex-info "Unsupported escape sequence"
                                            {:char c
                                             :type :lexer-error})))
          (and (not (:escaped? ctx))
               (= c \\)) (assoc-in acc [:ctx :escaped?] true)
          (and (not (:type ctx))
               (= c \?)) (-> acc
                             copy-semi-as-tokens
                             (update :tokens conj [:qmark]))
          (and (not (:type ctx)) (= c \*)) (-> acc
                                               copy-semi-as-tokens
                                               (update :tokens conj [:star]))
          (and (not (:type ctx))
               (= c \+)) (-> acc
                             copy-semi-as-tokens
                             (update :tokens conj [:plus]))
          (and (not (:type ctx)) (= c \.)) (-> acc
                                               copy-semi-as-tokens
                                               (update :tokens conj [:dot]))
          (and (not (:type ctx))
               (= c \{)) (-> acc
                             copy-semi-as-tokens
                             (add-type :quantifier))
          (and (= (:type ctx) :quantifier)
               (= c \})) (-> acc
                             (update :tokens conj [:quantifier semi])
                             (update :semi empty)
                             (update :ctx dissoc :type))

          (and (not (:type ctx))
               (= c \[)) (-> acc
                             copy-semi-as-tokens
                             (add-type :set))
          (and (#{:set :neg-set} (:type ctx))
               (= c \])) (-> acc
                             (update :tokens conj [(:type ctx) semi])
                             (update :semi empty)
                             (update :ctx dissoc :type))

          (and (= (:type ctx) :set)
               (= c \^)
               (empty? semi)) (-> acc
                                  (add-type :neg-set))




          :else (-> acc
                    (update :semi conj c)))]
    (println c "   ->   " (dissoc result :java-pattern-valid?))
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
    (prn result )
    (if (or (:escaped? (:ctx result))
            (:type (:ctx result))
            (seq (remove #{:escaped? :type} (keys (:ctx result)))))
      (throw (ex-info "Invalid regex" {:s s :type :lexer-error}))
      (:tokens
       (copy-semi-as-tokens result)))))
