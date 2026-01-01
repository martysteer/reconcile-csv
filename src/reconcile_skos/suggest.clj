(ns reconcile-skos.suggest
  "Suggest service for entity and type auto-completion"
  (:require [reconcile-skos.skos :as skos]
            [clojure.string :as str]))

;; Scoring weights for suggest results
(def suggest-weights
  {:pref-prefix 1.0    ; Prefix match on prefLabel
   :alt-prefix 0.9     ; Prefix match on altLabel
   :pref-contains 0.5  ; Contains match on prefLabel
   :alt-contains 0.4}) ; Contains match on altLabel

;; Helper functions

(defn normalize-for-suggest
  "Normalize string for suggest matching"
  [s]
  (str/lower-case (str/trim s)))

(defn prefix-match?
  "Check if label starts with prefix"
  [prefix label]
  (let [norm-prefix (normalize-for-suggest prefix)
        norm-label (normalize-for-suggest label)]
    (str/starts-with? norm-label norm-prefix)))

(defn contains-match?
  "Check if label contains prefix (not at start)"
  [prefix label]
  (let [norm-prefix (normalize-for-suggest prefix)
        norm-label (normalize-for-suggest label)]
    (and (str/includes? norm-label norm-prefix)
         (not (str/starts-with? norm-label norm-prefix)))))

(defn score-label-match
  "Score a label match for suggest"
  [prefix label label-type match-type]
  (let [weight-key (keyword (str (name label-type) "-" (name match-type)))
        weight (get suggest-weights weight-key 0.3)]
    {:score weight
     :label label
     :type label-type
     :match-type match-type}))

(defn find-matching-labels
  "Find all matching labels in a concept"
  [prefix concept]
  (let [pref-labels (map :value (:pref-labels concept))
        alt-labels (map :value (:alt-labels concept))

        ;; Prefix matches
        pref-prefix (filter #(prefix-match? prefix %) pref-labels)
        alt-prefix (filter #(prefix-match? prefix %) alt-labels)

        ;; Contains matches (fallback)
        pref-contains (filter #(contains-match? prefix %) pref-labels)
        alt-contains (filter #(contains-match? prefix %) alt-labels)

        ;; Score all matches
        scored-matches (concat
                         (map #(score-label-match prefix % :pref :prefix) pref-prefix)
                         (map #(score-label-match prefix % :alt :prefix) alt-prefix)
                         (map #(score-label-match prefix % :pref :contains) pref-contains)
                         (map #(score-label-match prefix % :alt :contains) alt-contains))]

    (if (empty? scored-matches)
      nil
      ;; Return best match for this concept
      (apply max-key :score scored-matches))))

(defn find-matches
  "Find all concepts matching the prefix"
  [prefix concepts-map]
  (let [concept-matches (keep
                          (fn [[uri concept]]
                            (when-let [match (find-matching-labels prefix concept)]
                              (assoc concept
                                     :match-score (:score match)
                                     :matched-label (:label match)
                                     :match-type (:type match))))
                          concepts-map)]
    concept-matches))

(defn rank-matches
  "Rank matches by score and alphabetically"
  [matches]
  (sort-by
    (juxt
      (comp - :match-score)  ; Score descending
      :matched-label)        ; Then alphabetically
    matches))

(defn format-suggest-result
  "Format a concept for suggest response"
  [concept]
  {:id (:id concept)
   :name (:matched-label concept)
   :n:type {:id "skos:Concept" :name "SKOS Concept"}
   :description (or (:definition concept) (:scope-note concept))})

(defn suggest-entities
  "Suggest entities based on prefix search"
  [prefix concepts & {:keys [limit] :or {limit 10}}]
  (if (str/blank? prefix)
    ;; Empty prefix - return top N concepts alphabetically
    (let [all-concepts (vals concepts)
          sorted (sort-by skos/get-best-label all-concepts)
          limited (take limit sorted)
          results (map (fn [c] {:id (:id c)
                                 :name (skos/get-best-label c)
                                 :n:type {:id "skos:Concept" :name "SKOS Concept"}
                                 :description (or (:definition c) (:scope-note c))})
                       limited)]
      {:code "/api/status/ok"
       :status "200 OK"
       :prefix prefix
       :result (vec results)})
    ;; Non-empty prefix - search and rank
    (let [matches (find-matches prefix concepts)
          ranked (rank-matches matches)
          limited (take limit ranked)
          results (map format-suggest-result limited)]
      {:code "/api/status/ok"
       :status "200 OK"
       :prefix prefix
       :result (vec results)})))

(defn suggest-types
  "Suggest concept schemes (types)"
  [prefix schemes]
  (let [all-types (concat
                    ;; Generic SKOS Concept type
                    [{:id "skos:Concept"
                      :name "SKOS Concept"}]
                    ;; All loaded concept schemes
                    (map (fn [[uri scheme]]
                           {:id (:id scheme)
                            :name (or (:value (first (:labels scheme)))
                                     (:id scheme))})
                         schemes))
        ;; Filter by prefix if provided
        filtered (if (str/blank? prefix)
                   all-types
                   (filter #(prefix-match? prefix (:name %)) all-types))]
    {:code "/api/status/ok"
     :status "200 OK"
     :prefix prefix
     :result (vec filtered)}))

(defn flyout-html
  "Generate flyout HTML preview for a concept"
  [concept-id concepts]
  (if-let [concept-entry (skos/get-concept-by-id concept-id)]
    (let [concept (val concept-entry)
          name (skos/get-best-label concept)
          definition (:definition concept)
          scope-note (:scope-note concept)
          notation (:notation concept)
          description (or definition scope-note "No description available")]
      {:id concept-id
       :html (str "<div style='font-family: sans-serif; padding: 8px;'>"
                  "<div style='font-weight: bold; font-size: 14px; margin-bottom: 4px;'>" name "</div>"
                  (when notation
                    (str "<div style='font-size: 11px; color: #666; margin-bottom: 4px;'>Notation: " notation "</div>"))
                  "<div style='font-size: 12px; color: #333;'>" description "</div>"
                  "</div>")})
    ;; Concept not found
    {:id concept-id
     :html "<div style='padding: 8px;'>Concept not found</div>"}))
