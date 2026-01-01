# reconcile-skos: SKOS Vocabulary Reconciliation Service

## Project Overview

**Project Name:** reconcile-skos  
**Version:** 0.2.0  
**Branch:** skos  
**Previous Name:** reconcile-csv  

Transform the existing CSV-based reconciliation service into a SKOS vocabulary reconciliation service that implements the W3C Reconciliation API v0.2.

## Goals

1. Replace CSV file input with SKOS/RDF vocabulary file support
2. Implement W3C Reconciliation API v0.2 compliance
3. Enable reconciliation against SKOS concepts using prefLabel, altLabel, hiddenLabel
4. Support SKOS semantic relationships in results (broader, narrower, related)
5. Maintain OpenRefine compatibility

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        reconcile-skos                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────────┐  │
│  │  Grafter     │───▶│   Concept    │───▶│  Reconciliation  │  │
│  │  RDF Parser  │    │    Index     │    │     Engine       │  │
│  └──────────────┘    └──────────────┘    └──────────────────┘  │
│         │                   │                     │             │
│         ▼                   ▼                     ▼             │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────────┐  │
│  │  All RDF     │    │  In-Memory   │    │  Fuzzy Matching  │  │
│  │  Formats     │    │  Concept     │    │  (dice coeff)    │  │
│  │  via RDF4J   │    │  Store       │    │                  │  │
│  └──────────────┘    └──────────────┘    └──────────────────┘  │
│                                                                  │
├─────────────────────────────────────────────────────────────────┤
│                      HTTP API Layer                              │
├─────────────────────────────────────────────────────────────────┤
│  GET  /              │ Service info page                        │
│  GET  /reconcile     │ Service manifest (no params)             │
│  GET  /reconcile     │ Reconciliation query (with params)       │
│  POST /reconcile     │ Batch reconciliation queries             │
│  GET  /suggest       │ Entity suggest (auto-complete)           │
│  GET  /suggest/type  │ Type suggest (SKOS ConceptScheme)        │
│  GET  /view/:id      │ Concept preview page                     │
│  GET  /flyout        │ Flyout HTML for concept                  │
│  GET  /preview       │ Preview HTML for concept                 │
└─────────────────────────────────────────────────────────────────┘
```

## Technology Stack

### RDF Processing: Grafter (Swirrl)

We use **Grafter** (`io.github.swirrl/grafter.io`) for RDF parsing:

- **Why Grafter?**
  - Production-proven library from Swirrl (UK Linked Data consultancy)
  - Actively maintained (v3.0.0, February 2023)
  - 196 GitHub stars - most popular Clojure RDF library
  - Clojure-idiomatic API with native data structures
  - Built on Eclipse RDF4J (lighter than Apache Jena)
  - Supports ALL common RDF formats

- **Supported Formats:**
  - Turtle (`.ttl`) - recommended
  - RDF/XML (`.rdf`, `.xml`, `.owl`)
  - N-Triples (`.nt`)
  - N-Quads (`.nq`)
  - TriG (`.trig`)
  - JSON-LD (`.json`, `.jsonld`)
  - Notation3 (`.n3`)

### Runtime Requirements

| Requirement | Version | Notes |
|-------------|---------|-------|
| Java | 17+ | Required by Grafter 3.0 |
| Clojure | 1.11.1+ | Required by Grafter 3.0 |
| Leiningen | 2.9+ | Build tool |

## SKOS Data Model Mapping

### Core SKOS Elements → Reconciliation API

| SKOS Element | Reconciliation API Element | Notes |
|--------------|---------------------------|-------|
| `skos:Concept` | Entity | Primary matchable unit |
| `skos:ConceptScheme` | Type | Groups of concepts |
| `skos:prefLabel` | Entity.name | Primary search field |
| `skos:altLabel` | Searchable label | Secondary search field |
| `skos:hiddenLabel` | Searchable label | Tertiary search field |
| `skos:notation` | Entity.id (fallback) | If no URI fragment |
| `skos:definition` | Entity.description | For disambiguation |
| `skos:scopeNote` | Entity.description | Alternative description |
| `skos:broader` | Property | Hierarchical relation |
| `skos:narrower` | Property | Hierarchical relation |
| `skos:related` | Property | Associative relation |
| Concept URI | Entity.id | Unique identifier |

### Label Search Priority

When matching queries against concepts:

1. **Exact match on prefLabel** → score: 1.0
2. **Fuzzy match on prefLabel** → score: 0.7-0.99
3. **Exact match on altLabel** → score: 0.9
4. **Fuzzy match on altLabel** → score: 0.6-0.89
5. **Match on hiddenLabel** → score: 0.5-0.8

### Multi-language Support

- All labels include `@lang` tags from RDF (via Grafter's LangString)
- Queries can specify preferred language via properties
- Default: match all languages, prefer exact language match

## W3C Reconciliation API v0.2 Compliance

### Service Manifest (Required Fields)

```json
{
  "versions": ["0.2"],
  "name": "SKOS Vocabulary Reconciliation Service",
  "identifierSpace": "http://example.org/vocabulary/",
  "schemaSpace": "http://www.w3.org/2004/02/skos/core#",
  "defaultTypes": [
    {
      "id": "skos:Concept",
      "name": "SKOS Concept"
    }
  ],
  "view": {
    "url": "http://localhost:8000/view/{{id}}"
  },
  "preview": {
    "url": "http://localhost:8000/preview?id={{id}}",
    "width": 400,
    "height": 300
  },
  "suggest": {
    "entity": {
      "service_url": "http://localhost:8000",
      "service_path": "/suggest",
      "flyout_service_path": "/flyout?id=${id}"
    },
    "type": {
      "service_url": "http://localhost:8000",
      "service_path": "/suggest/type"
    }
  },
  "extend": {
    "propose_properties": {
      "service_url": "http://localhost:8000",
      "service_path": "/properties"
    }
  }
}
```

### Reconciliation Query Response

```json
{
  "result": [
    {
      "id": "concept_123",
      "name": "Architecture",
      "description": "The art and science of designing buildings",
      "score": 0.95,
      "match": false,
      "type": [
        {
          "id": "skos:Concept",
          "name": "SKOS Concept"
        }
      ]
    }
  ]
}
```

### Data Extension Properties

Exposed SKOS properties for data extension:

| Property ID | Name | Description |
|-------------|------|-------------|
| `skos:prefLabel` | Preferred Label | Primary label |
| `skos:altLabel` | Alternative Label | Synonyms |
| `skos:definition` | Definition | Scope definition |
| `skos:scopeNote` | Scope Note | Usage context |
| `skos:broader` | Broader Concept | Parent concepts |
| `skos:narrower` | Narrower Concept | Child concepts |
| `skos:related` | Related Concept | Associated concepts |
| `skos:inScheme` | In Scheme | Parent vocabulary |
| `skos:notation` | Notation | Classification code |

## File Format Support

### Input Formats (via Grafter/RDF4J)

| Format | Extensions | MIME Type |
|--------|------------|-----------|
| Turtle | `.ttl` | `text/turtle` |
| RDF/XML | `.rdf`, `.xml`, `.owl` | `application/rdf+xml` |
| N-Triples | `.nt` | `application/n-triples` |
| N-Quads | `.nq` | `application/n-quads` |
| TriG | `.trig` | `application/trig` |
| JSON-LD | `.json`, `.jsonld` | `application/ld+json` |
| Notation3 | `.n3` | `text/n3` |

### Format Detection

1. Check file extension (automatic via Grafter)
2. Explicit `:format` parameter override
3. Default: auto-detect

## Configuration

### Command Line Interface

```bash
lein run [--port PORT] <vocabulary-file> [<vocabulary-file2> ...]
```

**Arguments:**
- `<vocabulary-file>` - One or more SKOS/RDF vocabulary files to load
- `--port PORT` - HTTP server port (default: 8000)

**Features:**
- **Multiple vocabulary files**: Load and merge multiple SKOS files into a unified reconciliation index
- **Custom port**: Specify a different port for the HTTP server
- **Auto-format detection**: Automatically detects RDF format from file extension
- **Fallback to RDF/XML**: Non-standard extensions (e.g., `.skosxml`) automatically try RDF/XML format

### Example Usage

```bash
# Load a single vocabulary file (default port 8000)
lein run vocabulary.ttl

# Load a single file with custom port
lein run --port 9000 vocabulary.ttl

# Load multiple vocabulary files and merge them
lein run file1.ttl file2.ttl file3.ttl

# Load FAST chronological and geographic vocabularies
lein run FASTChronological.skosxml FASTGeographic.skosxml

# Load multiple files with custom port
lein run --port 9000 vocab1.rdf vocab2.ttl vocab3.skosxml

# All FAST vocabularies on custom port
lein run --port 8080 FAST*.skosxml
```

**Multiple File Loading:**
When loading multiple vocabulary files, the service will:
1. Load each file sequentially
2. Merge all concepts into a unified in-memory index
3. Merge all ConceptSchemes (displayed as types in OpenRefine)
4. Build a single label index across all vocabularies
5. Report total counts: concepts, schemes, and unique labels

**Output Example:**
```
Loading 2 SKOS vocabularies...
  Loading: FASTChronological.skosxml
    Loaded 6287 RDF triples
    Found 695 SKOS concepts, 1 concept schemes
  Loading: FASTFormGenre.skosxml
    Loaded 59228 RDF triples
    Found 7437 SKOS concepts, 1 concept schemes
Building unified label index...
  Total concepts: 8132
  Total concept schemes: 1
  Total unique labels: 11776
```

## Dependencies

### project.clj

```clojure
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
    
    ;; Fuzzy matching (existing)
    [fuzzy-string "0.1.3"]
  ]
  
  :plugins [[lein-ring "0.12.6"]]
  :main reconcile-skos.core
  :aot [reconcile-skos.core]
  
  ;; Java 17+ required
  :java-source-paths []
  :jvm-opts ["-Xmx2g"])
```

### Removed Dependencies

- `csv-map "0.1.0"` - No longer needed
- `org.clojure/tools.nrepl "0.2.3"` - Optional, dev only

### Estimated JAR Size

| Component | Size |
|-----------|------|
| Clojure + Ring + Compojure | ~6MB |
| Grafter.io + RDF4J | ~4-5MB |
| fuzzy-string | ~50KB |
| **Total** | **~10-12MB** |

## API Endpoints

### GET /reconcile (no params) - Service Manifest

Returns the service manifest JSON.

### GET/POST /reconcile - Reconciliation

**Query Parameters:**
- `query` - Single query JSON
- `queries` - Batch queries JSON
- `callback` - JSONP callback (optional)

### GET /suggest - Entity Suggest

**Query Parameters:**
- `prefix` - Search prefix string
- `cursor` - Pagination offset (optional)

### GET /suggest/type - Type Suggest

Returns available ConceptSchemes.

### GET /view/:id - Concept View

Returns HTML page for a concept.

### GET /preview - Concept Preview

**Query Parameters:**
- `id` - Concept identifier

Returns embeddable HTML preview.

### GET /flyout - Flyout Preview

**Query Parameters:**
- `id` - Concept identifier

Returns JSON with flyout HTML.

### GET /properties - Property Proposals

**Query Parameters:**
- `type` - Type identifier

Returns available SKOS properties for data extension.

### POST /extend - Data Extension

Fetches property values for given concept IDs.

## Error Handling

| HTTP Status | Condition |
|-------------|-----------|
| 200 | Success |
| 400 | Malformed query |
| 404 | Concept not found |
| 413 | Batch too large |
| 500 | Server error |

## Security Considerations

1. **CORS**: Required for W3C API v0.2 compliance
2. **Input validation**: Sanitize all query inputs
3. **Resource limits**: Configurable batch size limits
4. **No authentication**: Local service only (as per original design)

## Performance Considerations

1. **In-memory index**: Load all concepts at startup
2. **Lowercase normalization**: Pre-compute for fuzzy matching
3. **Grafter streaming**: Efficient parsing of large vocabularies
4. **Connection pooling**: Jetty defaults

## Testing Strategy

1. **Unit tests**: SKOS parsing, fuzzy matching, scoring
2. **Integration tests**: Full API endpoint testing
3. **Compatibility tests**: OpenRefine integration
4. **Sample vocabularies**: Include test SKOS files in multiple formats

## Grafter Usage Examples

### Loading RDF Statements

```clojure
(ns reconcile-skos.skos
  (:require [grafter-2.rdf4j.io :as gio]
            [grafter-2.rdf.protocols :as pr]))

;; Auto-detect format from extension
(def triples (gio/statements "vocabulary.ttl"))

;; Explicit format
(def triples (gio/statements "vocab.xml" :format :rdf))

;; From URL
(def triples (gio/statements "http://example.org/vocab.ttl"))
```

### Filtering SKOS Concepts

```clojure
(def skos-ns "http://www.w3.org/2004/02/skos/core#")

(defn skos-property? [pred]
  (.startsWith (str pred) skos-ns))

(defn extract-concepts [triples]
  (->> triples
       (filter #(skos-property? (:p %)))
       (group-by :s)))
```

## Detailed Feature Specifications

### Priority 1: Core Functionality

#### 1.1 Type Filtering by ConceptScheme

**Status:** Planned
**Impact:** Critical when loading multiple vocabularies
**Location:** `src/reconcile_skos/reconcile.clj:92-98`

**Purpose:**
Enable users to filter reconciliation queries to specific SKOS ConceptSchemes. Essential when loading multiple FAST vocabularies (Geographic, Topical, Personal, etc.) to search only within a specific domain.

**Current State:**
```clojure
(defn filter-by-type
  "Filter concepts by type if specified in query"
  [concepts query]
  (if-let [type-filter (:type query)]
    ;; TODO: Implement type filtering based on ConceptScheme
    concepts
    concepts))
```

**Implementation Specification:**

1. **Query Input:**
   ```json
   {
     "query": "London",
     "type": "http://id.worldcat.org/fast/ontology/Geographic"
   }
   ```

2. **Filtering Logic:**
   - Extract `type` parameter from reconciliation query
   - If type is `"skos:Concept"` → return all concepts (no filtering)
   - If type matches a ConceptScheme URI → filter concepts where `skos:inScheme` includes that URI
   - Support both full URIs and ConceptScheme IDs

3. **ConceptScheme Matching:**
   - Match by full URI: `"http://id.worldcat.org/fast/"`
   - Match by ConceptScheme ID from manifest `defaultTypes`
   - Handle concepts with multiple `skos:inScheme` values

4. **Edge Cases:**
   - Concepts without `skos:inScheme` → exclude from type-filtered results
   - Unknown type specified → return empty results with appropriate logging
   - Multiple schemes in one file → correctly associate concepts with schemes

**Example Use Case:**
```bash
# User loads multiple FAST vocabularies
lein run FASTGeographic.skosxml FASTTopical.skosxml FASTPersonal.skosxml

# OpenRefine reconciliation query with type filter
{
  "query": "Shakespeare",
  "type": "Personal Names",
  "limit": 5
}
# Returns only personal name concepts, excluding geographic/topical matches
```

**Testing:**
- Load 2+ vocabularies with different ConceptSchemes
- Query with type filter should return subset
- Query without type filter should return all matches
- Verify manifest `defaultTypes` includes all ConceptSchemes

---

#### 1.2 Entity Suggest (Auto-complete)

**Status:** Stubbed
**Impact:** High - improves OpenRefine UX
**Location:** `src/reconcile_skos/suggest.clj:6-12`

**Purpose:**
Provide auto-complete suggestions as users type in OpenRefine reconciliation dialogs. Returns ranked suggestions based on prefix matching against SKOS labels.

**Current State:**
```clojure
(defn suggest-entities
  [prefix concepts & {:keys [limit] :or {limit 10}}]
  ;; TODO: Prefix search on prefLabels and altLabels
  {:code "/api/status/ok"
   :status "200 OK"
   :prefix prefix
   :result []})
```

**Implementation Specification:**

1. **Endpoint:** `GET /suggest?prefix=<string>&limit=<int>&callback=<string>`

2. **Request Parameters:**
   - `prefix` (required) - Search string (e.g., "Lond")
   - `limit` (optional, default: 10) - Max results to return
   - `cursor` (optional) - Pagination offset (future enhancement)
   - `callback` (optional) - JSONP callback

3. **Response Format:**
   ```json
   {
     "code": "/api/status/ok",
     "status": "200 OK",
     "prefix": "Lond",
     "result": [
       {
         "id": "1204271",
         "name": "London (England)",
         "n:type": {"id": "skos:Concept", "name": "SKOS Concept"},
         "description": "Capital city of the United Kingdom"
       },
       {
         "id": "1204272",
         "name": "London (Ontario)",
         "n:type": {"id": "skos:Concept", "name": "SKOS Concept"},
         "description": "City in southwestern Ontario"
       }
     ]
   }
   ```

4. **Matching Algorithm:**
   - **Phase 1: Exact Prefix Match**
     - Search normalized `prefLabel` values starting with prefix
     - Search normalized `altLabel` values starting with prefix
     - Rank: prefLabel exact prefix > altLabel exact prefix

   - **Phase 2: Contains Match (if < limit results)**
     - Search normalized labels containing prefix (not at start)
     - Add to results until limit reached

   - **Phase 3: Ranking**
     - Exact prefix match on prefLabel (score: 1.0)
     - Exact prefix match on altLabel (score: 0.9)
     - Contains match in prefLabel (score: 0.5)
     - Contains match in altLabel (score: 0.4)
     - Sort by score descending, then alphabetically

5. **Normalization:**
   - Lowercase both prefix and labels
   - Trim whitespace
   - Consider diacritic folding (future enhancement)

6. **Performance Optimization:**
   - Use existing `label-index` for fast lookup
   - Create prefix trie index for efficient prefix matching (Priority 2)
   - Cache recent queries (Priority 2)

**Example Interaction:**
```bash
# User types "rest" in OpenRefine
GET /suggest?prefix=rest&limit=10

# Returns suggestions:
# - "Restoration, 1814-1868 (Spain)"
# - "Restaurants"
# - "Prehistoric period"  (contains "rest")
```

**Testing:**
- Prefix matches return results
- Results limited to specified limit
- Empty prefix returns top N most common concepts
- Handles special characters in prefix
- Returns results in < 100ms for 10k+ concept vocabularies

---

#### 1.3 HTML Preview and Flyout

**Status:** Placeholder
**Impact:** Medium-High - better user experience
**Location:** `src/reconcile_skos/core.clj:125`, `src/reconcile_skos/suggest.clj:25`

**Purpose:**
Display rich HTML previews of SKOS concepts in OpenRefine when hovering over or viewing reconciliation matches. Shows concept details, hierarchical relationships, and related terms.

**Current State:**
```clojure
;; Preview endpoint
(defn preview-handler [request]
  ;; TODO: Implement proper HTML preview
  (html-response "<div>Preview functionality coming soon...</div>"))

;; Flyout endpoint
(defn flyout-html [concept-id concepts]
  ;; TODO: Create compact HTML preview
  {:id concept-id :html "<p>Flyout for concept</p>"})
```

**Implementation Specification:**

1. **Endpoints:**
   - `GET /preview?id=<concept-id>` - Embeddable HTML preview
   - `GET /flyout?id=<concept-id>` - Flyout JSON with HTML

2. **Preview Response (GET /preview?id=123):**
   ```html
   <div style="padding: 10px; font-family: sans-serif; max-width: 380px;">
     <h3 style="margin: 0 0 10px 0; font-size: 16px;">London (England)</h3>

     <div style="margin-bottom: 10px;">
       <strong>Type:</strong> Geographic Concept<br>
       <strong>Notation:</strong> fst01204271
     </div>

     <div style="margin-bottom: 10px;">
       <strong>Definition:</strong> Capital city of the United Kingdom
     </div>

     <div style="margin-bottom: 10px;">
       <strong>Alternative Labels:</strong><br>
       • Greater London (England)<br>
       • Londinium
     </div>

     <div style="border-top: 1px solid #ccc; padding-top: 10px; margin-top: 10px;">
       <strong>Broader:</strong><br>
       • England

       <strong style="display: block; margin-top: 8px;">Narrower:</strong><br>
       • Westminster (London, England)<br>
       • City of London (England)
     </div>
   </div>
   ```

3. **Flyout Response (GET /flyout?id=123):**
   ```json
   {
     "id": "1204271",
     "html": "<div>...same HTML as preview...</div>"
   }
   ```

4. **HTML Structure:**
   - **Header:** Display `prefLabel` (largest available language, prefer English)
   - **Metadata Section:**
     - Type: ConceptScheme name (e.g., "FAST Geographic")
     - Notation: `skos:notation` value if available
     - URI: Concept URI (optional, collapsible)

   - **Description Section:**
     - Definition: `skos:definition` if available
     - Scope Note: `skos:scopeNote` if available

   - **Labels Section:**
     - Alternative Labels: All `skos:altLabel` values (bulleted list)
     - Hidden Labels: All `skos:hiddenLabel` values (if present, smaller text)

   - **Relationships Section:**
     - Broader: List `skos:broader` concepts (with labels, max 5)
     - Narrower: List `skos:narrower` concepts (with labels, max 5)
     - Related: List `skos:related` concepts (with labels, max 3)
     - Show "... and N more" if truncated

5. **Styling Guidelines:**
   - Width: Max 380px (fits OpenRefine preview panel)
   - Height: Max 300px (as specified in manifest)
   - Font: Sans-serif, 14px base
   - Colors: Neutral grays, minimal styling
   - Responsive: Works in small panels

6. **Error Handling:**
   - Concept not found → Return minimal HTML with error message
   - Missing fields → Gracefully skip sections
   - Broken relationships → Show URI instead of label

**Example Use Cases:**
- User hovers over "London" in reconciliation results → sees preview with broader term "England"
- User clicks "View" link → opens `/view/:id` with full concept page
- Helps disambiguate between "London (England)" vs "London (Ontario)"

**Testing:**
- Preview displays for concept with all fields populated
- Preview displays for minimal concept (only prefLabel)
- Handles missing broader/narrower gracefully
- HTML renders correctly in OpenRefine preview panel
- Flyout JSON properly encodes HTML

---

### Priority 2: Performance & Scalability

#### 2.1 Streaming/Chunked Loading for Large Files

**Status:** Planned
**Impact:** Critical for large FAST vocabularies
**Current Issue:** Memory overflow when loading 500MB+ files

**Problem Statement:**
Current implementation loads entire RDF file into memory before processing:
```clojure
(defn load-vocabulary-file [file-path]
  (let [triples (load-rdf file-path)  ; Loads ALL triples into memory
        extracted (extract-concepts triples)]))
```

**File Size Reality:**
```
FASTChronological.skosxml:  423 KB   ✓ Works fine
FASTFormGenre.skosxml:      4.0 MB   ✓ Works fine
FASTGeographic.skosxml:     220 MB   ⚠ Slow, high memory
FASTCorporate.skosxml:      424 MB   ⚠ Very slow
FASTTopical.skosxml:        471 MB   ⚠ Very slow
FASTPersonal.skosxml:       849 MB   ✗ May fail with default 2GB heap
```

**Implementation Specification:**

1. **Streaming RDF Parser:**
   ```clojure
   (defn load-rdf-streaming
     "Load RDF statements in chunks, processing incrementally"
     [file-path chunk-size]
     (with-open [stream (io/input-stream file-path)]
       (let [parser (gio/rdf-parser stream :format :rdf)]
         ;; Process triples in chunks
         (partition-all chunk-size (gio/statements parser)))))
   ```

2. **Incremental Concept Building:**
   ```clojure
   (defn load-vocabulary-streaming
     "Load vocabulary file in chunks to avoid memory overflow"
     [file-path]
     (println "  Loading (streaming):" file-path)
     (let [chunk-size 10000
           concepts-atom (atom {})
           schemes-atom (atom {})
           triple-count (atom 0)]

       ;; Process each chunk
       (doseq [chunk (load-rdf-streaming file-path chunk-size)]
         (swap! triple-count + (count chunk))
         (when (zero? (mod @triple-count 50000))
           (println "    Processed" @triple-count "triples..."))

         ;; Extract concepts from chunk and merge
         (let [chunk-data (extract-concepts chunk)]
           (swap! concepts-atom merge (:concepts chunk-data))
           (swap! schemes-atom merge (:schemes chunk-data))))

       (println "    Loaded" @triple-count "RDF triples")
       (println "    Found" (count @concepts-atom) "SKOS concepts")
       {:concepts @concepts-atom :schemes @schemes-atom}))
   ```

3. **Progress Reporting:**
   - Print progress every 50k triples
   - Show estimated time remaining (if possible)
   - Display current memory usage
   - Example output:
   ```
   Loading (streaming): FASTPersonal.skosxml
     Processed 50000 triples... (memory: 512MB)
     Processed 100000 triples... (memory: 734MB)
     Processed 150000 triples... (memory: 891MB)
     ...
     Loaded 1250000 RDF triples
     Found 125483 SKOS concepts
   ```

4. **Memory Management:**
   - Use transducers for efficient processing
   - Clear intermediate data structures after each chunk
   - Suggest GC after each file when loading multiple vocabularies
   - Monitor heap usage, warn if > 80% used

5. **Fallback Behavior:**
   - Auto-detect file size before loading
   - If < 10MB: Use current fast loading
   - If >= 10MB: Use streaming loading
   - Allow CLI override: `--streaming true/false`

6. **JVM Heap Recommendations:**
   ```bash
   # For loading all FAST vocabularies (total ~2.5GB)
   lein run -J-Xmx4g FAST*.skosxml

   # Update project.clj default
   :jvm-opts ["-Xmx4g"]  ; Increase from 2g to 4g
   ```

**Testing:**
- Load 500MB+ file without OOM error
- Verify all concepts extracted correctly
- Compare concept count: streaming vs. non-streaming (should match)
- Memory usage stays reasonable (< max heap - 500MB)
- Progress updates print regularly

**Performance Targets:**
- FASTPersonal.skosxml (849MB): Load in < 5 minutes with 4GB heap
- Memory usage: < 3GB peak when loading largest file
- Progress updates every 10-20 seconds

---

#### 2.2 Prefix Trie Index for Fast Suggest

**Status:** Planned
**Impact:** High - improves suggest performance
**Prerequisite:** Priority 1.2 (Entity Suggest) must be implemented first

**Purpose:**
Create a prefix trie (also called "prefix tree") data structure to enable sub-millisecond prefix matching for entity suggest auto-complete. Essential for responsive UX when searching large vocabularies.

**Current Approach (Naive):**
```clojure
;; Linear scan through all labels - O(n) where n = number of labels
(defn suggest-entities-naive [prefix concepts]
  (filter
    (fn [concept]
      (let [label (get-best-label concept)]
        (.startsWith (str/lower-case label) (str/lower-case prefix))))
    (vals concepts)))
```

**Problem:**
- **Performance:** O(n) lookup time
- With 100k concepts (FAST Personal Names): 50-200ms per query
- Unacceptable for real-time auto-complete

**Solution: Prefix Trie Index**

**Data Structure:**
```clojure
;; Trie node structure
{:char \l
 :concepts #{"uri1" "uri2"}  ; Concepts with labels starting with this prefix
 :children {\o {:char \o
                :concepts #{"uri1"}
                :children {\n {:char \n ...}}}}}

;; Example for "london":
;; Root -> l -> o -> n -> d -> o -> n
;;         ↓    ↓    ↓    ↓    ↓    ↓
;;        []   []   []   []   []  [concept-uris]
```

**Implementation Specification:**

1. **Trie Construction:**
   ```clojure
   (defn build-prefix-trie
     "Build prefix trie from label index for fast prefix matching"
     [label-index]
     (let [trie (atom {})]
       (doseq [[normalized-label concept-uris] label-index]
         (insert-into-trie! trie normalized-label concept-uris))
       @trie))

   (defn insert-into-trie!
     "Insert a label and its concept URIs into trie"
     [trie label concept-uris]
     (loop [node trie
            chars (seq label)
            depth 0]
       (if (empty? chars)
         ;; End of string - store concept URIs here
         (swap! node update :concepts (fnil into #{}) concept-uris)
         ;; Continue down the trie
         (let [char (first chars)]
           (swap! node update-in [:children char] #(or % {}))
           (recur (get-in @node [:children char])
                  (rest chars)
                  (inc depth))))))
   ```

2. **Prefix Search:**
   ```clojure
   (defn prefix-search-trie
     "Find all concepts with labels starting with prefix - O(k + m) where k=prefix length, m=matches"
     [trie prefix]
     (let [normalized-prefix (str/lower-case prefix)]
       (loop [node trie
              chars (seq normalized-prefix)]
         (cond
           ;; No match for this prefix
           (nil? node)
           #{}

           ;; Found prefix node - collect all concepts in subtree
           (empty? chars)
           (collect-all-concepts node)

           ;; Continue traversing
           :else
           (recur (get-in node [:children (first chars)])
                  (rest chars))))))

   (defn collect-all-concepts
     "Collect all concept URIs from a trie node and its descendants"
     [node]
     (let [direct (:concepts node)
           from-children (mapcat collect-all-concepts
                                 (vals (:children node)))]
       (into (or direct #{}) from-children)))
   ```

3. **Integration with load-vocabularies:**
   ```clojure
   (defn load-vocabularies
     [file-paths]
     ;; ... existing loading logic ...
     (let [idx (build-label-index merged-concepts)
           trie (build-prefix-trie idx)]  ; NEW: Build trie
       (reset! label-index idx)
       (reset! prefix-trie trie)  ; NEW: Store trie in atom
       {:concepts (count merged-concepts)
        :schemes (count merged-schemes)
        :labels (count idx)}))
   ```

4. **Memory Considerations:**
   - Trie memory overhead: ~2-3x the label index size
   - For 100k unique labels: ~5-10MB additional memory
   - Trade-off: Memory for speed (acceptable)

5. **Optimizations:**
   - Limit trie depth to 20 characters (handles 99.9% of labels)
   - Store only concept URIs in leaf nodes, not full concepts
   - Consider compressed trie (PATRICIA trie) for very large vocabularies

**Performance Targets:**
- Prefix search: < 5ms for any prefix (even in 100k concept vocabulary)
- Trie construction: < 1 second for 100k labels
- Memory overhead: < 50MB for largest FAST vocabulary

**Testing:**
- Build trie for test vocabulary
- Verify prefix "lon" returns all concepts starting with "london", "long", etc.
- Compare results: trie-based vs. linear scan (should be identical)
- Benchmark: measure query time improvement (should be 10-100x faster)
- Test edge cases: empty prefix, non-existent prefix, special characters

---

#### 2.3 Query Result Caching

**Status:** Planned
**Impact:** Medium - improves perceived performance
**Dependencies:** None

**Purpose:**
Cache reconciliation and suggest query results to improve response times for repeated queries. Particularly useful during OpenRefine reconciliation sessions where users often query the same terms multiple times.

**Implementation Specification:**

1. **Cache Strategy:**
   - **Type:** LRU (Least Recently Used) cache
   - **Size:** 1000 entries (configurable)
   - **TTL:** Until vocabulary reload
   - **Key:** Query string + type filter + limit
   - **Value:** Full response map

2. **Cache Implementation:**
   ```clojure
   (ns reconcile-skos.cache
     (:require [clojure.core.cache :as cache]))

   ;; Cache atom
   (def query-cache
     (atom (cache/lru-cache-factory {} :threshold 1000)))

   (defn cache-key
     "Generate cache key from query parameters"
     [query-str type-filter limit]
     (str query-str "|" type-filter "|" limit))

   (defn get-cached
     "Get cached result if available"
     [query-str type-filter limit]
     (let [k (cache-key query-str type-filter limit)]
       (when (cache/has? @query-cache k)
         (let [cached-value (cache/hit @query-cache k)]
           (reset! query-cache cached-value)
           (cache/lookup cached-value k)))))

   (defn put-cached!
     "Store result in cache"
     [query-str type-filter limit result]
     (let [k (cache-key query-str type-filter limit)]
       (swap! query-cache cache/miss k result)))

   (defn clear-cache!
     "Clear all cached results (call when vocabulary reloads)"
     []
     (reset! query-cache (cache/lru-cache-factory {} :threshold 1000)))
   ```

3. **Integration with reconcile-query:**
   ```clojure
   (defn reconcile-query
     "Process a single reconciliation query with caching"
     [query-input]
     (let [parsed (parse-query query-input)
           query-str (extract-query-string parsed)
           type-filter (:type parsed)
           limit (or (:limit parsed) 5)]

       ;; Check cache first
       (if-let [cached (get-cached query-str type-filter limit)]
         cached
         ;; Cache miss - compute and store
         (let [result (compute-reconciliation-result parsed)]
           (put-cached! query-str type-filter limit result)
           result))))
   ```

4. **Cache Statistics:**
   - Track hit rate
   - Log cache performance periodically
   - Expose stats via admin endpoint (future)

5. **Cache Invalidation:**
   - Clear cache when vocabularies are reloaded
   - No TTL needed (vocabulary is static)
   - Manual clear via REPL if needed

**Performance Targets:**
- Cache hit response time: < 1ms
- Cache hit rate: > 30% during typical reconciliation sessions
- Memory overhead: < 20MB for 1000 cached queries

**Testing:**
- First query: compute result (slow)
- Repeat query: return cached result (fast)
- Different queries: compute independently
- Cache eviction: LRU behavior verified
- Cache cleared on vocabulary reload

---

## Future Enhancements (Out of Scope for Current Phase)

- SPARQL endpoint integration
- Persistent storage/caching across restarts
- Authentication/API keys
- Advanced property filtering
- Language preference boosting
- Scoring enhancements
- Data extension implementation
- Configuration file support
