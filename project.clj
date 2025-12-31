(defproject reconcile-skos "0.2.0-SNAPSHOT"
  :description "A SKOS Reconciliation Service for OpenRefine"
  :url "https://github.com/martysteer/reconcile-csv"
  :license {:name "BSD 2-Clause"
            :file "LICENSE"}

  :dependencies [
    ;; Core
    [org.clojure/clojure "1.11.1"]

    ;; Web server
    [ring/ring-core "1.10.0"]
    [ring/ring-jetty-adapter "1.10.0"]
    [ring/ring-json "0.5.1"]
    [ring-cors "0.1.13"]
    [compojure "1.7.0"]

    ;; JSON
    [org.clojure/data.json "2.4.0"]

    ;; RDF/SKOS parsing (Grafter)
    [io.github.swirrl/grafter.io "3.0.0"]

    ;; Fuzzy matching
    [fuzzy-string "0.1.3"]
  ]

  :plugins [[lein-ring "0.12.6"]]
  :main reconcile-skos.core
  :aot [reconcile-skos.core]

  ;; Java 17+ required
  :jvm-opts ["-Xmx2g"])
  
