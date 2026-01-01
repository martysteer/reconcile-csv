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

## Future Enhancements (Out of Scope)

- Multiple vocabulary support
- SPARQL endpoint integration
- Persistent storage/caching
- Authentication/API keys
- Concept scheme hierarchies
