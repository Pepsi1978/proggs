# Bekannte Bugs: Lokale OpenAI-kompatible LLM-Server (Integration)

> PFLICHT-LESEN vor Arbeit mit lokalen/selbst-gehosteten OpenAI-kompatiblen Servern: **Ollama, LM Studio,
> vLLM, llama.cpp (llama-server), LocalAI, text-generation-webui**. Stand: zuletzt recherchiert am 2026-06-08.
> Zweite Seite: `best-practices/apis/local-openai-compatible.md`.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Dieser Kurzcheck ist die Vorab-Pflichtlektüre
> (Stufe A, `Read` mit `limit=80`). Der Volltext darunter ist Pflicht bei JEDEM Fehler in
> diesem Bereich (Stufe B). Der Kurzcheck ersetzt den Volltext nicht.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | 404 / connection refused ⭐ | `base_url` mit `/v1`; `127.0.0.1` statt `localhost` | §1, §2 |
| 2 | SDK crasht "Missing credentials" ⭐ | API-Key nie leer — Platzhalter `"ollama"` setzen | §4 |
| 3 | Langer Prompt abgeschnitten ⭐ | Ollama ignoriert `num_ctx`: Modelfile/`OLLAMA_CONTEXT_LENGTH` | §5 |
| 4 | Erster Request timeout ⭐ | Timeout ≥ 60 s; Modell vorladen; `keep_alive` bewusst | §8 |
| 5 | vLLM Tool-Calls ignoriert ⭐ | `--enable-auto-tool-choice` UND `--tool-call-parser` | §9 |
| 6 | `logprobs`/`n` fehlen | Ueber `/v1` still verworfen → native API | §14, §15 |
| 7 | "model not found" | Modell pullen/laden; vLLM `--served-model-name`, `/v1/models` | §7, §18 |
| 8 | json_schema+grammar-Fehler | llama.cpp: nur EINS angeben, nicht beides | §13 |

---

## A) base_url / Endpoint-Fallen

### 1. `/v1`-Suffix vergessen → 404 (LM Studio: sogar 200 OK mit Error-Body) ⭐
- **Ursache:** alle Routen unter `/v1`. LM Studio antwortet ohne Suffix mit HTTP 200 + Fehler im Body → Clients interpretieren als Erfolg.
- **FIX:** `base_url` immer mit `/v1`.
- **Quelle:** https://lmstudio.ai/docs/developer/openai-compat

### 2. `localhost` vs `127.0.0.1` (IPv6-`::1`) → connection refused
- **Ursache:** `localhost` löst teils auf IPv6 `::1`, Server bindet nur IPv4.
- **FIX:** explizit `http://127.0.0.1:<port>/v1`; LAN-Zugriff: Server auf `0.0.0.0` binden.
- **Quelle:** https://github.com/block/goose/issues/4318

### 3. `0.0.0.0`-Binding & Firewall für LAN-Zugriff
- **FIX:** Ollama `OLLAMA_HOST=0.0.0.0:11434`; llama.cpp/vLLM `--host 0.0.0.0`; Firewall-Regel.

---

## B) Dummy-API-Key

### 4. Leerer API-Key crasht openai-python (≥ 2.34.0) ⭐
- **Symptom:** `OpenAIError: Missing credentials` beim Instanziieren von `OpenAI(api_key="")`.
- **Ursache:** neue Credential-Validierung (leerer String ist falsy).
- **FIX:** nicht-leeren Platzhalter (`"ollama"`/`"not-needed"`/`"lm-studio"`); lokale Server ignorieren den Wert.
- **Quelle:** https://github.com/openai/openai-python/issues/3224

---

## C) Context-Window / `num_ctx` (gefährlichste stille Falle)

### 5. Ollama `/v1` ignoriert `num_ctx` → stille Truncation auf ~4096 ⭐
- **Symptom:** lange Prompts ohne Fehler abgeschnitten; Modell „vergisst" den Anfang.
- **Ursache:** der OpenAI-kompatible Endpunkt akzeptiert `num_ctx` NICHT im Body (nur native `/api/generate`); Modelfile-Default 2048, Runtime oft 4096.
- **FIX:** pro Modell ein Modelfile mit `PARAMETER num_ctx <wert>` + benanntes Modell; alternativ native API; neuere Ollama: `OLLAMA_CONTEXT_LENGTH` als Server-Default (verifizieren).
- **Quelle:** https://github.com/openclaw/openclaw/issues/4028 · https://www.serverman.co.uk/ai/ollama/ollama-context-window/

### 6. textgen-webui: Kontext wird auf Slots aufgeteilt
- **FIX:** `ctx_size = slots × gewünschter_kontext`.

---

## D) Model-Loading & Cold-Start

### 7. „No model loaded" / „model not found"
- **Ursache:** Modell nicht gepullt/geladen (LM Studio Server-Tab; Ollama `ollama pull`).
- **FIX:** vorher pullen/laden; LM Studio Modellwechsel im laufenden Server möglich.

### 8. Cold-Start-Latenz → Timeout beim ersten Request ⭐
- **Symptom:** erster Request hängt/timeout't (5–30 s), Folge schnell.
- **Ursache:** Modell lädt in RAM/VRAM; Ollama entlädt nach 5 Min Idle (`OLLAMA_KEEP_ALIVE` default `5m`).
- **FIX:** First-Request-Timeout ≥ 60 s; Modell beim App-Start vorladen; `OLLAMA_KEEP_ALIVE=-1`/lange Dauer; Status `curl :11434/api/ps`. ACHTUNG: per-Request-`keep_alive` überschreibt die Env — viele Frontends senden still `"keep_alive":"5m"`.
- **Quelle:** https://vibecodewithkai.substack.com/p/the-ollama_keep_alive-trap-and-why

---

## E) Tool / Function Calling

### 9. vLLM: Tool-Calling braucht zwei Flags, sonst ignoriert ⭐
- **FIX:** `--enable-auto-tool-choice` UND `--tool-call-parser <parser>` (modellabhängig: `hermes`, `llama3_json`, `mistral`).
- **Quelle:** https://docs.vllm.ai/en/latest/features/tool_calling/

### 10. llama.cpp: malformed/incomplete JSON-Argumente bei Tool-Calls
- **FIX:** `--jinja` mit korrektem Chat-Template; Schema via `response_format`/Grammar erzwingen; client-seitig defensiv parsen + Retry.
- **Quelle:** https://github.com/ggml-org/llama.cpp/issues/22072

### 11. llama.cpp `--jinja` fügt System-Message ein → stört Fine-Tunes
- **FIX:** bei Fine-Tunes Template/Tools-Handling prüfen; ggf. eigenes Template ohne Auto-System-Injection.

### 12. Tool-Calling nur für bestimmte Modelle/Templates, uneinheitlich
- **FIX:** nur Modelle mit Tool-Template; Server-Parser zum Modell passend.

---

## F) `response_format` / strukturierte Ausgabe

### 13. llama.cpp: „Either 'json_schema' or 'grammar' can be specified, but not both"
- **Ursache:** JSON-Schema wird intern in Grammar übersetzt; zusätzlich gesetzte Grammar → Konflikt.
- **FIX:** nur EINS angeben (`response_format` ODER `grammar`); reines JSON `{type:"json_object"}`.
- **Quelle:** https://github.com/ggml-org/llama.cpp/issues/11847

---

## G) Parameter-Abweichungen

### 14. Ollama `/v1` droppt still `logprobs`/`top_logprobs` ⭐
- **Versionen:** Ollama OpenAI-Pfad — **won't-fix** (ollama #16117 CLOSED NOT_PLANNED 2026-05-12).
- **FIX:** für Token-Wahrscheinlichkeiten native `/api/*`-Schnittstelle; OpenAI-Pfad dafür nicht verlässlich.
- **Quelle:** https://github.com/ollama/ollama/issues/16117

### 15. `n` & `finish_reason` unvollständig (Ollama)
- **FIX:** nicht auf `n>1` verlassen — clientseitig mehrfach anfragen; `finish_reason` kann `null` sein.

### 16. Streaming- vs Non-Streaming-Antwortstruktur unterscheidet sich
- **FIX:** Stream-Parser nur auf garantierte Felder stützen; Abschluss am `[DONE]`/letzten Chunk.

---

## H) vLLM-spezifisch

### 17. CUDA OOM durch `gpu_memory_utilization`/`max_model_len`
- **FIX:** bei OOM `--gpu-memory-utilization` anpassen ODER `--max-num-batched-tokens`/`--max-model-len` senken.

### 18. `--served-model-name` vs. tatsächlicher Modell-Pfad
- **Symptom:** „model not found", weil interner Name der HF-Pfad ist.
- **FIX:** `--served-model-name <alias>` setzen und im Client verwenden; `/v1/models` zum Verifizieren.

---

## I) textgen-webui / LocalAI

### 19. textgen-webui: API nicht aktiv ohne `--api`
- **FIX:** mit `--api` starten (Port via `--api-port`), `http://127.0.0.1:5000/v1`.

### 20. textgen-webui: `/v1/models` liefert „wrong models"
- **FIX:** geladenes Modell explizit per Name ansprechen, Listing nicht als Wahrheit.

---

## Fix-Status (Stand 2026-06-08)

| Frueherer Bug | Status | Bezug |
|---|---|---|
| logprobs auf Ollama `/v1` | **won't-fix** (ollama #16117 CLOSED NOT_PLANNED) | Bug 14 — native API nutzen |
| leerer api_key crasht SDK | **per Design** ab openai-python 2.34.0 | Bug 4 — Platzhalter setzen |

**Noch NICHT gefixt / per Design:** `/v1`-Suffix-Pflicht (1), num_ctx-Ignoranz (5), Cold-Start (8), vLLM-Tool-Flags (9), llama.cpp Tool-JSON (10), json_schema/grammar-Konflikt (13).

---

## Pflicht-Checkliste vor lokaler Integration

- [ ] `base_url` mit `/v1`, `127.0.0.1` statt `localhost`?
- [ ] API-Key-Platzhalter (nicht leer)?
- [ ] Ollama-Kontext über Modelfile/`OLLAMA_CONTEXT_LENGTH` (nicht über `/v1`-Body)?
- [ ] First-Request-Timeout ≥ 60 s, Modell vorgeladen, `keep_alive` bewusst (Client-`5m`-Override geprüft)?
- [ ] vLLM Tool-Flags gesetzt? `/v1/models` zum Verifizieren des Namens?
- [ ] `logprobs`/`n` nicht blind vorausgesetzt?

## 🔗 Bezug zu Best Practices

Zweite Seite der Medaille (wie man es richtig macht): `best-practices/apis/local-openai-compatible.md`. Die dortige Mapping-Tabelle „🔗 Bezug zum Bug-Almanach“ verlinkt jede Best-Practice zurueck auf die hier dokumentierten Bug-Abschnitte (bidirektional, ohne Duplikation).


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
- [mistral-api](mistral-api.md)
- [oauth-device-code](oauth-device-code.md)
- [openai-api](openai-api.md)
- [openrouter-api](openrouter-api.md)
- [other-llm-apis](other-llm-apis.md)
- [xai-grok-api](xai-grok-api.md)
