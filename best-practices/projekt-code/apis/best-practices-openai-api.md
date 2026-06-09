# OpenAI API — Best Practices (Stand 2026-06-08)

> Gegenstück zu `bugs/apis/openai-api.md`. Offiziell empfohlen (platform.openai.com, developers.openai.com,
> OpenAI Cookbook). (Researcher-Recherche 2026-06-08.)

## 1. Responses API vs. Chat Completions
- **Responses API für ALLE neuen Projekte** (OpenAI-Empfehlung): bessere Cache-Nutzung, stateful (Reasoning-/Tool-Kontext), eingebaute Tools, höhere Intelligenz bei Reasoning-Modellen. Chat Completions = Legacy (bleibt). Assistants API: Sunset 26.08.2026. Quelle: https://platform.openai.com/docs/guides/migrate-to-responses · offiziell

## 2. Structured Outputs (strict)
- Immer `strict:true` (Constrained Sampling statt best-effort); in JEDEM Objekt `additionalProperties:false` + ALLE Felder `required`; optionale Felder als Nullable-Union (`["string","null"]`) statt weglassen. Structured Outputs = Antwort an Nutzer strukturieren; Function Calling = Tools anbinden. Quelle: https://developers.openai.com/api/docs/guides/structured-outputs · offiziell

## 3. Function/Tool Calling
- Antworten als Array von 0..n Tool-Calls behandeln; klare Funktions-/Parameter-Beschreibungen + System-Prompt steuert WANN; `parallel_tool_calls` gezielt (bei abhängigen Schritten/Reasoning `false`); `tool_choice` auto/erzwingen/none. Quelle: https://platform.openai.com/docs/guides/function-calling · offiziell

## 4. Reasoning-Modelle (o-Reihe / gpt-5.x)
- `reasoning_effort` als Tuning-Knopf (none/minimal/low/medium/high/xhigh; gpt-5.5 default medium); klares Ziel + Constraints + Output-Contract statt Schritt-für-Schritt; Responses API + `developer`-Rolle bevorzugen; `temperature` NICHT als Hebel, Token-Budget großzügig (Reasoning-Tokens zählen mit). Quelle: https://platform.openai.com/docs/guides/reasoning-best-practices · https://cookbook.openai.com/examples/gpt-5/gpt-5_prompting_guide · offiziell

## 5. Prompt Caching / Token-Effizienz
- Automatisches Caching ab 1024 Tokens (gratis, bis 80 % weniger TTFT, bis 90 % weniger Input-Kosten): stabilen Inhalt (System/Schema/Instruktionen) NACH VORNE, volatilen ans Ende; `prompt_cache_key` so wählen, dass Prefix-Kombi <15 req/min. **Batch API (50 % Rabatt)** für Async; `service_tier="flex"` ebenfalls 50 % + Caching. Quelle: https://developers.openai.com/api/docs/guides/prompt-caching · https://developers.openai.com/api/docs/guides/batch · offiziell

## 6. Streaming
- `stream_options.include_usage:true` → Usage im finalen Extra-Chunk (alle anderen `usage:null`). Quelle: https://platform.openai.com/docs/api-reference/chat-streaming/streaming · offiziell

## 7. SDK-Konfiguration
- Default `max_retries=2` (Connection/408/409/429/≥500) + exp. Backoff; KEINE eigene Retry-Schleife darüber stapeln (sonst stille 10-min-Hangs). Default-Timeout 10 Min, bei Reasoning/xhigh erhöhen. Quelle: https://github.com/openai/openai-python · offiziell

## 🔗 Bezug zum Bug-Almanach
| Best-Practice | Bug-Abschnitt (`bugs/apis/openai-api.md`) |
|---|---|
| 1 Responses API | A1–A4 |
| 2 Structured Outputs | G19–G21 |
| 3 Tool Calling | D12–D14 |
| 4 Reasoning | B5–B7 |
| 5 Caching/Batch | (Token-Effizienz) |
| 6 Streaming | C8–C11 |
| 7 SDK | I23–I24 |
