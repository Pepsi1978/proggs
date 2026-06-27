# Datenmodell & Kategorien Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (das Wichtigste zuerst)

| Signal / Frage | Sofort-Regel |
|----------------|--------------|
| Grund-Architektur | **Drei-Speicher-Muster:** Graph (Entitäten+Beziehungen) + Memories (destillierte Fakten) + Chunks (Rohtext, doppelt indiziert: Keyword + semantisch) — bei jedem Ingest parallel füllen |
| Pflicht-Metadaten pro Eintrag | `timestamp`, `source/provenance`, `category`, `confidence` (0-1), `valid_from/valid_until` (Geltung), `tags` |
| Confidence | Jeder extrahierte Eintrag bekommt Score 0-1 (1.0 = wörtlich im Quelltext, niedriger = inferiert/paraphrasiert) |
| Sich ändernde Fakten (Adresse, Job) | **Bi-temporal** (Zep): alten Fakt invalidieren mit Gültigkeits-Fenster statt löschen → Zeitreise-Abfragen |
| Scopes | Owner-Entity beim Init ("wer bin ich") + Kategorie/Domäne als Scope (programmieren/persönlich/inventar/aufgaben/journal) |
| Tags vs. Hierarchie | Tags ERGÄNZEN die Hierarchie ("Tunnel durch die Wände der Silos"), ersetzen sie nicht |
| Graph vs. Vektor vs. strukturiert | Beziehung→Graph, Konzept→Vektor, exakter Ort/Feld→strukturiert (siehe [[orchestrator-und-suche]] §2.1) |
| Extraktion | Deterministisch (YAML/CSV/JSON/Wikilinks) + LLM (Prosa→Entitäten/Fakten) kombinieren |
