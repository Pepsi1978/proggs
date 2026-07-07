# Bekannte Bugs & Fallen: Firecrawl (Web-Scrape/Search-API)

> **PFLICHT-Lesen vor jeder Firecrawl-Nutzung** (Recherche-Pipeline, `mm-research.py`, Firecrawl-MCP).
> Fokussiert auf **Free-Plan-Limits + Integration** (verifiziert 2026-06-20 via WebSearch +
> Live-Test). Bewusst NICHT erschoepfend — eine vollstaendige `bug-almanach-recherche` zu Firecrawl
> kann spaeter folgen (mit Franks OK). Loesungen funktionserhaltend.
>
> **Stand:** 2026-06-20, v2.11.0-Abgleich 2026-06-25. Quellen: docs.firecrawl.dev/rate-limits,
> firecrawl.dev/pricing, eigener Live-curl/urllib-Test 2026-06-20 + 2026-06-25 (V1 noch aktiv,
> Guthaben 9.827 Credits). **Anker:** firecrawl-free=1000-credits/mo; **API-Version unserer Pipeline = v1**.
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
| 7 | ⭐ `/crawl` ohne `limit` | Default = **10.000 Seiten** (1 Credit/Seite) = Kostenexplosion → IMMER `limit` klein + `include_paths` (Glob) setzen, sonst folgt der Crawler ALLEN Domain-Links. |
| 8 | JSON-Extraktion | `formats:[{"type":"json","schema":...}]` — **nackter String `"json"` schlaegt fehl**. `doc.json` = Plain-Dict (nicht Pydantic) → Validierung in `try/except`. Scrape+JSON = **5 Credits/Seite**. |
| 9 | `/search`-Treffer ohne Content | Kann als `SearchResultWeb` zurueckkommen (Scrape fehlgeschlagen) → vor Attribut-Zugriff `hasattr(doc,'metadata')` pruefen (sonst `AttributeError`). |
| 10 | Key = Team | Jedes Firecrawl-**Team** hat einen eigenen `fc-`-Key; welches Team angesteuert wird, entscheidet der Key. Team/Guthaben pruefen: `GET /v2/team/credit-usage` (Bearer). |
| 11 | ⭐ V1→V2-Migrationsfalle (`/search`) | Unsere Pipeline nutzt `/v1/search` → flaches `data`-Array (`fc.get("data")`). **In `/v2/search` ist `data` in `.web`/`.news`/`.images` aufgeteilt** → `fc.get("data")` waere dort **leer, ohne Fehler** (stille 0-Quellen). Endpoint NIE blind von v1 auf v2 heben. (v2.11.0, 2026-06-25.) |
| 12 | `pii`-Format entfernt | v2 lehnt `"pii"` in `formats` jetzt **aktiv ab** (HTTP-Fehler); ersetzt durch `redactPII` (bool/Objekt). Unsere Pipeline nutzt nur `["markdown"]` → **nicht betroffen**. Nur relevant, falls jemand `pii` einbaut. |

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

## 3b. Weitere v2-API-Fallen (recherchiert 2026-06-20, via mm-research/Live-Test)

- **`/crawl` Default-Limit = 10.000 Seiten → Kostenexplosion** (1 Credit/Seite). IMMER `limit` klein +
  `include_paths` (Glob) setzen, sonst folgt der Crawler ALLEN Links der Domain.
- **`formats: "json"` als nackter String schlaegt fehl** → Dict-Form `{"type":"json","schema":...}` (oder `"prompt"`).
  `doc.json` kommt als **Plain-Dict** (nicht Pydantic-Instanz) zurueck → Validierung in `try/except`. Scrape mit
  JSON-Extraktion kostet **5 Credits/Seite** (1 Basis + 4 Extraktion).
- **`/search`-Treffer kann ohne Content zurueckkommen** (`SearchResultWeb`, wenn der Scrape fehlschlug) →
  vor Attribut-Zugriff `hasattr(doc,'metadata')` pruefen (sonst `AttributeError`).
- **Vage `Field(description=...)`** degradiert die JSON-Extraktion (Beschreibungen gehen ans LLM) → praezise beschreiben.
- **Concurrency „intentionally limited"** — bei >Hunderttausenden Seiten/Monat Bottleneck; Success-Rate ~96 % bei Scale (Retries kosten extra).
- **Kosten-Multiplikatoren:** JS-Rendering ~5×, Premium-Proxy 10–25×, Browser-Sandbox 2 Credits/Min. Failed Requests i.d.R. credit-frei, Retries nicht.
- **Key = Team:** `fc-`-Keys sind team-scoped; mm-research nutzt den Key in der SK-Datei. Team/Guthaben: `GET /v2/team/credit-usage` (Bearer) → `remainingCredits`/`planCredits`.

(Voller Rechercheauszug verlustfrei in `~/.mm-research/answer.json` zum Recherche-Zeitpunkt.)

## 3c. Firecrawl v2.11.0 (gemeldet 2026-06-25) — was uns betrifft, was nicht

Frank meldete das v2.11.0-Changelog mit der Frage, ob es die gehaeuften Such-Fehlschlaege erklaert.
**Ergebnis des Abgleichs gegen unsere Pipeline (`mm-research.py` → `/v1/search`, `formats:["markdown"]`):**

| Changelog-Punkt | Betrifft uns? | Detail |
|-----------------|---------------|--------|
| **`pii`-Format → `redactPII`** (alte `pii` jetzt *rejected*) | **Nein** | Wir senden nur `["markdown"]`. Falle nur, wenn jemand `pii` einbaut. |
| **`/search` `.data` → `.web/.news/.images`** (SDK + v2) | **Latent (Migrationsfalle)** | Gilt fuer **v2**. Unsere v1-`fc.get("data")` laeuft weiter; bei v2-Umstieg waere `data` leer → 0 Quellen ohne Fehler. Siehe Kurzcheck #11. |
| **Keyless access** fuer `/scrape`,`/search`,`/interact`,`/parse` (offizielle MCP/CLI/SDK) | Nein (neutral) | Wir nutzen weiter unseren Bearer-Key (team-scoped, Guthaben zaehlt). |
| **Scrape-Worker stalls bei grossen LLM-Extractor-Inputs gefixt** (vorher dropped jobs / worker restarts) | Nein | Betrifft nur `json`/Extractor-Scrapes. Wir scrapen Markdown ohne Extractor. |
| **`deterministicJson`-Format** (JSON ohne LLM/Request, pro Site gecacht) | Chance | Guenstigeres wiederholtes Struktur-Scraping — Best-Practices §7. |
| **Research Index** (`/v2/search/research/papers`, `/github`) | Chance | arXiv/Paper-Recherche, SOTA arXiv-Recall — Best-Practices §7. |
| **PDF-Cap 30→50 MB**, Monitor-Verbesserungen, Security-Bumps (axios/esbuild/ws/openssl) | Nein | Keine Pipeline-Relevanz. |

**Warum die Suchen NICHT am Update lagen (live verifiziert 2026-06-25):**
- `/v1/search` und `/v1/team/credit-usage` antworten beide normal → **V1 ist nicht abgeschaltet**.
- Guthaben **9.827 Credits** (OpenRouter-Promo) → **keine Credit-Erschoepfung**.
- Wahrscheinliche echte Ursache frueherer Fehlschlaege: der dokumentierte *research-contamination*-Bug
  (parallele Laeufe ueberschrieben sich `sources.json`/`answer.json` → abgeschnittene/leere Ergebnisse),
  bereits per `MM_OUTDIR`-Trennung in `mm-research.py` (Z. 34-36) gefixt. NICHT Firecrawl.

**Lehre:** Bei Such-Fehlschlaegen zuerst lokal pruefen (OUTDIR-Kollision, Free-Limit 2 concurrent,
Guthaben via `GET /v2/team/credit-usage`) — ein Anbieter-Changelog ist selten die Ursache, solange
wir auf dem stabilen **v1**-Endpoint bleiben. **Quelle:** Franks v2.11.0-Changelog + Live-Test 2026-06-25.

## 🔗 Bezug zur Best-Practices-Seite

`best-practices/apis/firecrawl.md` — wie man Firecrawl von vornherein richtig + sparsam nutzt
(2-parallel-Muster, search-vs-scrape-Wahl, Credit-Budgetierung, mm-research.py-Pipeline).

---

## Quellen
- docs.firecrawl.dev/rate-limits (Free: 2 concurrent, 5 search/min, 10 scrape/min, 1 crawl/min)
- firecrawl.dev/pricing (Free: 1000 Credits/Monat)
- eesel.ai/blog/firecrawl-pricing, costbench.com/software/web-scraping/firecrawl/free-plan (2026)
- eigener Live-Test 2026-06-20 (urllib-UA / Cloudflare 1010)
- Firecrawl v2.11.0-Changelog (von Frank gemeldet 2026-06-25) + Live-Test 2026-06-25 (v1 aktiv, 9.827 Credits)
