(ns reconcile-skos.extend
  "Data extension service - fetch properties for concepts")

(def skos-properties
  "Available SKOS properties for data extension"
  [{:id "skos:prefLabel" :name "Preferred Label"}
   {:id "skos:altLabel" :name "Alternative Label"}
   {:id "skos:definition" :name "Definition"}
   {:id "skos:scopeNote" :name "Scope Note"}
   {:id "skos:broader" :name "Broader Concept"}
   {:id "skos:narrower" :name "Narrower Concept"}
   {:id "skos:related" :name "Related Concept"}
   {:id "skos:notation" :name "Notation"}
   {:id "skos:inScheme" :name "In Scheme"}])

(defn propose-properties
  "Return available properties for a given type"
  [type-id]
  {:type type-id
   :properties skos-properties})

(defn extend-data
  "Fetch property values for given concept IDs"
  [ids properties concepts]
  ;; TODO: Fetch property values for each concept
  {:meta []
   :rows {}})
