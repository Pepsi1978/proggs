# Bekannte Bugs & Fallen: Firecrawl (Web-Scrape/Search-API)

> **PFLICHT-Lesen vor jeder Firecrawl-Nutzung** (Recherche-Pipeline, `mm-research.py`, Firecrawl-MCP).
> Fokussiert auf **Free-Plan-Limits + Integration** (verifiziert 2026-06-20 via WebSearch +
> Live-Test). Bewusst NICHT erschoepfend — eine vollstaendige `bug-almanach-recherche` zu Firecrawl
> kann spaeter folgen (mit Franks OK). Loesungen funktionserhaltend.
>
> **Stand:** 2026-06-20. Quellen: docs.firecrawl.dev/rate-limits, firecrawl.dev/pricing, eigener
> Live-curl/urllib-Test 2026-06-20. **Anker:** firecrawl-free=1000-credits/mo.
>
> Gegenstueck (richtige Nutzung): `best-practices/apis/firecrawl.md`.
> Recherche-Strategie (wann/wie): `~/.claude/rules/research-strategy.md`.

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Signal / Situation | Sofort-Regel |
|---|--------------------|--------------|
| 1 | ⭐ Free-Plan-Limits | **1.000 Credits/Monat**, **2 concurrent requests**, **5 Suchen/min**, 10 Scrapes/min, 1 Crawl/min. Knapp — Credits bewusst einsetzen. |
| 2 | ⭐ Python `urllib` → HTTP 403 „error code 1010" | Cloudflare blockt den urllib-Default-UA. **`User-Agent: curl/8.5.0`** (o.ä.) setzen → kommt durch. (Live verifiziert.) |
| 3 | ⭐ Mehrere Recherche-Unterthemen parallel | Free erlaubt **nur 2 gleichzeitig** → Continuous-Spawning mit **2** (2 starten, warten, 2 neue). NIE 7 auf einmal (429). |
| 4 | Credits sparen | `limit` der Suche klein halten; **vor jeder Recherche Frank fragen** (Firecrawl vs. Opus, siehe research-strategy). |
| 5 | `/search` vs. `/scrape` | `/v1/search` = Suche + scrapt Top-`limit`-Treffer (1 Credit/Treffer); `/v1/scrape` = genau 1 URL. |
| 6 | Auth | Firecrawl nutzt `Authorization: Bearer <key>` (NICHT x-api-key — das ist der MiniMax-Go-Endpoint!). Key in `~/SK/OpenCode/firecrawl-api-key.txt`. |

---

## 1. Free-Plan: harte Limits (verifiziert 2026-06-20)

| Limit | Free-Wert | Konsequenz |
|-------|-----------|------------|
| Credits/Monat | **1.000** (monatlich erneuert, keine Kreditkarte) | ~1 Credit pro gescrapter Seite → bei vielen Recherchen schnell weg |
| Gleichzeitige Requests (concurrency) | **2** | Schwarm max 2 parallel (nicht 7 wie bei Opus-Researchern) |
| Such-Requests | **5/Minute** | mehrere Suchen drosseln |
| Scrape-Requests | 10/Minute | — |
| Crawl-Requests | 1/Minute | — |

**Konsequenz fuer Recherche-Schwaerme:** Ein Skill mit N Unterthemen (z.B. Bug-Almanach mit 7
Aspekten) darf **nicht** 7 Firecrawl-Calls auf einmal absetzen. Muster: **2 starten → auf Ergebnisse
warten → 2 neue starten** (Continuous-Spawning mit 2), bis alle durch sind. Sonst Rate-Limit (429)
und verschwendete Credits. (Quelle: docs.firecrawl.dev/rate-limits)

---

## 2. ⭐ Python `urllib` wird von Cloudflare geblockt (403 / „error code 1010")

**Symptom:** Ein `urllib.request`-POST an einen Firecrawl- ODER opencode.ai-Endpoint gibt
HTTP **403** mit Body `error code: 1010`, obwohl der Key stimmt und `curl` mit demselben Key durchkommt.
**Ursache:** `urllib` sendet den Default-User-Agent `Python-urllib/3.x`. Cloudflare (vor den Gateways)
blockt diese Signatur (1010 = „banned based on browser signature"). `curl` hat einen akzeptierten UA.
**Versionen:** beobachtet 2026-06-20 (opencode.ai/zen Gateway; Firecrawl-API analog absichern).
**FIX (funktionserhaltend):** Im urllib-Request einen akzeptierten User-Agent setzen, z.B.
`headers={"User-Agent": "curl/8.5.0", ...}`. So macht es `mm-research.py`.
**Quelle:** eigener Live-Test 2026-06-20 (`mm-research.py`).

---

## 3. Integration in die Recherche-Pipeline

- **Werkzeug:** `~/proggs/mm-research.py` ruft `/v1/search` (Bearer) → uebergibt die gescrapten
  Markdown-Quellen an MiniMax M3 (max Thinking) → kompakte, quellentreue Antwort. Die Rohdaten laufen
  NIE durch den teuren Claude/Opus-Kontext.
- **Credit-Kosten:** `/v1/search` mit `limit=N` scrapt bis zu N Treffer → ~N Credits pro Suche.
  Darum `limit` klein halten (Default 5; fuer schnelle Checks 2-3).
- **Pflicht-Frage:** Vor jeder Firecrawl-Recherche Frank fragen (Credit-Kontrolle) — siehe
  `~/.claude/rules/research-strategy.md` §1.

---

## 4. Kopplung zur Best-Practices-Seite

`best-practices/apis/firecrawl.md` — wie man Firecrawl von vornherein richtig + sparsam nutzt
(2-parallel-Muster, search-vs-scrape-Wahl, Credit-Budgetierung, mm-research.py-Pipeline).

---

## Quellen
- docs.firecrawl.dev/rate-limits (Free: 2 concurrent, 5 search/min, 10 scrape/min, 1 crawl/min)
- firecrawl.dev/pricing (Free: 1000 Credits/Monat)
- eesel.ai/blog/firecrawl-pricing, costbench.com/software/web-scraping/firecrawl/free-plan (2026)
- eigener Live-Test 2026-06-20 (urllib-UA / Cloudflare 1010)
