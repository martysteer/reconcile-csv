(ns reconcile-skos.html
  "HTML preview generation for SKOS concepts"
  (:require [reconcile-skos.skos :as skos]
            [clojure.string :as str]))

;; HTML escaping

(defn escape-html
  "Escape HTML special characters"
  [s]
  (when s
    (-> s
        (str/replace "&" "&amp;")
        (str/replace "<" "&lt;")
        (str/replace ">" "&gt;")
        (str/replace "\"" "&quot;")
        (str/replace "'" "&#39;"))))

;; Helper functions

(defn resolve-concept-label
  "Resolve a concept URI to its label"
  [uri concepts-map]
  (if-let [concept (get concepts-map uri)]
    (skos/get-best-label concept)
    (last (str/split uri #"[/#]"))))  ; Fallback to URI fragment

(defn format-label-list
  "Format a list of labels with language tags"
  [labels]
  (when (seq labels)
    (str/join ", "
              (map (fn [label]
                     (if (:lang label)
                       (str (:value label) " (" (name (:lang label)) ")")
                       (:value label)))
                   labels))))

(defn format-uri-list
  "Format a list of related concept URIs as labels"
  [uris concepts-map max-display]
  (when (seq uris)
    (let [total (count uris)
          displayed (take max-display uris)
          remaining (- total max-display)
          labels (map #(resolve-concept-label % concepts-map) displayed)]
      (if (> remaining 0)
        (str (str/join ", " labels) " <em>... and " remaining " more</em>")
        (str/join ", " labels)))))

;; HTML generation

(defn concept-to-html
  "Generate styled HTML preview for a SKOS concept"
  [concept concepts-map]
  (let [name (escape-html (skos/get-best-label concept))
        uri (escape-html (:uri concept))
        id (escape-html (:id concept))
        notation (escape-html (:notation concept))
        facet (escape-html (:facet concept))
        definition (escape-html (:definition concept))
        scope-note (escape-html (:scope-note concept))
        pref-labels (:pref-labels concept)
        alt-labels (:alt-labels concept)
        broader (:broader concept)
        narrower (:narrower concept)
        related (:related concept)
        in-scheme (:in-scheme concept)]

    (str
      "<div style='font-family: sans-serif; padding: 10px; max-width: 380px;'>"

      ;; Header with name
      "<div style='font-size: 16px; font-weight: bold; color: #1a1a1a; margin-bottom: 8px;'>"
      name
      "</div>"

      ;; Metadata section
      "<div style='font-size: 11px; color: #666; margin-bottom: 10px;'>"
      (when notation
        (str "<div><strong>Notation:</strong> " notation "</div>"))
      (when facet
        (str "<div><strong>FAST Facet:</strong> " facet "</div>"))
      "<div><strong>URI:</strong> <span style='word-break: break-all;'>" uri "</span></div>"
      "</div>"

      ;; Description
      (when (or definition scope-note)
        (str "<div style='font-size: 12px; color: #333; margin-bottom: 10px; line-height: 1.4;'>"
             (or definition scope-note)
             "</div>"))

      ;; Labels section
      (when (or (seq pref-labels) (seq alt-labels))
        (str "<div style='font-size: 11px; margin-bottom: 10px;'>"
             (when (> (count pref-labels) 1)
               (str "<div style='margin-bottom: 4px;'>"
                    "<strong>Preferred Labels:</strong> "
                    (escape-html (format-label-list pref-labels))
                    "</div>"))
             (when (seq alt-labels)
               (str "<div style='margin-bottom: 4px;'>"
                    "<strong>Alternative Labels:</strong> "
                    (escape-html (format-label-list alt-labels))
                    "</div>"))
             "</div>"))

      ;; Relationships section
      (when (or (seq broader) (seq narrower) (seq related))
        (str "<div style='font-size: 11px; border-top: 1px solid #e0e0e0; padding-top: 8px;'>"
             (when (seq broader)
               (str "<div style='margin-bottom: 4px;'>"
                    "<strong>Broader:</strong> "
                    (format-uri-list broader concepts-map 5)
                    "</div>"))
             (when (seq narrower)
               (str "<div style='margin-bottom: 4px;'>"
                    "<strong>Narrower:</strong> "
                    (format-uri-list narrower concepts-map 5)
                    "</div>"))
             (when (seq related)
               (str "<div style='margin-bottom: 4px;'>"
                    "<strong>Related:</strong> "
                    (format-uri-list related concepts-map 3)
                    "</div>"))
             "</div>"))

      "</div>")))

(defn concept-to-full-html
  "Generate full HTML page for a concept"
  [concept concepts-map base-uri]
  (let [name (escape-html (skos/get-best-label concept))
        uri (escape-html (:uri concept))
        id (escape-html (:id concept))
        notation (escape-html (:notation concept))
        facet (escape-html (:facet concept))
        definition (escape-html (:definition concept))
        scope-note (escape-html (:scope-note concept))
        pref-labels (:pref-labels concept)
        alt-labels (:alt-labels concept)
        hidden-labels (:hidden-labels concept)
        broader (:broader concept)
        narrower (:narrower concept)
        related (:related concept)
        in-scheme (:in-scheme concept)]

    (str
      "<!DOCTYPE html>"
      "<html>"
      "<head>"
      "<meta charset='UTF-8'>"
      "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
      "<title>" name " - SKOS Concept</title>"
      "<style>"
      "body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; "
      "       max-width: 900px; margin: 40px auto; padding: 0 20px; line-height: 1.6; color: #333; }"
      "h1 { color: #1a1a1a; border-bottom: 2px solid #007bff; padding-bottom: 10px; }"
      "h2 { color: #444; margin-top: 30px; font-size: 18px; border-bottom: 1px solid #ddd; padding-bottom: 5px; }"
      ".metadata { background: #f5f5f5; padding: 15px; border-radius: 5px; margin: 20px 0; font-size: 14px; }"
      ".metadata dt { font-weight: bold; color: #666; float: left; clear: left; width: 120px; }"
      ".metadata dd { margin-left: 130px; margin-bottom: 10px; word-break: break-word; }"
      ".label-list { margin: 10px 0; }"
      ".label-item { display: inline-block; background: #e3f2fd; padding: 4px 10px; "
      "              margin: 3px; border-radius: 3px; font-size: 14px; }"
      ".lang-tag { color: #666; font-size: 12px; }"
      ".relationship { margin: 15px 0; }"
      ".relationship h3 { font-size: 15px; color: #555; margin-bottom: 8px; }"
      ".concept-link { display: inline-block; background: #fff3cd; padding: 5px 10px; "
      "                margin: 3px; border-radius: 3px; text-decoration: none; color: #856404; }"
      ".concept-link:hover { background: #ffeaa7; }"
      ".back-link { display: inline-block; margin-top: 30px; color: #007bff; text-decoration: none; }"
      ".back-link:hover { text-decoration: underline; }"
      "</style>"
      "</head>"
      "<body>"

      ;; Title
      "<h1>" name "</h1>"

      ;; Metadata
      "<div class='metadata'>"
      "<dl>"
      "<dt>URI:</dt><dd>" uri "</dd>"
      "<dt>ID:</dt><dd>" id "</dd>"
      (when notation
        (str "<dt>Notation:</dt><dd>" notation "</dd>"))
      (when facet
        (str "<dt>FAST Facet:</dt><dd>" facet "</dd>"))
      (when (seq in-scheme)
        (str "<dt>Concept Scheme:</dt><dd>"
             (str/join ", " (map #(or (resolve-concept-label % concepts-map) %) in-scheme))
             "</dd>"))
      "</dl>"
      "</div>"

      ;; Definition
      (when (or definition scope-note)
        (str "<h2>Definition</h2>"
             "<p>" (or definition scope-note) "</p>"))

      ;; Preferred Labels
      (when (seq pref-labels)
        (str "<h2>Preferred Labels</h2>"
             "<div class='label-list'>"
             (str/join ""
                       (map (fn [label]
                              (str "<span class='label-item'>"
                                   (escape-html (:value label))
                                   (when (:lang label)
                                     (str " <span class='lang-tag'>(" (name (:lang label)) ")</span>"))
                                   "</span>"))
                            pref-labels))
             "</div>"))

      ;; Alternative Labels
      (when (seq alt-labels)
        (str "<h2>Alternative Labels</h2>"
             "<div class='label-list'>"
             (str/join ""
                       (map (fn [label]
                              (str "<span class='label-item'>"
                                   (escape-html (:value label))
                                   (when (:lang label)
                                     (str " <span class='lang-tag'>(" (name (:lang label)) ")</span>"))
                                   "</span>"))
                            alt-labels))
             "</div>"))

      ;; Hidden Labels
      (when (seq hidden-labels)
        (str "<h2>Hidden Labels</h2>"
             "<div class='label-list'>"
             (str/join ""
                       (map (fn [label]
                              (str "<span class='label-item'>"
                                   (escape-html (:value label))
                                   (when (:lang label)
                                     (str " <span class='lang-tag'>(" (name (:lang label)) ")</span>"))
                                   "</span>"))
                            hidden-labels))
             "</div>"))

      ;; Broader Concepts
      (when (seq broader)
        (str "<div class='relationship'>"
             "<h3>Broader Concepts</h3>"
             (str/join ""
                       (map (fn [uri]
                              (let [label (resolve-concept-label uri concepts-map)
                                    concept-id (when-let [c (get concepts-map uri)] (:id c))]
                                (if concept-id
                                  (str "<a href='" base-uri "/view/" concept-id "' class='concept-link'>"
                                       (escape-html label) "</a>")
                                  (str "<span class='concept-link'>" (escape-html label) "</span>"))))
                            broader))
             "</div>"))

      ;; Narrower Concepts
      (when (seq narrower)
        (str "<div class='relationship'>"
             "<h3>Narrower Concepts</h3>"
             (str/join ""
                       (map (fn [uri]
                              (let [label (resolve-concept-label uri concepts-map)
                                    concept-id (when-let [c (get concepts-map uri)] (:id c))]
                                (if concept-id
                                  (str "<a href='" base-uri "/view/" concept-id "' class='concept-link'>"
                                       (escape-html label) "</a>")
                                  (str "<span class='concept-link'>" (escape-html label) "</span>"))))
                            narrower))
             "</div>"))

      ;; Related Concepts
      (when (seq related)
        (str "<div class='relationship'>"
             "<h3>Related Concepts</h3>"
             (str/join ""
                       (map (fn [uri]
                              (let [label (resolve-concept-label uri concepts-map)
                                    concept-id (when-let [c (get concepts-map uri)] (:id c))]
                                (if concept-id
                                  (str "<a href='" base-uri "/view/" concept-id "' class='concept-link'>"
                                       (escape-html label) "</a>")
                                  (str "<span class='concept-link'>" (escape-html label) "</span>"))))
                            related))
             "</div>"))

      ;; Back link
      "<a href='" base-uri "/reconcile' class='back-link'>← Back to service</a>"

      "</body>"
      "</html>")))

(defn not-found-html
  "Generate HTML for concept not found"
  [concept-id]
  (str
    "<!DOCTYPE html>"
    "<html>"
    "<head>"
    "<meta charset='UTF-8'>"
    "<title>Concept Not Found</title>"
    "<style>"
    "body { font-family: sans-serif; max-width: 600px; margin: 100px auto; "
    "       padding: 20px; text-align: center; }"
    "h1 { color: #d32f2f; }"
    ".back-link { display: inline-block; margin-top: 20px; color: #007bff; text-decoration: none; }"
    ".back-link:hover { text-decoration: underline; }"
    "</style>"
    "</head>"
    "<body>"
    "<h1>Concept Not Found</h1>"
    "<p>The concept with ID <code>" (escape-html concept-id) "</code> could not be found.</p>"
    "<a href='/reconcile' class='back-link'>← Back to service</a>"
    "</body>"
    "</html>"))
