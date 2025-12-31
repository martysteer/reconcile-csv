<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>SKOS Reconciliation Service</title>
<style>
  body {
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
    max-width: 800px;
    margin: 40px auto;
    padding: 0 20px;
    line-height: 1.6;
    color: #333;
  }
  h1 {
    color: #2c5282;
    border-bottom: 3px solid #4299e1;
    padding-bottom: 10px;
  }
  h2 {
    color: #2d3748;
    margin-top: 30px;
  }
  code {
    background-color: #f7fafc;
    padding: 2px 6px;
    border-radius: 3px;
    font-family: 'Monaco', 'Courier New', monospace;
    font-size: 0.9em;
    color: #e53e3e;
  }
  .info-box {
    background-color: #ebf8ff;
    border-left: 4px solid #4299e1;
    padding: 15px;
    margin: 20px 0;
  }
  .endpoint {
    background-color: #f7fafc;
    padding: 10px;
    margin: 10px 0;
    border-radius: 5px;
    font-family: 'Monaco', 'Courier New', monospace;
    font-size: 0.9em;
  }
  ul {
    margin: 10px 0;
  }
  li {
    margin: 5px 0;
  }
  a {
    color: #4299e1;
    text-decoration: none;
  }
  a:hover {
    text-decoration: underline;
  }
  .badge {
    display: inline-block;
    background-color: #48bb78;
    color: white;
    padding: 3px 8px;
    border-radius: 3px;
    font-size: 0.85em;
    font-weight: bold;
  }
</style>
</head>
<body>

<h1>🎯 SKOS Reconciliation Service</h1>

<p class="badge">W3C Reconciliation API v0.2</p>

<div class="info-box">
  <strong>Service Status:</strong> Running and ready to reconcile!<br>
  <strong>Endpoint:</strong> <code>http://localhost:8000/reconcile</code>
</div>

<h2>What is this?</h2>

<p>
  This is a <strong>SKOS vocabulary reconciliation service</strong> for
  <a href="https://openrefine.org" target="_blank">OpenRefine</a>.
  It enables you to match your messy data against structured SKOS vocabularies,
  thesauri, and controlled terminology lists.
</p>

<p>
  Rather than dealing with inconsistent subject headings, variant spellings, or
  uncontrolled terminology, you can reconcile your data against authoritative
  vocabularies like Library of Congress Subject Headings (LCSH), UNESCO thesaurus,
  Getty AAT, or your own domain-specific SKOS vocabularies.
</p>

<h2>How to use in OpenRefine</h2>

<ol>
  <li>Open your project in <a href="https://openrefine.org" target="_blank">OpenRefine</a></li>
  <li>Select the column containing terms you want to standardize</li>
  <li>Click <strong>Reconcile</strong> → <strong>Start reconciling...</strong></li>
  <li>Click <strong>Add Standard Service...</strong></li>
  <li>Enter this URL: <code>http://localhost:8000/reconcile</code></li>
  <li>Select <strong>SKOS Concept</strong> as the entity type</li>
  <li>Click <strong>Start Reconciling</strong></li>
</ol>

<h2>Features</h2>

<ul>
  <li><strong>Fuzzy matching</strong> - Handles typos and variations using Sørensen-Dice coefficient</li>
  <li><strong>SKOS-aware scoring</strong> - Prioritizes preferred labels over alternatives</li>
  <li><strong>Multi-language support</strong> - Works with language-tagged labels (@en, @de, @fr, etc.)</li>
  <li><strong>Data extension</strong> - Fetch related SKOS properties (broader, narrower, definitions)</li>
  <li><strong>Fast local matching</strong> - No external API calls, all matching happens in-memory</li>
</ul>

<h2>Supported SKOS Properties</h2>

<p>You can extend your reconciled data with these SKOS properties:</p>

<ul>
  <li><code>skos:prefLabel</code> - Preferred Label</li>
  <li><code>skos:altLabel</code> - Alternative Labels</li>
  <li><code>skos:definition</code> - Concept Definition</li>
  <li><code>skos:scopeNote</code> - Scope Note</li>
  <li><code>skos:broader</code> - Broader Concepts</li>
  <li><code>skos:narrower</code> - Narrower Concepts</li>
  <li><code>skos:related</code> - Related Concepts</li>
  <li><code>skos:notation</code> - Classification Notation</li>
  <li><code>skos:inScheme</code> - Parent Vocabulary</li>
</ul>

<h2>API Endpoints</h2>

<div class="endpoint"><strong>GET/POST</strong> /reconcile - Reconciliation queries</div>
<div class="endpoint"><strong>GET</strong> /suggest - Entity auto-complete</div>
<div class="endpoint"><strong>GET</strong> /suggest/type - Type suggestions</div>
<div class="endpoint"><strong>GET</strong> /preview - Concept preview</div>
<div class="endpoint"><strong>GET</strong> /view/:id - Full concept view</div>
<div class="endpoint"><strong>GET</strong> /properties - Available properties</div>
<div class="endpoint"><strong>POST</strong> /extend - Data extension</div>

<h2>Resources</h2>

<ul>
  <li><a href="https://www.w3.org/2004/02/skos/" target="_blank">SKOS Specification (W3C)</a></li>
  <li><a href="https://openrefine.org" target="_blank">OpenRefine</a></li>
  <li><a href="https://reconciliation-api.github.io/specs/latest/" target="_blank">W3C Reconciliation API Specification</a></li>
  <li><a href="https://github.com/martysteer/reconcile-csv" target="_blank">GitHub Repository</a></li>
</ul>

<hr style="margin: 40px 0; border: none; border-top: 1px solid #e2e8f0;">

<p style="text-align: center; color: #718096; font-size: 0.9em;">
  reconcile-skos v0.2.0 |
  <a href="https://github.com/martysteer/reconcile-csv/blob/skos/LICENSE">BSD-2 Clause License</a>
</p>

</body>
</html>
