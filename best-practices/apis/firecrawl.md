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
| 7 | Paper-/arXiv-Recherche | **Research Index** (`/v2/search/research/papers`,`/github`) — SOTA arXiv-Recall, 2 Credits/10 Treffer. Fuer Direktiven-/Paper-Recherche besser als normale Websuche (v2.11.0). |
| 8 | Wiederholtes Struktur-Scraping | **`deterministicJson`** statt `json` — kein LLM pro Request, pro-Site gecachter Extractor → guenstiger + konsistent (v2.11.0). |

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

## 5. Firecrawl als OpenRouter-Web-Search-Engine (BYOK) — Promo + Credit-Oekonomie (recherchiert 2026-06-20)

Firecrawl ist auch als **Such-Engine im OpenRouter `web_search`-Server-Tool** waehlbar (`engine=firecrawl`,
so unterstuetzt von `or-research.py`) — neben Exa/Parallel/Perplexity.

- **Auto-Account:** Firecrawl in OpenRouter aktivieren + ToS akzeptieren → es wird **automatisch ein
  Firecrawl-Konto, verknuepft mit der E-Mail, angelegt** („no separate Firecrawl account needed"). `offiziell`
- **Promo (Stand 2026-06, ZEITLICH BEGRENZT):** **10.000 Gratis-Credits, verfallen nach 3 Monaten** fuer
  „new accounts". Das fruehere Launch-Angebot (Hobby-Plan + 100.000 Credits) ist **abgelaufen**. `offiziell`
- **Bestehendes Konto (gleiche E-Mail):** Nicht OpenRouter-spezifisch geklaert. Die analoge **n8n**-Integration
  (gleiches Auto-Provisioning) sagt laut Firecrawl-Blog explizit: „If you already have a Firecrawl account, a
  new **team** ... will be created on it and the promotional credits will be applied there." → plausibel auch
  bei OpenRouter (neues Team auf bestehendem Konto + Credits dort), aber nicht hart belegt. **Definitiv:
  Firecrawl-Dashboard pruefen** (Credit-Stand / neues Team). `extern`/Analogie
- **Credit-Kosten ueber OpenRouter:** **2 Credits pro 10 Ergebnisse + 5 Credits pro gescraptem Ergebnis**
  (1 Basis-Scrape + 4 fuer Highlights); OpenRouter erhebt KEINE Zusatzgebuehr — Verbrauch laeuft direkt ueber
  die Firecrawl-Credits. `offiziell`
- **Konsequenz fuer die Pipeline:** `or-research.py engine=firecrawl` nutzt diese Credits (BYOK, Volltext-Quellen);
  `engine=parallel` (Default) nutzt OpenRouter-pay-per-use (~$0.005/Suche) OHNE Firecrawl-Credits. Bei reichlich
  Firecrawl-Credits (Promo aktiv) ist `engine=firecrawl` eine Volltext-Alternative; sonst `parallel` zur Credit-Schonung.

---

## 6. Endpoint-Wahl, Credit-Kosten & neue Spar-Formate (v2, recherchiert 2026-06-20)

**Endpoints (7+):** `/scrape` (1 URL → Markdown/JSON), `/search` (Suche + scrapt Top-N), `/crawl` (folgt
Links), `/map` (nur URLs), `/agent` (autonom, prompt-basiert), `/interact` (Klicks/Formulare), `/browser`
(Sandbox, bis 20 Sessions), `/parse` (PDF/Doc bis 50 MB, Rust, ~5× schneller), `/monitor` (Change-Detection,
nur Deltas). **Wahl:** URL bekannt → `/scrape`; Thema → `/search`; viele verlinkte Seiten → `/crawl`
(mit `limit`+`include_paths`!); unbekannt wo → `/agent`.

**Credit-Kosten (Budget):** Scrape **1**/Seite · Scrape mit JSON-Extraktion **5**/Seite (1+4) · Search
**2 pro 10 Treffer** (+1/gescrapter Seite) · Crawl 1/Seite (Default-Limit **10.000**!) · Browser 2/Min ·
JS-Rendering ~**5×** · Premium-Proxy **10–25×**. Failed Requests i.d.R. credit-frei.

**Spar-Formate (neu 2026):** **Question** (NL-Frage → grounded Antwort, bis **100× weniger Tokens**),
**Highlights** (nur passende Saetze/Zeilen), **`/monitor`** (nur Deltas, bis 90 % weniger),
**`onlyCleanContent`** (Nav/Ads/Cookie-Banner raus), **Lockdown** (`lockdown:true` = cache-only, kein
Outbound, Zero Data Retention). Zusaetzlich: nur benoetigte `formats` anfordern (nicht markdown+html+screenshot),
`Field(description=...)` praezise (bessere Extraktion → weniger Re-Runs).

**Team/Key:** `fc-`-Keys sind **team-scoped** — welches Team (= welches Guthaben) genutzt wird, entscheidet
der Key. Pruefen: `GET https://api.firecrawl.dev/v2/team/credit-usage` (Bearer) → `remainingCredits`.

---

## 7. Neu in v2.11.0 (gemeldet 2026-06-25) — zwei nutzbare Spar-/Qualitaets-Hebel

**Research Index** (`offiziell`, v2.11.0) — spezialisierter Index ueber **3 Mio+ arXiv-Papers + den
zugehoerigen GitHub-Code** (Issues, gemergte PRs, READMEs, taeglich aktualisiert). Endpunkte:
`GET /v2/search/research/papers`, `/papers/:id`, `/papers/:id/similar`, `/github`. Laut Firecrawl
**SOTA-Recall auf arXivQA (+18 % ggue. naechstbestem Anbieter)**, **2 Credits / 10 Treffer**
(ZDR-Teams 10/10). → **Fuer Paper-lastige Recherchen** (Direktiven-Recherche, Superintelligenz,
Paper-Auswertung) die treffsicherere + guenstigere Wahl als normale Websuche. Der alte
`/v2/research/*`-Mount bleibt als deprecated Alias.

**`deterministicJson`-Format** (`offiziell`, v2.11.0) — strukturiertes JSON **ohne LLM pro Request**:
Firecrawl baut einen wiederverwendbaren Extractor fuer das Schema und cacht ihn pro Site → wiederholte
Scrapes sind **guenstiger und konsistent** (kein Modell-Jitter). Nutzung: `formats:[{"type":"deterministicJson","schema":...}]`
(NICHT mit `json` kombinierbar). → Fuer **wiederkehrendes** Struktur-Scraping derselben Seiten dem
LLM-`json` (5 Credits/Seite) vorziehen.

**Wichtig fuer unsere Pipeline:** `mm-research.py` nutzt weiter den stabilen **`/v1/search`**-Endpoint
(`fc.get("data")`). v2.11.0 strukturiert `/v2/search`-Ergebnisse in `.web/.news/.images` um — ein
v2-Umstieg waere ein **Breaking Change** (siehe Almanach Kurzcheck #11). Guthaben-Stand 2026-06-25:
**9.827 Credits** (OpenRouter-Promo aktiv).

## 🔗 Bezug zum Bug-Almanach

| Best-Practice-Abschnitt (diese Datei) | Bug-Almanach-Abschnitt (`bugs/apis/firecrawl.md`) |
|---|---|
| §1 Credit-Schonung, §2 Parallelitaet | Kurzcheck #1, #3; §1 Free-Plan |
| §3 Saubere Pipeline, §4 Python-Aufruf | Kurzcheck #2, #6; §2, §3 |
| §6 Endpoint-Wahl und Credit-Kosten | Kurzcheck #5, #7, #8, #9; §3b |
| §7 Research Index / deterministicJson | Kurzcheck #11, #12; §3c |

---

## Quellen
- docs.firecrawl.dev/rate-limits, firecrawl.dev/pricing `offiziell`
- Firecrawl v2.11.0-Changelog (von Frank gemeldet 2026-06-25) `offiziell`; Live-Test 2026-06-25 (v1 aktiv, 9.827 Credits)
- openrouter.ai/docs/guides/features/plugins/web-search, firecrawl.dev/blog/firecrawl-search-openrouter, firecrawl.dev/blog/firecrawl-n8n-partnership `offiziell` (recherchiert 2026-06-20 via mm-research + or-research)
- eesel.ai/blog/firecrawl-pricing, costbench.com (Free-Plan 2026) `extern`
- eigener Live-Test 2026-06-20 (`mm-research.py`)
