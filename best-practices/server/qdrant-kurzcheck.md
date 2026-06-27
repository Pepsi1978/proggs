# Qdrant (Vektordatenbank) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Speicher dimensionieren | Bei grossen Collections Vektoren UND HNSW `on_disk: true` (beides!); Bulk mit `hnsw m=0` | §1 |
| 2 | Collection anlegen | `size` == Embedding-Dim (1536); Distanz bewusst (Cosine pre-normalisiert) | §2 |
| 3 | Absichern | `QDRANT__SERVICE__API_KEY` + an 127.0.0.1/VPN-IP binden + Volume; Image-Version pinnen | §3 |
| 4 | Client anbinden | korrekter Port (REST 6333), ohne TLS explizite `http://`-URL | §4 |
| 5 | Tunen | Quantisierung (Scalar int8/TurboQuant 1.18); `hnsw_ef` zur Suche steuern; Memory-Monitoring | §5 |
| 6 | Hierarchische Kategorien (Haupt/Unter) filtern | Pfad-String `Haupt/Unter` fuer den Anzeige-Wert + **separates `parent`-Feld** (Keyword-Index) fuer „alles unter Haupt" (`MatchValue`) — KEIN Praefix-Operator in Qdrant. Index VOR Ingest. | §6 |
