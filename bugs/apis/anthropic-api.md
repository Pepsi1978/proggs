# Bekannte Bugs: Anthropic Claude API (Messages API, Integration)

> PFLICHT-LESEN vor Arbeit an einer Anthropic-Claude-API-Integration (Client-seitig).
> Stand: zuletzt recherchiert am 2026-06-08, **re-recherchiert am 2026-07-02** (Engine A: Firecrawl+MiniMax). Versions-Anker: `anthropic-version: 2023-06-01` (weiterhin
> Pflichtwert), aktuelle Modelle Opus 4.8/Sonnet 5. Zweite Seite: `best-practices/apis/anthropic-api.md`.

> **Update 2026-07-02:** Keine belegten neuen Messages-/Prompt-Caching-/Tool-Use-/Batch-API-Bugs seit 2026-06-08 gefunden. Bestehende Migrationsfallen bleiben relevant: Prefill wird bei aktuellen Claude-Modellen nicht unterstuetzt, Adaptive Thinking ersetzt manuelle `budget_tokens` bei neueren Modellen, und Sampling-Parameter koennen bei Opus 4.7+ 400-Fehler ausloesen.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Dieser Kurzcheck ist die Vorab-Pflichtlektüre
> (Stufe A, `Read` mit `limit=80`). Der Volltext darunter ist Pflicht bei JEDEM Fehler in
> diesem Bereich (Stufe B). Der Kurzcheck ersetzt den Volltext nicht.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | Direkter HTTP-Call | `x-api-key` (nicht Bearer) + `anthropic-version: 2023-06-01` | §1, §2 |
| 2 | 400 Messages-Struktur | `system` Top-Level, `max_tokens` Pflicht, Rollen alternieren | §3, §4, §5 |
| 3 | 400 tool_use/tool_result | Paarweise + ID-gematcht; beim Compacten nie nur eine Seite | §7 |
| 4 | Streaming Tool-Args | `input_json_delta` erst bei `content_block_stop` parsen | §9 |
| 5 | Fehler nach HTTP-200 | SSE-`error`-Event behandeln, auf `message_stop` warten | §10, §11 |
| 6 | Prompt Caching greift nicht | TTL 5 Min, Mindest-Tokens, byte-identischer Prefix | §14, §15, §16 |
| 7 | Multi-Turn mit thinking | thinking-Blocks unverändert 1:1 in History zurück | §20 |
| 8 | 429/529 | `anthropic-ratelimit-*`+`retry-after` lesen, 529≠500≠504 | §21, §22 |
| 9 | Doppelte Retries / Hangs | SDK `max_retries=2`; bei eigener Retry-Logik `max_retries=0` | §26 |

---

## Auth & Header

### 1. `anthropic-version`-Header fehlt → „header is required"
- **Symptom:** Request schlägt fehl, auch bei korrektem Body.
- **Ursache:** Pflicht bei direktem HTTP (nicht via SDK). Proxies/Gateways vergessen ihn oft.
- **Versionen:** per Design.
- **FIX:** immer `anthropic-version: 2023-06-01` mitschicken.
- **Quelle:** https://platform.claude.com/docs/en/api/versioning

### 2. Falscher Auth-Header (`Authorization: Bearer`) → 401
- **Ursache:** Anthropic nutzt `x-api-key: <key>`, NICHT Bearer.
- **FIX:** `x-api-key`-Header. (401 kann auch Tippfehler/abgelaufen sein.)
- **Quelle:** https://platform.claude.com/docs/en/api/errors

---

## Messages-Struktur

### 3. `system` als Message-Rolle statt Top-Level → 400 ⭐
- **Ursache:** anders als OpenAI ist `system` ein Top-Level-Parameter (`system: "..."` oder Array von Text-Blocks), kein Eintrag im `messages`-Array.
- **FIX:** `system` aus `messages` herausziehen.
- **Quelle:** https://docs.anthropic.com/en/api/messages

### 4. Nicht-alternierende / doppelte gleiche Rollen → 400
- **Ursache:** zwei `user`- oder zwei `assistant`-Messages hintereinander verboten; oft durch Context-Compaction.
- **FIX:** gleiche Rollen zu einer Message mit mehreren content-Blocks zusammenfassen; erste Message = `user`.
- **Quelle:** https://github.com/anthropics/claude-code/issues/5662

### 5. `max_tokens` fehlt → 400
- **Ursache:** Pflichtfeld (anders als OpenAI optional).
- **FIX:** immer setzen.

### 6. Prefill (assistant-Message am Ende) bei Opus 4.x/Sonnet 4.6 → 400
- **Symptom:** `Prefilling assistant messages is not supported for this model.`
- **Versionen:** Opus 4.8/4.7/4.6, Sonnet 4.6 — per Design.
- **FIX:** stattdessen Structured Outputs / `output_config.format` / System-Prompt-Anweisung.

---

## Tool Use

### 7. Orphan/fehlendes `tool_result` → 400 ⭐
- **Symptom:** „tool_use blocks without corresponding tool_result" oder umgekehrt.
- **Ursache:** jeder `tool_use` braucht ein `tool_result` mit passender ID in der nächsten user-Message. Entsteht durch Compaction, die nur eine Seite löscht.
- **FIX:** tool_use/tool_result paarweise + ID-gematcht halten; bei Verlust eines Partners BEIDE entfernen, nie nur eine Seite kappen.
- **Quelle:** https://github.com/anthropics/claude-code/issues/18947

### 8. Beta-Header von 3rd-Party-Gateway gedroppt → 400 bei Tool-Calls
- **Ursache:** Client schickt Beta-Felder + `anthropic-beta`-Header; Gateway leitet Body durch, droppt Header → API sieht unbekannte Felder.
- **FIX:** Gateway so konfigurieren, dass `anthropic-beta` weitergeleitet wird.

---

## Streaming (SSE)

### 9. `input_json_delta` zu früh geparst → JSONDecodeError ⭐
- **Ursache:** `tool_use`-Block startet mit `input: {}`; echte Args kommen als `input_json_delta`-Fragmente (`partial_json`). Erst nach `content_block_stop` vollständig.
- **FIX:** Fragmente konkatenieren, ERST bei `content_block_stop` parsen.
- **Quelle:** https://docs.anthropic.com/en/docs/build-with-claude/streaming

### 10. Fehler NACH HTTP-200 mitten im Stream
- **Symptom:** 200 erhalten, dann `overloaded_error`/`error`-Event; Standard-Error-Handling greift nicht.
- **FIX:** SSE-`event: error` explizit parsen und behandeln; nicht nur HTTP-Status prüfen.

### 11. Stream endet ohne `message_stop` (Connection-Closed)
- **Symptom:** Antwort unvollständig, SDK hängt.
- **Ursache:** vorzeitiger Connection-Close, oft bei großen Tool-JSON-Payloads.
- **FIX:** Idle-Timeout/Watchdog; bei fehlendem `message_stop` Request neu starten; auf vollständiges `message_stop` vor „fertig" warten.
- **Quelle:** https://github.com/anthropics/anthropic-sdk-typescript/issues/842

### 12. Ping-Events vom SDK still gedroppt → „stalled" nicht erkennbar
- **Ursache:** SSE emittiert `ping`-Events (~15–30 s) als Liveness-Signal; TS-SDK reichte sie nicht durch.
- **Versionen:** anthropic-sdk-typescript — **gefixt** (Ping-aware Watchdog, Issue #998 CLOSED 2026-04-27). Auf aktuelle SDK-Version updaten.
- **FIX:** SDK aktualisieren; bei eigenem Parser Ping als Lebenszeichen werten.
- **Quelle:** https://github.com/anthropics/anthropic-sdk-typescript/issues/998

### 13. `text_delta` auf offenem `tool_use`-Block (Proxy-Bug) → SDK crasht
- **Symptom:** „Content block is not a text block".
- **Ursache:** manche Proxies (z. B. sglang) emittieren `text_delta` mit gleichem Index wie offener `tool_use`-Block. Spec: `text_delta` nur für text, `input_json_delta` nur für tool_use.
- **FIX:** korrekten Block-Typ je Delta-Index sicherstellen (relevant bei selbstgebauten/Proxy-SSE-Servern).

---

## Prompt Caching

### 14. Cache-TTL still von 1 h auf 5 Min reduziert → stille Regression ⭐
- **Symptom:** früher funktionierende Caches missen plötzlich (Calls >5 Min auseinander).
- **FIX:** optional 1h-TTL anfordern; Call-Intervalle <5 Min halten.
- **Quelle:** https://platform.claude.com/docs/en/build-with-claude/prompt-caching

### 15. Unter Mindest-Token-Grenze → Cache still ignoriert (voller Preis)
- **Ursache:** min. 1024 Tokens (Opus/Sonnet) bzw. 2048 (Haiku) pro gecachtem Block.
- **FIX:** Block-Größe prüfen; `usage.cache_creation_input_tokens`/`cache_read_input_tokens` monitoren.

### 16. Prefix-Drift durch kleinste Änderung → Cache-Miss
- **Ursache:** Cache trifft nur bei BYTE-identischem Prefix. Umsortierte `tools`, geänderte `tool_choice`, Bild im Prompt invalidieren.
- **FIX:** deterministische Reihenfolge für tools/Dokumente; Cacheable-Prefix unverändert lassen.

### 17. Falsch platzierter `cache_control`-Breakpoint → falscher Cache, voller Preis
- **Ursache:** `cache_control` cacht ALLES davor; falsche Stelle cacht das Falsche, schlägt nicht fehl.
- **FIX:** Breakpoint ans Ende des stabilen Prefix (System+Tools+statischer Kontext).

---

## Extended Thinking / Reasoning

### 18. `budget_tokens` < 1024 → 400
- **FIX:** ≥ 1024; bei 1024 starten.

### 19. thinking + großes `max_tokens` ohne Streaming → Timeout/ValueError
- **Ursache:** SDK verlangt Streaming wenn `max_tokens` > 21.333.
- **FIX:** bei großem Budget/max_tokens streamen.

### 20. thinking-Block bei Multi-Turn nicht unverändert zurückgeschickt ⭐
- **Ursache:** `thinking`/`redacted_thinking`-Blocks (kommen VOR text-Blocks) müssen beim letzten assistant-Turn KOMPLETT und UNVERÄNDERT zurück.
- **FIX:** thinking-Blocks 1:1 in die History übernehmen, nicht editieren/zusammenfassen.

---

## Rate Limits & Fehler-Codes

### 21. 429 `rate_limit_error` / `anthropic-ratelimit-*` ignoriert
- **Ursache:** RPM/ITPM/OTPM pro Tier; bei Usage-Spike auch Acceleration-Limit.
- **FIX:** `anthropic-ratelimit-*` + `retry-after` lesen, Backoff, Traffic graduell hochfahren. Token-Counting hat SEPARATE Limits.

### 22. 529 `overloaded_error` (oft mit 500/504 verwechselt)
- **Ursache:** API global überlastet (nicht dein Konto).
- **FIX:** Retry mit Backoff; 500=intern, 504=Timeout (→ streamen), 529=overloaded — unterschiedlich behandeln.

### 23. 413 `request_too_large` (32 MB) — von Cloudflare, nicht der API
- **FIX:** Payload (Bilder/Docs) verkleinern. Messages/Token-Counting 32 MB, Batch 256 MB, Files 500 MB.

### 24. 402/403/404 — Billing / Permission / falsches Modell/Endpoint
- **FIX:** 402=Billing, 403=Key-Permission, 404=falsches Modell oder `/v1`-Pfad vergessen. `request_id`-Header für Support mitschicken.

---

## SDK-Fallen

### 25. Default-Timeout 10 Min — non-streaming darüber wirft ValueError
- **FIX:** `.stream()` + `get_final_message()`/`finalMessage()` nutzen; bei direkter Integration TCP-Keep-Alive setzen.

### 26. Stacking-Retries: SDK `max_retries=2` + eigene Retries → bis 12 Requests, stille Hangs ⭐
- **Ursache:** SDK retryt 2× default (Connection-Err, 408, 409, 429, ≥500). Eigene Retry-Schicht stackt: (2+1)×(3+1)=12.
- **FIX:** bei eigener Retry-Logik `max_retries=0` am Client setzen.

### 27. `Retry-After`-Cap-Deadlock bei rate-limited Accounts
- **FIX:** Server-`retry-after` respektieren statt clientseitig zu kappen.

---

## Batch API & Token Counting

### 28. Batch-Partial-Results & 29-Tage-Verfall
- **FIX:** auf Partial-Failure designen; Results innerhalb 29 Tagen herunterladen; pro Request Fehler einzeln behandeln. (24h-SLA-Hard-Timeout.)

### 29. Token-Counting-Endpoint hat eigene RPM-Limits
- **FIX:** Counting-Aufrufe drosseln/cachen, nicht mit Message-Budget vermischen.

### 30. Beta-Feature wird GA / ändert sich → `anthropic-beta`-Header bricht
- **Ursache:** Beta-Namen `feature-name-YYYY-MM-DD`; nach GA kann der Header obsolet/fehlerhaft werden.
- **FIX:** Beta-Header aktuell halten; nach GA entfernen; Gateways müssen ihn durchreichen.

---

## Fix-Status (Stand 2026-06-08)

| Frueherer Bug | Status | Bezug |
|---------------|--------|-------|
| Ping-Events vom TS-SDK gedroppt | **gefixt** (anthropic-sdk-typescript #998, CLOSED 2026-04-27) | Bug 12 — SDK updaten |

**Noch NICHT gefixt / per Design:** `x-api-key`-Header (2), `system` Top-Level (3), `tool_use`/`tool_result`-Paarung (7), `input_json_delta`-Parsing (9), Cache-TTL 5 Min + Prefix-Regeln (14–17), thinking-Block-Echo (20), SDK-Retry-Stacking (26).

**Ehrlichkeits-Hinweis:** Zu Breaking-Changes zwischen `anthropic-version`-Datumswerten gibt es keine offizielle Detailliste — die Doku verweist nur auf die generische Versioning-Policy.

---

## Pflicht-Checkliste vor Anthropic-Integration

- [ ] `x-api-key` + `anthropic-version: 2023-06-01` gesetzt?
- [ ] `system` als Top-Level, `max_tokens` gesetzt, Rollen alternieren?
- [ ] tool_use/tool_result paarweise + ID-gematcht (auch beim Compacten)?
- [ ] Streaming: `input_json_delta` erst bei `content_block_stop` geparst, `error`-Event behandelt, auf `message_stop` gewartet?
- [ ] Prompt Caching: TTL/Mindest-Tokens/Prefix-Stabilität beachtet, `usage`-Cache-Felder monitort?
- [ ] thinking-Blocks unverändert in History echoed?
- [ ] 429/529 unterschieden, `anthropic-ratelimit-*` gelesen, SDK `max_retries` nicht doppelt?

## 🔗 Bezug zu Best Practices

Zweite Seite der Medaille (wie man es richtig macht): `best-practices/apis/anthropic-api.md`. Die dortige Mapping-Tabelle „🔗 Bezug zum Bug-Almanach“ verlinkt jede Best-Practice zurueck auf die hier dokumentierten Bug-Abschnitte (bidirektional, ohne Duplikation).


---

<!-- verwandte-almanache (auto, bug-almanac-system) -->
## 🔗 Verwandte Almanache (siehe auch)

Diese Bereiche ueberschneiden sich mit diesem Almanach — bei Arbeit hier oft ebenfalls relevant. Wird einer von ihnen vom bug-almanac-guard getriggert, lohnt sich meist ein Blick (mindestens Kurzcheck) auch hier:

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
- [xai-grok-api](xai-grok-api.md)
