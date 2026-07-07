# mem0 (mem0ai — KI-Memory-Layer des "zweiten Gehirns") Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

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
