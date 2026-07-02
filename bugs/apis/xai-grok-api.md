# Bekannte Bugs: xAI Grok API (Integration)

> PFLICHT-LESEN vor Arbeit an einer xAI-Grok-Integration (api.x.ai). Stand: zuletzt recherchiert am 2026-06-08.
> Versions-Anker: aktuell `grok-4.3`; 8 ältere Slugs am 15.05.2026 retired (lösen still auf
> 4.3 um). Zweite Seite: `best-practices/apis/xai-grok-api.md`.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Dieser Kurzcheck ist die Vorab-Pflichtlektüre
> (Stufe A, `Read` mit `limit=80`). Der Volltext darunter ist Pflicht bei JEDEM Fehler in
> diesem Bereich (Stufe B). Der Kurzcheck ersetzt den Volltext nicht.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | Alter Slug läuft fehlerfrei ⭐ | 8 Slugs still auf `grok-4.3` redirected — explizit pinnen, Kosten/Qualität prüfen | §A |
| 2 | `reasoning_effort` setzen | Bewusst `none/low/medium/high`; bei grok-4 nur korrekt für 4.3 | §A |
| 3 | Tools / structured outputs ⭐ | Streaming kann das nicht → `stream=false` | §B/§C |
| 4 | Schema für Tools/Output | Strict-Regeln (kein leeres `enum`/`anyOf`, `prefixItems` statt Array-`items`) | §C |
| 5 | 429 trotz Restbudget ⭐ | Eigenes Exp-Backoff, keine Retry-Header; Reasoning-Tokens in TPM | §D |
| 6 | Endpunkt-Wahl | OpenAI-kompatibler `/v1`, nicht der deprecatete Anthropic-Pfad | §E |
| 7 | Live/Web-Search | Nur EIN Domain-Filter (≤5 Domains), Verbrauch überwachen | §F |

---

## A. Modelle & Deprecations (am kritischsten)

### 1. Stille Modell-Retirement vom 15.05.2026 — Slugs lösen weiter auf ⭐
- **Symptom:** Code mit altem Slug läuft fehlerfrei weiter, aber Rechnung steigt + Output-Qualität ändert sich unbemerkt. KEIN Fehler.
- **Ursache:** 8 Slugs (`grok-3`, `grok-code-fast-1`, `grok-4-0709`, `grok-4-fast-reasoning`, `grok-4-fast-non-reasoning`, `grok-4-1-fast-reasoning`, `grok-4-1-fast-non-reasoning`, `grok-imagine-image-pro`) retired, redirecten still auf `grok-4.3`. `code-fast-1` bucht jetzt $1.25/$2.50 pro 1M; Reasoning-Slugs landen bei `low`, Non-Reasoning bei `none` effort.
- **Versionen:** ab 15.05.2026 12:00 PT.
- **FIX:** `grok-4.3` explizit pinnen + `reasoning_effort` bewusst (`none/low/medium/high`); Eval-Baselines neu; Cost-per-Token-Alerts.
- **Quelle:** https://docs.x.ai/developers/migration/may-15-retirement

### 2. `reasoning_effort` wirft bei grok-4/4.3 einen Error
- **Ursache:** nur `grok-3-mini(-fast)` unterstützen den Parameter.
- **FIX:** `reasoning_effort` nur für mini-Modelle senden; bei grok-4-Familie weglassen.
- **Quelle:** https://docs.oracle.com/en-us/iaas/Content/generative-ai/xai-grok-4.htm

---

## B. Streaming (SSE)

### 3. Streaming unterstützt KEINE `response_format` / tool_calling ⭐
- **Ursache:** laut Doku nur Non-Streaming.
- **FIX:** für structured outputs/Tools `stream=false`; oder Deltas manuell akkumulieren + upstream validieren.
- **Quelle:** https://docs.x.ai/docs/guides/streaming-response

### 4. `usage` in extra leerem Final-Chunk NACH `finish_reason=stop`
- **Ursache:** bei `stream_options:{include_usage:true}` kommt ein zusätzlicher leerer Chunk mit `usage` nach dem stop-Chunk.
- **FIX:** Stream bis zum echten `[DONE]` lesen; finalen leeren Chunk gesondert auf `usage` prüfen.
- **Versionen:** litellm #17136 **CLOSED/fixed** 2026-02-19 (Client-seitig in litellm behoben).

### 5. text + tool_calls in einer Response → tool_calls-Deltas verloren
- **Ursache:** SSE enthält nur `delta.content`, tool_calls-Deltas werden (besonders über Responses↔ChatCompletions-Proxies) nicht geforwarded.
- **FIX:** ChatCompletions-Endpoint direkt nutzen; bei Proxy tool_calls separat aus Output-Array ziehen.

### 6. Responses-API-Streaming inkompatibel mit OpenAI-SDK-State-Machine
- **Symptom:** `RuntimeError` (fehlendes `response.completed`) oder HTTP 400.
- **FIX:** ChatCompletions statt Responses-API, oder natives `xai-sdk`.

---

## C. Structured Outputs / Tool Calling

### 7. Structured outputs nur Grok-4-Familie + Schema-Keyword-Fallen
- **Symptom:** 400 bei bestimmten Schemas; oder „akzeptiert" aber nicht erzwungen.
- **Ursache/Details:** 400-Reject bei leerem `enum`/`anyOf`, `items` als Array (→ `prefixItems`), `maxContains`/`minContains`, Property-Schema `true`/`false`. Nur best-effort (NICHT garantiert): `not`, `if/then/else`, `allOf` mit mehreren Subschemas, Constraints über Limits (>2048 Zeichen, >256 Items, >64 Properties). `additionalProperties` muss explizit `true`; keine zirkulären `$ref`.
- **FIX:** Schema vereinfachen, `prefixItems` statt Array-`items`, Output zusätzlich client-seitig validieren.
- **Quelle:** https://docs.x.ai/developers/model-capabilities/text/structured-outputs

### 8. Tool-Argumente sind IMMER strict
- **Ursache:** für Tool-Args ist `strict` implizit `true` → gleiche Schema-Regeln wie #7.
- **FIX:** Tool-Parameter-Schemas an structured-output-Regeln anpassen.

### 9. `generateObject` schlägt mit grok-4-fast-reasoning fehl (Vercel AI SDK)
- **Versionen:** vercel/ai #9315 **CLOSED/fixed** 2025-11-14; Slug ohnehin seit 15.05.2026 retired.
- **FIX:** auf `grok-4.3` migrieren + Schema gemäß #7.

---

## D. Rate Limits & Kosten

### 10. 429 unabhängig vom Restbudget + keine Retry-Header ⭐
- **Symptom:** 429 trotz nicht ausgeschöpftem Monatsbudget; kein `retry-after`/`x-ratelimit`-Header dokumentiert.
- **Ursache:** RPM/TPM getrennt vom Spending; 6 Tiers nach kumulativem Spend. Reasoning- UND gecachte Tokens zählen in TPM.
- **FIX:** eigenes Exponential-Backoff (1/2/4/8 s), nicht auf Header verlassen; TPM-Budget inkl. Reasoning-Tokens einplanen.
- **Quelle:** https://docs.x.ai/docs/key-information/consumption-and-rate-limits

### 11. Multi-Agent- und Image/Video-Endpunkte mit eigenen niedrigeren Limits
- **FIX:** diese Endpunkte separat drosseln.

### 12. 402 payment_required nach Ablauf der Promo-Credits
- **Ursache:** Promo-Credits laufen 30 Tage nach Account-Erstellung ab.
- **FIX:** rechtzeitig Zahlungsmethode hinterlegen.

---

## E. Auth & Kompatibilität

### 13. 401 invalid api key
- **FIX:** `Authorization: Bearer xai-...` exakt, Whitespace trimmen, bei console.x.ai neu generieren.

### 14. Anthropic-SDK-Kompatibilität ist DEPRECATED
- **Ursache:** xAI deprecatet den `/v1/messages`-Anthropic-Pfad.
- **FIX:** auf OpenAI-kompatiblen `/v1` (`base_url=https://api.x.ai/v1`) oder natives `xai-sdk` migrieren.
- **Quelle:** https://docs.x.ai/docs/guides/migration

---

## F. Live Search / Web Search

### 15. „Live Search" = jetzt Web-Search-Tool — Parameter-Fallen
- **Symptom:** `allowed_domains` + `excluded_domains` zusammen → funktioniert nicht; `enableImageSearch` fehlt im Vercel-AI-SDK.
- **FIX:** nur EINEN Domain-Filter pro Request (je max 5 Domains); für image-search Responses-API/Python-SDK. Kosten/Defaults nicht klar dokumentiert → Verbrauch überwachen.
- **Quelle:** https://docs.x.ai/docs/guides/live-search

---

## Fix-Status (Stand 2026-06-08)

| Frueherer Bug | Status | Bezug |
|---|---|---|
| 8 Grok-Slugs (grok-3, grok-4-fast-* …) | **retired** 15.05.2026 (lösen still auf 4.3 um) | Bug 1 — pinnen + Kosten/Qualität prüfen |
| Grok usage im falschen Stream-Chunk (litellm) | **gefixt** (litellm #17136, CLOSED 2026-02-19) | Bug 4 |
| `generateObject` grok-4-fast-reasoning (Vercel) | **gefixt** (vercel/ai #9315, CLOSED 2025-11-14) | Bug 9 |

**Noch NICHT gefixt / per Design:** `reasoning_effort` nur mini (2), Streaming ohne response_format/Tools (3), strict Schema (7/8), 429 ohne Header (10), Anthropic-Pfad deprecated (14).

**Ehrlichkeits-Hinweis:** Zu Grok gibt es weniger Community-Issues als bei OpenAI; einige Symptome stammen aus Suchsnippets — bei Aufnahme per `gh issue view` verifizieren.

---

## Pflicht-Checkliste vor xAI-Grok-Integration

- [ ] `grok-4.3` explizit gepinnt (kein retired Slug), `reasoning_effort` bewusst gesetzt?
- [ ] Kosten-/Qualitäts-Baseline nach 15.05.2026-Retirement neu erstellt?
- [ ] Für Tools/structured outputs `stream=false`?
- [ ] Eigenes Exponential-Backoff (keine Retry-Header), TPM inkl. Reasoning-Tokens?
- [ ] OpenAI-kompatibler `/v1` (nicht der deprecatete Anthropic-Pfad)?

## 🔗 Bezug zu Best Practices

Zweite Seite der Medaille (wie man es richtig macht): `best-practices/apis/xai-grok-api.md`. Die dortige Mapping-Tabelle „🔗 Bezug zum Bug-Almanach“ verlinkt jede Best-Practice zurueck auf die hier dokumentierten Bug-Abschnitte (bidirektional, ohne Duplikation).


---

<!-- verwandte-almanache (auto, bug-almanac-system) -->
## 🔗 Verwandte Almanache (siehe auch)

Diese Bereiche ueberschneiden sich mit diesem Almanach — bei Arbeit hier oft ebenfalls relevant. Wird einer von ihnen vom bug-almanac-guard getriggert, lohnt sich meist ein Blick (mindestens Kurzcheck) auch hier:

- [anthropic-api](anthropic-api.md)
- [api-integration-general](api-integration-general.md)
- [cli-impersonation-subscription-auth](cli-impersonation-subscription-auth.md)
- [deepseek-api](deepseek-api.md)
- [google-gemini-api](google-gemini-api.md)
- [groq-api](groq-api.md)
- [local-openai-compatible](local-openai-compatible.md)
- [mistral-api](mistral-api.md)
- [oauth-device-code](oauth-device-code.md)
- [openai-api](openai-api.md)
- [openrouter-api](openrouter-api.md)
- [other-llm-apis](other-llm-apis.md)
