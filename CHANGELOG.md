# Changelog - reconcile-skos

## [0.2.0-SNAPSHOT] - 2025-12-31

### Overview
Complete transformation from CSV-based reconciliation service to SKOS vocabulary reconciliation service implementing W3C Reconciliation API v0.2.

### Major Changes

#### Environment & Dependencies
- **Upgraded**: Java 8 → Java 17 (OpenJDK 17.0.17)
- **Upgraded**: Clojure 1.5.1 → 1.11.1
- **Upgraded**: Ring 1.2.0 → 1.10.0
- **Upgraded**: Compojure 1.1.6 → 1.7.0
- **Added**: Grafter.io 3.0.0 (RDF/SKOS parsing via Eclipse RDF4J)
- **Added**: ring-cors 0.1.13 (W3C API v0.2 compliance)
- **Added**: ring-json 0.5.1 (JSON middleware)
- **Removed**: csv-map (no longer needed)

#### Architecture
- **New modular namespace structure**:
  - `reconcile-skos.core` - HTTP server & routing
  - `reconcile-skos.skos` - RDF/SKOS parsing with Grafter
  - `reconcile-skos.manifest` - W3C API v0.2 service manifest
  - `reconcile-skos.reconcile` - Fuzzy matching & scoring
  - `reconcile-skos.suggest` - Entity/type suggestions
  - `reconcile-skos.extend` - Data extension

#### Features

**SKOS Vocabulary Parsing** (Phase 2)
- Auto-detection of RDF formats: Turtle, RDF/XML, N-Triples, N-Quads, TriG, JSON-LD, Notation3
- Complete SKOS property extraction:
  - Labels: prefLabel, altLabel, hiddenLabel (with language tags)
  - Literals: definition, scopeNote, notation
  - Relationships: broader, narrower, related, inScheme
- ConceptScheme detection for type filtering
- Efficient in-memory indexing (24 unique labels from 7 concepts)

**Reconciliation Engine** (Phase 3)
- SKOS-aware scoring: prefLabel (1.0) > altLabel (0.9) > hiddenLabel (0.7)
- Fuzzy matching via Sørensen-Dice coefficient
- Typo tolerance (e.g., "Architectur" → "Architecture" @ 0.95)
- Exact match detection (score 1.0 = match:true)
- Batch query processing with parallel execution (pmap)
- Result limits (default: 5 candidates)

**W3C Reconciliation API v0.2** (Phase 4 & 8)
- Standards-compliant service manifest
- Dynamic ConceptScheme→Type mapping
- All required endpoints:
  - GET/POST /reconcile - Main reconciliation
  - GET /suggest - Entity auto-complete
  - GET /suggest/type - Type suggestions
  - GET /preview - Concept preview
  - GET /view/:id - Full concept view
  - GET /properties - Property proposals
  - POST /extend - Data extension
- CORS enabled for OpenRefine compatibility
- JSONP callback support

#### Test Results
✓ Loads 66 RDF triples from Turtle file
✓ Extracts 7 SKOS concepts with all properties
✓ Builds searchable index with 24 unique labels
✓ Multi-language support (English, German, French)
✓ Exact match: "Architecture" → 1.0 (match=true)
✓ Fuzzy match: "Architectur" → 0.95
✓ AltLabel: "Building design" → 0.9
✓ Batch processing: 3 queries in parallel
✓ Server starts on port 8000
✓ Service manifest generation working

#### Documentation
- Comprehensive README with usage examples
- Professional HTML landing page
- Detailed API endpoint documentation
- SKOS vocabulary examples (LCSH, UNESCO, Getty AAT)
- OpenRefine integration guide

### Breaking Changes
- **Project renamed**: reconcile-csv → reconcile-skos
- **CLI interface changed**: Now accepts SKOS/RDF files instead of CSV
- **Namespace changed**: reconcile-csv.* → reconcile-skos.*
- **Dependencies**: Requires Java 17+ (was Java 8)

### Usage

#### Basic
```bash
java -jar reconcile-skos-0.2.0.jar vocabulary.ttl
```

#### With Options (Future)
```bash
java -jar reconcile-skos-0.2.0.jar vocabulary.ttl --port 3000 --lang en
```

#### OpenRefine
Add this URL in OpenRefine:
```
http://localhost:8000/reconcile
```

### Implementation Phases Completed
- ✅ Phase 0: Environment Setup (Java 17, Clojure 1.11.1)
- ✅ Phase 1: Project Configuration & Namespace Restructure
- ✅ Phase 2: SKOS Parsing with Grafter
- ✅ Phase 3: Reconciliation Engine
- ✅ Phase 4: Service Manifest
- ✅ Phase 8: HTTP Layer with CORS
- 🔄 Phase 5-7, 9: Suggest/Preview/Extend/CLI (placeholders implemented)

### Known Limitations
- CLI argument parsing not yet implemented (uses defaults)
- Preview/view pages show placeholders
- Data extension returns empty results
- No type/property filtering yet

### Contributors
- Original reconcile-csv: Michael Bauer (@mihi-tr)
- SKOS transformation: Marty Steer
- Built with Grafter by Swirrl

### License
BSD-2 Clause

---

Generated with Claude Code
