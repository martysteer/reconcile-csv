(ns reconcile-skos.skos
  "SKOS vocabulary parsing and indexing using Grafter"
  (:require [grafter-2.rdf4j.io :as gio]
            [grafter-2.rdf.protocols :as pr]
            [clojure.string :as str]
            [clojure.java.io :as io])
  (:import [java.io File]))

;; SKOS namespace constants
(def skos-ns "http://www.w3.org/2004/02/skos/core#")
(def rdf-ns "http://www.w3.org/1999/02/22-rdf-syntax-ns#")
(def rdfs-ns "http://www.w3.org/2000/01/rdf-schema#")

;; FAST namespace constants (for facet extraction)
(def fast-ns "http://id.worldcat.org/fast/ontology/")

;; SKOS property URIs
(def skos-concept (str skos-ns "Concept"))
(def skos-concept-scheme (str skos-ns "ConceptScheme"))
(def skos-pref-label (str skos-ns "prefLabel"))
(def skos-alt-label (str skos-ns "altLabel"))
(def skos-hidden-label (str skos-ns "hiddenLabel"))
(def skos-definition (str skos-ns "definition"))
(def skos-scope-note (str skos-ns "scopeNote"))
(def skos-notation (str skos-ns "notation"))
(def skos-broader (str skos-ns "broader"))
(def skos-narrower (str skos-ns "narrower"))
(def skos-related (str skos-ns "related"))
(def skos-in-scheme (str skos-ns "inScheme"))
(def rdf-type (str rdf-ns "type"))

;; FAST property URIs
(def fast-facet (str fast-ns "facet"))

;; Application state
(def concepts (atom {}))
(def concept-schemes (atom {}))
(def label-index (atom {}))

;; Streaming threshold (10MB)
(def streaming-threshold-bytes (* 10 1024 1024))

;; Override for streaming mode (can be set via CLI)
(def force-streaming (atom nil))

;; Helper functions

(defn uri-str
  "Convert URI object to string"
  [uri]
  (str uri))

(defn extract-id-from-uri
  "Extract ID from URI (fragment or last path segment)"
  [uri-str]
  (let [uri-string (str uri-str)]
    (or
      ;; Try fragment first (e.g., http://example.org/vocab#concept123 -> concept123)
      (when-let [idx (str/last-index-of uri-string "#")]
        (subs uri-string (inc idx)))
      ;; Otherwise use last path segment (e.g., http://example.org/vocab/concept123 -> concept123)
      (when-let [idx (str/last-index-of uri-string "/")]
        (subs uri-string (inc idx)))
      ;; Fallback to full URI
      uri-string)))

(defn get-literal-value
  "Extract value from a literal, handling LangString"
  [literal]
  (cond
    ;; Handle string literals
    (string? literal)
    {:value literal :lang nil}

    ;; Handle Grafter LangString
    (instance? grafter_2.rdf.protocols.LangString literal)
    {:value (pr/raw-value literal)
     :lang (keyword (pr/lang literal))}

    ;; Handle other literals
    :else
    {:value (pr/raw-value literal) :lang nil}))

(defn normalize-label
  "Normalize a label for searching (lowercase)"
  [label]
  (str/lower-case (str/trim label)))

;; Memory Monitoring

(defn get-memory-usage-mb
  "Get current memory usage in MB"
  []
  (let [runtime (Runtime/getRuntime)
        used-memory (- (.totalMemory runtime) (.freeMemory runtime))]
    (/ used-memory 1024.0 1024.0)))

(defn get-max-memory-mb
  "Get maximum available memory in MB"
  []
  (let [runtime (Runtime/getRuntime)
        max-memory (.maxMemory runtime)]
    (/ max-memory 1024.0 1024.0)))

(defn get-memory-usage-percent
  "Get memory usage as percentage"
  []
  (let [runtime (Runtime/getRuntime)
        max-memory (.maxMemory runtime)
        used-memory (- (.totalMemory runtime) (.freeMemory runtime))]
    (* 100.0 (/ used-memory max-memory))))

(defn warn-if-low-memory
  "Print warning if memory usage is high"
  []
  (let [usage-pct (get-memory-usage-percent)]
    (when (> usage-pct 80.0)
      (println (format "⚠ WARNING: Memory usage high (%.1f%%)" usage-pct))
      (println (format "  Used: %.1fMB / Max: %.1fMB"
                       (get-memory-usage-mb)
                       (get-max-memory-mb))))))

(defn print-memory-stats
  "Print current memory statistics"
  []
  (println (format "  Memory: %.1fMB used / %.1fMB max (%.1f%%)"
                   (get-memory-usage-mb)
                   (get-max-memory-mb)
                   (get-memory-usage-percent))))

;; File Size Detection

(defn get-file-size-bytes
  "Get file size in bytes"
  [file-path]
  (.length (io/file file-path)))

(defn get-file-size-mb
  "Get file size in MB"
  [file-path]
  (/ (get-file-size-bytes file-path) 1024.0 1024.0))

(defn should-use-streaming?
  "Determine if streaming mode should be used based on file size"
  [file-path]
  (if-let [forced @force-streaming]
    forced
    (> (get-file-size-bytes file-path) streaming-threshold-bytes)))

;; RDF Loading

(defn load-rdf
  "Load RDF statements from file (auto-detect format, fallback to RDF/XML)"
  [file-path]
  (println "Loading RDF from:" file-path)
  (try
    (vec (gio/statements file-path))
    (catch clojure.lang.ExceptionInfo e
      ;; If format inference failed, try explicit RDF/XML format
      ;; (common for .skosxml and other non-standard extensions)
      (if (= :could-not-infer-file-format (:error (ex-data e)))
        (do
          (println "Format auto-detection failed, trying RDF/XML format...")
          (try
            (vec (gio/statements file-path :format :rdf))
            (catch Exception e2
              (println "Error loading RDF as RDF/XML:" (.getMessage e2))
              (throw e2))))
        (do
          (println "Error loading RDF:" (.getMessage e))
          (throw e))))
    (catch Exception e
      (println "Error loading RDF:" (.getMessage e))
      (throw e))))

(defn load-rdf-streaming
  "Load RDF statements from file in streaming mode with progress reporting"
  [file-path chunk-size]
  (println "Loading RDF in streaming mode from:" file-path)
  (println (format "  File size: %.1fMB" (get-file-size-mb file-path)))
  (try
    (let [statements-seq (try
                          (gio/statements file-path)
                          (catch clojure.lang.ExceptionInfo e
                            (if (= :could-not-infer-file-format (:error (ex-data e)))
                              (do
                                (println "Format auto-detection failed, trying RDF/XML format...")
                                (gio/statements file-path :format :rdf))
                              (throw e))))
          ;; Process in chunks with progress reporting
          processed (atom 0)
          result (doall
                   (map-indexed
                     (fn [idx stmt]
                       (when (zero? (mod idx chunk-size))
                         (swap! processed + chunk-size)
                         (println (format "    Processed %,d triples..." @processed))
                         (print-memory-stats)
                         (warn-if-low-memory))
                       stmt)
                     statements-seq))]
      (println (format "    Total: %,d triples loaded" (count result)))
      (vec result))
    (catch Exception e
      (println "Error loading RDF in streaming mode:" (.getMessage e))
      (throw e))))

;; Triple Filtering

(defn predicate-is?
  "Check if triple predicate matches URI"
  [uri triple]
  (= (uri-str (:p triple)) uri))

(defn filter-triples-by-predicate
  "Filter triples by predicate URI"
  [triples predicate-uri]
  (filter #(predicate-is? predicate-uri %) triples))

(defn group-triples-by-subject
  "Group triples by subject URI"
  [triples]
  (group-by #(uri-str (:s %)) triples))

;; Concept Extraction

(defn extract-labels
  "Extract labels of a specific type (prefLabel, altLabel, hiddenLabel)"
  [subject-triples predicate-uri]
  (vec
    (map
      (fn [triple]
        (get-literal-value (:o triple)))
      (filter #(predicate-is? predicate-uri %) subject-triples))))

(defn extract-literal
  "Extract single literal value (for definition, scopeNote, etc.)"
  [subject-triples predicate-uri]
  (when-let [triple (first (filter #(predicate-is? predicate-uri %) subject-triples))]
    (let [lit (get-literal-value (:o triple))]
      (:value lit))))

(defn extract-uri-refs
  "Extract URI references (for broader, narrower, related, inScheme)"
  [subject-triples predicate-uri]
  (vec
    (map
      (fn [triple] (uri-str (:o triple)))
      (filter #(predicate-is? predicate-uri %) subject-triples))))

(defn is-concept?
  "Check if subject is a skos:Concept"
  [subject-triples]
  (some
    #(and (predicate-is? rdf-type %)
          (= (uri-str (:o %)) skos-concept))
    subject-triples))

(defn is-concept-scheme?
  "Check if subject is a skos:ConceptScheme"
  [subject-triples]
  (some
    #(and (predicate-is? rdf-type %)
          (= (uri-str (:o %)) skos-concept-scheme))
    subject-triples))

(defn extract-concept
  "Extract a single SKOS concept from subject triples"
  [uri subject-triples]
  (when (is-concept? subject-triples)
    (let [id (extract-id-from-uri uri)
          pref-labels (extract-labels subject-triples skos-pref-label)
          alt-labels (extract-labels subject-triples skos-alt-label)
          hidden-labels (extract-labels subject-triples skos-hidden-label)]
      {:uri uri
       :id id
       :pref-labels pref-labels
       :alt-labels alt-labels
       :hidden-labels hidden-labels
       :definition (extract-literal subject-triples skos-definition)
       :scope-note (extract-literal subject-triples skos-scope-note)
       :notation (extract-literal subject-triples skos-notation)
       :broader (extract-uri-refs subject-triples skos-broader)
       :narrower (extract-uri-refs subject-triples skos-narrower)
       :related (extract-uri-refs subject-triples skos-related)
       :in-scheme (extract-uri-refs subject-triples skos-in-scheme)
       :facet (extract-literal subject-triples fast-facet)})))

(defn extract-concept-scheme
  "Extract a single SKOS ConceptScheme from subject triples"
  [uri subject-triples]
  (when (is-concept-scheme? subject-triples)
    (let [id (extract-id-from-uri uri)
          labels (extract-labels subject-triples skos-pref-label)]
      {:uri uri
       :id id
       :labels labels})))

(defn extract-concepts
  "Extract all SKOS concepts from RDF triples"
  [triples]
  (let [grouped (group-triples-by-subject triples)
        concepts-map (reduce
                       (fn [acc [uri subject-triples]]
                         (if-let [concept (extract-concept uri subject-triples)]
                           (assoc acc uri concept)
                           acc))
                       {}
                       grouped)
        schemes-map (reduce
                      (fn [acc [uri subject-triples]]
                        (if-let [scheme (extract-concept-scheme uri subject-triples)]
                          (assoc acc uri scheme)
                          acc))
                      {}
                      grouped)]
    {:concepts concepts-map
     :schemes schemes-map}))

;; Label Indexing

(defn get-best-label
  "Get the best display label for a concept (prefer prefLabel in any language)"
  [concept]
  (or
    (:value (first (:pref-labels concept)))
    (:value (first (:alt-labels concept)))
    (:id concept)))

(defn index-label
  "Add a label to the search index"
  [index normalized-label concept-uri label-type]
  (let [entry {:uri concept-uri :type label-type}
        existing (get index normalized-label [])]
    (assoc index normalized-label (conj existing entry))))

(defn build-label-index
  "Build searchable index mapping normalized labels to concept URIs"
  [concepts-map]
  (reduce
    (fn [index [uri concept]]
      (let [;; Index all prefLabels
            index (reduce
                    (fn [idx label]
                      (index-label idx (normalize-label (:value label)) uri :pref))
                    index
                    (:pref-labels concept))
            ;; Index all altLabels
            index (reduce
                    (fn [idx label]
                      (index-label idx (normalize-label (:value label)) uri :alt))
                    index
                    (:alt-labels concept))
            ;; Index all hiddenLabels
            index (reduce
                    (fn [idx label]
                      (index-label idx (normalize-label (:value label)) uri :hidden))
                    index
                    (:hidden-labels concept))]
        index))
    {}
    concepts-map))

;; Public API

(defn load-vocabulary-file
  "Load a SKOS vocabulary file and return extracted data (does not modify atoms)"
  [file-path]
  (println "  Loading:" file-path)
  (let [use-streaming (should-use-streaming? file-path)
        _ (when use-streaming
            (println "  Using streaming mode (file > 10MB)"))
        triples (if use-streaming
                  (load-rdf-streaming file-path 50000)  ; Progress every 50k triples
                  (load-rdf file-path))
        _ (println "    Loaded" (count triples) "RDF triples")
        extracted (extract-concepts triples)
        concepts-map (:concepts extracted)
        schemes-map (:schemes extracted)
        _ (println "    Found" (count concepts-map) "SKOS concepts," (count schemes-map) "concept schemes")]
    {:concepts concepts-map
     :schemes schemes-map}))

(defn load-vocabulary
  "Load a single SKOS vocabulary file and build indexes"
  [file-path]
  (println "Loading SKOS vocabulary from:" file-path)
  (let [data (load-vocabulary-file file-path)
        concepts-map (:concepts data)
        schemes-map (:schemes data)
        idx (build-label-index concepts-map)
        _ (println "Built label index with" (count idx) "unique labels")]
    (reset! concepts concepts-map)
    (reset! concept-schemes schemes-map)
    (reset! label-index idx)
    {:concepts (count concepts-map)
     :schemes (count schemes-map)
     :labels (count idx)}))

(defn load-vocabularies
  "Load multiple SKOS vocabulary files and merge them into a single index"
  [file-paths]
  (println "Loading" (count file-paths) "SKOS vocabularies...")
  (let [;; Load all files
        all-data (mapv load-vocabulary-file file-paths)
        ;; Merge all concepts and schemes
        merged-concepts (apply merge (map :concepts all-data))
        merged-schemes (apply merge (map :schemes all-data))
        ;; Build unified index
        _ (println "Building unified label index...")
        idx (build-label-index merged-concepts)
        _ (println "  Total concepts:" (count merged-concepts))
        _ (println "  Total concept schemes:" (count merged-schemes))
        _ (println "  Total unique labels:" (count idx))]
    ;; Update atoms with merged data
    (reset! concepts merged-concepts)
    (reset! concept-schemes merged-schemes)
    (reset! label-index idx)
    {:concepts (count merged-concepts)
     :schemes (count merged-schemes)
     :labels (count idx)}))

(defn get-concept
  "Get a concept by URI"
  [uri]
  (get @concepts uri))

(defn get-concept-by-id
  "Get a concept by ID (searches through all concepts)"
  [id]
  (first
    (filter
      #(= (:id (val %)) id)
      @concepts)))

(defn search-by-label
  "Search for concepts by normalized label (exact match)"
  [label]
  (let [normalized (normalize-label label)
        matches (get @label-index normalized)]
    (map
      (fn [match]
        (assoc (get @concepts (:uri match))
               :match-type (:type match)))
      matches)))

(defn get-all-concept-schemes
  "Get all concept schemes"
  []
  @concept-schemes)

(defn get-concept-count
  "Get total number of loaded concepts"
  []
  (count @concepts))

(defn get-label-count
  "Get total number of indexed labels"
  []
  (count @label-index))
