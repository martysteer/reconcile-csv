(ns reconcile-skos.core
  "Main entry point and HTTP routing for SKOS reconciliation service"
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [ring.middleware.cors :refer [wrap-cors]]
            [clojure.data.json :as json]
            [reconcile-skos.skos :as skos]
            [reconcile-skos.manifest :as manifest]
            [reconcile-skos.reconcile :as reconcile]
            [reconcile-skos.suggest :as suggest]
            [reconcile-skos.extend :as extend])
  (:gen-class))

;; Application state
(def config (atom {:base-uri "http://localhost:8000"
                   :port 8000
                   :name "SKOS Reconciliation Service"}))

(defn json-response
  "Create a JSON response with optional JSONP callback"
  ([data] (json-response nil data))
  ([callback data]
   {:status 200
    :headers {"Content-Type" (if callback "application/javascript" "application/json")}
    :body (if callback
            (str callback "(" (json/write-str data) ")")
            (json/write-str data))}))

(defn index-page
  "Landing page for the service"
  [request]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (slurp "index.html.tpl")})

(defn reconcile-handler
  "Handle reconciliation requests"
  [request]
  (let [params (:params request)
        callback (:callback params)]
    (cond
      ;; No parameters - return service manifest
      (and (nil? (:query params)) (nil? (:queries params)))
      (json-response callback (manifest/service-manifest @config))

      ;; Single query
      (:query params)
      (json-response callback (reconcile/reconcile-query (:query params) @skos/concepts))

      ;; Batch queries
      (:queries params)
      (json-response callback (reconcile/reconcile-batch (:queries params) @skos/concepts))

      :else
      {:status 400 :body "Invalid request"})))

(defn suggest-handler
  "Handle entity suggest requests"
  [request]
  (let [params (:params request)
        prefix (:prefix params)
        callback (:callback params)]
    (json-response callback (suggest/suggest-entities prefix @skos/concepts))))

(defn suggest-type-handler
  "Handle type suggest requests"
  [request]
  (let [params (:params request)
        prefix (:prefix params)
        callback (:callback params)]
    (json-response callback (suggest/suggest-types prefix @skos/concepts))))

(defn flyout-handler
  "Handle flyout preview requests"
  [request]
  (let [params (:params request)
        id (:id params)
        callback (:callback params)]
    (json-response callback (suggest/flyout-html id @skos/concepts))))

(defn preview-handler
  "Handle preview requests"
  [request]
  ;; TODO: Return embeddable HTML preview
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body "<div>Preview placeholder</div>"})

(defn view-handler
  "Handle view requests"
  [id]
  ;; TODO: Return full HTML page for concept
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (str "<html><body><h1>Concept: " id "</h1></body></html>")})

(defn properties-handler
  "Handle property proposal requests"
  [request]
  (let [params (:params request)
        type-id (:type params)
        callback (:callback params)]
    (json-response callback (extend/propose-properties type-id))))

(defn extend-handler
  "Handle data extension requests"
  [request]
  (let [body (:body request)
        ids (:ids body)
        properties (:properties body)
        callback (get-in request [:params :callback])]
    (json-response callback (extend/extend-data ids properties @skos/concepts))))

(defroutes app-routes
  (GET "/" [] index-page)
  (GET "/reconcile" [] reconcile-handler)
  (POST "/reconcile" [] reconcile-handler)
  (GET "/suggest" [] suggest-handler)
  (GET "/suggest/type" [] suggest-type-handler)
  (GET "/flyout" [] flyout-handler)
  (GET "/preview" [] preview-handler)
  (GET "/view/:id" [id] (view-handler id))
  (GET "/properties" [] properties-handler)
  (POST "/extend" [] extend-handler)
  (route/not-found {:status 404 :body "Not found"}))

(def app
  (-> app-routes
      wrap-params
      (wrap-cors :access-control-allow-origin [#".*"]
                 :access-control-allow-methods [:get :post :put :delete]
                 :access-control-allow-headers ["Content-Type"])
      wrap-json-body
      wrap-json-response))

(defn -main
  "Main entry point - load vocabulary and start server"
  [vocab-file & args]
  ;; TODO: Parse CLI arguments (--port, --base-uri, etc.)
  ;; TODO: Load SKOS vocabulary with Grafter
  ;; TODO: Build concept index
  (println "Starting SKOS Reconciliation Service")
  (println "Loading vocabulary from:" vocab-file)
  (println "Service URL:" (str "http://localhost:" (:port @config)))
  (run-jetty app {:port (:port @config) :join? false}))
