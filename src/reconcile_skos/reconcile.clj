(ns reconcile-skos.reconcile
  "Reconciliation query processing and scoring"
  (:require [fuzzy-string.core :as fuzzy]
            [reconcile-skos.skos :as skos]
            [clojure.data.json :as json]
            [clojure.string :as str]))

;; Scoring weights for different label types
(def label-weights
  {:pref 1.0    ; prefLabel - highest priority
   :alt 0.9     ; altLabel - second priority
   :hidden 0.7  ; hiddenLabel - lowest priority
   })

;; Minimum score threshold for results
(def min-score 0.3)

;; Helper Functions

(defn normalize-query
  "Normalize query string for matching"
  [query]
  (str/lower-case (str/trim query)))

(defn calculate-fuzzy-score
  "Calculate fuzzy match score using Sørensen-Dice coefficient"
  [query-str label-str]
  (let [normalized-query (normalize-query query-str)
        normalized-label (normalize-query label-str)]
    (if (= normalized-query normalized-label)
      1.0  ; Exact match
      (fuzzy/dice normalized-query normalized-label))))

(defn score-label
  "Score a single label against the query"
  [query label label-type]
  (let [fuzzy-score (calculate-fuzzy-score query (:value label))
        weight (get label-weights label-type 0.5)
        final-score (* fuzzy-score weight)]
    {:score final-score
     :label (:value label)
     :lang (:lang label)
     :type label-type}))

(defn score-concept
  "Calculate best matching score for a concept against a query"
  [query concept]
  (let [;; Score all prefLabels
        pref-scores (map #(score-label query % :pref) (:pref-labels concept))
        ;; Score all altLabels
        alt-scores (map #(score-label query % :alt) (:alt-labels concept))
        ;; Score all hiddenLabels
        hidden-scores (map #(score-label query % :hidden) (:hidden-labels concept))
        ;; Combine all scores
        all-scores (concat pref-scores alt-scores hidden-scores)
        ;; Get best score
        best-score (if (empty? all-scores)
                     {:score 0.0 :label "" :lang nil :type :none}
                     (apply max-key :score all-scores))]
    (assoc concept
           :match-score (:score best-score)
           :matched-label (:label best-score)
           :matched-lang (:lang best-score)
           :matched-type (:type best-score))))

;; Query Processing

(defn parse-query
  "Parse a reconciliation query from JSON string or map"
  [query-input]
  (cond
    ;; Already a map
    (map? query-input)
    query-input

    ;; JSON string
    (string? query-input)
    (try
      (json/read-str query-input :key-fn keyword)
      (catch Exception e
        {:query query-input}))

    ;; Fallback
    :else
    {:query (str query-input)}))

(defn extract-query-string
  "Extract the query string from parsed query"
  [parsed-query]
  (or (:query parsed-query) ""))

(defn normalize-type-name
  "Normalize type name for matching (remove 'FAST ' prefix if present)"
  [type-name]
  (if (str/starts-with? type-name "FAST ")
    (subs type-name 5)  ; Remove "FAST " prefix
    type-name))

(defn filter-by-type
  "Filter concepts by FAST facet type if specified in query"
  [concepts query]
  (if-let [type-filter (:type query)]
    (if (= type-filter "skos:Concept")
      ;; Special case: return all concepts
      concepts
      ;; Filter by facet value
      (let [normalized-type (normalize-type-name type-filter)]
        (filter #(= (:facet %) normalized-type) concepts)))
    ;; No type filter specified
    concepts))

(defn filter-by-properties
  "Filter concepts by property constraints if specified"
  [concepts query]
  (if-let [properties (:properties query)]
    ;; TODO: Implement property filtering
    concepts
    concepts))

(defn apply-limit
  "Apply limit to results"
  [results limit]
  (take (or limit 5) results))

(defn format-type-for-concept
  "Format type information for a concept based on its FAST facet"
  [concept]
  (if-let [facet (:facet concept)]
    [{:id (str "FAST " facet)
      :name (str "FAST " facet)}]
    [{:id "skos:Concept"
      :name "SKOS Concept"}]))

(defn format-reconciliation-candidate
  "Format a concept as a reconciliation candidate"
  [concept]
  {:id (:id concept)
   :name (skos/get-best-label concept)
   :score (:match-score concept)
   :match (>= (:match-score concept) 1.0)  ; Exact match only
   :type (format-type-for-concept concept)
   :description (or (:definition concept) (:scope-note concept))})

(defn reconcile-query
  "Process a single reconciliation query"
  [query-input]
  (let [parsed (parse-query query-input)
        query-str (extract-query-string parsed)
        limit (or (:limit parsed) 5)
        ;; Get all concepts
        all-concepts (vals @skos/concepts)
        ;; Apply filters
        filtered (-> all-concepts
                     (filter-by-type parsed)
                     (filter-by-properties parsed))
        ;; Score all concepts
        scored (map #(score-concept query-str %) filtered)
        ;; Filter by minimum score
        above-threshold (filter #(>= (:match-score %) min-score) scored)
        ;; Sort by score descending
        sorted (sort-by :match-score > above-threshold)
        ;; Apply limit
        limited (apply-limit sorted limit)
        ;; Format results
        candidates (map format-reconciliation-candidate limited)]
    {:result (vec candidates)}))

(defn reconcile-batch
  "Process multiple reconciliation queries in parallel"
  [queries-input]
  (let [parsed-queries (cond
                         ;; Already a map
                         (map? queries-input)
                         queries-input

                         ;; JSON string
                         (string? queries-input)
                         (json/read-str queries-input :key-fn keyword)

                         ;; Fallback
                         :else
                         {})
        ;; Process each query in parallel
        results (into {}
                      (pmap
                        (fn [[query-id query]]
                          [query-id (reconcile-query query)])
                        parsed-queries))]
    results))

;; Advanced Scoring (for future enhancement)

(defn boost-by-language
  "Boost score if language matches preferred language"
  [score lang preferred-lang]
  (if (and lang preferred-lang (= lang (keyword preferred-lang)))
    (* score 1.1)  ; 10% boost for language match
    score))

(defn boost-by-scheme
  "Boost score if concept is in specified scheme"
  [score concept scheme-uri]
  (if (some #(= % scheme-uri) (:in-scheme concept))
    (* score 1.05)  ; 5% boost for scheme match
    score))

;; Statistics

(defn get-score-distribution
  "Analyze score distribution for debugging"
  [scored-concepts]
  (let [scores (map :match-score scored-concepts)
        count-total (count scores)
        count-exact (count (filter #(>= % 1.0) scores))
        count-high (count (filter #(and (>= % 0.8) (< % 1.0)) scores))
        count-medium (count (filter #(and (>= % 0.5) (< % 0.8)) scores))
        count-low (count (filter #(and (>= % min-score) (< % 0.5)) scores))]
    {:total count-total
     :exact count-exact
     :high count-high
     :medium count-medium
     :low count-low}))
