# Firecrawl (Web-Scrape/Search-API) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) |
|---|-----------|--------------------------|
| 1 | Vor jeder Firecrawl-Recherche | **Frank fragen** (Firecrawl/MiniMax vs. Opus) — Free hat nur 1.000 Credits/Monat |
| 2 | Mehrere Unterthemen | **max 2 parallel** (Continuous-Spawning mit 2), nie 7 — Free erlaubt nur 2 concurrent |
| 3 | Auswertung der Quellen | Rohdaten NICHT in Opus laden → `mm-research.py` (MiniMax M3 max Thinking) wertet aus |
| 4 | Credits budgetieren | `/search` `limit` klein (Default 5; Checks 2-3); 1 Credit ≈ 1 gescrapte Seite |
| 5 | Suche vs. einzelne Seite | Thema offen → `/v1/search`; bekannte URL → `/v1/scrape` (spart Credits) |
| 6 | Aus Python aufrufen | `Authorization: Bearer`, **User-Agent setzen** (sonst Cloudflare 403/1010), `encoding='utf-8'` |
| 7 | Paper-/arXiv-Recherche | **Research Index** (`/v2/search/research/papers`,`/github`) — SOTA arXiv-Recall, 2 Credits/10 Treffer. Fuer Direktiven-/Paper-Recherche besser als normale Websuche (v2.11.0). |
| 8 | Wiederholtes Struktur-Scraping | **`deterministicJson`** statt `json` — kein LLM pro Request, pro-Site gecachter Extractor → guenstiger + konsistent (v2.11.0). |
