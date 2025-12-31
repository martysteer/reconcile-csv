# reconcile-skos

A SKOS Vocabulary Reconciliation Service for OpenRefine implementing the W3C Reconciliation API v0.2.

## Introduction

**reconcile-skos** enables you to reconcile your data against SKOS (Simple Knowledge Organization System) vocabularies, thesauri, and controlled vocabularies directly within OpenRefine.

Unlike traditional entity reconciliation services that query remote APIs, reconcile-skos loads your vocabulary file locally and provides fast, fuzzy matching against SKOS concepts using their preferred labels (prefLabel), alternative labels (altLabel), and hidden labels (hiddenLabel).

Perfect for:
- Standardizing subject headings against library thesauri
- Matching terms to Getty vocabularies (AAT, TGN, ULAN)
- Reconciling against domain-specific SKOS vocabularies
- Cleaning up inconsistent terminology using controlled vocabularies

## Features

- **W3C Reconciliation API v0.2 compliant** - Full OpenRefine compatibility
- **Multi-format support** - Turtle, RDF/XML, N-Triples, N-Quads, TriG, JSON-LD, Notation3
- **SKOS-aware matching** - Prioritizes prefLabel > altLabel > hiddenLabel
- **Fuzzy matching** - Uses Sørensen-Dice coefficient for similarity scoring
- **Multi-language support** - Handles language-tagged labels (@en, @de, @fr, etc.)
- **Data extension** - Fetch SKOS properties (broader, narrower, related, definitions)
- **Fast in-memory indexing** - Optimized for vocabularies with thousands of concepts

## Requirements

- **Java 17 or higher** (OpenJDK recommended)
- **Leiningen 2.9+** (for development)
- A SKOS vocabulary file in any RDF format

## Usage

### Pre-compiled JAR

```bash
java -jar reconcile-skos-0.2.0.jar <vocabulary-file> [options]

Options:
  --port PORT           HTTP port (default: 8000)
  --base-uri URI        Base URI for concept identifiers
  --lang LANG           Preferred language code (default: all)
```

### Examples

```bash
# Load a Turtle vocabulary
java -jar reconcile-skos-0.2.0.jar lcsh.ttl

# Load RDF/XML with custom port
java -jar reconcile-skos-0.2.0.jar vocab.rdf --port 3000

# Load with language preference (prioritize English labels)
java -jar reconcile-skos-0.2.0.jar unesco.ttl --lang en
```

### Using with Leiningen

```bash
lein run <vocabulary-file> [options]
```

## Setting up in OpenRefine

1. Start the reconciliation service with your vocabulary file
2. In OpenRefine, select a column to reconcile
3. Click **Reconcile** → **Start reconciling...**
4. Click **Add Standard Service...**
5. Enter: `http://localhost:8000/reconcile`
6. Select "SKOS Concept" as the entity type
7. Configure matching settings and reconcile!

### Accessing Reconciled Data

After reconciliation, use these GREL expressions:

```
cell.recon.match.id        # Concept URI
cell.recon.match.name      # Preferred label
cell.recon.match.score     # Match confidence (0.0 - 1.0)
cell.recon.best.id         # Best match even if not confirmed
```

### Data Extension

Use OpenRefine's **Add columns from reconciled values** to fetch SKOS properties:

- **Preferred Label** (skos:prefLabel)
- **Alternative Labels** (skos:altLabel)
- **Definition** (skos:definition)
- **Scope Note** (skos:scopeNote)
- **Broader Concepts** (skos:broader)
- **Narrower Concepts** (skos:narrower)
- **Related Concepts** (skos:related)
- **Notation** (skos:notation)
- **In Scheme** (skos:inScheme)

## Supported RDF Formats

| Format | Extensions | MIME Type |
|--------|------------|-----------|
| Turtle | `.ttl` | `text/turtle` |
| RDF/XML | `.rdf`, `.xml`, `.owl` | `application/rdf+xml` |
| N-Triples | `.nt` | `application/n-triples` |
| N-Quads | `.nq` | `application/n-quads` |
| TriG | `.trig` | `application/trig` |
| JSON-LD | `.json`, `.jsonld` | `application/ld+json` |
| Notation3 | `.n3` | `text/n3` |

Format is auto-detected from file extension.

## Building from Source

```bash
# Clone the repository
git clone https://github.com/martysteer/reconcile-csv.git
cd reconcile-csv
git checkout skos

# Install dependencies
lein deps

# Run directly
lein run vocabulary.ttl

# Build standalone JAR
lein uberjar
java -jar target/reconcile-skos-0.2.0-standalone.jar vocabulary.ttl
```

## SKOS Vocabulary Examples

- **Library of Congress Subject Headings (LCSH)** - http://id.loc.gov/authorities/subjects.html
- **UNESCO Thesaurus** - http://vocabularies.unesco.org/browser/thesaurus/en/
- **Getty Art & Architecture Thesaurus (AAT)** - http://www.getty.edu/research/tools/vocabularies/aat/
- **EU Vocabularies** - https://op.europa.eu/en/web/eu-vocabularies

## W3C Reconciliation API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/` | GET | Landing page with usage instructions |
| `/reconcile` | GET/POST | Reconciliation queries & service manifest |
| `/suggest` | GET | Entity suggest (auto-complete) |
| `/suggest/type` | GET | Type suggest (ConceptSchemes) |
| `/preview` | GET | HTML preview for concept |
| `/view/:id` | GET | Full concept view page |
| `/properties` | GET | Available properties for data extension |
| `/extend` | POST | Fetch property values for concepts |
| `/flyout` | GET | Flyout preview HTML |

## Architecture

reconcile-skos is built with:
- **Clojure 1.11.1** - Functional programming language
- **Grafter 3.0.0** - RDF parsing via Eclipse RDF4J
- **Ring/Compojure** - HTTP server and routing
- **fuzzy-string** - Sørensen-Dice coefficient matching

## Performance

- Vocabulary loading: ~1-5 seconds for 10,000 concepts
- Index build: In-memory, optimized for lookup performance
- Concurrent queries: Batch queries processed in parallel
- Memory: ~2GB heap recommended for large vocabularies

## License

Copyright © 2013-2025 Michael Bauer, Open Knowledge Foundation

Distributed under the BSD-2 Clause license. See LICENSE for details.

## Acknowledgments

- Original reconcile-csv by Michael Bauer (@mihi-tr)
- SKOS transformation by Marty Steer
- Built with Grafter by Swirrl (https://github.com/swirrl)
- Implements W3C Reconciliation API v0.2
