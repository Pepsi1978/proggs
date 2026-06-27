# Firecrawl (Web-Scrape/Search-API) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

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
