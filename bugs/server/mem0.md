# Bekannte Bugs & Fallen: mem0 (mem0ai — KI-Memory-Layer des "zweiten Gehirns")

> **PFLICHT-LESEN vor Arbeit an einem selbst gehosteten mem0-Setup** (Memory.add/search/get_all,
> Embedder-/Vector-Store-Config, Memory-Qualitaet). mem0 ist der "Bibliothekar" zwischen dem REST-
> Wrapper und Qdrant im zweiten Gehirn. Loesungen funktionserhaltend (Direktive #3).
> **Zweite Seite der Medaille:** `best-practices/server/mem0.md`.
>
> **Stand:** recherchiert am **2026-06-22** (Firecrawl + MiniMax M3, quellentreu). **Anker:** **mem0ai 2.0.7**
> (live: `pip show mem0ai` im Container), Embedder Gemini `gemini-embedding-001` @1536, Vector-Store Qdrant 1.18.2,
> Python 3.12. Verwandt: [`qdrant.md`](qdrant.md), [`self-hosted-ai-agent-server.md`](self-hosted-ai-agent-server.md),
> [`apis/google-gemini-api.md`](../apis/google-gemini-api.md).
>
> **Versions-Hinweis:** Die Quellen beschreiben v2.x oft im Kontext der Migration auf "v3". Unsere
> installierte **2.0.7** nutzt bereits die `filters={...}`-API (live verifiziert) — Versions-Labels in
> den Quellen ("v2 alt / v3 neu") nicht 1:1 auf die PyPI-Nummer uebertragen; im Zweifel live testen.

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | ⭐ Gehirn fuellt sich mit Muell / erfundenen "Fakten" | **mem0 hat KEIN Qualitaets-Gate** — jeder extrahierte Fakt wird gespeichert. In einem Audit 90–98 % Junk (Halluzinationen, Duplikate). **Der Extraktions-Prompt ist der Flaschenhals, NICHT das Modell.** `custom_instructions` + `includes/excludes` setzen, Junk monitoren. | §1 |
| 2 | ⭐ `ValueError: shapes (0,1536) and (768,) not aligned` | Nicht-OpenAI-Embedder (Gemini!) liefern oft NICHT 1536 dims. `embedding_model_dims` (Vector-Store) UND `embedding_dims` (Embedder) explizit auf die ECHTE Modell-Dimension setzen, gleicher Wert. | §2 |
| 3 | ⭐ Halluzination wird endlos wiederholt (Feedback-Loop) | mem0 unterscheidet beim Extrahieren NICHT zwischen abgerufener Erinnerung und neuem Input → einmal gespeicherter Quatsch wird beim Recall re-extrahiert. 808 Kopien EINER Halluzination real dokumentiert. Recalled-Memories nicht zurueck in `add(infer=True)` fuettern. | §1, §5 |
| 4 | `ValueError` bei `search("q", user_id="x")` | Entity-IDs in `filters={"user_id": "x"}` + `top_k` (NICHT top-level `user_id=`/`limit=`). Gilt fuer unsere 2.0.7 (live). | §3 |
| 5 | Hybride Suche (BM25) wirkt "tot" bei Qdrant | `fastembed` fehlt → BM25 wird STILL deaktiviert (nur Log-Warning, semantik-only). `pip install fastembed`. | §4 |
| 6 | Daten weg nach Redeploy | mem0 hat keine eingebaute Sync/Export — Persistenz haengt am Vector-Store-Volume (siehe `qdrant.md` §3). | §6 |
| 7 | `[nlp]`-Extras-Build schlaegt fehl | Python 3.13 hat keine spaCy/Thinc-Wheels → **Python 3.10–3.12** (wir: 3.12 ✓). | §6 |

---

## 🔗 Bezugs-Tabelle: Bug ↔ Best-Practice
| Bug-Abschnitt (hier) | Best-Practice (`best-practices/server/mem0.md`) |
|----------------------|-------------------------------------------------|
| §1 Memory-Qualitaet | §1 Qualitaet steuern (custom_instructions, Gate) |
| §2 embedding_dims | §2 Embedder/Vector-Store-Config |
| §3 API/filters | §3 API richtig aufrufen |
| §4 fastembed | §2 Hybrid-Suche aktivieren |
| §5 Dedup/Feedback-Loop | §1 |
| §6 Betrieb | §4 |

---

## 1. ⭐ Memory-Qualitaet: Halluzinationen & Junk (der wichtigste Punkt fuers Gehirn)
**Symptom:** Das Gehirn fuellt sich mit erfundenen "Fakten", Profilen nicht existierender Personen,
Dutzenden Kopien derselben Aussage. In einem realen Audit (10.134 Eintraege, 32 Tage): **90–98 % Junk.**
**Ursachen (belegt):**
- **Kein Qualitaets-Gate:** "Every extracted fact goes straight to the vector store." Andere Systeme
  (Stanford Generative Agents, LangMem, Letta) scoren Kandidaten VOR dem Speichern — mem0 nicht.
- **Der Extraktions-Prompt ist der Flaschenhals, NICHT das Modell:** Wechsel von einem 2B-Modell auf
  Claude Sonnet 4.6 senkte den Junk nur von ~98 % auf ~90 % ("a better model extracts more
  indiscriminately"). Ein besseres LLM allein loest es NICHT.
- **Boot-File-Restating:** >50 % einer Collection waren immer wieder neu extrahierte System-Prompt-Fakten
  ("Agent uses she/her" 50+×, "Operator prefers Telegram" 200+×).
- **Harvard D3:** "indiscriminate memory storage performs worse than using no memory at all"; Filtern VOR
  dem Speichern brachte +10 % Leistung.
**Junk-Aufschluesselung (Audit `mem0#4573`, 10.134 Eintraege, 97,8 % Junk):** Boot-File-Restating **52,7 %**
(System-Prompt wird als Memory gespeichert), System-/Architektur-Dumps 8,2 %, halluzinierte Profile 5,2 %,
Rest Feedback-Loop-Amplifikation. `isNoiseMessage()` erkennt den System-Prompt NICHT als Rauschen.
**FIX (funktionserhaltend) — Qualitaet aktiv steuern (offiziell dokumentiert):**
- **`custom_instructions` STRENG setzen** (vormals `custom_fact_extraction_prompt`; in mem0 **hoechste
  Prioritaet**, ueber includes/excludes). Mem0-Doku woertlich: *„Start with strict instructions (only store
  confirmed facts), then relax — it's easier to allow more than to clean up polluted memory. Test before
  production."* Genau festlegen, WAS gespeichert wird und was NICHT (z.B. „Speichere nur bestaetigte Fakten
  ueber Frank/Projekte; ignoriere System-Prompt-Inhalte, Smalltalk, Tool-Configs").
- **`includes`/`excludes`** (Topics fokussieren/skippen) + **`custom_categories`** (2–3 Stueck, z.B.
  `projekt`/`gesundheit`/`praeferenzen`; mem0 klassifiziert per LLM nach `metadata.categories`).
- **Confidence-Gate:** Extraktionen unter **Score 0.7** verwerfen (Cookbook-Pattern). Such-`threshold`
  (v3-Default 0.1) filtert niedrig-relevante Treffer.
- **Self-Contained-Regel** (mem0 Prompt-Guideline): jede Memory fuer sich verstaendlich, Pronomen durch
  Namen ersetzen, 15–80 Woerter; relative Zeit an Datum heften („letzte Woche" → konkretes Datum).
- **Negative Few-Shot-Beispiele** im Extraktions-Prompt (zeigt, was NICHT gespeichert wird — fehlt in mem0 default).
- **Feedback-API** (POSITIVE/NEGATIVE/VERY_NEGATIVE pro `memory_id`) + periodisches **Junk-Audit** (mem0 hat
  KEIN offizielles Audit-Tool — manuell `get_all` + erfundene Profile/Near-Duplikate loeschen).
- **Was mem0 NICHT hat (Issue #4573 — bewusst sein):** kein hartes Quality-Gate vor Storage, keine
  **REJECT-Action** (nur ADD/UPDATE/DELETE/NONE), kein Feedback-Loop-Marking abgerufener Memories. Die
  Sauberkeit kommt also v.a. aus `custom_instructions` + Schreib-Disziplin (§5), nicht aus der Pipeline.
**Unser Stack:** Gemini (gutes Modell) reicht laut Befund NICHT allein. Fuer Franks Gehirn: strenge
`custom_instructions` von Anfang an + 2–3 `custom_categories` + gelegentliches Junk-Audit. **TODO pruefen:**
Bug `mem0#4999` (in 2.0.0 gab `search()` fuer ALLE Treffer Score 1.0 → Confidence-Gate ausgehebelt) — ob das
auf unserer **2.0.7** noch greift, vor produktivem Confidence-Gating verifizieren.
**Quelle:** GitHub mem0#4573 (Audit 97,8 %), docs.mem0.ai (custom-instructions, controlling-memory-ingestion,
custom-categories, oss-v2-to-v3), mem0#4999 · Recherche + Eskalation 2026-06-22.

## 2. ⭐ embedding_model_dims — der haeufigste Hard-Error (Gemini & Co.)
**Symptom:** `ValueError: shapes (0,1536) and (768,) not aligned: 1536 (dim 1) != 768 (dim 0)` beim ersten `add`.
**Ursache:** mem0 nimmt **default 1536** an (OpenAI-Mass). Nicht-OpenAI-Embedder liefern oft andere Dimensionen
(z.B. Gemini `text-embedding-004` = 768) → Mismatch mit der Qdrant-Collection.
**FIX:** Bei JEDEM nicht-OpenAI-Embedder die echte Output-Dimension EXPLIZIT setzen — und zwar an BEIDEN Stellen
mit demselben Wert: Vector-Store `embedding_model_dims` UND Embedder `embedding_dims`.
**Unser Stack ✓:** `gemini-embedding-001 @1536`, in `app.py` sind `embedder.config.embedding_dims=1536` UND
`vector_store.config.embedding_model_dims=1536` gesetzt — exakt richtig (DIM-Invariante, eigene Sonde im Code).
**Quelle:** GitHub mem0-Issue (Kommentar @parshvadaftari), Azure-Tutorial · Recherche 2026-06-22 + eigener Stack.

## 3. API: filters/user_id, top_k, threshold, rerank, messages
- **Entity-IDs in `filters`:** `search(query, filters={"user_id": "frank"}, top_k=N)` und
  `get_all(filters={"user_id": "frank"})` — NICHT top-level `user_id=`/`limit=` (sonst `ValueError`).
  Gilt fuer unsere **2.0.7** (live verifiziert; war der Fix #47071 letzte Session). `add(payload, user_id=…)`
  nimmt `user_id` weiter als kwarg.
- **Geaenderte Defaults (Migrations-relevant):** `top_k` 100→20, `threshold` `None`→`0.1` (filtert
  niedrig-relevante Treffer weg — fuer altes Verhalten `threshold=0.0`), `rerank` `True`→`False`.
- **`messages`-Validierung:** muss `str`/`dict`/`list[dict]` sein; `None` → `Mem0ValidationError VALIDATION_003`.
- **`add()`-Events:** liefert ggf. nur noch `ADD` (frueher `ADD`/`UPDATE`/`DELETE`) — Code, der die
  anderen Events erwartet, anpassen. (Unser Wrapper zaehlt nur `results` — robust.)
- **Graph Store** in neueren Versionen entfernt (`enable_graph`/`graph_store`).
**Quelle:** mem0-Migrations-Doku, PyPI · Recherche 2026-06-22 + eigener Vorfall #47071.

## 4. fastembed: BM25/Hybrid-Suche bei Qdrant wird STILL deaktiviert
**Symptom:** Hybride Suche (semantisch + Keyword) liefert schlechtere Recall als erwartet; im Log:
`"fastembed not installed — BM25 keyword search disabled"`.
**Ursache:** Fuer Qdrant braucht mem0 das Paket `fastembed` fuer BM25 — fehlt es, faellt die Suche
**stillschweigend** auf semantik-only zurueck (nur Log-Warning, kein Fehler).
**FIX:** `pip install fastembed` im mem0-Container, wenn hybride Suche gewollt ist. **Unser Stack pruefen:**
ist `fastembed` im `sb-mem0-api`-Image? Wenn hybride Suche fuers Gehirn erwuenscht → ergaenzen.
**Quelle:** mem0-Migrations-Doku · Recherche 2026-06-22.

## 5. Duplikate & Feedback-Loop
**Symptom:** Dieselben Fakten zigfach gespeichert; eine einmal halluzinierte Aussage vermehrt sich endlos.
**Ursache:** mem0 unterscheidet beim Extrahieren NICHT zwischen einer ABGERUFENEN Erinnerung und NEUEM
Gespraechsinhalt → wird eine Erinnerung (auch eine falsche) in den naechsten Kontext gegeben und mit
`infer=True` re-verarbeitet, extrahiert mem0 sie erneut. Real: 808 Kopien EINER Halluzination.
Qdrant-Upsert dedupliziert NICHT automatisch (siehe `qdrant.md` §2d).
**FIX:** Abgerufene Erinnerungen NICHT ungefiltert zurueck in `add(infer=True)` geben (Feedback-Loop-
Prevention). Periodisch Cosine-Similarity-Cluster auf Near-Duplikate pruefen und zusammenfuehren/loeschen.
Die in 2.x/3.x beworbene "automatische Deduplikation" + Multi-Signal-Suche (semantic+BM25+entity) hilft,
ersetzt aber kein bewusstes Vorgehen.
**Quelle:** Production-Audit-Blog · Recherche 2026-06-22.

## 6. Betrieb (Persistenz, Python, qdrant-client)
- **Persistenz:** mem0 selbst hat keine Sync/Export-Mechanik — die Daten leben im Vector-Store. Volume
  fuer Qdrant Pflicht (`qdrant.md` §3), sonst Datenverlust bei Redeploy ("lost three days of memory").
- **Python:** mit `[nlp]`-Extras (spaCy) aktuell **3.10–3.12** noetig (3.13 ohne Wheels → Build-Fehler).
  Wir: 3.12 ✓.
- **qdrant-client:** neuere mem0-Versionen verlangen `qdrant-client >=1.12.0`.
- **Concurrent Writes:** betrifft v.a. ChromaDB ("database is locked") — wir nutzen Qdrant, weniger relevant.
**Quelle:** mem0-/CrewAI-Doku, Migrations-Notizen · Recherche 2026-06-22.

---

## Pflicht-Checkliste vor mem0-Betrieb
- [ ] `embedding_model_dims` (Vector-Store) == `embedding_dims` (Embedder) == echte Modell-Dimension (1536)?
- [ ] `custom_instructions`/`includes`/`excludes` gesetzt, um Junk zu begrenzen?
- [ ] Plan fuer Junk-Audit (Near-Duplikate, erfundene Profile) bei einem dauerhaft genutzten Gehirn?
- [ ] Recalled-Memories NICHT ungefiltert zurueck in `add(infer=True)` (Feedback-Loop)?
- [ ] `filters={"user_id": …}` + `top_k` (nicht top-level)? `threshold` bewusst?
- [ ] `fastembed` installiert, falls hybride Suche gewollt?
- [ ] Python 3.10–3.12, `qdrant-client>=1.12.0`, Qdrant-Volume persistent?

---

## Quellen (Stand 2026-06-22)
Firecrawl + MiniMax M3 (quellentreu): mem0-Migrations-Doku (v2→v3, API/Defaults), GitHub-Issue
(embedding_model_dims/Gemini), Production-Audit-Blog (10.134-Eintraege-Junk-Analyse, Harvard-D3-Bezug),
PyPI/CrewAI-Doku. Konkrete $/Cent-Kosten pro `add(infer=True)` und mem0-spezifische Qdrant-Verbindungs-
fallen waren in den Quellen nicht belegt (→ `qdrant.md` §4 fuer WRONG_VERSION_NUMBER).
