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

(defn format-concept-scheme-type
  "Format a ConceptScheme as a type object"
  [[uri scheme]]
  {:id (:id scheme)
   :name (or (:value (first (:labels scheme)))
             (:id scheme))})

(defn get-default-types
  "Get default types from loaded ConceptSchemes"
  []
  (let [schemes @skos/concept-schemes]
    (if (empty? schemes)
      ;; No schemes loaded, return generic SKOS Concept type
      [{:id "skos:Concept"
        :name "SKOS Concept"}]
      ;; Include both generic SKOS Concept and specific ConceptSchemes
      (cons
        {:id "skos:Concept"
         :name "SKOS Concept"}
        (map format-concept-scheme-type schemes)))))

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
