# reconcile-skos: Future Enhancements

This document captures planned enhancements that are not part of the immediate roadmap (Priority 1 & 2). These features may be implemented in future phases based on user needs and project priorities.

**For detailed specifications of Priority 1 & Priority 2 features**, see:
- `SPECIFICATION.md` - Detailed feature specifications
- `TASKS.md` - Implementation task breakdown

---

## Priority 3: Enhanced Matching & Search Quality

### 3.1 Advanced Scoring Enhancements

**Impact:** Medium - Better match quality
**Status:** Code structure exists, needs implementation
**Location:** `src/reconcile_skos/reconcile.clj:170-184`

**Current State:**
Functions are stubbed but not integrated into the scoring algorithm.

**Planned Enhancements:**

1. **Language Preference Boosting**
   - Boost scores when matched label language matches user's preferred language
   - Use `Accept-Language` header or explicit `lang` parameter
   - Example: User prefers English → "London" (en) scores higher than "Londres" (fr)
   - Implementation: 10% score boost for language match

2. **ConceptScheme Preference Boosting**
   - Boost scores for concepts from specific schemes
   - Useful when user is working primarily with one vocabulary
   - Example: Geographic reconciliation → boost FAST Geographic concepts
   - Implementation: 5% score boost for scheme match

3. **Notation Matching**
   - Exact match on `skos:notation` should score very high
   - Example: Query "fst01204271" → exact notation match scores 0.95+
   - Helps users reconcile by identifier codes

4. **Exact vs. Partial Match Weighting**
   - Currently: Exact match = 1.0, fuzzy uses Sørensen-Dice
   - Enhancement: Penalize partial matches more aggressively
   - Prefer shorter labels for exact matches (fewer characters = more specific)

**Implementation Notes:**
- Add configuration for boost weights
- Make language preference configurable (CLI or config file)
- Test with multilingual vocabularies

---

### 3.2 Property Constraints Filtering

**Impact:** Medium - More precise queries
**Status:** Stubbed
**Location:** `src/reconcile_skos/reconcile.clj:100-106`

**Purpose:**
Enable users to filter reconciliation results based on SKOS property values. Part of W3C Reconciliation API specification.

**Example Query:**
```json
{
  "query": "London",
  "type": "Geographic",
  "properties": [
    {
      "pid": "broader",
      "v": "http://id.worldcat.org/fast/1219920"  // England
    }
  ]
}
```
Returns only geographic "London" concepts that have "England" as a broader term.

**Implementation Requirements:**
- Parse `properties` array from query
- For each property constraint:
  - Extract property ID (e.g., "broader", "related")
  - Extract expected value (URI or literal)
  - Filter concepts where property matches value
- Support multiple property constraints (AND logic)

**Use Cases:**
- Find geographic places within a specific country
- Find concepts related to a specific topic
- Find concepts in a specific hierarchical branch

**Challenges:**
- Property values can be URIs or literals
- Need to handle missing properties gracefully
- Performance impact on large result sets

---

### 3.3 Better ID Extraction & URI Handling

**Impact:** Low-Medium - Cleaner IDs in results
**Status:** Basic implementation exists
**Location:** `src/reconcile_skos/skos.clj:39-51`

**Current Approach:**
1. Try URI fragment (after `#`)
2. Try last path segment (after last `/`)
3. Fallback to full URI

**Enhancements:**

1. **Prefer `skos:notation` as ID**
   - Many vocabularies have explicit notation codes
   - Example: FAST uses "fst01204271" as notation
   - More user-friendly than URI fragments

2. **Configurable ID Extraction Strategy**
   - Allow users to specify preferred ID source
   - Options: `notation`, `fragment`, `path`, `full-uri`
   - CLI flag: `--id-source notation`

3. **Better Handling of Numeric IDs**
   - Current: "1204271" (from URI)
   - Preferred: "fst01204271" (from notation)
   - Add prefix to disambiguate

4. **Multiple ID Sources**
   - Store both notation and URI-based ID
   - Return notation as primary ID
   - Include URI as alternate identifier

**Implementation:**
```clojure
(defn extract-id [concept]
  (or
    (:notation concept)           ; Prefer notation
    (extract-id-from-uri (:uri concept))  ; Fallback to URI
    (:uri concept)))              ; Last resort
```

---

## Priority 4: Data Extension (OpenRefine Advanced Features)

### 4.1 Property Proposals

**Impact:** Medium - Enables data extension in OpenRefine
**Status:** Returns empty list
**Location:** `src/reconcile_skos/extend.clj:8-16`

**Purpose:**
Tell OpenRefine which SKOS properties are available for extending data. Users can then add columns with broader/narrower/related concepts, definitions, etc.

**Implementation:**

1. **Define Available Properties**
   ```clojure
   (def extendable-properties
     [{:id "broader"
       :name "Broader Concepts"
       :type {:id "/type/object"}
       :description "Parent concepts in hierarchy"}
      {:id "narrower"
       :name "Narrower Concepts"
       :type {:id "/type/object"}
       :description "Child concepts in hierarchy"}
      {:id "related"
       :name "Related Concepts"
       :type {:id "/type/object"}
       :description "Associated concepts"}
      {:id "definition"
       :name "Definition"
       :type {:id "/type/text"}
       :description "Concept definition"}
      {:id "notation"
       :name "Notation"
       :type {:id "/type/text"}
       :description "Concept notation code"}])
   ```

2. **Implement `propose-properties` Function**
   - Accept optional `type-id` parameter (concept scheme filter)
   - Return list of available properties
   - Format according to W3C spec

3. **Testing**
   - OpenRefine should show these properties in "Add columns from reconciled values"
   - Selecting a property should trigger data extension request

---

### 4.2 Data Extension Implementation

**Impact:** Medium - Allows enriching data with SKOS properties
**Status:** Stub implementation
**Location:** `src/reconcile_skos/extend.clj:24-28`, `src/reconcile_skos/core.clj:165`

**Purpose:**
Return property values for reconciled concepts, enabling users to enrich their data with hierarchical relationships, definitions, etc.

**Example Request:**
```json
{
  "ids": ["1204271", "1204272"],
  "properties": [
    {"id": "broader"},
    {"id": "definition"}
  ]
}
```

**Example Response:**
```json
{
  "rows": {
    "1204271": {
      "broader": [{"id": "1219920", "name": "England"}],
      "definition": [{"str": "Capital city of the United Kingdom"}]
    },
    "1204272": {
      "broader": [{"id": "1205807", "name": "Ontario"}],
      "definition": [{"str": "City in southwestern Ontario, Canada"}]
    }
  },
  "meta": [
    {"id": "broader", "name": "Broader Concepts"},
    {"id": "definition", "name": "Definition"}
  ]
}
```

**Implementation Requirements:**

1. **Parse JSON Request Body**
   - Currently: `TODO: Parse JSON body properly`
   - Need to read POST body as JSON
   - Extract `ids` and `properties` arrays

2. **Fetch Property Values**
   - For each concept ID:
     - Look up concept
     - For each requested property:
       - Extract property value(s)
       - Format according to type (object vs. text)

3. **Format Response**
   - Object properties (broader, narrower, related): Return `{:id :name}`
   - Text properties (definition, notation): Return `{:str "..."}`
   - Handle missing properties gracefully (return empty array)

4. **Resolve Related Concepts**
   - For object properties, look up related concept to get name
   - Fallback to URI if concept not found

**Use Cases:**
- Add "Broader Term" column to show parent geographic regions
- Add "Definition" column for concept descriptions
- Add "Related Topics" column for cross-references

---

## Priority 5: Configuration & Usability

### 5.1 Extended CLI Options

**Impact:** Medium - More flexible deployment
**Status:** Partially implemented (--port and multiple files exist)
**Location:** `src/reconcile_skos/core.clj:323-346`

**Additional CLI Flags:**

1. **`--name <service-name>`**
   - Override default service name in manifest
   - Example: `--name "My FAST Reconciliation Service"`
   - Default: "SKOS Reconciliation Service"

2. **`--base-uri <uri>`**
   - Custom base URI for concept identifiers
   - Example: `--base-uri "http://example.org/concepts/"`
   - Default: `http://localhost:<port>/`

3. **`--lang <language>`**
   - Preferred language filter for labels
   - Example: `--lang en` → prefer English labels
   - Affects which label is displayed as "name" in results

4. **`--max-memory <size>`**
   - JVM heap size hint/reminder
   - Example: `--max-memory 4g`
   - Print warning if loading large files with small heap

5. **`--log-level <level>`**
   - Control logging verbosity
   - Options: `debug`, `info`, `warn`, `error`
   - Default: `info`

6. **`--cache-size <number>`**
   - Set query cache size
   - Default: 1000 entries
   - Set to 0 to disable caching

**Example Usage:**
```bash
lein run --port 9000 \
         --name "FAST Reconciliation" \
         --base-uri "http://id.worldcat.org/fast/" \
         --lang en \
         --log-level info \
         FAST*.skosxml
```

---

### 5.2 Configuration File Support

**Impact:** Low-Medium - Easier complex deployments
**Status:** Not implemented
**Location:** New feature

**Purpose:**
Allow users to specify all configuration in a file instead of CLI arguments. Useful for complex deployments with many settings.

**Configuration File Format (config.edn):**
```clojure
{:server {:port 9000
          :base-uri "http://id.worldcat.org/fast/"}
 :service {:name "FAST Reconciliation Service"
           :identifier-space "http://id.worldcat.org/fast/"
           :schema-space "http://www.w3.org/2004/02/skos/core#"}
 :vocabularies ["FASTGeographic.skosxml"
                "FASTTopical.skosxml"
                "FASTPersonal.skosxml"]
 :scoring {:weights {:pref-label 1.0
                     :alt-label 0.9
                     :hidden-label 0.7}
           :min-score 0.3
           :language-boost 0.1
           :scheme-boost 0.05}
 :performance {:cache-size 1000
               :streaming-threshold-mb 10
               :max-memory "4g"}
 :logging {:level :info
           :request-logging true}}
```

**Implementation:**
1. Read config file if `--config <file>` provided
2. Merge config file settings with CLI arguments (CLI overrides file)
3. Apply settings to application configuration
4. Validate configuration on startup

**Benefits:**
- Reproducible deployments
- Version control configuration
- Document production settings
- Complex multi-vocabulary setups

---

## Priority 6: Robustness & Monitoring

### 6.1 Error Handling & Validation

**Impact:** Medium - Better stability
**Status:** Basic error handling exists
**Improvements Needed:**

1. **SKOS Structure Validation on Load**
   - Warn about concepts without `skos:prefLabel`
   - Warn about concepts without `skos:inScheme`
   - Validate URI syntax
   - Report malformed RDF

2. **Better Error Messages**
   - User-friendly error messages for common issues
   - File not found → suggest checking path
   - Format error → suggest correct format
   - OOM error → suggest increasing heap size

3. **Graceful Degradation**
   - Missing labels → use URI as fallback
   - Missing definitions → skip description
   - Broken relationships → show URI, don't crash

4. **Request Validation Middleware**
   - Validate reconciliation query structure
   - Validate suggest parameters
   - Return 400 Bad Request for malformed queries

**Example Validation:**
```clojure
(defn validate-concept [concept]
  (let [warnings []]
    (when (empty? (:pref-labels concept))
      (conj warnings "Concept has no prefLabel"))
    (when (empty? (:in-scheme concept))
      (conj warnings "Concept has no inScheme"))
    warnings))
```

---

### 6.2 Metrics & Monitoring

**Impact:** Low-Medium - Operational visibility
**Status:** Not implemented
**Purpose:** Provide visibility into service performance and usage

**Metrics to Track:**

1. **Query Response Times**
   - Average, median, p95, p99 response times
   - Track by endpoint (reconcile, suggest, etc.)
   - Identify slow queries

2. **Cache Performance**
   - Hit rate
   - Miss rate
   - Cache size
   - Eviction count

3. **Memory Usage**
   - Heap usage
   - Garbage collection statistics
   - Memory per vocabulary

4. **Request Counts**
   - Total requests
   - Requests by endpoint
   - Batch vs. single queries
   - Error rates

**Implementation Options:**

1. **Simple Console Logging**
   ```clojure
   ;; Periodically log statistics
   Every 5 minutes:
     Queries: 1234 (avg 45ms)
     Cache hit rate: 34%
     Memory: 1.2GB / 4GB
   ```

2. **Admin Endpoint**
   ```bash
   GET /admin/stats
   {
     "uptime": "4h 23m",
     "requests": {
       "total": 5432,
       "reconcile": 4231,
       "suggest": 1201
     },
     "cache": {
       "hits": 1423,
       "misses": 2808,
       "hit_rate": 0.336
     },
     "memory": {
       "used_mb": 1234,
       "max_mb": 4096
     }
   }
   ```

3. **Structured Logging**
   - JSON-formatted logs
   - Integration with log aggregation tools
   - Example: Logstash, Splunk, Datadog

---

## Implementation Priority Suggestions

If implementing these enhancements, suggested order:

1. **Quick Wins:**
   - CLI options (5.1) - Easy to implement, immediate value
   - Error messages (6.1) - Improve user experience significantly
   - Notation-based IDs (3.3) - Small change, better UX

2. **High Value:**
   - Property proposals (4.1) - Unlocks data extension
   - Data extension (4.2) - Major OpenRefine feature
   - Language/scoring boosts (3.1) - Better match quality

3. **Advanced:**
   - Configuration files (5.2) - For production deployments
   - Property constraints (3.2) - Advanced querying
   - Metrics/monitoring (6.2) - Production operations

4. **Nice to Have:**
   - Admin endpoint - Operational visibility
   - Structured logging - Integration with tools

---

## Decision Points

When considering these enhancements, evaluate:

1. **User Demand:** Which features do users request most?
2. **Complexity:** Implementation effort vs. value delivered
3. **Dependencies:** Some features require others (e.g., trie index for suggest)
4. **Maintenance:** Ongoing support burden
5. **Standards Compliance:** W3C Reconciliation API requirements vs. nice-to-haves

---

## Related Documentation

- **SPECIFICATION.md** - Priority 1 & 2 detailed specifications
- **TASKS.md** - Priority 1 & 2 implementation tasks
- **CHANGELOG.md** - Version history and completed features
- **README.md** - User-facing documentation and quick start
