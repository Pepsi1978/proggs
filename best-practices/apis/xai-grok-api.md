# xAI Grok API — Best Practices (Stand 2026-07-02)

> Gegenstück zu `bugs/apis/xai-grok-api.md`. Offiziell empfohlen (Quellen). (Researcher-Recherche 2026-06-09; Re-Recherche 2026-07-02.)

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Kurzcheck = Stufe-A-Pflichtlektüre
> (`Read` mit `limit=80`). Volltext bei Fehlern im Bereich (Stufe B) und vor
> Hochrisiko-Arbeit (Stufe C).

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Modellwahl | `grok-4.3` explizit pinnen, nie retired Slugs | §1 |
| 2 | Denktiefe | `reasoning_effort` gilt jetzt auch für 4.3 (`none/low/medium/high`) | §2 |
| 3 | Structured Outputs | `json_schema`; Limits 2048 Zeichen/256 Items/64 Props, kein zirkulärer `$ref` | §3 |
| 4 | Tools | Tool-Args immer strict → Schema einfach halten; `messages` pflegen | §4 |
| 5 | Live Search | Nur EIN Domain-Filter (≤5); Verbrauch überwachen | §5 |
| 6 | Streaming | Bei Reasoning Timeout hochsetzen; für Tools/Output `stream=false` | §6 |
| 7 | Rate-Limits | Eigenes Exp-Backoff; 429-Body zwischen Rate- und Credit-Limit unterscheiden | §7 |
| 8 | SDK & Auth | OpenAI-`/v1`; direkte Slugs ohne `xai/`, Gateway-Präfixe separat | §8 |

## 1. Modellwahl & Pinning
- Standard fuer Chat/Coding ist `grok-4.3` (1M Kontext, $1.25/$2.50 pro 1M In/Out) — explizit pinnen, nie auf retired Slugs verlassen. Quelle: https://docs.x.ai/developers/models · offiziell
- Aktuelle Familie: `grok-4.3` (kein Reasoning-Flag noetig, steuerbar), `grok-4.20-0309-reasoning`/`-non-reasoning`, `grok-4.20-multi-agent-0309` (Research/Orchestrierung), `grok-build-0.1` (256k, guenstiger). Wissensstand Nov 2024 — fuer Aktuelles Live Search nutzen. Quelle: https://docs.x.ai/developers/models · offiziell

## 2. reasoning_effort (WICHTIGE Aktualisierung)
- `reasoning_effort` gilt jetzt AUCH fuer `grok-4.3` mit `none/low/medium/high`, Default `low`. (Aenderung gegenueber alter Almanach-Annahme „nur mini-Modelle".) Quelle: https://docs.x.ai/developers/model-capabilities/text/reasoning · offiziell
- `high` fuer Mathe/Mehrschritt-Logik, `none` nur bei reiner Geschwindigkeit. Reasoning-Tokens werden wie Output berechnet; Timeout bei Reasoning explizit hochsetzen. `presencePenalty`, `frequencyPenalty` und `stop` bei Reasoning-Modellen weglassen. Quelle: https://docs.x.ai/developers/model-capabilities/text/reasoning · offiziell

## 3. Structured Outputs
- `response_format` mit `type:"json_schema"` fuer garantierte Schema-Konformitaet; nur Grok-4-Familie. Schema per Pydantic/Zod, JSON Schema Draft 2020-12 bevorzugt (Draft-07 akzeptiert). Quelle: https://docs.x.ai/developers/model-capabilities/text/structured-outputs · offiziell
- Limits: max 2048 Zeichen/String, 256 Array-Items, 64 Properties. `additionalProperties` explizit setzen, keine zirkulären `$ref`, `anyOf`/`oneOf` statt komplexem `allOf`. Regex-`pattern` nur im dokumentierten ECMAScript-Subset nutzen; Lookaround, Backreferences und Unicode-Properties vermeiden. Quelle: https://docs.x.ai/developers/model-capabilities/text/structured-outputs · offiziell

## 4. Function Calling / Tools
- Tool-Argumente sind IMMER strict (`strict` implizit `true`) → gleiche Schema-Regeln wie structured outputs. Tool-Parameter-Schemas entsprechend einfach halten. Quelle: https://docs.x.ai/docs/guides/function-calling · offiziell
- Multi-Turn: `messages`-Array pflegen — User-Input, Assistant-Antwort und Tool-Ergebnisse anhaengen, damit Kontext ueber Turns erhalten bleibt. Quelle: https://docs.x.ai/cookbook/examples/multi_turn_conversation · offiziell

## 5. Live Search (Web-Search-Tool)
- Aktivierung ueber `tools`-Parameter (xAI-SDK, OpenAI Responses API, Vercel AI SDK). Nur EIN Domain-Filter pro Request: `allowed_domains` ODER `excluded_domains`, je max 5 Domains — nie beide zusammen. Quelle: https://docs.x.ai/docs/guides/tools/overview · offiziell
- Bilder: `enable_image_understanding` (Bildanalyse via `view_image`) und `enable_image_search` getrennt; Zitate kommen automatisch. Verbrauch überwachen (Kosten pro Quelle nicht klar dokumentiert). Das alte `search_parameters` nicht mehr verwenden; auf `web_search`/`x_search` im Responses-Endpoint migrieren. Quelle: https://docs.x.ai/developers/tools/web-search · offiziell

## 6. Streaming (SSE)
- `stream:true` fuer alle Text-Modelle (SSE-Deltas). Bei Reasoning-Modellen Request-Timeout manuell hochsetzen (Beispiele bis 3600s), sonst vorzeitiger Verbindungsabbruch. Quelle: https://docs.x.ai/docs/guides/streaming-response · offiziell
- Fuer structured outputs / Tool-Calling sicherheitshalber `stream=false` (Streaming-Doku deckt response_format/Tools nicht ab); sonst Deltas akkumulieren + upstream validieren. Quelle: https://docs.x.ai/docs/guides/streaming-response · offiziell

## 7. Rate Limits & Token-Budget
- Getrennte RPM/TPM pro Modell nach Tier (kumulativer Spend seit 01.01.2026, Tier 0 frei bis Tier 4 ab $5000; Tiers downgraden nie). 429 → eigenes Exponential-Backoff (kein dokumentierter `retry-after`-Header). Quelle: https://docs.x.ai/docs/key-information/consumption-and-rate-limits · offiziell
- In TPM zählen Prompt-, Completion-, Reasoning- UND gecachte Prompt-Tokens (letztere zu reduziertem Preis abgerechnet, aber zählen mit) → Reasoning-Budget einplanen. Bei 429 immer Body lesen: Rate-Limit backoffen, Credit-/Spend-Limit an Benutzer/Console melden. Quelle: https://docs.x.ai/docs/key-information/consumption-and-rate-limits · offiziell

## 8. SDK & Auth
- Drei gleichberechtigte Wege: natives `xai-sdk` (Python), OpenAI-SDK mit `base_url=https://api.x.ai/v1`, `@ai-sdk/xai` (JS). Anthropic-Pfad NICHT mehr — deprecatet. Quelle: https://docs.x.ai/developers/quickstart · offiziell
- Key via console.x.ai erzeugen, als `XAI_API_KEY` exportieren; Header `Authorization: Bearer xai-...` exakt (Whitespace trimmen). Direkte xAI-API nutzt native Modell-Slugs ohne `xai/`; Gateway-/Aggregator-Präfixe (`x-ai/...`) nicht auf die direkte API übertragen. Quelle: https://console.x.ai/team/default/api-keys · offiziell

## 🔗 Bezug zum Bug-Almanach
| Best-Practice | Bug-Abschnitt (`bugs/apis/xai-grok-api.md`) |
|---|---|
| 1 Modellwahl & Pinning | 1 |
| 2 reasoning_effort | 2 (KORRIGIERT: gilt jetzt auch grok-4.3) |
| 3 Structured Outputs | 7, 9 |
| 4 Function Calling / Tools | 8 |
| 5 Live Search | 15 |
| 6 Streaming | 3, 4, 5, 6 |
| 7 Rate Limits & Token-Budget | 10, 10a, 11, 12 |
| 8 SDK & Auth | 13, 14, 18, 19 |
