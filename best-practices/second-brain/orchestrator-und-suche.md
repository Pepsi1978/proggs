# Orchestrator-Agent & hybride Suche — Best Practices (Stand 2026-06-21)

> Teil des Second-Brain-Wissens (`best-practices/second-brain/`). Behandelt die zwei Herzstücke
> des lokalen "Dirigenten": (1) wie der Orchestrator-/Router-Agent eine Anfrage versteht und an die
> richtige Such-Art routet, (2) welche Suche fuer welche Anfrage die richtige ist und wie man sie
> fusioniert. Quellen: `extern` (RAG-/Agentic-RAG-Fachblogs, arXiv-Surveys, Vektor-DB-Doku, 2025-2026).
> Schwester-Dateien: [[datenmodell]], [[memory-backends]], [[multi-client-zugriff]].

---

## ⚡ Kurzcheck (das Wichtigste zuerst)

| Signal / Frage | Sofort-Regel |
|----------------|--------------|
| Anfrage = "wo ist X / was ist in Schublade Y" (Inventar, exakter Ort) | **Strukturierte Metadaten-Abfrage** (SQL-artig auf Feldern), NICHT Embedding-Ähnlichkeit |
| Anfrage = exakter Name / Fehlercode / Versions-Nr / seltener Fachbegriff | **Keyword/BM25** (lexikalisch) |
| Anfrage = Konzept / Idee / paraphrasiert / "irgendwas über …" | **Vektor/semantisch** |
| Anfrage = Beziehung / "wer/was hängt mit X zusammen" / Multi-Hop | **Graph-Traversierung** |
| Unklar / gemischt (Standardfall) | **Hybrid: Vektor + BM25, fusioniert mit RRF (k=60), danach Cross-Encoder-Reranking** |
| Router-Wahl | **Semantic-/Embedding-Router als Default** (1 Vektorvergleich, billig); LLM-Router nur bei echter Mehrdeutigkeit; Regel-Router für klare Stichwort-Fälle |
| Latenz/Kosten-Falle | LLM für jede Routing-Entscheidung = teuer + langsam. Mehr Kontext ≠ besser (Rauschen senkt Trefferqualität — Context Rot) |
| Embedding-Modell DE/EN | mehrsprachiges, lokal lauffähiges Modell (z. B. BGE-M3) — `extern`/abgeleitet, vor Einsatz an EIGENEM Korpus testen |

**Merksatz:** Der Router entscheidet *welche* Suche, die Suche entscheidet *welche Treffer*. Beide
klein und billig halten — der teure LLM-Verstand gehört in die Aufgabe, nicht in die Türsteher-Frage.

---

## 1. Der Orchestrator-/Router-Agent

### 1.1 Drei Router-Ebenen (`extern`)
Routing passiert in agentischen Systemen auf drei Ebenen — wichtig, um nicht alles in einen Topf zu werfen:

| Ebene | Was sie tut | Muster |
|-------|-------------|--------|
| **Intra-Agent** | Mikro-Entscheidung *innerhalb* eines Agenten ("suche ich oder antworte ich direkt?") | ReAct (Thought → Action → Observation → Repeat); jeder Schritt nachvollziehbar |
| **Inter-Agent** | Übergabe zwischen spezialisierten Agenten in einer Pipeline | Recherche → Analyse → Synthese |
| **Orchestrator-Level** | Master-Agent zerlegt die Anfrage in Subtasks und delegiert an Spezialisten | Supervisor / Orchestrator-Worker |

Für Franks Second Brain ist der **lokale Dirigent** ein Orchestrator-Level-Router: Er nimmt die
Anfrage, entscheidet Kategorie + Such-Art, ruft das Server-Memory (per MCP/API) und baut das Ergebnis ein.

### 1.2 Die drei Router-Typen im Vergleich (`extern`)

| Merkmal | LLM-Router | Embedding-/Semantic-Router | Regel-Router |
|---------|-----------|----------------------------|--------------|
| Flexibilität | hoch (versteht Mehrdeutigkeit) | mittel-hoch | niedrig |
| Token-/Latenzkosten | **hoch** (LLM-Call pro Entscheidung) | **niedrig** (nur 1 Vektorvergleich) | sehr niedrig |
| Auditierbarkeit | hoch bei ReAct | mittel | hoch (deterministisch) |
| Mehrdeutige Anfragen | gut | gut bei sauberen Kategorie-Profilen | schlecht |

**Semantic-Router** (Default): Jede Kategorie/Such-Art bekommt ein Profil-Embedding (kurze
Beschreibung), die Anfrage wird per Cosine-Similarity dagegen verglichen → günstig, schnell,
kein LLM nötig. **LLM-Router** nur, wenn die Anfrage echt mehrdeutig ist. **Regel-Router** für
eindeutige Stichwort-Trigger ("wo ist", "speicher das", "Aufgabe:").

> **Empfehlung (abgeleitet aus den Quellen):** Hybrid-Router — zuerst billiger Regel-/Semantic-Check,
> Eskalation auf LLM-Router nur im Zweifelsfall. Das deckt sich mit Franks eigenem 3-Schichten-Gedanken
> (Kern lokal, teurer Verstand nur wo nötig).

### 1.3 Bewährte Agentic-RAG-Muster (`extern`)
Orchestrator-Worker (Supervisor delegiert), Routing (Anfrage verteilen), Parallelization (mehrere
Such-Agenten gleichzeitig), Evaluator-Optimizer (Selbstkorrektur), Self-Healing-Index (Agent
reichert den Index eigenständig an, Mensch bestätigt), Adaptive Intent (häufige Anfragen aus echtem
Nutzungsverhalten lernen).

### 1.4 Pitfalls (`extern`)
- **Token-Bloat:** Ein Agent, der alles können soll, kann nichts richtig ("jack-of-all-trades").
- **Cost-Explosion:** Großes LLM für Aufgaben, die ein kleines Modell oder eine Regel lösen.
- **Context-Degradation (Context Rot):** Mehr Kontext bringt erst etwas, dann *schadet* es (Rauschen,
  Redundanz). Genau Franks Argument für schlanken Kern + gezieltes Nachladen.
- **Failure-Cascades:** Ein Agenten-Fehler kippt das Gesamtsystem → enge Scopes, Fallbacks.
- **One-Shot-Retrieval:** Klassisches RAG sucht einmal; reicht es nicht, ist Schluss. Agentic RAG
  darf nachsuchen/umformulieren.

### 1.5 Latenz/Kosten niedrig halten (`extern`)
Lokale Modelle (vLLM / llama.cpp) für Datenhoheit + gleiche API; Confidence-/Semantic-Routing statt
LLM-Routing pro Entscheidung; KV-Cache nutzen; Edge/lokale Retrieval-Engine. (Konkrete ms/$-Benchmarks
für ein *persönliches* Memory geben die Quellen nicht her — selbst messen.)

---

## 2. Hybride Suche — welche Suche wann

### 2.1 Die abgeleitete Routing-Entscheidung (Franks Kernfrage)
Die Quellen liefern die Bausteine, aber keine fertige 3-Wege-Tabelle. Hier die **aus den belegten
Fragmenten abgeleitete** Entscheidungsregel (ehrlich als Synthese gelabelt, nicht wörtlich aus einer Quelle):

| Anfrage-Typ | Beispiel | Such-Art | Beleg-Fragment |
|-------------|----------|----------|----------------|
| Exakter Identifier | Fehlercode, SKU, Eigenname, seltener Fachbegriff | **BM25/Keyword** | "BM25 wins on exact identifiers, SKUs, error codes, acronyms" (Q3) |
| Strukturierter Fakt / Ort | "wo ist mein Paddel", "was ist in Schublade 3", Datum/Status | **Strukturierter Metadaten-Filter** (Feld-Abfrage) | Metadaten-Filter "narrow to the right slice before ranking" (Q7) |
| Konzept / paraphrasiert | "was hatte ich zu Entropie-Thesen", "Idee ähnlich wie …" | **Vektor/semantisch** | "dense vectors handle paraphrase and conceptual queries" (Q2/Q3) |
| Beziehung / Multi-Hop | "wer/was hängt mit X zusammen", "wie kam ich auf Y" | **Graph-Traversierung** | "graphs for relationships, multi-hop reasoning" (Q5) |
| Default / gemischt | die meisten echten Anfragen | **Hybrid (Vektor+BM25 → RRF → Reranking)** | "vector+BM25 fusion is the production default" (Q3) |

Empfohlener dynamischer Fluss (Q5): erst breit semantisch (Kandidaten finden), dann mit Graph/Filter
einengen (Beziehungen/Slice).

### 2.2 RRF — Reciprocal Rank Fusion (am besten belegt) (`extern`)
- **Formel:** `score(d) = Σ 1/(k + rank)` über alle Trefferlisten. **Default k=60** (Elasticsearch-Default
  + Cormack et al. 2009).
- **Warum RRF statt gewichteter Score-Summe:** BM25-Scores sind unbeschränkt positiv, Cosine ist in
  [-1,1] — naive Gewichtung lässt BM25 dominieren. RRF nutzt nur **Rangpositionen**, umgeht das Problem.
- **Empirie (WANDS E-Commerce):** Hybrid +7,4 % NDCG gegenüber BM25 oder KNN allein.
- **Vorgehen (Q3):** Mit RRF als Baseline starten, bestätigen dass Hybrid beide Einzelsignale schlägt,
  erst dann zu gewichteter Summe wechseln — und nur mit Eval-Set.

### 2.3 Cross-Encoder-Reranking (`extern`)
Dritte Stufe (keine Fusion): erst breit per Hybrid Top-50…200 holen, dann mit einem Cross-Encoder,
der Query+Dokument GEMEINSAM liest, neu bewerten → Präzisions-Schicht. Beispiele: `bge-reranker`,
Cohere Rerank, ColBERT, Voyage rerank-2.5 (Instruktions-folgend, 32K-Kontext; Hersteller-Angabe
+7,94 % vs. Cohere v3.5). Für ein persönliches Memory optional, aber hebt die Trefferqualität spürbar.

### 2.4 Chunking (`extern`)
- **Fixed-size mit Overlap** (z. B. 800 Token, 15-20 % Overlap): einfach, robust, kann Konzepte zerschneiden.
- **Strukturell/semantisch** (an Überschriften/Absätzen/Code-Blöcken): variable Größe, braucht Parser.
- **Sliding-Window (zur Query-Zeit):** Treffer auf Nachbar-Fenster erweitern (Kontext-Kontinuität).
- **Parent-Child (Multi-Vector):** Kinder (Absätze) indizieren, Eltern (Abschnitt/Seite) ans LLM geben.
- **Rezept (Q7):** Mit Fixed-Chunks (15-20 % Overlap) + wenigen Metadatenfeldern (type, category, date)
  starten → Hybrid (BM25 ∪ Vektor) + RRF → Reranking Top-50 → Top-5 → bei Abschneide-/Inkohärenz-Problemen
  auf strukturell/Parent-Child hochstufen → k, Filter, Gewichte über Recall/Latenz tunen.

### 2.5 GraphRAG / Hybrid Graph+Vektor (`extern`)
GraphRAG baut aus dem Korpus einen Entitäten-/Beziehungs-Graph und holt einen *verbundenen Teilgraph*
statt loser "ähnlicher" Chunks. HybridRAG (NVIDIA/BlackRock-Studie) fusioniert Graph + Vektor und
schlägt beide allein (Faithfulness, Answer-Relevance, Context-Recall) — relevant für Franks
Beziehungs-Anfragen. Mittelweg ohne neuen Stack: BM25-basiertes Hybrid-RAG (OpenSearch).

### 2.6 Embedding-Modell (DE/EN, lokal)
Die Suchqualität hängt stark vom Embedding-Modell ab — gegen den EIGENEN Korpus wählen, nicht
nachträglich. Für gemischt Deutsch/Englisch + lokal lauffähig ist **BGE-M3** ein verbreiteter Kandidat
(mehrsprachig, dense+sparse+multi-vector in einem). **Hinweis:** BGE-M3 wurde von den Firecrawl-Quellen
dieses Laufs NICHT belegt — diese Empfehlung ist `extern`/abgeleitet aus allgemeinem Stand und vor
Einsatz an Franks realen Daten zu testen.

### 2.7 Score-Inkompatibilität (Architektur-Kernpunkt) (`extern`)
BM25 (unbeschränkt) und Cosine ([-1,1]) sind **nicht vergleichbar** → ohne Normalisierung dominiert
BM25 jede gewichtete Mischung. Lösung: RRF (ignoriert Roh-Scores) oder explizite Normalisierung
(min-max/L2). Vendor-Stand: OpenSearch `score-ranker-processor` (RRF, ab 2.19), Elasticsearch `rrf`-Retriever
(8.16 GA, `rank_constant` 60), Weaviate Default seit v1.24 Relative-Score-Fusion, Qdrant server-seitiges
RRF ab v1.10.

---

## Offene Punkte / ehrliche Lücken
- Keine persönlich-Memory-spezifischen Latenz-/Kosten-Benchmarks in den Quellen (selbst messen).
- Die saubere 3-Wege-Routing-Tabelle (2.1) ist Synthese aus Fragmenten, kein Wortlaut einer Quelle.
- Konkrete lokale Small-LM-Empfehlung als Router-Backend (Phi/Gemma/Qwen) nicht belegt — Kandidaten testen.
- BGE-M3 nicht quellenbelegt (siehe 2.6).

## Quellen (Auswahl, `extern`, 2025-2026)
RRF/Hybrid: Azure AI Search Hybrid + RRF (Microsoft Learn), Elastic RRF-Reference, MongoDB RRF, Qdrant
Hybrid+Reranking. Reranking: ZeroEntropy Reranking-Guide 2026, Ailog Cross-Encoder-Guide. Chunking:
Firecrawl "Best Chunking Strategies 2026", Weaviate Chunking. GraphRAG: Meilisearch GraphRAG-Guide,
Neo4j GraphRAG, Memgraph Vector-vs-Graph-Routing. Router/Agentic RAG: arXiv Agentic-RAG-Survey
(2501.09136), Aurelio Semantic-Router, Decagon "What is an LLM Router", Milvus "Routing & Hybrid Retrieval".
Embeddings: BAAI/bge-m3 (Hugging Face), BentoML Open-Source-Embedding-Guide 2026.
