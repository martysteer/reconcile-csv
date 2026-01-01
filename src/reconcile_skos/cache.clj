(ns reconcile-skos.cache
  "Query result caching for reconciliation requests"
  (:require [clojure.core.cache :as cache]))

;; Cache configuration
(def cache-size 1000)  ; Maximum number of cached queries

;; Cache state
(def query-cache (atom (cache/lru-cache-factory {} :threshold cache-size)))

;; Cache statistics
(def cache-stats (atom {:hits 0 :misses 0}))

;; Cache key generation

(defn cache-key
  "Generate a cache key from query parameters"
  [query-str type-filter limit]
  (str query-str "|" (or type-filter "") "|" limit))

;; Cache operations

(defn get-cached
  "Get a cached result if available"
  [query-str type-filter limit]
  (let [key (cache-key query-str type-filter limit)
        cached (cache/lookup @query-cache key)]
    (when cached
      (swap! cache-stats update :hits inc))
    cached))

(defn put-cached!
  "Cache a query result"
  [query-str type-filter limit result]
  (let [key (cache-key query-str type-filter limit)]
    (swap! query-cache cache/miss key result)))

(defn record-miss!
  "Record a cache miss"
  []
  (swap! cache-stats update :misses inc))

(defn clear-cache!
  "Clear all cached results"
  []
  (reset! query-cache (cache/lru-cache-factory {} :threshold cache-size))
  (reset! cache-stats {:hits 0 :misses 0}))

;; Statistics

(defn get-cache-stats
  "Get cache statistics"
  []
  (let [stats @cache-stats
        total (+ (:hits stats) (:misses stats))
        hit-rate (if (zero? total)
                   0.0
                   (/ (double (:hits stats)) total))]
    (assoc stats
           :total total
           :hit-rate hit-rate
           :size (count @query-cache))))

(defn get-hit-rate
  "Get cache hit rate as percentage"
  []
  (let [stats (get-cache-stats)]
    (* 100.0 (:hit-rate stats))))

(defn print-cache-stats
  "Print cache statistics"
  []
  (let [stats (get-cache-stats)]
    (println (format "Cache stats: %d hits, %d misses, %.1f%% hit rate, %d entries"
                     (:hits stats)
                     (:misses stats)
                     (* 100.0 (:hit-rate stats))
                     (:size stats)))))

;; Cache warming (optional)

(defn warm-cache
  "Pre-populate cache with common queries"
  [common-queries reconcile-fn]
  (doseq [query common-queries]
    (let [result (reconcile-fn query)]
      (put-cached! (:query query)
                   (:type query)
                   (or (:limit query) 5)
                   result))))
