# Researcher & Web-Recherche Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Bei Fehlern in diesem Bereich den VOLLTEXT `researcher.md` lesen.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | PDF (System Card) über WebFetch | Über 10 MB bricht WebFetch ab → `curl -sL -o x.pdf` + `pdftotext -layout` (Tabellen zusätzlich mit `-raw` gegenprüfen) | §1 |
| 2 | openai.com liefert 403 | WebFetch/curl gesperrt → im Browser lesen (claude-in-chrome) oder Sekundärquelle kennzeichnen | §2 |
| 3 | Zahlen aus WebFetch-Zusammenfassung | Zellen werden verschoben oder vertauscht → Primärtext (curl, eingebettetes JSON) gegenprüfen | §3 |
| 4 | Leaderboard rendert clientseitig | arena.ai / vals.ai / artificialanalysis.ai: Daten stecken als JSON im HTML → per curl + Regex/json lesen | §4 |
