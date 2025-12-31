(ns reconcile-skos.suggest
  "Suggest service for entity and type auto-completion")

(defn suggest-entities
  "Suggest entities based on prefix search"
  [prefix concepts & {:keys [limit] :or {limit 10}}]
  ;; TODO: Prefix search on prefLabels and altLabels
  {:code "/api/status/ok"
   :status "200 OK"
   :prefix prefix
   :result []})

(defn suggest-types
  "Suggest concept schemes (types)"
  [prefix schemes]
  ;; TODO: Return available ConceptSchemes
  {:code "/api/status/ok"
   :status "200 OK"
   :prefix prefix
   :result []})

(defn flyout-html
  "Generate flyout HTML preview for a concept"
  [concept-id concepts]
  ;; TODO: Create compact HTML preview
  {:id concept-id
   :html "<div>Concept preview</div>"})
