# Speicher-Schema (1:1-Layer, AS-BUILT) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Frage / Situation | Sofort-Regel |
|---|-------------------|--------------|
| 1 | Was wird gespeichert? | **Text 1:1** (PFLICHT) + **Titel** (optional, = Schluessel) + **Kategorie** (optional). Keine KI-Bearbeitung |
| 2 | Etiketten in den Text einweben? | **NIE.** Nur reiner Inhalt in den Vektor; Titel/Kategorie/Datum GETRENNT ins Payload |
| 3 | Gleicher Titel nochmal speichern | **Ersetzt** den alten Eintrag (`doc_id = t_<sha1(user::titel)>`). Ohne Titel → neue UUID (`d_<uuid>`) |
| 4 | Langer Text | Chunking **nur fuer die Suche** (4000 Zeichen / 200 Overlap); der **volle Text liegt 1:1 im Payload JEDES Chunks** → exakter Abruf gibt das ganze Dokument |
| 5 | Abruf-Wege (alle liefern 1:1) | `by-title` (exakt), `by-category`, `by-date`, `search` (semantisch + Filter), `list`, `forget` |
| 6 | Gefilterte Suche | **Erst Payload-Filter (Kategorie/Datum) eingrenzen, DANN semantisch** — nicht hinterher. `category` Keyword-indiziert, `created_at` Datetime-indiziert |
| 7 | Embedding asymmetrisch | Speichern `RETRIEVAL_DOCUMENT`, Suchen `RETRIEVAL_QUERY` (bessere Treffer); `output_dimensionality=1536` EXPLIZIT |
| 8 | Kategorie-Frage ("alle meine Ziele") | Auf `by-category` routen (VOLLSTAENDIG), nicht semantisch top-5 |
| 9 | Kategorienamen | ASCII-lowercase, kurz (siehe feste Liste §5). Agent pflegt die Liste mitwachsend (Phase 4) |
