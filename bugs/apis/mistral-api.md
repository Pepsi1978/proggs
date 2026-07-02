# Bekannte Bugs: Mistral AI API (Integration)

> PFLICHT-LESEN vor Arbeit an einer Mistral-Integration (api.mistral.ai / La Plateforme).
> Stand: zuletzt recherchiert am 2026-06-08, **re-recherchiert am 2026-07-02** (Engine A: Firecrawl+MiniMax). Zweite Seite: `best-practices/apis/mistral-api.md`.

> **Update 2026-07-02:** Neue Changelog-Funde seit 2026-06-08: OCR 4 (`mistral-ocr-4-0`) seit 22.06.2026 mit `include_blocks` und erweitertem `pages`-Parameter; Leanstral 1.5 (`labs-leanstral-1-5`) seit 29.06.2026, Retirement 30.09.2026. Keine neuen belegten Aenderungen an `tool_call_id`, JSON-Mode oder Codestral-Rate-Limits gefunden.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Dieser Kurzcheck ist die Vorab-Pflichtlektüre
> (Stufe A, `Read` mit `limit=80`). Der Volltext darunter ist Pflicht bei JEDEM Fehler in
> diesem Bereich (Stufe B). Der Kurzcheck ersetzt den Volltext nicht.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | Tool-Calling (400/422) ⭐ | `tool_call_id` = exakt 9 Zeichen `[a-zA-Z0-9]`, unveraendert durchreichen | §4 |
| 2 | "number of function calls" ⭐ | Pro `tool_call` genau eine `tool`-Antwort, gleiche Anzahl | §5 |
| 3 | JSON-Mode haengt ⭐ | Wort „JSON" muss im Prompt stehen, sonst Whitespace-Stream | §14 |
| 4 | Codestral / FIM (404) | Eigener Endpunkt+Key, FIM unter `/v1/fim/completions` | §2, §20 |
| 5 | Token-Count / 400 | `mistral-common`-Tokenizer; Kontext-Ueberschreitung = hartes 400 | §12, §13 |
| 6 | Modell „not found" (400/404) ⭐ | Datierte Version pinnen, Aliase zentral; aggressive Deprecations | §22, §23 |
| 7 | 401 bei Auth | Nur nackten Key; `Bearer` genau einmal; Key zum Endpunkt | §1, §2 |
| 8 | 429 trotz normaler Last | Workspace-weite Limits; `Retry-After`/`X-RateLimit-Remaining` | §21 |

---

## A. Authentifizierung

### 1. Doppeltes `Bearer`-Präfix → 401
- **Ursache:** Client verlangt `Bearer ` im Key-Feld, HTTP-Client setzt es nochmal → `Bearer Bearer sk-...`.
- **FIX:** nur nackten Key speichern, `Bearer ` genau einmal im Header.
- **Quelle:** https://github.com/Dokploy/dokploy/issues/3576

### 2. Codestral-Key ≠ Platform-Key (401 bei falschem Endpunkt)
- **Ursache:** `codestral.mistral.ai` braucht personal Codestral-Key (eigenes Dashboard, außerhalb Org-Limits); `api.mistral.ai` den Workspace-Key.
- **FIX:** Key zum Endpunkt passend wählen.
- **Quelle:** https://docs.mistral.ai/api/endpoint/fim

### 3. Key nicht persistiert / Keychain-Eintrag fehlt → 401
- **FIX:** Key neu erzeugen, Scope/Permissions + Workspace prüfen.

---

## B. Tool / Function Calling (häufigste Fehlerquelle)

### 4. `tool_call_id` muss EXAKT 9 Zeichen `[a-zA-Z0-9]` sein → 400/422 ⭐ KRITISCH
- **Symptom:** 400/422 `Tool call id was {id} but must be a-z, A-Z, 0-9, with a length of 9`.
- **Ursache:** Mistral validiert IDs strikt; OpenAI-kompatible Clients erzeugen andere Formate.
- **Versionen:** alle tool-fähigen Modelle — per Design (Client adaptiert; sst/opencode #1680 **CLOSED** 2025-11-06).
- **FIX:** IDs vor dem Senden auf 9 alphanumerische Zeichen mappen UND dieselbe gemappte ID in der `tool`-Antwort; bei eigenem Code Modell-IDs unverändert durchreichen, nicht selbst neu erzeugen.
- **Quelle:** https://github.com/sst/opencode/issues/1680

### 5. Anzahl `tool_calls` ≠ Anzahl `tool`-Antworten → Fehler ⭐
- **Symptom:** 400/422 „Not the same number of function calls and responses".
- **Versionen:** parallele Tools / Agents API — mistralai/client-python #234 **CLOSED/fixed** 2026-02-26.
- **FIX:** strikte Sequenz: assistant(tool_calls) → für jeden Call eine `tool`-Message mit `tool_call_id` → erst dann nächster Call. Keine Antwort weglassen.
- **Quelle:** https://github.com/mistralai/client-python/issues/234

### 6. `tool_call_id` darf nicht fehlen
- **FIX:** in jeder tool-Antwort den exakten `tool_call_id` mitgeben.

### 7. Max 128 Tools pro Request
- **FIX:** Tools filtern; lange Beschreibungen (zählen in den Kontext) kürzen.

### 8. `tool_choice:"any"` erzwingt Tool, aber NICHT welches; `"auto"` kann überspringen
- **FIX:** für deterministische Einzel-Tools `tool_choice` auf das spezifische Tool-Objekt; bei `auto` defensiv prüfen ob `tool_calls` fehlen.

### 9. Parallele Tool-Calls in beliebiger Reihenfolge zurück
- **FIX:** über `tool_call_id` zuordnen, nicht über Position; `parallel_tool_calls:false` erzwingt Einzel-Calls.

---

## C. Endpunkt-Eigenheiten / OpenAI-Kompatibilität

### 10. System-only-Message wird abgelehnt
- **FIX:** immer eine user-Message anhängen.

### 11. `n > 1` nicht (überall) unterstützt
- **Ursache:** z. B. `mistral-large-2512` kein `n>1`.
- **FIX:** mehrere Completions via separate Requests; `n` nicht voraussetzen.

### 12. Tokenizer-Mismatch — manuelles Token-Zählen weicht ab
- **Ursache:** eigener Tokenizer (≠ OpenAI/Llama).
- **FIX:** `mistral-common`-Tokenizer für Counts; Kontext = Input+Output.

### 13. Kontext-Überschreitung → 400 (statt sanfter Truncation)
- **FIX:** vorab Tokens zählen, History kürzen/zusammenfassen.

---

## D. JSON-Mode / Structured Output

### 14. JSON-Mode ohne „JSON" im Prompt → Endlos-Whitespace ⭐
- **Symptom:** unendlicher Whitespace-Stream, Request hängt.
- **Ursache:** bei `response_format` JSON-Mode MUSS „JSON" im Prompt stehen.
- **FIX:** „JSON" explizit in den Prompt (z. B. „Antworte als JSON…").
- **Quelle:** https://docs.mistral.ai/resources/known-limitations

### 15. JSON-Mode garantiert kein Schema
- **FIX:** für strikte Strukturen Function Calling oder `json_schema` statt reinem `json_object`.

---

## E. Streaming (SSE)

### 16. Usage fehlt ohne `stream_options.include_usage`
- **FIX:** `stream_options:{include_usage:true}`.

### 17. `[DONE]`-Terminator + Delta-Akkumulation bei Tool-Calls
- **FIX:** `[DONE]` separat (nicht JSON-parsen); Tool-Deltas über `index` zusammensetzen, dann parsen.

### 18. Streaming-Timeout / Pufferung
- **Ursache:** Streams timen nach 10 Min Inaktivität aus; manche HTTP-Libs puffern SSE falsch.
- **FIX:** längere Timeouts (~120 s), SSE-Pufferung im Client deaktivieren.

### 19. `stream:true` muss im Payload stehen (nicht nur im SDK-Aufruf)
- **FIX:** `stream:true` explizit in den JSON-Body.

---

## F. Codestral / FIM

### 20. FIM nur unter `/v1/fim/completions` am Codestral-Endpunkt
- **Symptom:** 404 bei FIM gegen `/v1/chat/completions`.
- **FIX:** FIM an `https://codestral.mistral.ai/v1/fim/completions` mit `prompt` (+ optional `suffix`), nicht `messages`.

---

## G. Rate Limits

### 21. 429 trotz „normaler" Nutzung — Workspace-weite Limits
- **Ursache:** Limits je Workspace, geteilt über ALLE Keys (RPS + Tokens/min + Tokens/Monat); Free-Tier sehr niedrig (~1 RPS).
- **FIX:** `Retry-After` respektieren, `X-RateLimit-Remaining` vorab prüfen, Scale-Tier; Codestral-Endpunkt umgeht Org-Limits.

---

## H. Modell-Versionen / Deprecations

### 22. Aggressive Modell-Deprecations 2025/2026 ⭐
- **Symptom:** plötzlich 400/404 „model not found".
- **Ursache:** häufiges Ausmustern/Umbenennen (mistral-large 2407→2411→medium-2505; Nemo→small-2503; Small 3.1 deprecated 2025-11-06; Magistral 1.1 bis 2025-10-31).
- **FIX:** Modell-Aliase zentralisieren; Changelog regelmäßig prüfen.
- **Quelle:** https://docs.mistral.ai/getting-started/changelog

### 23. `-latest` vs. datierte Version — stille Verhaltensänderung
- **FIX:** in Produktion datierte Versionen pinnen, `-latest` nur Dev; vor Migration Regressionstest.

---

## I. Multimodal / Limits

### 24. Vision/Audio/File-Limits
- **Ursache:** Bild max 20 MB; Audio max 60 min/500 MB; File-Upload max 512 MB, 30 Tage Retention.
- **FIX:** vorab validieren/komprimieren; große Dateien chunken.

---

## Fix-Status (Stand 2026-06-08)

| Frueherer Bug | Status | Bezug |
|---|---|---|
| calls/responses-Mismatch (client-python) | **gefixt** (client-python #234, CLOSED 2026-02-26) | Bug 5 — Sequenz-Regel bleibt aber zu beachten |
| Tool-Call-ID-Format (opencode) | **CLOSED** 2025-11-06 (Client angepasst) | Bug 4 — Mistral-9-Zeichen-Regel bleibt per Design |
| mistral-large 2407, Nemo, Small 3.1, Magistral 1.1 | **deprecated/umbenannt** | Bug 22 — migrieren |

**Noch NICHT gefixt / per Design:** 9-Zeichen-`tool_call_id` (4), JSON-Mode-„JSON"-Pflicht (14), eigener Tokenizer (12), Codestral-Endpunkt-Trennung (2/20), Workspace-Limits (21).

**Ehrlichkeits-Hinweis:** `prefix`/Prefill-Support und `safe_prompt` ergaben keinen konkret belegten Bug — bewusst weggelassen statt zu raten. Die „Error Codes"-Doku betrifft die Workflows-API (WF_xxxx), nicht die Chat-Completions-HTTP-Codes.

---

## Pflicht-Checkliste vor Mistral-Integration

- [ ] `tool_call_id` = 9 alphanumerische Zeichen, in Call UND Antwort identisch?
- [ ] Pro `tool_call` genau eine `tool`-Antwort (gleiche Anzahl)?
- [ ] JSON-Mode: Wort „JSON" im Prompt?
- [ ] Codestral/FIM am richtigen Endpunkt + Key?
- [ ] `mistral-common`-Tokenizer für Counts, Modell-Version gepinnt?
- [ ] `Retry-After`/`X-RateLimit-Remaining` beachtet (Workspace-weit)?

## 🔗 Bezug zu Best Practices

Zweite Seite der Medaille (wie man es richtig macht): `best-practices/apis/mistral-api.md`. Die dortige Mapping-Tabelle „🔗 Bezug zum Bug-Almanach“ verlinkt jede Best-Practice zurueck auf die hier dokumentierten Bug-Abschnitte (bidirektional, ohne Duplikation).


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
- [oauth-device-code](oauth-device-code.md)
- [openai-api](openai-api.md)
- [openrouter-api](openrouter-api.md)
- [other-llm-apis](other-llm-apis.md)
- [xai-grok-api](xai-grok-api.md)
