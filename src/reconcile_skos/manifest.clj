(ns reconcile-skos.manifest
  "W3C Reconciliation API v0.2 service manifest")

(defn service-manifest
  "Generate the service manifest for W3C Reconciliation API v0.2"
  [config]
  {:versions ["0.2"]
   :name (:name config "SKOS Vocabulary Reconciliation Service")
   :identifierSpace (:base-uri config "http://localhost:8000/")
   :schemaSpace "http://www.w3.org/2004/02/skos/core#"
   :defaultTypes [{:id "skos:Concept"
                   :name "SKOS Concept"}]
   :view {:url (str (:base-uri config "http://localhost:8000") "/view/{{id}}")}
   :preview {:url (str (:base-uri config "http://localhost:8000") "/preview?id={{id}}")
             :width 400
             :height 300}
   :suggest {:entity {:service_url (:base-uri config "http://localhost:8000")
                      :service_path "/suggest"
                      :flyout_service_path "/flyout?id=${id}"}
             :type {:service_url (:base-uri config "http://localhost:8000")
                    :service_path "/suggest/type"}}
   :extend {:propose_properties {:service_url (:base-uri config "http://localhost:8000")
                                 :service_path "/properties"}}})
