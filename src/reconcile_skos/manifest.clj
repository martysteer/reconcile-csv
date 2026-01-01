(ns reconcile-skos.manifest
  "W3C Reconciliation API v0.2 service manifest"
  (:require [reconcile-skos.skos :as skos]))

(defn get-base-url
  "Get base URL from config, ensuring it ends without trailing slash"
  [config]
  (let [base (or (:base-uri config) "http://localhost:8000")]
    (if (.endsWith base "/")
      (subs base 0 (dec (count base)))
      base)))

(defn get-unique-facets
  "Get unique FAST facet values from loaded concepts"
  []
  (let [all-concepts (vals @skos/concepts)
        facets (set (keep :facet all-concepts))]  ; keep removes nil values
    (sort facets)))

(defn format-facet-as-type
  "Format a FAST facet as a type object with 'FAST ' prefix"
  [facet]
  {:id (str "FAST " facet)
   :name (str "FAST " facet)})

(defn get-default-types
  "Get default types from loaded FAST facets"
  []
  (let [facets (get-unique-facets)]
    (if (empty? facets)
      ;; No facets found, return generic SKOS Concept type
      [{:id "skos:Concept"
        :name "SKOS Concept"}]
      ;; Include both generic SKOS Concept and specific FAST facets
      (cons
        {:id "skos:Concept"
         :name "SKOS Concept"}
        (map format-facet-as-type facets)))))

(defn service-manifest
  "Generate the service manifest for W3C Reconciliation API v0.2"
  [config]
  (let [base-url (get-base-url config)
        service-name (or (:name config)
                         "SKOS Vocabulary Reconciliation Service")
        identifier-space (or (:identifier-space config)
                             (str base-url "/"))]
    {:versions ["0.2"]
     :name service-name
     :identifierSpace identifier-space
     :schemaSpace "http://www.w3.org/2004/02/skos/core#"
     :defaultTypes (get-default-types)
     :view {:url (str base-url "/view/{{id}}")}
     :preview {:url (str base-url "/preview?id={{id}}")
               :width 400
               :height 300}
     :suggest {:entity {:service_url base-url
                        :service_path "/suggest"
                        :flyout_service_path "/flyout?id=${id}"}
               :type {:service_url base-url
                      :service_path "/suggest/type"}}
     :extend {:propose_properties {:service_url base-url
                                   :service_path "/properties"}
              :property_settings []}}))

(defn get-service-metadata
  "Get service metadata including statistics"
  [config]
  (merge
    (service-manifest config)
    {:_metadata {:concepts (skos/get-concept-count)
                 :schemes (count @skos/concept-schemes)
                 :labels (skos/get-label-count)
                 :version "0.2.0"}}))
