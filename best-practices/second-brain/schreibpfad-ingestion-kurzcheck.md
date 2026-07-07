# Schreib-Pfad & Ingestion (Klassifikation, Extraktion, Dedup) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (das Wichtigste zuerst)

| Signal / Frage | Sofort-Regel |
|----------------|--------------|
| Was speichern? | **Extrahieren, nicht roh ablegen.** Rohe Transkripte = verrauschtes Retrieval. Erst zu strukturierten Fakten/Entitäten/Präferenzen destillieren, DANN schreiben. |
| Welcher Typ? | Klassifizieren in: episodisch (Ereignis+Zeit), semantisch (Fakt/Präferenz/Entität), prozedural (Regel/Workflow), kurz-/langfristig |
| Dedup beim Schreiben | Entity-Linking als Primitiv (Mem0: parallele `{collection}_entities`); "habe ich das schon?" vor dem Add |
| Konflikt (neuer widerspricht altem Fakt) | **Alten invalidieren + neuen hinzufügen** (LWW-artig mit expliziter Invalidierung), bei zeitlichen Fakten Gültigkeits-Fenster (bi-temporal, Zep) statt Überschreiben |
| Pflicht-Metadaten pro Eintrag | Timestamp, Quelle/Provenance, Kategorie, Geltungsdauer/Expiry, Confidence — Decay einbauen |
| Voice ("speicher das" im Auto) | Schreib-Call **asynchron** (keine Latenz im Gespräch); User-ID aus der App-Auth ableiten, nicht neu erfinden |
| Vertrauen | **Erst explizit, dann automatisch** — Auto-Speichern erst, wenn das System sich bewährt hat; Mensch bleibt Mitschreiber |
| RAG vs. Memory | RAG = Relevanz als Eigenschaft des INHALTS (Allgemeinwissen); Memory = Relevanz als Eigenschaft des NUTZERS (persönlicher Kontext) — getrennt halten |
