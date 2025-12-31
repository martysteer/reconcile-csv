# reconcile-skos: Task List

## Project Transformation: CSV → SKOS Reconciliation Service

**Branch:** `skos`  
**Start Date:** 2024  
**Target:** W3C Reconciliation API v0.2 compliant SKOS service  
**RDF Library:** Grafter (io.github.swirrl/grafter.io v3.0.0)

---

## Phase 0: Environment Setup & Prerequisites

### 0.1 Java Environment
- [ ] **0.1.1** Verify Java 17+ is installed (`java -version`)
- [ ] **0.1.2** If needed, install Java 17+ (e.g., `brew install openjdk@17` on macOS)
- [ ] **0.1.3** Set JAVA_HOME to Java 17+ installation
- [ ] **0.1.4** Verify: `java -version` shows 17 or higher

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

## Notes

- Grafter 3.0 requires Java 17+ and Clojure 1.11.1+
- The existing `fuzzy-string` library works unchanged
- CORS is mandatory for W3C Reconciliation API v0.2
- OpenRefine is the primary client to test against
- Grafter handles all RDF format detection automatically
