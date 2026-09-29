# Bekannte Bugs: Researcher & Web-Recherche (WebFetch, Benchmark-Quellen)

> PFLICHT-LESEN vor Web-Recherchen mit WebFetch/WebSearch, besonders zu KI-Modellen und Benchmarks.
> Stand: 2026-09-29 (Engine C, Sonnet-5-Schwarm, Recherche „KI-Modell-Benchmarks“). Versions-Anker: Claude Code 2.1.284, WebFetch-Tool.
> Gegenseite: `best-practices/claude-tooling/researcher.md` (Abschnitt „Benchmark- und Modellvergleiche“).

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | PDF (System Card) über WebFetch | Über 10 MB bricht WebFetch ab → `curl -sL -o x.pdf` + `pdftotext -layout` (Tabellen zusätzlich mit `-raw` gegenprüfen) | §1 |
| 2 | openai.com liefert 403 | WebFetch/curl gesperrt → im Browser lesen (claude-in-chrome) oder Sekundärquelle kennzeichnen | §2 |
| 3 | Zahlen aus WebFetch-Zusammenfassung | Zellen werden verschoben oder vertauscht → Primärtext (curl, eingebettetes JSON) gegenprüfen | §3 |
| 4 | Leaderboard rendert clientseitig | arena.ai / vals.ai / artificialanalysis.ai: Daten stecken als JSON im HTML → per curl + Regex/json lesen | §4 |

## §1 WebFetch: `maxContentLength size of 10485760 exceeded`
- **Symptom:** WebFetch auf große PDFs (Sonnet-5.5-System-Card 13 MB, Opus-5.5-Card 17 MB) bricht mit diesem Fehler ab.
- **Ursache:** hartes 10-MB-Limit des WebFetch-Tools.
- **Fix:** `curl -sL -o card.pdf "<url>"` und `pdftotext -layout card.pdf card.txt` (poppler ist in Git Bash vorhanden), dann gezielt per grep lesen. Tabellen mit verrutschten Spalten zusätzlich mit `pdftotext -raw` prüfen (Beispiel: Opus-5.5-Card Tab. 4.1.2.A).
- **Quelle:** Researcher-Läufe vom 29.09.2026 (System Cards Sonnet 5, Sonnet 5.5, Opus 5.5, Fable 5.1).

## §2 openai.com: HTTP 403 für WebFetch und curl
- **Symptom:** Ankündigungen und System Cards von OpenAI (openai.com, deploymentsafety.openai.com) liefern 403.
- **Fix:** Seite im echten Browser (claude-in-chrome) lesen; sonst Sekundärquelle nutzen und als solche kennzeichnen.

## §3 WebFetch-Zusammenfassungen verfälschen Tabellen
- **Symptom:** Zahlen landen in der falschen Spalte (FrontierCode 1.1: GPT-6-Sol-Wert in der Sonnet-Spalte; HLE: Sonnet-4.6-Werte als Sonnet-5-Werte ausgegeben).
- **Ursache:** WebFetch fasst die Seite mit einem kleinen Modell zusammen; Bildtabellen und breite Tabellen werden umgebrochen.
- **Fix:** Jede Benchmark-Zahl gegen Primärtext (curl-HTML, eingebettetes JSON, PDF-Text) prüfen. Suchmaschinen-Snippets vertauschen ebenfalls Modellversionen (Kimi-K2.6-Werte als K2.7/K3).

## §4 Clientseitig gerenderte Leaderboards
- **Symptom:** WebFetch liefert leere oder abgeschnittene Tabellen (arena.ai, vals.ai, artificialanalysis.ai, swebench.com).
- **Fix:** `curl -sL -A "Mozilla/5.0" <url>`. arena.ai: `\"` durch `"` ersetzen, dann ab `"leaderboard":{"arenaSlug"` das Array `"entries":[` lesen (Felder rank, rating, ratingUpper/Lower, votes, modelDisplayName). Vals: eingebettete Props (`accuracy`, `cost_per_test`) nach `html.unescape`. Artificial Analysis: eingebettetes JSON je Effort-Variante.
- **Falle:** `gpt-5.6-sol` und `gpt-6-sol` sind verschiedene Modelle — Modell-Key prüfen, nicht nur den Anzeigenamen.

## Fix-Status
Alle vier Punkte sind Werkzeug- bzw. Seiteneigenschaften, kein Fix angekündigt (Stand 2026-09-29).
