# Bekannte Bugs: Google Gemini API (Integration)

> PFLICHT-LESEN vor Arbeit an einer Gemini-API-Integration (Client-seitig).
> Stand: zuletzt recherchiert am 2026-06-08, **re-recherchiert am 2026-07-02** (Engine A: Firecrawl+MiniMax). Versions-Anker: NEUES einheitliches SDK `google-genai`
> (altes `google-generativeai`/`@google/generative-ai` deprecated seit 30.11.2025), Modelle Gemini
> 2.5/3.x. Zweite Seite: `best-practices/apis/google-gemini-api.md`.

> **Update 2026-07-02:** Keine neuen belastbaren `google-genai`-/`thinkingConfig`-/`finishReason`-Bugs seit 2026-06-08 gefunden. Bestaetigt bleibt der Deprecation-/Shutdown-Druck: alte 1.0/1.5- und Preview-Modelle sind weg; weitere 2.5-/Embedding-Varianten laufen 2026 aus. Modell-IDs weiter pinnen und Deprecations vor Releases pruefen.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Dieser Kurzcheck ist die Vorab-Pflichtlektüre
> (Stufe A, `Read` mit `limit=80`). Der Volltext darunter ist Pflicht bei JEDEM Fehler in
> diesem Bereich (Stufe B). Der Kurzcheck ersetzt den Volltext nicht.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | ⭐ 200 OK + leerer Text / Crash | Vor `response.text`: blockReason + finishReason | §D10, §B5 |
| 2 | ⭐ `finishReason: MAX_TOKENS`, leer | Thinking frisst Budget — `maxOutputTokens` hoch | §B4 |
| 3 | SDK-Init bricht | Neues `google-genai`, `genai.Client(...)` | §A1, §A2 |
| 4 | 400 INVALID_ARGUMENT bei Tools | Schema auf OpenAPI-Subset, Nesting ≤ 32 | §E11, §E12 |
| 5 | ⭐ 404 NOT_FOUND auf Modell | IDs pinnen (nicht `-latest`); Deprecations | §G17, §G18 |
| 6 | 403 PERMISSION_DENIED trotz Key | Billing + API aktivieren, Key→Projekt | §C6 |
| 7 | Streaming liefert Muell | `?alt=sse` anhaengen, zeilenweise parsen | §I21 |
| 8 | API-Key uebergeben | Header `x-goog-api-key`, nie `?key=` Query | §C8 |

---

## A. SDK-Wechsel (deprecated → google-genai)

### 1. Altes SDK ist tot
- **Symptom:** neue Features (Live API, thinkingConfig) fehlen, keine Bugfixes.
- **Ursache:** `google-generativeai` (Py) + `@google/generative-ai` (JS) deprecated seit 30.11.2025.
- **FIX:** auf `google-genai`/`@google/genai` migrieren (GA seit Mai 2025); Go: `google.golang.org/genai`.
- **Quelle:** https://ai.google.dev/gemini-api/docs/libraries

### 2. Client-Init geändert
- **Symptom:** Code mit `genai.GenerativeModel(...)`/`vertexai.init()` bricht.
- **FIX:** `client = genai.Client(api_key=...)` bzw. `genai.Client(vertexai=True, project=..., location=...)`. Config-Parsing für function calling/multi-turn/response separat durchgehen.
- **Quelle:** https://docs.cloud.google.com/vertex-ai/generative-ai/docs/deprecations/genai-vertexai-sdk

### 3. Vertex `generative_models` wird entfernt (24.06.2026)
- **FIX:** früh auf `google-genai` mit `vertexai=True` umstellen.

---

## B. Thinking-Tokens / Generation Config (KRITISCH bei 2.5/3)

### 4. Leere Antwort mit `finishReason: MAX_TOKENS` ⭐
- **Symptom:** 200 OK, leerer Text, finishReason MAX_TOKENS.
- **Ursache:** Gemini 2.5/3 Flash haben Thinking DEFAULT AN; thinking-Tokens zählen gegen `maxOutputTokens` (anders als OpenAI). Kleines Limit → Denken frisst alles, 0 Output.
- **Versionen:** gemini-2.5-flash/-pro, gemini-3-*-preview — per Design.
- **FIX:** `generationConfig.thinkingConfig.thinkingBudget = 1024` + `maxOutputTokens = 8192`; wo kein Reasoning nötig: `thinkingBudget: 0`. Im OpenAI-Endpoint `reasoning_effort: "none"` (geht NICHT bei 2.5 Pro / 3).
- **Quelle:** https://github.com/googleapis/python-genai/issues/782 · https://discuss.ai.google.dev/t/finishreason-max-tokens-but-text-is-empty/81874

### 5. Fehlendes/stilles `finishReason` bei Limit
- **Symptom:** Antwort still abgeschnitten, kein finishReason im letzten Chunk.
- **FIX:** IMMER `candidate.finishReason` UND Längenheuristik prüfen; bei MAX_TOKENS als unvollständig behandeln, ggf. mit höherem Budget neu.
- **Quelle:** https://discuss.ai.google.dev/t/gemini-2-5-api-bug-missing-finishreason-when-max-token-limit-is-reached/75837

---

## C. Auth & Quota

### 6. 403 PERMISSION_DENIED trotz neuem Key
- **Ursache:** Billing NICHT aktiviert (auch für Free-Tier nötig) ODER `generativelanguage.googleapis.com` nicht aktiviert ODER Key nicht zum Projekt verknüpft.
- **FIX:** Billing aktivieren, API einschalten, Key dem richtigen Projekt zuordnen.
- **Quelle:** https://discuss.ai.google.dev/t/gemini-api-returns-403-permission-denied-even-with-a-new-paid-project-and-new-api-key/166965

### 7. 429 → eskaliert zu 403
- **FIX:** clientseitiges Rate-Limiting + Backoff; Quota in GCP erhöhen. Free default ~60 RPM/Modell.

### 8. API-Key-Übergabe sicher
- **FIX:** Key per `x-goog-api-key`-Header (nicht `?key=` im Query — landet in Logs/Referer); Quota-Projekt ggf. `x-goog-user-project`.

---

## D. Safety / Content Filtering

### 9. `BLOCK_NONE` blockt trotzdem
- **Ursache:** senkt nur die Schwelle der konfigurierbaren Kategorien; harte, nicht abschaltbare Schicht (CSAM etc.) bleibt.
- **FIX:** akzeptieren, dass manche Prompts nie generiert werden; defensiv behandeln (#10).
- **Quelle:** https://ai.google.dev/gemini-api/docs/safety-settings

### 10. 200 OK + leerer `text` (kein Error) ⭐
- **Symptom:** `response.text` wirft ValueError (Py) / ist undefined (JS).
- **Ursache:** finishReason SAFETY/RECITATION → keine Candidates mit Text.
- **FIX:** vor Textzugriff `promptFeedback.blockReason` + `candidate.finishReason` + `safetyRatings` prüfen; bei Block sauberen Fallback liefern.

---

## E. Function Calling / Tools

### 11. 400 INVALID_ARGUMENT bei Schema
- **Ursache:** nur OpenAPI-Subset. Erlaubt: `type, nullable, required, format, description, properties, items, enum`. NICHT: `default, optional, maximum, oneOf`; `anyOf` z. T. nicht (2.0 Flash).
- **FIX:** Schema auf Subset reduzieren; volle JSON-Schema-Features in `description` beschreiben.
- **Quelle:** https://ai.google.dev/gemini-api/docs/function-calling

### 12. Schema zu groß/tief
- **Ursache:** max Nesting 32, `$defs`-Rekursion auf 2 begrenzt.
- **FIX:** Property-Namen kürzen, Verschachtelung/Anzahl reduzieren.

### 13. FunctionCalling- vs. responseSchema-Divergenz
- **FIX:** Schemas getrennt validieren, nicht 1:1 wiederverwenden.

---

## F. OpenAI-Kompatibilitäts-Endpunkt (`/v1beta/openai/`)

### 14. `/responses` fehlt
- **Ursache:** Kompat bietet nur `/chat/completions`, nicht OpenAIs Responses API.
- **FIX:** auf Chat-Completions-Pfad festlegen.

### 15. `usage` in JEDEM Stream-Chunk (statt nur letztem)
- **FIX:** Client tolerant machen, nur letzten Wert nehmen.

### 16. Reasoning nicht abschaltbar bei 2.5 Pro / 3
- **FIX:** höheres `max_tokens` einplanen; `thinking_config` via `extra_body`.

---

## G. Modellnamen / Deprecations

### 17. 404 NOT_FOUND auf altem Modell ⭐
- **Ursache:** 1.0/1.5 abgeschaltet; 2.0-flash(-lite) Shutdown 01.06.2026.
- **FIX:** auf `gemini-2.5-flash(-lite)` migrieren; 2.5-flash Shutdown 16.10.2026 → ggf. direkt `gemini-3.5-flash`.
- **Quelle:** https://ai.google.dev/gemini-api/docs/deprecations

### 18. Preview-Modelle verschwinden
- **FIX:** in Produktion stabile, versionierte IDs pinnen (nicht `-latest`); Deprecation-Seite überwachen.

---

## H. Multimodal / File API

### 19. Inline-Bild scheitert über Größe
- **FIX:** ab ~20 MB die File API nutzen (bis 2 GB/Datei, 48 h gespeichert), nicht inline base64.
- **Quelle:** https://ai.google.dev/gemini-api/docs/file-input-methods

### 20. Fehlender/falscher `mimeType`
- **FIX:** bei inline-Daten korrekten MIME-Type explizit setzen.

---

## I. Streaming

### 21. SSE vs. JSON-Array ⭐
- **Symptom:** Stream-Parsing liefert Müll/einen Block.
- **Ursache:** `streamGenerateContent` gibt per Default ein fortlaufendes JSON-Array; nur mit `?alt=sse` echte SSE.
- **FIX:** `?alt=sse` anhängen und zeilenweise parsen, sonst JSON-Array-Streaming-Parser.

### 22. Abgeschnittene Streaming-Antwort
- **FIX:** letzten Chunk auf finishReason prüfen, Chunks akkumulieren, bei MAX_TOKENS Budget erhöhen.

---

## Fix-Status (Stand 2026-06-08)

| Frueherer Zustand | Status | Bezug |
|---|---|---|
| `google-generativeai`/`@google/generative-ai` | **deprecated** 30.11.2025 | Bug 1 — auf `google-genai` migrieren |
| Gemini 1.0/1.5 | **abgeschaltet** | Bug 17 |
| Gemini 2.0-flash(-lite) | Shutdown **01.06.2026** | Bug 17 |

**Noch NICHT gefixt / per Design:** Thinking im Output-Budget (4), 200-OK-leerer-Text (10), OpenAPI-Subset-Schema (11), `?alt=sse`-Streaming (21), usage-pro-Chunk im Kompat-Layer (15).

**Ehrlichkeits-Hinweis:** Einige Fix-Details stammen aus GitHub-Issues/Foren der `python-genai`, nicht alle aus offizieller Doku.

---

## Pflicht-Checkliste vor Gemini-Integration

- [ ] Neues `google-genai`-SDK?
- [ ] `thinkingBudget` gesetzt + `maxOutputTokens` großzügig?
- [ ] Vor `response.text` blockReason + finishReason + safetyRatings geprüft (kein blinder Zugriff)?
- [ ] Function-Schema auf OpenAPI-Subset reduziert, Nesting ≤ 32?
- [ ] Modell-ID gepinnt (nicht `-latest`), Deprecations geprüft?
- [ ] Key per Header (nicht Query), Billing aktiviert?
- [ ] Streaming mit `?alt=sse`?

## 🔗 Bezug zu Best Practices

Zweite Seite der Medaille (wie man es richtig macht): `best-practices/apis/google-gemini-api.md`. Die dortige Mapping-Tabelle „🔗 Bezug zum Bug-Almanach“ verlinkt jede Best-Practice zurueck auf die hier dokumentierten Bug-Abschnitte (bidirektional, ohne Duplikation).


---

<!-- verwandte-almanache (auto, bug-almanac-system) -->
## 🔗 Verwandte Almanache (siehe auch)

Diese Bereiche ueberschneiden sich mit diesem Almanach — bei Arbeit hier oft ebenfalls relevant. Wird einer von ihnen vom bug-almanac-guard getriggert, lohnt sich meist ein Blick (mindestens Kurzcheck) auch hier:

- [anthropic-api](anthropic-api.md)
- [api-integration-general](api-integration-general.md)
- [cli-impersonation-subscription-auth](cli-impersonation-subscription-auth.md)
- [deepseek-api](deepseek-api.md)
- [groq-api](groq-api.md)
- [local-openai-compatible](local-openai-compatible.md)
- [mistral-api](mistral-api.md)
- [oauth-device-code](oauth-device-code.md)
- [openai-api](openai-api.md)
- [openrouter-api](openrouter-api.md)
- [other-llm-apis](other-llm-apis.md)
- [xai-grok-api](xai-grok-api.md)
