# OpenRouter API — Best Practices (Stand 2026-06-09)

> Gegenstueck zu `bugs/apis/openrouter-api.md`. Offiziell empfohlen (Quellen). (Researcher-Recherche 2026-06-09.)

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Kurzcheck = Stufe-A-Pflichtlektüre
> (`Read` mit `limit=80`). Volltext bei Fehlern im Bereich (Stufe B) und vor
> Hochrisiko-Arbeit (Stufe C).

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Attribution | `HTTP-Referer` + `X-Title` im Client-Konstruktor (sonst Content leer) | §1 |
| 2 | Routing | `provider.order` + `allow_fallbacks:false` für Determinismus | §2 |
| 3 | Qualität/Datenschutz | `quantizations`-Allowlist, `data_collection:"deny"` für Compliance | §3 |
| 4 | Kosten/Speed | `:floor`/`:nitro` bzw. `sort`; `max_price` als Preisdeckel | §4 |
| 5 | Modell-Routing | `anbieter/modell`, Liste via `/api/v1/models`, `models`-Fallback-Array | §5 |
| 6 | Streaming/SSE | `:`-Kommentare überspringen, `[DONE]` + Mid-Stream-`error` prüfen | §6 |
| 7 | Tool Calling | OpenAI-Shape; Args pro `index` über Chunks akkumulieren | §7 |
| 8 | Output/Limits | `json_schema` + `strict:true`; `:free`=20 RPM/50 RPD; 402 ≠ 429 | §8 |

## 1. Unified Gateway & Attribution-Header
- Base-URL `https://openrouter.ai/api/v1`, Auth via `Authorization: Bearer <OPENROUTER_API_KEY>`; als OpenAI-Drop-in den Base-URL umstellen, sonst offizielle SDKs (`@openrouter/sdk`, `openrouter`) fuer Typsicherheit. Quelle: https://openrouter.ai/docs/quickstart · offiziell
- `HTTP-Referer` (deine Site) + `X-Title` (App-Name) bei jedem Request mitsenden — aktiviert Leaderboard/Attribution; ohne werden Antworten bei Non-localhost-Keys still leer. Header im Client-Konstruktor (`default_headers`) setzen, nicht per Request-Override. Quelle: https://openrouter.ai/docs/quickstart · offiziell

## 2. Provider-Routing bewusst steuern
- Fuer deterministisches Verhalten `provider.order: ["anthropic","openai"]` + `provider.allow_fallbacks: false` setzen — verhindert stille Substitution auf abweichendes Backend. Provider-Slugs per Copy-Button von der Modell-Seite holen (inkl. Varianten wie `deepinfra/turbo`). Quelle: https://openrouter.ai/docs/guides/routing/provider-selection · offiziell
- `provider.require_parameters: true`, sobald `response_format`, Tools, `stop` o.ae. genutzt werden — routet nur zu Providern, die ALLE Parameter unterstuetzen (sonst werden sie still ignoriert). Quelle: https://openrouter.ai/docs/guides/routing/provider-selection · offiziell
- `provider.only`/`provider.ignore` als Allow-/Blocklist; sie mergen mit den Account-weiten Einstellungen. Quelle: https://openrouter.ai/docs/guides/routing/provider-selection · offiziell

## 3. Qualitaet & Datenschutz absichern
- `provider.quantizations: ["fp16","bf16"]` (oder `fp8`) als Allowlist gegen Garbage-/Encoding-Output durch Int4/FP4-Backends. Quelle: https://openrouter.ai/docs/guides/routing/provider-selection · offiziell
- `provider.data_collection: "deny"` schliesst Provider aus, die Daten speichern/trainieren — fuer Compliance mit `require_parameters: true` kombinieren. Quelle: https://openrouter.ai/docs/guides/routing/provider-selection · offiziell

## 4. Kosten- & Geschwindigkeits-Routing
- `:floor`-Suffix (= guenstigster Provider) fuer kostensensible Produktion mit `allow_fallbacks: true`; `:nitro`-Suffix bzw. `provider.sort: "throughput"` fuer geschwindigkeitskritische Pfade. Beide Suffixe/`sort` deaktivieren das Default-Load-Balancing. Quelle: https://openrouter.ai/docs/guides/routing/provider-selection · offiziell
- `provider.max_price: {prompt: 1, completion: 2}` als Preisdeckel ($/M Token); blockt zu teure Provider, gut mit `sort` kombinierbar. Quelle: https://openrouter.ai/docs/guides/routing/provider-selection · offiziell

## 5. Modell-Routing & Fallback-Array
- Modell-String `anbieter/modell`; Modell-Liste dynamisch via `/api/v1/models` beziehen statt hardcoden (Slugs, besonders `:free`, verschwinden). Quelle: https://openrouter.ai/docs/quickstart · offiziell
- Fuer Resilienz `models`-Array (Fallback-Kette) angeben; `openrouter/auto` (Auto-Router) waehlt bei Tool-Calls per Tool-Call-Error-Rate ("Auto Exacto") zuverlaessige Provider. Quelle: https://openrouter.ai/docs/guides/features/tool-calling · offiziell

## 6. Streaming / SSE robust verarbeiten
- SSE-Kommentarzeilen (`: OPENROUTER PROCESSING`) per Spec ueberspringen, `[DONE]`-Sentinel vor JSON-Parse abfangen; etablierten Parser nutzen (`eventsource-parser`, OpenAI-SDK, Vercel AI SDK). Quelle: https://openrouter.ai/docs/api/reference/streaming · offiziell
- Mid-Stream-Fehler kommen als SSE-Event mit Top-Level-`error` + `finish_reason: "error"` bei HTTP 200 — in jedem Chunk pruefen. Usage-Stats stehen im letzten Chunk (`usage`-Feld). Cancel via AbortController stoppt Abrechnung bei unterstuetzten Providern. Quelle: https://openrouter.ai/docs/api/reference/streaming · offiziell

## 7. Tool Calling
- OpenAI-kompatible Shape: `tools` in JEDEM Request mitsenden; `tool_choice` `"auto"` (Default) statt erzwungen, `parallel_tool_calls: false` fuer sequentielle, abhaengige Aufrufe. Beschreibende Funktionsnamen + ausfuehrliche Descriptions. Quelle: https://openrouter.ai/docs/guides/features/tool-calling · offiziell
- Beim Streaming Tool-Call-Argumente pro `tool_calls[].index` ueber alle Chunks akkumulieren und erst bei `finish_reason: "tool_calls"` parsen. Quelle: https://openrouter.ai/docs/guides/features/tool-calling · offiziell

## 8. Structured Output, Usage-Accounting & Limits
- `response_format: {type: "json_schema", json_schema: {...}}` mit `strict: true`, `additionalProperties: false`, klaren Property-Descriptions + `required`; funktioniert mit `stream: true`. Fuer Non-Streaming Response-Healing-Plugin gegen kaputtes JSON. Quelle: https://openrouter.ai/docs/guides/features/structured-outputs · offiziell
- Usage-Accounting im `usage`-Feld der Response (native Tokenizer = Abrechnungsgrundlage); spaeter via `/api/v1/generation?id=...` auditierbar. `:free` = 20 RPM, 50 RPD (<10 USD gekauft) bzw. 1000 RPD (>=10 USD). Limits/Credits per `GET /api/v1/key` pruefen; 402 (leeres Guthaben) NICHT als 429 retrien. Quelle: https://openrouter.ai/docs/api/reference/limits · offiziell

## 🔗 Bezug zum Bug-Almanach
| Best-Practice | Bug-Abschnitt (`bugs/apis/openrouter-api.md`) |
|---|---|
| 1 Unified Gateway & Attribution-Header | A1, A2 |
| 2 Provider-Routing bewusst steuern | B3, B6 |
| 3 Qualitaet & Datenschutz absichern | B4 |
| 4 Kosten- & Geschwindigkeits-Routing | B5 |
| 5 Modell-Routing & Fallback-Array | C7, F17 |
| 6 Streaming / SSE robust verarbeiten | D9, D10, D11, D12 |
| 7 Tool Calling | C7, C8 |
| 8 Structured Output, Usage-Accounting & Limits | B6, E13, E14, E15, F18 |
