# RAG-Retrieval Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Kategorie/Metadaten beim Suchen nutzen | **„Vector search finds meaning. Metadata defines scope."** Metadaten als SCOPE/Eligibility-Filter, NICHT als Relevanz-Ersatz. Die Bedeutungssuche bleibt der Kern. | §1 |
| 2 | Automatisch nach (Unter-)Kategorie filtern | **Weich + Fallback:** LLM leitet Kategorie aus der Frage ab → gefiltert suchen → bei 0/zu wenig Treffern (oder unsicherem Routing) **auf ungefiltert zurueckfallen**. Nie hart wegfiltern ohne Netz (Recall-Verlust). | §2 |
| 3 | Pre- vs. Post-Filter | **Pre-Filtering** ist der Production-Default (Qdrant nativ): erst Scope eingrenzen, dann ANN-Suche. Post-Filter nur fuer weiche Soft-Constraints (Boost) obendrauf. | §3 |
| 4 | Kategorie INS Embedding aufnehmen | Kategorie/Unterkategorie als kurzes Praefix an den Text VOR dem Embedden → bessere Treffer (intra-doc-Kohaesion). **Preis:** jede Kategorie-Aenderung erzwingt **Re-Embed** des Eintrags (set_payload reicht dann NICHT mehr). Bewusst abwaegen. | §4 |
| 5 | Reranking (Cross-Encoder) | Bei **kleiner** DB (hunderte bis wenige tausend) meist **NICHT noetig** — Standard-Pattern (Bi→Cross) ist fuer Skalierung; Lift v.a. bei grossen/lexikalisch harten Korpora. Erst einbauen, wenn Top-k stimmt aber Top-5 nicht. (Warnung: oft ueberdimensioniert.) | §5 |
| 6 | Hybrid-Suche (dense+sparse) | „Low-hanging fruit" gegen Vokabular-Mismatch (exakte Begriffe/Namen/IDs, die das Embedding verwaessert). Qdrant kann es nativ (Query API, RRF). Optionaler Ausbau, kein Muss bei kleiner DB. | §6 |
