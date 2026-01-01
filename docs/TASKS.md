# reconcile-skos: Task List

## Project Transformation: CSV → SKOS Reconciliation Service

**Branch:** `skos`  
**Start Date:** 2024  
**Target:** W3C Reconciliation API v0.2 compliant SKOS service  
**RDF Library:** Grafter (io.github.swirrl/grafter.io v3.0.0)

---

## Phase 0: Environment Setup & Prerequisites

### 0.2 Leiningen Update
- [ ] **0.2.1** Check Leiningen version (`lein --version`)
- [ ] **0.2.2** Update Leiningen to 2.9+ if needed (`lein upgrade`)
- [ ] **0.2.3** Clear local Maven cache if upgrading major versions: `rm -rf ~/.m2/repository/org/clojure`

### 0.3 Project Clojure Version Upgrade
- [ ] **0.3.1** Update `project.clj`: Change Clojure from `"1.5.1"` to `"1.11.1"`
- [ ] **0.3.2** Run `lein deps` to download new Clojure version
- [ ] **0.3.3** Run `lein repl` to verify Clojure 1.11.1 loads correctly
- [ ] **0.3.4** Test existing code still compiles: `lein compile`

### 0.4 Verify Grafter Compatibility
- [ ] **0.4.1** Add Grafter dependency temporarily: `[io.github.swirrl/grafter.io "3.0.0"]`
- [ ] **0.4.2** Run `lein deps` to download Grafter and RDF4J dependencies
- [ ] **0.4.3** Start REPL and test Grafter loads:
  ```clojure
  (require '[grafter-2.rdf4j.io :as gio])
  (println "Grafter loaded successfully!")
  ```
- [ ] **0.4.4** Test parsing a sample Turtle file (create test file if needed)

---

## Phase 1: Project Setup & Dependencies

### 1.1 Update Project Configuration
- [ ] **1.1.1** Rename project in `project.clj` from `reconcile-csv` to `reconcile-skos`
- [ ] **1.1.2** Update version to `0.2.0-SNAPSHOT`
- [ ] **1.1.3** Update project description to mention SKOS
- [ ] **1.1.4** Update project URL
- [ ] **1.1.5** Remove `csv-map` dependency
- [ ] **1.1.6** Remove `org.clojure/tools.nrepl` (or move to dev profile)
- [ ] **1.1.7** Add Grafter dependency:
  ```clojure
  [io.github.swirrl/grafter.io "3.0.0"]
  ```
- [ ] **1.1.8** Add `ring-cors "0.1.13"` for CORS support
- [ ] **1.1.9** Update Ring dependencies to `1.10.0`
- [ ] **1.1.10** Update Compojure to `1.7.0`
- [ ] **1.1.11** Add `ring/ring-json "0.5.1"` for JSON middleware
- [ ] **1.1.12** Update `org.clojure/data.json` to `"2.4.0"`
- [ ] **1.1.13** Keep `fuzzy-string "0.1.3"` (still works)
- [ ] **1.1.14** Run `lein deps :tree` to verify no conflicts
- [ ] **1.1.15** Run `lein compile` to verify everything builds

### 1.2 Namespace Restructure
- [ ] **1.2.1** Create new directory: `src/reconcile_skos/`
- [ ] **1.2.2** Create new source files:
  - `src/reconcile_skos/core.clj` - Main entry point & routes
  - `src/reconcile_skos/skos.clj` - SKOS parsing with Grafter
  - `src/reconcile_skos/reconcile.clj` - Reconciliation logic
  - `src/reconcile_skos/manifest.clj` - Service manifest
  - `src/reconcile_skos/suggest.clj` - Suggest services
  - `src/reconcile_skos/extend.clj` - Data extension service
- [ ] **1.2.3** Update namespace declarations to `reconcile-skos.*`
- [ ] **1.2.4** Update `:main` in project.clj to `reconcile-skos.core`
- [ ] **1.2.5** Update `:aot` in project.clj

### 1.3 Documentation Updates
- [ ] **1.3.1** Update `README.md` with new project name and usage
- [ ] **1.3.2** Update `index.html.tpl` landing page for SKOS
- [ ] **1.3.3** Create `CHANGELOG.md` documenting the transformation
- [ ] **1.3.4** Ensure LICENSE file is correct

---

## Phase 2: SKOS Data Model & Parsing with Grafter

### 2.1 SKOS Data Structures
- [ ] **2.1.1** Define Clojure record or map for SKOS concepts:
  ```clojure
  {:uri "http://example.org/concept/123"
   :id "123"
   :pref-labels [{:value "Architecture" :lang "en"}
                 {:value "Architektur" :lang "de"}]
   :alt-labels [{:value "Building design" :lang "en"}]
   :hidden-labels []
   :definition "The art and science of designing buildings"
   :scope-note nil
   :notation "720"
   :broader ["http://example.org/concept/100"]
   :narrower ["http://example.org/concept/124" "http://example.org/concept/125"]
   :related []
   :in-scheme "http://example.org/scheme"}
  ```
- [ ] **2.1.2** Define ConceptScheme structure
- [ ] **2.1.3** Create helper functions for label access by language
- [ ] **2.1.4** Create helper to get "best" label for display

### 2.2 Grafter RDF Parser Implementation
- [ ] **2.2.1** Create `skos.clj` with Grafter imports:
  ```clojure
  (ns reconcile-skos.skos
    (:require [grafter-2.rdf4j.io :as gio]
              [grafter-2.rdf.protocols :as pr]))
  ```
- [ ] **2.2.2** Define SKOS namespace constants:
  ```clojure
  (def skos-ns "http://www.w3.org/2004/02/skos/core#")
  (def rdf-ns "http://www.w3.org/1999/02/22-rdf-syntax-ns#")
  ```
- [ ] **2.2.3** Implement file loading function using Grafter:
  ```clojure
  (defn load-rdf [file-path]
    (gio/statements file-path))
  ```
- [ ] **2.2.4** Implement format-explicit loading:
  ```clojure
  (defn load-rdf-format [file-path format]
    (gio/statements file-path :format format))
  ```
- [ ] **2.2.5** Create predicate functions for SKOS properties:
  ```clojure
  (defn pref-label? [triple] ...)
  (defn alt-label? [triple] ...)
  (defn broader? [triple] ...)
  ```
- [ ] **2.2.6** Implement concept extraction from triples
- [ ] **2.2.7** Handle Grafter's LangString for language tags
- [ ] **2.2.8** Build in-memory concept index (map of URI → concept)
- [ ] **2.2.9** Implement ID extraction from URIs (fragment or last path segment)
- [ ] **2.2.10** Extract ConceptScheme information
- [ ] **2.2.11** Handle multiple ConceptSchemes in one file

### 2.3 Label Normalization & Indexing
- [ ] **2.3.1** Port lowercase normalization from CSV version
- [ ] **2.3.2** Preserve original labels as metadata
- [ ] **2.3.3** Create searchable label index (normalized label → concept URIs)
- [ ] **2.3.4** Index all label types (pref, alt, hidden)
- [ ] **2.3.5** Handle multi-valued labels correctly

---

## Phase 3: Reconciliation Engine

### 3.1 Query Processing
- [ ] **3.1.1** Parse reconciliation query JSON structure
- [ ] **3.1.2** Extract query string
- [ ] **3.1.3** Handle type filtering (by ConceptScheme URI)
- [ ] **3.1.4** Handle property constraints
- [ ] **3.1.5** Implement query limit handling (default: 5)
- [ ] **3.1.6** Support batch queries (`queries` parameter)

### 3.2 Matching & Scoring
- [ ] **3.2.1** Implement label matching priority:
  1. Exact prefLabel match → 1.0
  2. Fuzzy prefLabel match → 0.7-0.99
  3. Exact altLabel match → 0.9
  4. Fuzzy altLabel match → 0.6-0.89
  5. hiddenLabel match → 0.5-0.8
- [ ] **3.2.2** Port dice coefficient fuzzy matching from existing code
- [ ] **3.2.3** Implement language preference boost
- [ ] **3.2.4** Implement property value filtering
- [ ] **3.2.5** Calculate composite scores
- [ ] **3.2.6** Sort results by score descending

### 3.3 Result Formatting
- [ ] **3.3.1** Build reconciliation candidate objects
- [ ] **3.3.2** Include type information (skos:Concept + scheme)
- [ ] **3.3.3** Include description from definition/scopeNote
- [ ] **3.3.4** Set `match: true` only for exact matches (score = 1.0)
- [ ] **3.3.5** Apply result limit
- [ ] **3.3.6** Format response JSON

---

## Phase 4: W3C API v0.2 Service Manifest

### 4.1 Manifest Structure
- [ ] **4.1.1** Create `manifest.clj` namespace
- [ ] **4.1.2** Create manifest generation function
- [ ] **4.1.3** Include `versions: ["0.2"]`
- [ ] **4.1.4** Set configurable `name`
- [ ] **4.1.5** Generate `identifierSpace` from vocabulary base URI
- [ ] **4.1.6** Set `schemaSpace` to SKOS namespace
- [ ] **4.1.7** Build `defaultTypes` from detected ConceptSchemes
- [ ] **4.1.8** Configure `view` template URL
- [ ] **4.1.9** Configure `preview` metadata (width: 400, height: 300)
- [ ] **4.1.10** Configure `suggest` metadata for entities and types
- [ ] **4.1.11** Configure `extend` metadata for data extension

### 4.2 Dynamic Configuration
- [ ] **4.2.1** Allow base URI configuration via CLI
- [ ] **4.2.2** Auto-detect ConceptScheme(s) for defaultTypes
- [ ] **4.2.3** Support custom service name via CLI

---

## Phase 5: Suggest Services

### 5.1 Entity Suggest
- [ ] **5.1.1** Create `suggest.clj` namespace
- [ ] **5.1.2** Implement `/suggest` endpoint handler
- [ ] **5.1.3** Prefix search on prefLabels
- [ ] **5.1.4** Include altLabels in search
- [ ] **5.1.5** Return id, name, description for each result
- [ ] **5.1.6** Support cursor/pagination parameter
- [ ] **5.1.7** Limit results (default: 10)

### 5.2 Type Suggest
- [ ] **5.2.1** Implement `/suggest/type` endpoint
- [ ] **5.2.2** Return ConceptSchemes as types
- [ ] **5.2.3** Include skos:Concept as default type

### 5.3 Flyout Service
- [ ] **5.3.1** Implement `/flyout` endpoint
- [ ] **5.3.2** Return compact HTML preview in JSON
- [ ] **5.3.3** Include prefLabel, definition, notation

---

## Phase 6: Preview & View Services

### 6.1 Preview Service
- [ ] **6.1.1** Implement `/preview` endpoint
- [ ] **6.1.2** Return embeddable HTML (iframe-compatible)
- [ ] **6.1.3** Display:
  - Preferred label (all languages)
  - Alternative labels
  - Definition/scope note
  - Notation
  - Broader/narrower concepts (as text, not links)
- [ ] **6.1.4** Style for 400x300 px display
- [ ] **6.1.5** Include basic CSS inline

### 6.2 View Service
- [ ] **6.2.1** Implement `/view/:id` endpoint
- [ ] **6.2.2** Return full HTML page for concept
- [ ] **6.2.3** Display all concept properties
- [ ] **6.2.4** Include navigation links to related concepts
- [ ] **6.2.5** Link to broader/narrower/related concepts

---

## Phase 7: Data Extension Service

### 7.1 Property Proposals
- [ ] **7.1.1** Create `extend.clj` namespace
- [ ] **7.1.2** Implement `/properties` GET endpoint
- [ ] **7.1.3** Accept `type` query parameter
- [ ] **7.1.4** Return available SKOS properties:
  - skos:prefLabel
  - skos:altLabel
  - skos:definition
  - skos:scopeNote
  - skos:broader
  - skos:narrower
  - skos:related
  - skos:notation
  - skos:inScheme

### 7.2 Data Extension
- [ ] **7.2.1** Implement `/extend` POST endpoint
- [ ] **7.2.2** Parse extension query JSON (ids, properties)
- [ ] **7.2.3** Fetch property values for each concept ID
- [ ] **7.2.4** Handle entity references (broader/narrower/related)
- [ ] **7.2.5** Return properly formatted `meta` and `rows` response

---

## Phase 8: HTTP Layer & Middleware

### 8.1 CORS Support (Required for v0.2)
- [ ] **8.1.1** Add ring-cors middleware
- [ ] **8.1.2** Configure allowed origins (permissive for local use)
- [ ] **8.1.3** Handle preflight OPTIONS requests
- [ ] **8.1.4** Set appropriate CORS headers on all responses

### 8.2 Content Negotiation
- [ ] **8.2.1** Return JSON with proper `Content-Type: application/json`
- [ ] **8.2.2** Support JSONP via `callback` parameter
- [ ] **8.2.3** Add ring-json middleware for automatic JSON encoding

### 8.3 Error Handling
- [ ] **8.3.1** Return appropriate HTTP status codes (200, 400, 404, 500)
- [ ] **8.3.2** Handle malformed JSON gracefully with 400
- [ ] **8.3.3** Handle missing concepts with 404
- [ ] **8.3.4** Add request validation
- [ ] **8.3.5** Log errors appropriately

### 8.4 Route Updates
- [ ] **8.4.1** Update route definitions in `core.clj`
- [ ] **8.4.2** Add `/suggest/type` route
- [ ] **8.4.3** Add `/preview` route
- [ ] **8.4.4** Add `/properties` route
- [ ] **8.4.5** Add `/extend` route (POST)
- [ ] **8.4.6** Ensure all routes have CORS

---

## Phase 9: CLI & Configuration

### 9.1 Command Line Interface
- [ ] **9.1.1** Update `-main` function signature
- [ ] **9.1.2** Accept vocabulary file path as first argument
- [ ] **9.1.3** Parse optional arguments:
  - `--port PORT` (default: 8000)
  - `--base-uri URI`
  - `--lang LANG`
  - `--scheme-uri URI`
- [ ] **9.1.4** Use `clojure.tools.cli` or simple arg parsing
- [ ] **9.1.5** Print startup information:
  - Vocabulary file loaded
  - Number of concepts
  - Number of ConceptSchemes
  - Service URL
- [ ] **9.1.6** Print helpful usage on error

### 9.2 Validation
- [ ] **9.2.1** Validate vocabulary file exists
- [ ] **9.2.2** Validate file extension is supported RDF format
- [ ] **9.2.3** Catch and report Grafter parsing errors clearly
- [ ] **9.2.4** Warn if no SKOS concepts found
- [ ] **9.2.5** Warn if no ConceptScheme found

---

## Phase 10: Testing & Quality

### 10.1 Test Data
- [ ] **10.1.1** Create `test/resources/` directory
- [ ] **10.1.2** Create sample SKOS vocabulary in Turtle format
- [ ] **10.1.3** Create same vocabulary in RDF/XML format
- [ ] **10.1.4** Include multi-language labels (en, de, fr)
- [ ] **10.1.5** Include hierarchical relationships
- [ ] **10.1.6** Include related concepts
- [ ] **10.1.7** Include multiple ConceptSchemes

### 10.2 Unit Tests
- [ ] **10.2.1** Create `test/reconcile_skos/` directory
- [ ] **10.2.2** Test SKOS parsing with Grafter
- [ ] **10.2.3** Test concept extraction
- [ ] **10.2.4** Test label normalization
- [ ] **10.2.5** Test fuzzy matching
- [ ] **10.2.6** Test scoring algorithm
- [ ] **10.2.7** Test query parsing
- [ ] **10.2.8** Test manifest generation

### 10.3 Integration Tests
- [ ] **10.3.1** Test full reconciliation flow
- [ ] **10.3.2** Test suggest services
- [ ] **10.3.3** Test preview/view services
- [ ] **10.3.4** Test data extension
- [ ] **10.3.5** Test CORS headers
- [ ] **10.3.6** Test with OpenRefine (manual)

### 10.4 Build & Package
- [ ] **10.4.1** Run `lein test` - all tests pass
- [ ] **10.4.2** Run `lein uberjar` - creates standalone JAR
- [ ] **10.4.3** Test JAR execution with sample vocabulary
- [ ] **10.4.4** Verify JAR size is reasonable (~10-12MB)
- [ ] **10.4.5** Update `dist/` with new JAR

---

## Phase 11: Cleanup & Polish

### 11.1 Remove CSV Code
- [ ] **11.1.1** Delete `src/reconcile_csv/` directory (after code migrated)
- [ ] **11.1.2** Remove all CSV-related functions
- [ ] **11.1.3** Remove csv-map imports
- [ ] **11.1.4** Remove old JAR files from `dist/`
- [ ] **11.1.5** Clean `target/` directory
- [ ] **11.1.6** Remove old standalone JAR from root

### 11.2 Documentation
- [ ] **11.2.1** Complete README with:
  - Project description
  - Installation instructions
  - Usage examples (all formats)
  - CLI options
  - OpenRefine setup instructions
- [ ] **11.2.2** Document SKOS requirements
- [ ] **11.2.3** Add troubleshooting section
- [ ] **11.2.4** Document supported RDF formats
- [ ] **11.2.5** Add example SKOS vocabulary

### 11.3 Final Review
- [ ] **11.3.1** Code review for consistency
- [ ] **11.3.2** Check all TODO comments resolved
- [ ] **11.3.3** Verify W3C API v0.2 compliance
- [ ] **11.3.4** Performance test with large vocabulary (1000+ concepts)
- [ ] **11.3.5** Test with real-world SKOS files (e.g., UNESCO, LCSH sample)
- [ ] **11.3.6** Update version to `0.2.0` (remove SNAPSHOT)
- [ ] **11.3.7** Tag release in git

---

## Priority Order

**Phase 0 (Prerequisites) - Do First:**
- Environment setup (Java 17, Leiningen, Clojure upgrade)

**Critical Path (MVP):**
1. Phase 1 (Project Setup) 
2. Phase 2 (SKOS Parsing with Grafter)
3. Phase 3 (Reconciliation Engine)
4. Phase 4 (Manifest)
5. Phase 8 (HTTP Layer)
6. Phase 9 (CLI)

**Secondary (Full Feature):**
7. Phase 5 (Suggest)
8. Phase 6 (Preview/View)
9. Phase 7 (Data Extension)

**Final:**
10. Phase 10 (Testing)
11. Phase 11 (Cleanup)

---

## Estimated Effort

| Phase | Description | Effort |
|-------|-------------|--------|
| 0 | Environment Setup | 1-2 hours |
| 1 | Project Setup | 2-3 hours |
| 2 | SKOS Parsing (Grafter) | 4-6 hours |
| 3 | Reconciliation Engine | 4-6 hours |
| 4 | Service Manifest | 1-2 hours |
| 5 | Suggest Services | 2-3 hours |
| 6 | Preview/View | 2-3 hours |
| 7 | Data Extension | 3-4 hours |
| 8 | HTTP Layer | 2-3 hours |
| 9 | CLI | 1-2 hours |
| 10 | Testing | 3-4 hours |
| 11 | Cleanup | 2-3 hours |
| **Total** | | **27-41 hours** |

---

## Quick Reference: Key Grafter Functions

```clojure
;; Load RDF from file (auto-detect format)
(gio/statements "vocabulary.ttl")

;; Load with explicit format
(gio/statements "vocab.xml" :format :rdf)

;; Supported format keywords
:ttl :rdf :xml :nt :nq :trig :json :n3

;; Access triple components
(:s triple)  ; subject (URI)
(:p triple)  ; predicate (URI)
(:o triple)  ; object (URI, literal, or LangString)

;; Check for LangString
(instance? grafter.vocabularies.core.LangString (:o triple))

;; Get language from LangString
(pr/lang (:o triple))  ; returns :en, :de, etc.

;; Get raw value from literal
(pr/raw-value (:o triple))
```

---

## Phase 11: Priority 1 - Core Functionality Enhancements

### 11.1 Type Filtering by ConceptScheme

**Status:** Planned
**Location:** `src/reconcile_skos/reconcile.clj:92-98`
**Specification:** See SPECIFICATION.md §1.1

- [ ] **11.1.1** Read ConceptScheme details from manifest
  ```clojure
  (defn get-scheme-by-id [scheme-id]
    ;; Lookup ConceptScheme by ID or URI
    ...)
  ```
- [ ] **11.1.2** Implement ConceptScheme matching logic
  - Match by full URI
  - Match by ConceptScheme ID from manifest
  - Handle "skos:Concept" as special case (no filtering)
- [ ] **11.1.3** Implement type filtering in `filter-by-type`
  ```clojure
  (defn filter-by-type [concepts query]
    (if-let [type-filter (:type query)]
      (if (= type-filter "skos:Concept")
        concepts  ; No filtering
        (filter #(concept-in-scheme? % type-filter) concepts))
      concepts))
  ```
- [ ] **11.1.4** Implement `concept-in-scheme?` helper
  ```clojure
  (defn concept-in-scheme? [concept scheme-identifier]
    ;; Check if concept's :in-scheme includes the scheme
    ...)
  ```
- [ ] **11.1.5** Handle edge cases
  - Concepts without `skos:inScheme` property
  - Unknown type filter (log warning, return empty results)
  - Multiple schemes per concept
- [ ] **11.1.6** Test with multiple vocabularies
  - Load FASTGeographic + FASTTopical
  - Query with type="Geographic" → verify only geographic results
  - Query with type="skos:Concept" → verify all results
- [ ] **11.1.7** Update manifest to correctly populate `defaultTypes`
  - Ensure all ConceptSchemes appear in types list
  - Include both ID and name for each scheme

### 11.2 Entity Suggest (Auto-complete)

**Status:** Stubbed
**Location:** `src/reconcile_skos/suggest.clj:6-12`
**Specification:** See SPECIFICATION.md §1.2

- [ ] **11.2.1** Implement prefix matching on labels
  ```clojure
  (defn find-prefix-matches [prefix label-index]
    ;; Find all labels starting with prefix (case-insensitive)
    ...)
  ```
- [ ] **11.2.2** Implement contains matching (fallback)
  ```clojure
  (defn find-contains-matches [prefix label-index]
    ;; Find labels containing prefix (not at start)
    ...)
  ```
- [ ] **11.2.3** Implement ranking algorithm
  - Exact prefix match on prefLabel: score 1.0
  - Exact prefix match on altLabel: score 0.9
  - Contains match in prefLabel: score 0.5
  - Contains match in altLabel: score 0.4
  - Sort by score descending, then alphabetically
- [ ] **11.2.4** Implement suggest-entities function
  ```clojure
  (defn suggest-entities [prefix concepts & {:keys [limit] :or {limit 10}}]
    (let [normalized-prefix (normalize-label prefix)
          prefix-matches (find-prefix-matches normalized-prefix @label-index)
          contains-matches (if (< (count prefix-matches) limit)
                            (find-contains-matches normalized-prefix @label-index)
                            [])
          all-matches (concat prefix-matches contains-matches)
          ranked (rank-matches all-matches)
          limited (take limit ranked)]
      {:code "/api/status/ok"
       :status "200 OK"
       :prefix prefix
       :result (map format-suggest-result limited)}))
  ```
- [ ] **11.2.5** Implement suggest result formatter
  ```clojure
  (defn format-suggest-result [concept]
    {:id (:id concept)
     :name (get-best-label concept)
     :n:type {:id "skos:Concept" :name "SKOS Concept"}
     :description (or (:definition concept) (:scope-note concept))})
  ```
- [ ] **11.2.6** Update suggest-handler in core.clj
  - Extract prefix parameter
  - Extract limit parameter (default 10)
  - Call suggest-entities function
  - Return JSONP-wrapped response
- [ ] **11.2.7** Test suggest endpoint
  - Test prefix "lond" returns London-related concepts
  - Test empty prefix returns results
  - Test limit parameter works
  - Test special characters in prefix
- [ ] **11.2.8** Performance test
  - Verify response time < 100ms for 10k+ concept vocabulary
  - Profile bottlenecks if slow

### 11.3 HTML Preview and Flyout

**Status:** Placeholder
**Location:** `src/reconcile_skos/core.clj:125`, `src/reconcile_skos/suggest.clj:25`
**Specification:** See SPECIFICATION.md §1.3

- [ ] **11.3.1** Create HTML generation helper namespace
  ```clojure
  (ns reconcile-skos.html
    "HTML preview generation for SKOS concepts")
  ```
- [ ] **11.3.2** Implement concept-to-html function
  ```clojure
  (defn concept-to-html [concept concepts]
    ;; Generate styled HTML for concept
    ;; Include: prefLabel, altLabels, definition, broader/narrower
    ...)
  ```
- [ ] **11.3.3** Implement HTML sections
  - Header section with prefLabel
  - Metadata section (type, notation, URI)
  - Description section (definition, scope note)
  - Labels section (altLabels, hiddenLabels)
  - Relationships section (broader, narrower, related)
- [ ] **11.3.4** Implement relationship label resolution
  ```clojure
  (defn resolve-related-concept [uri concepts]
    ;; Get concept by URI and return its label
    ;; Fallback to URI if not found
    ...)
  ```
- [ ] **11.3.5** Handle truncation for large lists
  - Show max 5 broader/narrower concepts
  - Show max 3 related concepts
  - Display "... and N more" if truncated
- [ ] **11.3.6** Update preview-handler in core.clj
  ```clojure
  (defn preview-handler [request]
    (let [params (:params request)
          id (get params "id")
          concept (get-concept-by-id id)]
      (if concept
        (html-response (concept-to-html concept @concepts))
        (html-response "<div>Concept not found</div>"))))
  ```
- [ ] **11.3.7** Update flyout-html in suggest.clj
  ```clojure
  (defn flyout-html [concept-id concepts]
    (let [concept (get-concept-by-id concept-id concepts)]
      (if concept
        {:id concept-id
         :html (concept-to-html concept concepts)}
        {:id concept-id
         :html "<div>Concept not found</div>"})))
  ```
- [ ] **11.3.8** Test HTML generation
  - Test with fully populated concept (all fields)
  - Test with minimal concept (only prefLabel)
  - Test with missing broader/narrower
  - Test truncation for long lists
- [ ] **11.3.9** Visual testing in OpenRefine
  - Verify HTML renders in preview panel
  - Verify width fits (max 380px)
  - Verify height reasonable (max 300px)
  - Verify relationships are clickable (future enhancement)

---

## Phase 12: Priority 2 - Performance & Scalability

### 12.1 Streaming/Chunked Loading for Large Files

**Status:** Planned
**Location:** `src/reconcile_skos/skos.clj`
**Specification:** See SPECIFICATION.md §2.1

- [ ] **12.1.1** Research Grafter streaming capabilities
  - Check if `gio/statements` supports lazy evaluation
  - Test with large file to understand memory behavior
  - Review Grafter documentation for streaming patterns
- [ ] **12.1.2** Implement file size detection
  ```clojure
  (defn get-file-size [file-path]
    (.length (io/file file-path)))

  (defn should-use-streaming? [file-path]
    (> (get-file-size file-path) (* 10 1024 1024))) ; > 10MB
  ```
- [ ] **12.1.3** Implement streaming RDF loader
  ```clojure
  (defn load-rdf-streaming [file-path chunk-size]
    ;; Load RDF in chunks to avoid memory overflow
    ...)
  ```
- [ ] **12.1.4** Implement chunked concept extraction
  ```clojure
  (defn load-vocabulary-streaming [file-path]
    (let [chunk-size 10000
          concepts-acc (atom {})
          schemes-acc (atom {})]
      ;; Process chunks incrementally
      ...))
  ```
- [ ] **12.1.5** Add progress reporting
  - Print progress every 50k triples
  - Display current memory usage (if possible)
  - Show percentage complete (if file size known)
- [ ] **12.1.6** Implement memory monitoring
  ```clojure
  (defn get-memory-usage []
    (let [runtime (Runtime/getRuntime)
          used-memory (- (.totalMemory runtime) (.freeMemory runtime))]
      (/ used-memory 1024 1024))) ; Return MB

  (defn warn-if-low-memory []
    (let [runtime (Runtime/getRuntime)
          max-memory (.maxMemory runtime)
          used-memory (- (.totalMemory runtime) (.freeMemory runtime))
          usage-pct (/ used-memory max-memory)]
      (when (> usage-pct 0.8)
        (println "WARNING: Memory usage high (" (int (* usage-pct 100)) "%)"))))
  ```
- [ ] **12.1.7** Update load-vocabulary-file to use streaming
  ```clojure
  (defn load-vocabulary-file [file-path]
    (if (should-use-streaming? file-path)
      (load-vocabulary-streaming file-path)
      (load-vocabulary-fast file-path))) ; Existing implementation
  ```
- [ ] **12.1.8** Add CLI flag for streaming override
  - `--streaming true/false` to force behavior
  - Update parse-cli-args in core.clj
  - Pass through to load functions
- [ ] **12.1.9** Update project.clj JVM opts
  - Increase from `-Xmx2g` to `-Xmx4g`
  - Add documentation for large vocabulary loading
- [ ] **12.1.10** Test with large files
  - Test FASTGeographic.skosxml (220MB)
  - Test FASTTopical.skosxml (471MB)
  - Test FASTPersonal.skosxml (849MB)
  - Verify no OutOfMemoryError
  - Compare concept counts: streaming vs. fast (should match)
- [ ] **12.1.11** Benchmark performance
  - Measure load time for each large file
  - Monitor peak memory usage
  - Document recommended JVM heap sizes

### 12.2 Prefix Trie Index for Fast Suggest

**Status:** Planned
**Prerequisite:** Phase 11.2 (Entity Suggest) must be complete
**Location:** New file `src/reconcile_skos/trie.clj`
**Specification:** See SPECIFICATION.md §2.2

- [ ] **12.2.1** Create trie namespace
  ```clojure
  (ns reconcile-skos.trie
    "Prefix trie data structure for fast prefix matching")
  ```
- [ ] **12.2.2** Define trie data structure
  ```clojure
  ;; Node: {:char \a :concepts #{} :children {}}
  ;; Root: {:children {\a node, \b node, ...}}
  ```
- [ ] **12.2.3** Implement trie insertion
  ```clojure
  (defn insert-into-trie! [trie label concept-uris]
    ;; Insert label into trie, associating concept URIs
    ...)
  ```
- [ ] **12.2.4** Implement trie construction from label index
  ```clojure
  (defn build-prefix-trie [label-index]
    (let [trie (atom {:children {}})]
      (doseq [[label uris] label-index]
        (insert-into-trie! trie label uris))
      @trie))
  ```
- [ ] **12.2.5** Implement prefix search
  ```clojure
  (defn prefix-search-trie [trie prefix]
    ;; Find all concept URIs with labels starting with prefix
    ;; Return set of URIs
    ...)
  ```
- [ ] **12.2.6** Implement subtree collection
  ```clojure
  (defn collect-all-concepts [node]
    ;; Recursively collect all concept URIs from node and descendants
    ...)
  ```
- [ ] **12.2.7** Add trie to application state
  ```clojure
  ;; In skos.clj
  (def prefix-trie (atom nil))
  ```
- [ ] **12.2.8** Update load-vocabularies to build trie
  ```clojure
  (defn load-vocabularies [file-paths]
    ;; ... existing logic ...
    (let [idx (build-label-index merged-concepts)
          trie (build-prefix-trie idx)]  ; NEW
      (reset! label-index idx)
      (reset! prefix-trie trie)  ; NEW
      ...))
  ```
- [ ] **12.2.9** Update suggest-entities to use trie
  ```clojure
  (defn suggest-entities [prefix concepts & opts]
    (let [matching-uris (prefix-search-trie @prefix-trie prefix)
          matching-concepts (map #(get @concepts %) matching-uris)
          ...]
      ...))
  ```
- [ ] **12.2.10** Test trie functionality
  - Build trie for test vocabulary
  - Test prefix "lon" finds all london*/long* concepts
  - Test empty prefix behavior
  - Test non-existent prefix returns empty set
  - Compare trie results vs. linear scan (should match)
- [ ] **12.2.11** Benchmark trie performance
  - Measure trie construction time
  - Measure prefix search time vs. linear scan
  - Verify 10-100x speedup for large vocabularies
  - Document memory overhead

### 12.3 Query Result Caching

**Status:** Planned
**Location:** New file `src/reconcile_skos/cache.clj`
**Specification:** See SPECIFICATION.md §2.3

- [ ] **12.3.1** Add cache dependency to project.clj
  ```clojure
  [org.clojure/core.cache "1.0.225"]
  ```
- [ ] **12.3.2** Create cache namespace
  ```clojure
  (ns reconcile-skos.cache
    (:require [clojure.core.cache :as cache]))
  ```
- [ ] **12.3.3** Implement cache atom and helpers
  ```clojure
  (def query-cache (atom (cache/lru-cache-factory {} :threshold 1000)))

  (defn cache-key [query-str type-filter limit] ...)
  (defn get-cached [query-str type-filter limit] ...)
  (defn put-cached! [query-str type-filter limit result] ...)
  (defn clear-cache! [] ...)
  ```
- [ ] **12.3.4** Add cache statistics tracking (optional)
  ```clojure
  (def cache-stats (atom {:hits 0 :misses 0}))

  (defn record-hit! [] (swap! cache-stats update :hits inc))
  (defn record-miss! [] (swap! cache-stats update :misses inc))
  (defn get-hit-rate [] ...)
  ```
- [ ] **12.3.5** Update reconcile-query to use cache
  ```clojure
  (defn reconcile-query [query-input]
    (let [parsed (parse-query query-input)
          query-str (extract-query-string parsed)
          type-filter (:type parsed)
          limit (:limit parsed)]
      (if-let [cached (get-cached query-str type-filter limit)]
        (do (record-hit!) cached)
        (do (record-miss!)
            (let [result (compute-result parsed)]
              (put-cached! query-str type-filter limit result)
              result)))))
  ```
- [ ] **12.3.6** Update load-vocabularies to clear cache
  ```clojure
  (defn load-vocabularies [file-paths]
    (clear-cache!)  ; NEW: Clear cache on reload
    ;; ... rest of loading logic ...
    )
  ```
- [ ] **12.3.7** Test caching behavior
  - First query: cache miss, computes result
  - Repeat query: cache hit, returns cached
  - Different query: cache miss, computes result
  - Verify cache eviction (LRU behavior)
- [ ] **12.3.8** Test cache invalidation
  - Reload vocabulary → verify cache cleared
  - Manual clear-cache! → verify cache cleared
- [ ] **12.3.9** Benchmark cache performance
  - Measure cache hit response time (should be < 1ms)
  - Measure hit rate during typical reconciliation session
  - Verify memory overhead acceptable (< 20MB for 1000 entries)
- [ ] **12.3.10** Add cache statistics logging (optional)
  - Log hit rate periodically
  - Expose via admin endpoint (future)

---

## Notes

- Grafter 3.0 requires Java 17+ and Clojure 1.11.1+
- The existing `fuzzy-string` library works unchanged
- CORS is mandatory for W3C Reconciliation API v0.2
- OpenRefine is the primary client to test against
- Grafter handles all RDF format detection automatically
