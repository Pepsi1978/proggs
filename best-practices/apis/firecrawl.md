# Firecrawl (Web-Scrape/Search-API) — Best Practices

> **Zweite Seite der Medaille** zum Bug-Almanach `~/proggs/bugs/apis/firecrawl.md`: dort *was
> schiefgeht*, hier *wie man es von vornherein richtig + sparsam macht*. Fokus: Credit-Schonung im
> Free-Plan + Einbindung in die Recherche-Pipeline.
>
> **Stand:** 2026-06-20 (verifiziert via docs.firecrawl.dev/rate-limits + Live-Test). Quellen-Label
> `offiziell` (docs.firecrawl.dev) bzw. `extern`. Recherche-Strategie: `~/.claude/rules/research-strategy.md`.

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) |
|---|-----------|--------------------------|
| 1 | Vor jeder Firecrawl-Recherche | **Frank fragen** (Firecrawl/MiniMax vs. Opus) — Free hat nur 1.000 Credits/Monat |
| 2 | Mehrere Unterthemen | **max 2 parallel** (Continuous-Spawning mit 2), nie 7 — Free erlaubt nur 2 concurrent |
| 3 | Auswertung der Quellen | Rohdaten NICHT in Opus laden → `mm-research.py` (MiniMax M3 max Thinking) wertet aus |
| 4 | Credits budgetieren | `/search` `limit` klein (Default 5; Checks 2-3); 1 Credit ≈ 1 gescrapte Seite |
| 5 | Suche vs. einzelne Seite | Thema offen → `/v1/search`; bekannte URL → `/v1/scrape` (spart Credits) |
| 6 | Aus Python aufrufen | `Authorization: Bearer`, **User-Agent setzen** (sonst Cloudflare 403/1010), `encoding='utf-8'` |

---

## 1. Credit-Schonung (Free = 1.000/Monat, das knappste Gut)

- **Immer fragen vor dem Crawl** — die Pflicht-Frage aus `research-strategy.md` ist der wichtigste
  Spar-Hebel: Frank entscheidet pro Recherche, ob Firecrawl-Credits eingesetzt werden.
- **`limit` bewusst waehlen:** `/v1/search` scrapt bis zu `limit` Treffer → kostet ~`limit` Credits.
  Default 5; fuer schnelle Faktenchecks 2-3; nur bei breiten Themen hoeher.
- **Bekannte URL → `/v1/scrape`** statt `/v1/search` (1 Credit statt N).
- **Kein „Test-Crawl zum Ausprobieren"** — jeder Crawl kostet echtes Monatskontingent.

## 2. Parallelitaet: max 2 (Free)

Firecrawl Free erlaubt **2 gleichzeitige Requests** + **5 Suchen/Minute** `offiziell`. Bei einem
Recherche-Schwarm mit mehreren Unterthemen daher **Continuous-Spawning mit 2**: zwei Suchen starten,
auf Ergebnisse warten, zwei neue starten — bis alle Unterthemen abgedeckt sind. (Gegensatz zum
Opus-Researcher-Schwarm mit bis zu 7 parallel.)

## 3. Saubere Pipeline: Firecrawl → MiniMax → Opus

```
Firecrawl /v1/search (holt + scrapt Top-N)  →  MiniMax M3 max Thinking (filtert quellentreu)  →  Opus (arbeitet ein)
```
Werkzeug: `python3 ~/proggs/mm-research.py "<frage>" [limit]`. Vorteil: die gescrapten Rohseiten
(oft >20k Token) gehen direkt an MiniMax und **nie** in den teuren Opus-Kontext — gemessen ~100x
weniger Claude-Token als ein Opus-Researcher.

## 4. Python-Aufruf richtig (Cross-Platform)

- `Authorization: Bearer <key>` (Key aus `~/SK/OpenCode/firecrawl-api-key.txt`, Regel secrets-in-sk-folder).
- **User-Agent setzen** (`curl/8.5.0` o.ä.) — sonst blockt Cloudflare urllib mit 403/„error code 1010".
- `open(..., encoding='utf-8')`, JSON `ensure_ascii=False`, Pfade via `os.path.expanduser`
  (siehe `best-practices/claude-tooling/python-windows.md`).

---

## Quellen
- docs.firecrawl.dev/rate-limits, firecrawl.dev/pricing `offiziell`
- eesel.ai/blog/firecrawl-pricing, costbench.com (Free-Plan 2026) `extern`
- eigener Live-Test 2026-06-20 (`mm-research.py`)
