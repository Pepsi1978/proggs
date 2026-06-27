# OpenRouter API (Integration) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Dieser Kurzcheck ist die Vorab-Pflichtlektüre
> (Stufe A, `Read` mit `limit=80`). Der Volltext darunter ist Pflicht bei JEDEM Fehler in
> diesem Bereich (Stufe B). Der Kurzcheck ersetzt den Volltext nicht.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | Antwort erfolgreich aber `content` leer ⭐ | `HTTP-Referer` + `X-Title` im Client-Konstruktor immer senden | §A |
| 2 | Schwankende Qualität / anderes Backend ⭐ | `provider.order` + `allow_fallbacks:false` für Determinismus | §B |
| 3 | Garbled/CJK-Output | `provider.quantizations:["fp16","bf16"]` als Allowlist | §B |
| 4 | `response_format`/`stop` wirkt nicht | `provider.require_parameters:true` setzen | §B |
| 5 | SSE-Parser bricht ⭐ | `:`-Kommentarzeilen überspringen, `[DONE]` + Mid-Stream-`error` (HTTP 200) | §D |
| 6 | 429 / 402 | `:free`=50 RPD; 402 ≠ 429; `error.metadata.provider_name` auswerten | §E/§F |
| 7 | Modell-String / 404 | `anbieter/modell`, Liste via `/api/v1/models`, Fallback-`models`-Array | §F |
| 8 | Claude Code anbinden ⭐ | Base-URL `…/api` (NICHT `/api/v1`!), `ANTHROPIC_API_KEY=""`, Key in `ANTHROPIC_AUTH_TOKEN`; Anthropic 1P als Top-Provider | §G |
| 9 | CLI-Agent Modell-String | LiteLLM-Tools `openrouter/<v>/<m>`, Eigenbau nacktes `<v>/<m>`; Cursor braucht `/cursor`-Suffix | §G |
| 10 | Caching/Reasoning/neu | Auto-Caching nur Anthropic 1P; `provider.order` killt Sticky-Routing; `reasoning_details` unverändert zurück; `:nitro`≠Latenz | §H |
| 11 | **OpenCode** ⭐ | Login = `/connect` (kein `opencode auth login`/`OPENROUTER_API_KEY`-Env); `:free`/`:`-IDs crashen + oft kein Tool-Use; Routing pro Modell unter `options.provider` | §I |
| 12 | **Server-Tools / PDF** ⭐ | 6 Server-Tools (web_search/web_fetch/datetime/image_generation/apply_patch[Responses]/fusion); web_fetch-Engine `openrouter`=gratis; `file-parser`-OCR kostet auch bei BYOK, `native` ohne `annotations`; non-streaming web_search → Keep-Alive bricht JSON (§D9) | §35 |
| 13 | **`web_search` unter PARALLELITÄT → leer/kaputt** ⭐ | Last-Fehler (intermittent): bei vielen gleichzeitigen Requests leere Response/JSONDecodeError/Timeout/Provider-Routing-Müll — NICHT modellspezifisch (M3 läuft einzeln sauber). FIX: max 2 parallel (Continuous-Spawning) + Retry+Backoff + alle Exceptions fangen (`or-research.py` #47034/35) | §41 |
| 14 | **`:online` liefert keine Antwort, nur JSON-Tool-Calls** ⭐ | `<modell>:online` leakt agentische FOLGE-Suchen als Text (`{"name": "web_search", "input": {"query": …}}`) statt sie auszuführen → KEINE Antwort. Auch ohne Last. Detektor MUSS JSON-Tool-Call-Shape erkennen (Regex `\{"name":\s*"\w+",\s*"(input\|arguments)":`), nicht nur XML-Marker. Bei Mehrfachsuche-Themen lieber `web_search`-Server-Tool/Firecrawl | §42 |
