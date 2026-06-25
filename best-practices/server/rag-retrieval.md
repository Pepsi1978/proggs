# RAG-Retrieval — Best Practices (Routing, Metadaten-Filter, Embedding-Anreicherung, Reranking)

> Zweite Seite der Medaille zu `bugs/server/qdrant.md` (dort die Qdrant-spezifischen Filter-Fallen).
> Hier: wie man die SUCHE eines selbst gehosteten RAG/„zweiten Gehirns" intelligent macht — unabhaengig
> von der konkreten Vektor-DB. Bezug: second-brain-server (brain-api/agent), gemini-embedding-001 @1536.
> Funktionserhaltend (Direktive #3). Vor der Arbeit lesen (erst Bug-Almanach, dann diese Datei).
>
> **Stand:** recherchiert **2026-06-25** (Firecrawl+MiniMax M3 + OpenRouter `:online`, quellentreu;
> offizielle NVIDIA-RAG-Doku, Haystack, arxiv, qdrant.tech, Praxis-Artikel). **Anker:** gemini-embedding-001
> @1536, Qdrant 1.18.2. Verwandt: `bugs/server/qdrant.md` §7, `best-practices/server/qdrant.md` §6.

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Kategorie/Metadaten beim Suchen nutzen | **„Vector search finds meaning. Metadata defines scope."** Metadaten als SCOPE/Eligibility-Filter, NICHT als Relevanz-Ersatz. Die Bedeutungssuche bleibt der Kern. | §1 |
| 2 | Automatisch nach (Unter-)Kategorie filtern | **Weich + Fallback:** LLM leitet Kategorie aus der Frage ab → gefiltert suchen → bei 0/zu wenig Treffern (oder unsicherem Routing) **auf ungefiltert zurueckfallen**. Nie hart wegfiltern ohne Netz (Recall-Verlust). | §2 |
| 3 | Pre- vs. Post-Filter | **Pre-Filtering** ist der Production-Default (Qdrant nativ): erst Scope eingrenzen, dann ANN-Suche. Post-Filter nur fuer weiche Soft-Constraints (Boost) obendrauf. | §3 |
| 4 | Kategorie INS Embedding aufnehmen | Kategorie/Unterkategorie als kurzes Praefix an den Text VOR dem Embedden → bessere Treffer (intra-doc-Kohaesion). **Preis:** jede Kategorie-Aenderung erzwingt **Re-Embed** des Eintrags (set_payload reicht dann NICHT mehr). Bewusst abwaegen. | §4 |
| 5 | Reranking (Cross-Encoder) | Bei **kleiner** DB (hunderte bis wenige tausend) meist **NICHT noetig** — Standard-Pattern (Bi→Cross) ist fuer Skalierung; Lift v.a. bei grossen/lexikalisch harten Korpora. Erst einbauen, wenn Top-k stimmt aber Top-5 nicht. (Warnung: oft ueberdimensioniert.) | §5 |
| 6 | Hybrid-Suche (dense+sparse) | „Low-hanging fruit" gegen Vokabular-Mismatch (exakte Begriffe/Namen/IDs, die das Embedding verwaessert). Qdrant kann es nativ (Query API, RRF). Optionaler Ausbau, kein Muss bei kleiner DB. | §6 |

---

## §1 Metadaten = Scope, nicht Relevanz
Leitsatz aus der Praxis: **„Vector search finds meaning. Metadata defines scope."** Die semantische Suche
findet die Bedeutung; Metadaten (Kategorie, Datum, Quelle) grenzen den **erlaubten Bereich** ein. Wer
Kategorien benutzt, um Relevanz zu erzwingen (statt nur Scope), verliert Treffer (Recall). Konsequenz fuer
das zweite Gehirn: Kategorie ist Filter/Boost, die eigentliche Auffindbarkeit kommt aus Inhalt + Embedding.

## §2 Automatisches Kategorie-Routing — weich, mit Fallback
LLM-basiertes Ableiten von Metadaten-Filtern aus der Nutzerfrage ist etabliert (Haystack „extract metadata
filter", NVIDIA-RAG-Blueprint „natural-language filter generation"). **Robustheits-Regel (NVIDIA-Blueprint):**
- LLM nicht erreichbar → leerer Filter (ungefiltert).
- Ungueltige Filter-Generierung → `None`, ohne Filter weiter.
- Schema-Mismatch → ueberspringen, Original-Query behalten.
→ **Immer ein Fallback auf die ungefilterte Suche**, wenn der Filter fehlschlaegt ODER 0/zu wenig Treffer
liefert. Hartes Vorfiltern ohne Netz ist die Haupt-Recall-Falle. Praktisch fuers Gehirn: Abfrageagent
schlaegt eine (Unter-)Kategorie vor → gefiltert suchen → wenn leer/duenn, gleiche Query ungefiltert.

## §3 Pre-Filtering ist Default, Post-Filter nur fuer Soft-Constraints
Pre-Filtering (Scope VOR der ANN-Suche, von Qdrant nativ unterstuetzt) ist „the better default for most
production systems" — schneller + praeziser. Post-Filtering (erst Top-k, dann verwerfen) ist verschwenderisch
und darf NICHT der primaere Kontrollmechanismus sein. Hybrid-Empfehlung: **Pre-Filter fuer Eligibility/Scope
+ leichtes Post-/Boost fuer weiche Praeferenzen** (z.B. Aktualitaet, Quellenguete).

## §4 Kategorie/Metadaten ins Embedding aufnehmen (Trade-off)
arXiv (Jan 2026): **„prefixing metadata before the chunk yields strong retrieval accuracy, though it is
computationally expensive since any metadata update requires re-embedding."** Metadaten erhoehen die
intra-doc-Kohaesion und die Trennung relevant/irrelevant. Die staerksten Diskriminatoren sind
identifizierende Felder (im Paper: company/year); generische Titel bringen wenig.
- **Wenn man es nutzt:** Kategorie+Unterkategorie als kurzes, stabiles Praefix voranstellen
  (`[Programmieren > Best-Practices]\n\n<text>`).
- **Konsequenz beachten:** Eine Kategorie-Aenderung muss dann **re-embedden** (das billige `set_payload`
  reicht nicht mehr, weil die Kategorie im Vektor steckt). Embedding-Budget/Latenz einplanen.
- **Sparsamere Alternative** (komplexer): Metadaten separat embedden und mit dem Text-Vektor fusionieren
  („unified dual-encoder") → kein Voll-Re-Embed bei Aenderung, aehnliche Genauigkeit.

## §5 Reranking (Cross-Encoder) — bei kleiner DB meist unnoetig
Standard ist die zweistufige Kaskade (Bi-Encoder fuer Recall → Cross-Encoder fuer Precision, 50–100
Kandidaten). Der Hauptgrund ist **Skalierung**; bei wenigen hundert bis wenigen tausend Dokumenten faellt
dieses Argument weg. Lift typ. NDCG@10 +5–15 (bis +20 bei lexikalisch harten Daten), aber 10–100× teurer
pro Paar. Praxis-Warnungen: Reranking wird oft ueberdimensioniert eingebaut (dokumentierter Fall:
$15k/Monat unnoetig). **Empfehlung fuers zweite Gehirn (kleine DB):** zunaechst KEIN Reranking; erst einbauen,
wenn die Top-k die richtigen Passagen enthaelt, aber die Top-5 nicht — dann bringt ein leichter Cross-Encoder
mehr als ein staerkeres Embedding.

## §6 Hybrid-Suche (dense + sparse) — optionaler Qualitaetsausbau
Dense (Bedeutung) + sparse/BM25 (exakte Begriffe) fusioniert (RRF, in Qdrant `k=2`, gewichtetes RRF ab v1.17;
oder DBSF) faengt genau die Faelle, in denen das Embedding exakte Begriffe/Namen/IDs verwaessert
(„a good lunch" findet sparse nicht, dense schon — und umgekehrt). Qdrant macht das in EINEM Query-Call
(Universal Query API seit 1.10, named vectors dense+sparse, `Qdrant/bm25` als Sparse-Embedder). Gilt als
guenstig nachruestbar, ist aber bei kleiner DB kein Muss — eher dann, wenn viele Fachbegriffe/Eigennamen
gesucht werden.

## Quellen (Stand 2026-06-25)
NVIDIA RAG Blueprint (custom-metadata / natural-language filter), Haystack (extract metadata filter),
mindstudio.ai (pre- vs post-filtering), medium/logspace (retrieval weighting), arxiv 2502.00409 / 2605.10235
(routing), arxiv (metadata-enriched embeddings, Jan 2026), bigdataboutique / zeroentropy / nixiesearch /
OpenAI cookbook / towardsdatascience (reranking), qdrant.tech (hybrid Query API, RRF/DBSF). Firecrawl+MiniMax
(2026-06-25) + OpenRouter `:online`-Eskalation (2026-06-25).
