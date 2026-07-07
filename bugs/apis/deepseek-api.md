# Bekannte Bugs: DeepSeek API (Integration)

> PFLICHT-LESEN vor Arbeit an einer DeepSeek-Integration (api.deepseek.com). Stand: zuletzt recherchiert am 2026-06-08, **re-recherchiert am 2026-07-02** (Engine A: Firecrawl+MiniMax).
> Versions-Anker: `deepseek-v4-flash` + `deepseek-v4-pro`; Legacy-Aliase `deepseek-chat`/`deepseek-reasoner`
> Deprecation 2026-07-24, danach V4-Modelle direkt nutzen. Zweite Seite: `best-practices/apis/deepseek-api.md`.
> Compliance-Hinweis: Hosting in China — bei personenbezogenen Daten relevant.

> **Update 2026-07-02:** V4-Flash/Pro, 1M Kontext, 384K Max-Output und Concurrency-Limits (Flash 2500, Pro 500) bleiben bestaetigt. Wichtiger Integrationsbug: LiteLLM strippt bei V4-Thinking-Multiturn/Tool-Calls weiterhin `reasoning_content` und erzeugt 400; Workaround bleibt generationsabhaengig (R1 strippen, V4-Thinking zurueckgeben).

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Dieser Kurzcheck ist die Vorab-Pflichtlektüre
> (Stufe A, `Read` mit `limit=80`). Der Volltext darunter ist Pflicht bei JEDEM Fehler in
> diesem Bereich (Stufe B). Der Kurzcheck ersetzt den Volltext nicht.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | R1/`deepseek-reasoner` History 400 ⭐ | `reasoning_content` aus History strippen | §A |
| 2 | V3.2/V4 Thinking+Tool-Use 400 ⭐ | Umkehr: `reasoning_content` MUSS zurückgeschickt werden | §A |
| 3 | Tool-Calling nötig | `deepseek-chat`/V3 nutzen — reasoner kann kein `tool_choice` (400) | §B |
| 4 | Sampling beim Reasoner | `temperature`/`top_p`/penalties weglassen; `logprobs` wirft Error | §C |
| 5 | Antwort abgeschnitten | `max_tokens` explizit/dynamisch hoch, `finish_reason:length` prüfen | §D |
| 6 | 402 wirkt wie Auth-Fehler | 402 (Guthaben leer) ≠ 401; Backoff bei 429/503 | §E |
| 7 | Streaming Reasoner | `delta.reasoning_content` + `delta.content` getrennt akkumulieren | §F |

---

## A. reasoning_content (die zentrale Falle)

### 1. `reasoning_content` in History → 400 (klassisches R1/`deepseek-reasoner`) ⭐
- **Symptom:** Folge-Request 400, wenn vorige assistant-Nachricht `reasoning_content` enthält.
- **Ursache:** bei `deepseek-reasoner` darf die CoT NICHT in die nächste History. Doku: „if the `reasoning_content` field is included … the API will return a 400 error."
- **FIX:** beim Bauen der History nur `content` zurückschicken, `reasoning_content` strippen (lokal anzeigen/loggen OK).
- **Quelle:** https://api-docs.deepseek.com/guides/reasoning_model

### 2. `reasoning_content` FEHLT bei Tool-Use → 400 (V3.2/V4 Thinking) — GEGENTEIL von Bug 1 ⭐
- **Symptom:** seit V3.2 (~01.12.2025) brechen Tool-Calls mit 400: „The `reasoning_content` in the thinking mode must be passed back".
- **Ursache:** V3.2/V4 integriert Denken in Tool-Use; jede assistant-Nachricht mit `tool_calls` MUSS ihr `reasoning_content` mitschicken.
- **FIX:** bei Thinking-Mode (`thinking:{type:'enabled'}`) alle assistant-Messages mit `reasoning_content` versehen — exaktes echoen wenn vorhanden, sonst `reasoning_content:""`.
- **Quelle:** https://github.com/n8n-io/n8n/issues/22579

### 3. vLLM/Self-Host: `reasoning_content` = null, Denken landet in `content`
- **FIX:** vLLM mit `--enable-reasoning --reasoning-parser deepseek_r1` starten; aktuelle Version.
- **Quelle:** https://github.com/vllm-project/vllm/issues/13375

---

## B. Function / Tool Calling

### 4. `deepseek-reasoner` unterstützt KEIN `tool_choice` → 400 ⭐
- **Symptom:** 400 „deepseek-reasoner does not support this tool_choice"; bricht u. a. WebSearch.
- **Versionen:** `deepseek-reasoner` (R1) — **noch OFFEN** (deepseek-ai/DeepSeek-R1 #836, Stand 2026-06-08 OPEN). `deepseek-chat`/V3 unterstützt es.
- **FIX:** für tool-pflichtige Workflows `deepseek-chat`/V3; bei reasoner `tool_choice` weglassen (Tools im Prompt beschreiben).
- **Quelle:** https://github.com/deepseek-ai/DeepSeek-R1/issues/836

### 5. Structured Output / `with_structured_output` instabil beim Reasoner
- **FIX:** strukturierte Ausgaben über `deepseek-chat`; oder Beta-`strict`-Mode mit `deepseek-chat`.

### 6. `strict`-Mode Schema-Restriktionen (Beta)
- **Ursache:** nicht unterstützt: `minLength`/`maxLength`/`minItems`/`maxItems`. Pflicht: alle Properties `required`, `additionalProperties:false`.
- **FIX:** Schema bereinigen; Längen-Constraints in die Description.

---

## C. Unsupported Parameters (Reasoner)

### 7. Reasoner ignoriert/blockt Sampling-Parameter
- **Ursache:** `temperature`/`top_p`/`presence_penalty`/`frequency_penalty` wirkungslos (nur Warnung); `logprobs`/`top_logprobs` → echter Fehler.
- **FIX:** diese Parameter beim Reasoner nicht senden; Sampling nur bei `deepseek-chat`.

---

## D. Tokens / Kontextfenster

### 8. Niedriger `max_tokens`-Default schneidet Antworten ab
- **Ursache:** chat Default 4k/Max 8k; reasoner Default 32k/Max 64k (CoT zählt mit); Kontext 128k (Input+Output gemeinsam).
- **FIX:** `max_tokens` explizit hochsetzen; auf `finish_reason:"length"` prüfen, ggf. Continuation.

### 9. Statischer `max_tokens`-Default halbiert effektiven Kontext
- **FIX:** `max_tokens` dynamisch = Fenster − geschätzte Input-Tokens − Puffer.

---

## E. Auth / Billing / Verfügbarkeit

### 10. 402 statt 401 bei leerem Prepaid-Guthaben
- **Symptom:** 402 „You have run out of balance" — wirkt wie Auth-Fehler.
- **FIX:** Guthaben aufladen; 402 ≠ 401 behandeln, klare Meldung „Guthaben leer".
- **Quelle:** https://api-docs.deepseek.com/quick_start/error_codes

### 11. 401 bei falschem Key
- **FIX:** Key prüfen, `Authorization: Bearer` exakt. Gleicher Key gilt für `/v1`- UND `/anthropic`-Endpoint.

### 12. 429 / 503 unter Last (keine harten dokumentierten Limits)
- **Ursache:** dynamische Drosselung + Lastausfälle (Peak).
- **FIX:** Backoff bei 429/500/503; Off-Peak nutzen (Rabatte); Fallback-Provider für harte SLAs. NICHT Feature deaktivieren.

---

## F. Streaming / Caching / Kompatibilität

### 13. Streaming: `delta.reasoning_content` vs `delta.content` getrennt akkumulieren
- **Ursache:** reasoner streamt erst CoT (`delta.reasoning_content`), dann Antwort (`delta.content`); beide können null sein.
- **FIX:** beide Felder separat aufsummieren (Null-Guards), `[DONE]` abfangen.

### 14. Context-Caching = best-effort, kein garantierter Hit
- **Ursache:** automatisch (Disk), nur bei exaktem Prefix-Match, kein 100%-Hit.
- **FIX:** stabilen identischen Prefix vorne halten, Variables ans Ende; Hits über `prompt_cache_hit_tokens`/`miss_tokens` messen.

**Zusatz (kein Bug):** `https://api.deepseek.com` und `.../v1` funktionieren beide mit dem OpenAI-SDK (`v1` ≠ Modellversion); Anthropic-kompat unter `/anthropic`; Beta-Features (Prefix-Completion, FIM) am Beta-Endpoint. Deprecation `deepseek-chat`/`-reasoner` am **2026-07-24** → V4-flash/-pro.

---

## Fix-Status (Stand 2026-06-08)

| Frueherer Bug | Status | Bezug |
|---|---|---|
| reasoner ≠ `tool_choice` | **noch OFFEN** (DeepSeek-R1 #836, OPEN) | Bug 4 — Workaround `deepseek-chat` |
| `reasoning_content`-Verhalten | **per Design, umgekehrt je Generation** | Bug 1 vs. 2 — Generation-abhängiger Code |
| `deepseek-chat`/`deepseek-reasoner` | Deprecation **2026-07-24** | → V4-flash/-pro migrieren |

**Noch NICHT gefixt / per Design:** alle reasoner-Parameter-Restriktionen (7), niedrige max_tokens-Defaults (8), 429/503 ohne harte Limits (12), best-effort-Caching (14).

---

## Pflicht-Checkliste vor DeepSeek-Integration

- [ ] `reasoning_content`-Handling nach Generation: R1 → strippen, V3.2/V4-Thinking+Tools → zurückschicken?
- [ ] Für Tools `deepseek-chat`/V3 (nicht reasoner)?
- [ ] Beim Reasoner keine Sampling-Params/logprobs?
- [ ] `max_tokens` explizit (dynamisch), `finish_reason:length`-Check?
- [ ] 402 ≠ 401 unterschieden, Backoff bei 429/503?
- [ ] Streaming: reasoning_content + content getrennt akkumuliert?

## 🔗 Bezug zu Best Practices

Zweite Seite der Medaille (wie man es richtig macht): `best-practices/apis/deepseek-api.md`. Die dortige Mapping-Tabelle „🔗 Bezug zum Bug-Almanach“ verlinkt jede Best-Practice zurueck auf die hier dokumentierten Bug-Abschnitte (bidirektional, ohne Duplikation).


---

<!-- verwandte-almanache (auto, bug-almanac-system) -->
## 🔗 Verwandte Almanache (siehe auch)

Diese Bereiche ueberschneiden sich mit diesem Almanach — bei Arbeit hier oft ebenfalls relevant. Wird einer von ihnen vom bug-almanac-guard getriggert, lohnt sich meist ein Blick (mindestens Kurzcheck) auch hier:

- [anthropic-api](anthropic-api.md)
- [api-integration-general](api-integration-general.md)
- [cli-impersonation-subscription-auth](cli-impersonation-subscription-auth.md)
- [google-gemini-api](google-gemini-api.md)
- [groq-api](groq-api.md)
- [local-openai-compatible](local-openai-compatible.md)
- [mistral-api](mistral-api.md)
- [oauth-device-code](oauth-device-code.md)
- [openai-api](openai-api.md)
- [openrouter-api](openrouter-api.md)
- [other-llm-apis](other-llm-apis.md)
- [xai-grok-api](xai-grok-api.md)
