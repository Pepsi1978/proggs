# Bekannte Bugs: OpenAI API (Integration in eigene Software)

> PFLICHT-LESEN vor Arbeit an einer OpenAI-API-Integration (Client-seitig: eigener Code ruft die API).
> Stand: zuletzt recherchiert am 2026-06-08, **re-recherchiert am 2026-07-02** (Engine A: Firecrawl+MiniMax). Versions-Anker: API-Stand Juni 2026 (Responses API +
> Chat Completions parallel, GPT-5.x aktuell, GPT-4o/4.1/o4-mini in der API ~16./17.02.2026 retired).
> Zweite Seite (wie macht man es richtig): `best-practices/apis/openai-api.md`.

> **Update 2026-07-02:** Keine neuen belastbaren OpenAI-API-Aenderungen/Deprecations seit 2026-06-08 in den Quellen gefunden. Bereits dokumentierte GPT-5-/Structured-Output-/Reasoning-Fallen bleiben relevant: hohe Reasoning-Token-Kosten, leere Outputs bei zu kleinem Budget und malformed Structured Outputs in aelteren Bugreports.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Dieser Kurzcheck ist die Vorab-Pflichtlektüre
> (Stufe A, `Read` mit `limit=80`). Der Volltext darunter ist Pflicht bei JEDEM Fehler in
> diesem Bereich (Stufe B). Der Kurzcheck ersetzt den Volltext nicht.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | 404 `model_not_found` | Modellname aus Config, Deprecations-Seite prüfen (4o/4.1/o4-mini retired) | H22 |
| 2 | reasoning-Modell (o-Reihe/gpt-5) | Kein `temperature`/`top_p`; `max_completion_tokens`; `developer`-Rolle | A1, B5, B6 |
| 3 | `status: incomplete` / leere Antwort | Token-Budget großzügig, reasoning-Tokens zählen mit | A2 |
| 4 | Streaming Tool-Args | Pro `index` akkumulieren, erst nach Stream-Ende parsen | C8, C9 |
| 5 | Streaming-Parser crasht | `data: [DONE]` ist kein JSON; abfangen vor Parse | C11 |
| 6 | `usage` ist null beim Streaming | `stream_options.include_usage=true` setzen | C10 |
| 7 | 400 nach `tool_calls` | Für jede `tool_call_id` eine `tool`-Antwort anhängen | D12, D13 |
| 8 | 429 Rate-Limit | `retry-after-ms`/`x-ratelimit-*` lesen, Backoff mit Jitter, RPM≠TPM | F17, F18 |
| 9 | `strict:true`-Schema | `additionalProperties:false` + alle Keys `required`, `refusal` prüfen | G19, G21 |

---

## A) Chat Completions vs. Responses API (Migration)

### 1. `max_tokens` von reasoning-Modellen abgelehnt
- **Symptom:** 400 `Unsupported parameter: 'max_tokens' is not supported with this model. Use 'max_completion_tokens' instead.`
- **Ursache:** reasoning-Modelle (o-Reihe, gpt-5) ersetzten `max_tokens` durch `max_completion_tokens`; Responses API nennt es `max_output_tokens`.
- **Versionen:** o1/o3/o4-mini, gpt-5.x — per Design.
- **FIX:** Modellabhängig mappen — alte GPT-4 → `max_tokens`; reasoning + Chat Completions → `max_completion_tokens`; Responses API → `max_output_tokens`.
- **Quelle:** https://community.openai.com/t/why-was-max-tokens-changed-to-max-completion-tokens/938077 · https://developers.openai.com/api/docs/guides/migrate-to-responses

### 2. `max_completion_tokens` begrenzt NICHT die reasoning-Tokens → leere/incomplete Antwort ⭐
- **Symptom:** Antwort mit `status: incomplete` und leerem sichtbarem Output, Abrechnung höher als erwartet.
- **Ursache:** Das Limit deckt sichtbaren Output + reasoning-Tokens ab; zu niedrig → ganzes Budget geht ins Reasoning.
- **Versionen:** alle reasoning-Modelle — per Design.
- **FIX:** Limit großzügig wählen, `usage.completion_tokens_details.reasoning_tokens` auswerten, bei `incomplete` mit höherem Limit erneut anfragen.
- **Quelle:** https://platform.openai.com/docs/guides/reasoning

### 3. `previous_response_id` billt den Vorkontext weiter (Fehlannahme „gratis")
- **Symptom:** Höhere Input-Token-Kosten beim Verketten von Responses.
- **Ursache:** `previous_response_id` lädt den vorherigen Verlauf erneut als Input.
- **Versionen:** Responses API — per Design.
- **FIX:** Kosten einplanen oder bei langen Ketten nur nötigen Kontext mitführen.
- **Quelle:** https://developers.openai.com/api/docs/guides/migrate-to-responses

### 4. Chat-Completions-Streaming-Parser bricht an der Responses API
- **Symptom:** Streaming „liefert nichts"/falsche Felder nach Migration.
- **Ursache:** Responses API streamt typisierte Events (`response.output_text.delta`, `response.function_call_arguments.delta`, …) statt `choices[].delta`.
- **FIX:** Event-basierten Parser schreiben, der auf `type` jedes Events schaltet.
- **Quelle:** https://community.openai.com/t/responses-api-streaming-the-simple-guide-to-events/1363122

---

## B) Reasoning-Modelle (o1/o3/o4/gpt-5-Reihe)

### 5. `temperature`/`top_p` nicht unterstützt — teils ungewollt vom Wrapper gesendet
- **Symptom:** 400 `Unsupported parameter: 'temperature' is not supported with this model.` — auch wenn man es nicht selbst gesetzt hat.
- **Ursache:** reasoning-Modelle akzeptieren kein `temperature`/`top_p`; manche Default-Layer/CLIs senden es mit.
- **Versionen:** o1, o1-mini, o3-mini, gpt-5 — per Design. (Der CLIENT-Bug, der es ungewollt sendet: openai-python #2072 ist **CLOSED/fixed** 2025-02-03.)
- **FIX:** Bei reasoning-Modellen `temperature`/`top_p` komplett weglassen (nicht auf 1.0 setzen); sicherstellen, dass kein Default-Layer sie injiziert.
- **Quelle:** https://community.openai.com/t/o3-mini-unsupported-parameter-temperature/1140846 · https://github.com/openai/openai-python/issues/2072

### 6. `system` vs. `developer`-Rolle inkonsistent zwischen Modellen ⭐
- **Symptom:** `Unsupported value: 'messages[0].role' does not support 'system'` ODER umgekehrt `does not support 'developer'`.
- **Ursache:** neue reasoning-Modelle erwarten `developer`; o1-mini WEDER `system` NOCH `developer`; o1 (voll) akzeptiert `system`. Modell-/snapshot-abhängig.
- **Versionen:** o1/o1-mini/o3-mini (Anfang 2025) — per Design.
- **FIX:** Modellabhängig mappen — GPT-4 → `system`; neue reasoning → `developer`; o1-mini → Instruktion in erste `user`-Message. Bei 400 „role not supported" auf die andere Rolle zurückfallen.
- **Quelle:** https://community.openai.com/t/developer-role-not-accepted-for-o1-o1-mini-o3-mini/1110750 · https://github.com/openai/openai-dotnet/issues/330

### 7. o3-mini: erzwungenes `tool_choice` bricht beim Streaming
- **Symptom:** „Versuch" der Funktion landet als Plaintext in `delta.content` statt in `tool_calls`; `parallel_tool_calls` greift teils nicht.
- **Versionen:** o3-mini.
- **FIX:** bei o3-mini ohne erzwungenes `tool_choice` (auto) oder nicht-streamend; defensiv auch `delta.content` auf Tool-Argument-Fragmente prüfen.
- **Quelle:** https://community.openai.com/t/o3-mini-tool-choice-not-working-during-streaming-also-parallel-tool-calls-just-doesnt-work-with-this-model/1113520

---

## C) Streaming (SSE)

### 8. Tool-Call-Argumente kommen in JSON-Fragmenten ⭐
- **Symptom:** `JSON.parse()` eines einzelnen Delta-Chunks wirft / liefert Teil-Objekt.
- **Ursache:** Function-Call-Argumente werden als Deltas gestreamt.
- **FIX:** Argument-Strings pro Tool-Call-`index` akkumulieren, erst nach Stream-Ende (vollständiges JSON) parsen. NIE einzelne Chunks parsen.
- **Quelle:** https://developers.openai.com/api/reference/resources/chat/subresources/completions/streaming-events

### 9. `function.name` kommt in einem späteren Chunk als die Argumente
- **Symptom:** Tool-Call wird verworfen/falsch zusammengesetzt.
- **Ursache:** Felder eines Tool-Calls verteilen sich über mehrere Deltas.
- **FIX:** Tool-Calls nach `index`/`id` buffern, erst emittieren wenn Name + vollständige Argumente da sind.

### 10. `usage` nur im letzten Chunk und nur mit `stream_options.include_usage=true`
- **Symptom:** `usage` ist `null`/fehlt beim Streaming.
- **FIX:** `stream_options: {"include_usage": true}` setzen, `usage` aus letztem Chunk lesen.

### 11. `data: [DONE]`-Marker ist kein JSON
- **Symptom:** Parser crasht am Stream-Ende.
- **Ursache:** Chat Completions sendet als letzte SSE-Zeile wörtlich `data: [DONE]` (Sentinel). (Responses API nutzt typisierte Completion-Events.)
- **FIX:** vor JSON-Parse auf Literal `[DONE]` prüfen.

---

## D) Function / Tool Calling

### 12. Erzwungenes `tool_choice` deaktiviert parallele Calls
- **Symptom:** erwartete parallele Tool-Calls bleiben aus, sobald man eine Funktion erzwingt.
- **Ursache:** `tool_choice: {type:"function", function:{name:...}}` macht diese Funktion zum einzigen Call. Kein „MUSS X, DARF auch andere".
- **FIX:** für Parallelität `tool_choice:"auto"`/`"required"` (ohne festen Namen).
- **Quelle:** https://community.openai.com/t/giving-a-value-to-tool-choice-no-longer-allows-functions-to-be-called-in-parallel/623433

### 13. Fehlende `tool`-Antwort nach `tool_calls` → 400 im Folge-Request
- **Symptom:** 400, Konversation invalide.
- **Ursache:** auf jeden `tool_call` muss eine `role:"tool"`-Message mit passendem `tool_call_id` folgen — bei parallelen Calls für JEDE ID eine.
- **FIX:** für jeden zurückgegebenen `tool_call_id` genau eine `tool`-Message anhängen.

### 14. Azure: `parallel_tool_calls` zeitweise nicht unterstützt
- **FIX:** auf Azure passende `api-version`; Parameter nicht voraussetzen.
- **Quelle:** https://github.com/Azure/azure-rest-api-specs/issues/29545

---

## E) Auth (401)

### 15. `mismatched_project` — `OpenAI-Project`-Header passt nicht zum Key
- **Symptom:** 401 `mismatched_project`.
- **Ursache:** Projekt-scoped Key (`sk-proj-…`) + falscher `OpenAI-Project`-Header.
- **FIX:** Header weglassen (Key trägt sein Projekt selbst) oder exakt auf das Key-Projekt setzen.

### 16. 401 `Incorrect API key provided` — Org/Projekt-Mismatch, Whitespace
- **Ursache:** Tippfehler/Leerzeichen; Key gehört zu anderer Org/Projekt; falscher/fehlender `OpenAI-Organization`-Header bei Multi-Org.
- **FIX:** Key trimmen, Org/Projekt verifizieren, korrekten `OpenAI-Organization`-Header senden, `Authorization: Bearer <key>` exakt.
- **Quelle:** https://help.openai.com/en/articles/6882433-incorrect-api-key-provided

---

## F) Rate Limits (429)

### 17. 429 ohne Header-Auswertung → Thundering Herd / Endlos-Retry
- **Ursache:** auch fehlgeschlagene Requests zählen aufs Minuten-Limit; reines Sofort-Retry verschärft; Backoff ohne Jitter synchronisiert Retry-Wellen.
- **FIX:** `retry-after-ms` (auf 429) bevorzugt; sonst exponential Backoff MIT Jitter (`random(0, exp_delay)`); `x-ratelimit-remaining-*`/`-reset-*` proaktiv lesen.
- **Quelle:** https://cookbook.openai.com/examples/how_to_handle_rate_limits

### 18. TPM vs. RPM verwechselt → falsche Drossel-Strategie
- **FIX:** Limit-Typ aus Fehlermeldung/Headern bestimmen; bei TPM Batchgröße/Promptlänge senken, bei RPM Frequenz senken.

---

## G) Structured Outputs / JSON-Mode / `response_format`

### 19. `strict:true` verlangt `additionalProperties:false` + ALLE Properties in `required`
- **Symptom:** 400 „additionalProperties must be false when strict is true" / „all properties must be required".
- **FIX:** in JEDEM Objekt `additionalProperties:false`; alle Keys in `required`; optionale Felder als nullable (`"null"` im Typ) modellieren.
- **Quelle:** https://developers.openai.com/api/docs/guides/structured-outputs

### 20. `pattern`/`format`/`minLength`/`minimum` werden NICHT erzwungen
- **FIX:** Constraints zusätzlich client-seitig validieren und ggf. nachfordern.

### 21. Refusals — `refusal`-Feld statt schema-konformem Inhalt
- **FIX:** vor dem Parsen `message.refusal` prüfen und separat behandeln.

---

## H) Modell-Deprecations / Datums-Snapshots

### 22. gpt-4o / gpt-4.1 / o4-mini API-Retire Mitte Februar 2026 → GPT-5.x ⭐
- **Symptom:** nach Stichtag 404 `model_not_found` für hartkodierte alte Namen.
- **Ursache:** OpenAI retired gpt-4o, gpt-4.1(-mini), o4-mini in der API ~16./17.02.2026; `chatgpt-4o-latest` ab 17.02.2026 entfernt.
- **FIX:** Modellname konfigurierbar; auf GPT-5.1-Reihe migrieren; datierte Snapshots pinnen ABER Retire-Daten beachten; Deprecations-Seite prüfen.
- **Quelle:** https://developers.openai.com/api/docs/deprecations · https://openai.com/index/retiring-gpt-4o-and-older-models/

---

## I) SDK-Fallen (openai-python / openai-node)

### 23. Default-Timeout/Retries → hängende Requests / doppelter Backoff
- **Symptom:** `APITimeoutError`; Client „hängt"; `Retrying request to /chat/completions`.
- **Ursache:** SDK hat eigene Default-Timeouts + automatische Retries.
- **FIX:** `OpenAI(timeout=..., max_retries=...)` explizit; für Streaming/Reasoning großzügige Read-Timeouts (`httpx.Timeout(connect=…, read=…)`); `max_retries=0` wenn man Retry selbst steuert.
- **Quelle:** https://github.com/openai/openai-python/issues/1134

### 24. `base_url`-Override für kompatible Endpunkte — Pfad-/Versions-Fallen
- **FIX:** `base_url` inkl. Versionspfad setzen; Azure v1 erwartet `…/openai/v1`.

---

## J) Azure OpenAI — Unterschiede

### 25. `model` = Deployment-Name (nicht Modellname) + `api-version` Pflicht (Legacy)
- **Symptom:** `DeploymentNotFound`/404.
- **FIX:** `model` = Deployment-Name; bei Legacy-API `api-version` mitgeben. Neue v1-GA: `base_url` `…/openai/v1`, `api-version` nicht mehr nötig.
- **Quelle:** https://learn.microsoft.com/en-us/azure/foundry/openai/api-version-lifecycle

### 26. Azure: `developer`-Rolle abgelehnt (nur system/assistant/user/function/tool)
- **FIX:** aktuelle `api-version`, sonst `system` statt `developer` (modellabhängig mappen wie Bug 6).

---

## Fix-Status (Stand 2026-06-08)

| Frueherer Bug | Status | Bezug |
|---------------|--------|-------|
| CLI sendet `temperature`/`top_p` an o3-mini | **gefixt** (openai-python #2072, CLOSED 2025-02-03) | Bug 5 — der CLIENT-Bug; die Modell-Restriktion selbst bleibt per Design |
| gpt-4o/4.1/o4-mini in der API | **retired** ~16./17.02.2026 | Bug 22 — nicht „gefixt", sondern entfernt → migrieren |

**Noch NICHT gefixt / per Design (Workaround bleibt aktiv):** reasoning-Modelle ohne `temperature`/`top_p` (Bug 5), `system`/`developer`-Split (6), reasoning-Tokens im Output-Budget (2), Tool-Args-Fragmentierung (8), `[DONE]`-Sentinel (11), `tool_choice`-blockt-Parallelität (12), strict-Schema-Regeln (19–21).

**Ehrlichkeits-Hinweis:** Numerische Tier-/RPM-/TPM-Schwellen sind account-spezifisch und nicht fest dokumentierbar — nur das Header-Auswerte-Pattern (17/18) ist stabil.

---

## Pflicht-Checkliste vor OpenAI-Integration

- [ ] Modellname aus Config (nicht hartkodiert)? Deprecations-Seite geprüft?
- [ ] Parameter modellabhängig gemappt (reasoning: kein temperature, max_completion_tokens, developer-Rolle)?
- [ ] `max_completion_tokens`/`max_output_tokens` großzügig + `incomplete`-Check?
- [ ] Streaming: Tool-Args pro index akkumuliert, `[DONE]` abgefangen, `include_usage` gesetzt?
- [ ] Für jeden `tool_call_id` eine `tool`-Antwort?
- [ ] 429: `retry-after-ms`/`x-ratelimit-*` + Backoff mit Jitter?
- [ ] strict-Schema: `additionalProperties:false` + alle Keys `required`, `refusal` geprüft?
- [ ] SDK-Timeouts/Retries explizit (kein doppelter Backoff)?

## 🔗 Bezug zu Best Practices

Zweite Seite der Medaille (wie man es richtig macht): `best-practices/apis/openai-api.md`. Die dortige Mapping-Tabelle „🔗 Bezug zum Bug-Almanach“ verlinkt jede Best-Practice zurueck auf die hier dokumentierten Bug-Abschnitte (bidirektional, ohne Duplikation).


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
- [openrouter-api](openrouter-api.md)
- [other-llm-apis](other-llm-apis.md)
- [xai-grok-api](xai-grok-api.md)
