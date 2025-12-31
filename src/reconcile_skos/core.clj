(ns reconcile-skos.core
  "Main entry point and HTTP routing for SKOS reconciliation service"
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [ring.middleware.params :refer [wrap-params]]
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

;; HTTP Response Helpers

(defn json-response
  "Create a JSON response with optional JSONP callback"
  ([data] (json-response nil data))
  ([callback data]
   {:status 200
    :headers {"Content-Type" (if callback "application/javascript" "application/json")}
    :body (if callback
            (str callback "(" (json/write-str data) ")")
            (json/write-str data))}))

(defn html-response
  "Create an HTML response"
  [body]
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body body})

(defn error-response
  "Create an error response"
  ([status message]
   {:status status
    :headers {"Content-Type" "application/json"}
    :body (json/write-str {:error message})}))

;; Route Handlers

(defn index-page
  "Landing page for the service"
  [request]
  (try
    (html-response (slurp "index.html.tpl"))
    (catch Exception e
      (html-response
        (str "<html><body><h1>SKOS Reconciliation Service</h1>"
             "<p>Service is running. See <a href='/reconcile'>/reconcile</a> for the API manifest.</p>"
             "</body></html>")))))

(defn reconcile-handler
  "Handle reconciliation requests (GET/POST)"
  [request]
  (let [params (:params request)
        callback (:callback params)]
    (try
      (cond
        ;; No query parameters - return service manifest
        (and (nil? (:query params)) (nil? (:queries params)))
        (json-response callback (manifest/service-manifest @config))

        ;; Single query
        (:query params)
        (json-response callback (reconcile/reconcile-query (:query params)))

        ;; Batch queries
        (:queries params)
        (json-response callback (reconcile/reconcile-batch (:queries params)))

        :else
        (error-response 400 "Invalid request: missing query or queries parameter"))
      (catch Exception e
        (do
          (println "Error in reconcile-handler:" (.getMessage e))
          (error-response 500 (str "Internal server error: " (.getMessage e))))))))

(defn suggest-handler
  "Handle entity suggest requests"
  [request]
  (let [params (:params request)
        prefix (:prefix params "")
        callback (:callback params)]
    (try
      (json-response callback (suggest/suggest-entities prefix @skos/concepts))
      (catch Exception e
        (error-response 500 (str "Suggest error: " (.getMessage e)))))))

(defn suggest-type-handler
  "Handle type suggest requests"
  [request]
  (let [params (:params request)
        prefix (:prefix params "")
        callback (:callback params)]
    (try
      (json-response callback (suggest/suggest-types prefix @skos/concept-schemes))
      (catch Exception e
        (error-response 500 (str "Type suggest error: " (.getMessage e)))))))

(defn flyout-handler
  "Handle flyout preview requests"
  [request]
  (let [params (:params request)
        id (:id params)
        callback (:callback params)]
    (try
      (json-response callback (suggest/flyout-html id @skos/concepts))
      (catch Exception e
        (error-response 500 (str "Flyout error: " (.getMessage e)))))))

(defn preview-handler
  "Handle preview requests"
  [request]
  (let [params (:params request)
        id (:id params)]
    (try
      ;; TODO: Implement proper HTML preview
      (html-response
        (str "<div style='padding: 10px; font-family: sans-serif;'>"
             "<h3>Concept Preview</h3>"
             "<p>ID: " id "</p>"
             "<p>Preview functionality coming soon...</p>"
             "</div>"))
      (catch Exception e
        (html-response (str "<div>Error: " (.getMessage e) "</div>"))))))

(defn view-handler
  "Handle view requests"
  [id]
  (try
    ;; TODO: Implement proper concept view page
    (html-response
      (str "<html><head><title>Concept: " id "</title></head>"
           "<body style='font-family: sans-serif; max-width: 800px; margin: 40px auto; padding: 0 20px;'>"
           "<h1>Concept: " id "</h1>"
           "<p>Full concept view coming soon...</p>"
           "<p><a href='/reconcile'>← Back to service</a></p>"
           "</body></html>"))
    (catch Exception e
      (html-response (str "<html><body>Error: " (.getMessage e) "</body></html>")))))

(defn properties-handler
  "Handle property proposal requests"
  [request]
  (let [params (:params request)
        type-id (:type params)
        callback (:callback params)]
    (try
      (json-response callback (extend/propose-properties type-id))
      (catch Exception e
        (error-response 500 (str "Properties error: " (.getMessage e)))))))

(defn extend-handler
  "Handle data extension requests"
  [request]
  (try
    ;; TODO: Parse JSON body properly
    (let [params (:params request)
          callback (:callback params)]
      (json-response callback (extend/extend-data [] [] @skos/concepts)))
    (catch Exception e
      (error-response 500 (str "Extend error: " (.getMessage e))))))

;; Routes

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
  (route/not-found
    (error-response 404 "Not found")))

;; Request Logging Middleware

(defn format-timestamp
  "Format current timestamp for logging"
  []
  (let [now (java.time.LocalDateTime/now)
        formatter (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm:ss")]
    (.format now formatter)))

(defn format-params
  "Format query parameters for logging"
  [params]
  (if (empty? params)
    ""
    (str " " (pr-str params))))

(defn wrap-request-logger
  "Middleware to log incoming requests and responses"
  [handler]
  (fn [request]
    (let [start-time (System/currentTimeMillis)
          method (-> request :request-method name .toUpperCase)
          uri (:uri request)
          params (:params request)
          query-str (or (:query params) (:queries params))

          ;; Log incoming request
          _ (println (str "\n[" (format-timestamp) "] "
                         "→ " method " " uri))

          ;; Log query parameter if it's a reconciliation request
          _ (when query-str
              (println (str "   Query: " (pr-str query-str))))

          ;; Log other interesting params
          _ (when (and (not query-str) (not (empty? params)))
              (println (str "   Params:" (format-params params))))

          ;; Execute the request
          response (handler request)

          ;; Calculate processing time
          end-time (System/currentTimeMillis)
          duration (- end-time start-time)

          ;; Log response
          status (:status response)
          _ (println (str "   ← " status " (" duration "ms)"))]

      response)))

;; Middleware Stack

(def app
  (-> app-routes
      wrap-params
      wrap-request-logger  ; Add request logging
      (wrap-cors :access-control-allow-origin [#".*"]
                 :access-control-allow-methods [:get :post :put :delete :options]
                 :access-control-allow-headers ["Content-Type" "Authorization"])))

;; Main Entry Point

(defn -main
  "Main entry point - load vocabulary and start server"
  [vocab-file & args]
  (try
    (println "\n=== SKOS Reconciliation Service ===")
    (println "Version: 0.2.0")
    (println)

    ;; TODO: Parse CLI arguments (--port, --base-uri, etc.)
    ;; For now, use defaults

    ;; Load SKOS vocabulary
    (println "Loading vocabulary from:" vocab-file)
    (let [stats (skos/load-vocabulary vocab-file)]
      (println)
      (println "Vocabulary loaded successfully:")
      (println "  - Concepts:" (:concepts stats))
      (println "  - Concept Schemes:" (:schemes stats))
      (println "  - Unique Labels:" (:labels stats)))

    (println)
    (println "Starting HTTP server...")
    (println "  - Port:" (:port @config))
    (println "  - Base URL:" (:base-uri @config))
    (println)
    (println "Service endpoints:")
    (println "  - Service manifest: http://localhost:" (:port @config) "/reconcile")
    (println "  - Landing page: http://localhost:" (:port @config) "/")
    (println)
    (println "Add this URL to OpenRefine:")
    (println "  http://localhost:" (:port @config) "/reconcile")
    (println)
    (println "Request logging: ENABLED")
    (println "  (You'll see all HTTP requests and responses below)")
    (println)
    (println "Press Ctrl+C to stop the server")
    (println "========================================")
    (println)

    ;; Start Jetty server
    (run-jetty app {:port (:port @config) :join? true})

    (catch java.io.FileNotFoundException e
      (println "ERROR: Vocabulary file not found:" vocab-file)
      (System/exit 1))
    (catch Exception e
      (println "ERROR:" (.getMessage e))
      (.printStackTrace e)
      (System/exit 1))))
