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
        callback (get params "callback")]
    (try
      (cond
        ;; No query parameters - return service manifest
        (and (nil? (get params "query")) (nil? (get params "queries")))
        (json-response callback (manifest/service-manifest @config))

        ;; Single query
        (get params "query")
        (json-response callback (reconcile/reconcile-query (get params "query")))

        ;; Batch queries
        (get params "queries")
        (json-response callback (reconcile/reconcile-batch (get params "queries")))

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
        prefix (get params "prefix" "")
        callback (get params "callback")]
    (try
      (json-response callback (suggest/suggest-entities prefix @skos/concepts))
      (catch Exception e
        (error-response 500 (str "Suggest error: " (.getMessage e)))))))

(defn suggest-type-handler
  "Handle type suggest requests"
  [request]
  (let [params (:params request)
        prefix (get params "prefix" "")
        callback (get params "callback")]
    (try
      (json-response callback (suggest/suggest-types prefix @skos/concept-schemes))
      (catch Exception e
        (error-response 500 (str "Type suggest error: " (.getMessage e)))))))

(defn flyout-handler
  "Handle flyout preview requests"
  [request]
  (let [params (:params request)
        id (get params "id")
        callback (get params "callback")]
    (try
      (json-response callback (suggest/flyout-html id @skos/concepts))
      (catch Exception e
        (error-response 500 (str "Flyout error: " (.getMessage e)))))))

(defn preview-handler
  "Handle preview requests"
  [request]
  (let [params (:params request)
        id (get params "id")]
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
        type-id (get params "type")
        callback (get params "callback")]
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
          callback (get params "callback")]
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

(defn summarize-request
  "Generate a summary of the request for logging"
  [uri params]
  (let [query (get params "query")
        queries (get params "queries")
        prefix (get params "prefix")
        id (get params "id")
        callback (get params "callback")]
    (cond
      ;; Reconciliation queries
      queries
      (try
        (str "Batch reconciliation: " (count (json/read-str queries)) " queries")
        (catch Exception e
          "Batch reconciliation request"))

      query
      (let [q-str (if (string? query) query (str query))
            truncated (if (> (count q-str) 50)
                        (str (subs q-str 0 47) "...")
                        q-str)]
        (str "Reconcile query: \"" truncated "\""))

      ;; Suggest requests
      prefix
      (str "Suggest: prefix=\"" prefix "\"")

      ;; View/preview/flyout
      id
      (str "ID lookup: " id)

      ;; No params - probably manifest
      (and (= uri "/reconcile") (empty? params))
      "Service manifest request"

      ;; Other params
      (not (empty? params))
      (str "Params: " (pr-str params))

      ;; Default
      :else
      nil)))

(defn extract-result-count
  "Extract result count from response body if it's JSON"
  [response]
  (try
    (when (and (= 200 (:status response))
               (string? (:body response)))
      (let [body (:body response)]
        (cond
          ;; Single query result
          (and (.contains body "\"result\":[")
               (not (.contains body "\"q0\"")))
          (let [parsed (json/read-str body :key-fn keyword)]
            (count (:result parsed)))

          ;; Batch query results
          (.contains body "\"q0\"")
          (let [parsed (json/read-str body :key-fn keyword)
                total (reduce + (map #(count (:result (val %))) parsed))]
            (str total " total results across " (count parsed) " queries"))

          :else
          nil)))
    (catch Exception e nil)))

(defn wrap-request-logger
  "Middleware to log incoming requests and responses"
  [handler]
  (fn [request]
    (let [start-time (System/currentTimeMillis)
          method (-> request :request-method name .toUpperCase)
          uri (:uri request)
          params (:params request)
          summary (summarize-request uri params)

          ;; Log incoming request
          _ (println (str "\n[" (format-timestamp) "] "
                         "→ " method " " uri))

          ;; Log request summary
          _ (when summary
              (println (str "   " summary)))

          ;; Execute the request
          response (handler request)

          ;; Calculate processing time
          end-time (System/currentTimeMillis)
          duration (- end-time start-time)

          ;; Extract result info
          result-info (extract-result-count response)

          ;; Log response
          status (:status response)
          _ (println (str "   ← " status " (" duration "ms)"
                         (when result-info (str " - " result-info " results"))))]

      response)))

;; Middleware Stack
;; Order matters! Middleware wraps from bottom to top, so:
;; 1. Request enters wrap-cors (outermost)
;; 2. Request goes to wrap-params (parses query parameters)
;; 3. Request goes to wrap-request-logger (logs with parsed params)
;; 4. Request reaches app-routes (innermost)

(def app
  (-> app-routes
      wrap-request-logger  ; Logs requests (needs parsed params)
      wrap-params          ; Parses query parameters
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
