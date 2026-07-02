# Mistral API — Best Practices (Stand 2026-07-02)

> Gegenstueck zu `bugs/apis/mistral-api.md`. Offiziell empfohlen (Quellen). (Researcher-Recherche 2026-06-09, Re-Recherche 2026-07-02.)
> Update 2026-07-02: OCR 4 gezielt nutzen, wenn strukturierte OCR-Bloecke/BBoxen gebraucht werden; Leanstral 1.5 wegen Retirement 30.09.2026 nicht als langfristigen Default pinnen.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Kurzcheck = Stufe-A-Pflichtlektüre
> (`Read` mit `limit=80`). Volltext bei Fehlern im Bereich (Stufe B) und vor
> Hochrisiko-Arbeit (Stufe C).

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Modell waehlen | Datierte Version pinnen (`-2512`), `-latest` nur Dev | §1 |
| 2 | Strukturierte Ausgabe | `json_schema` statt `json_object` (Schema-garantiert) | §2 |
| 3 | Tool-Calling | Pro Call eine `tool`-Antwort, IDs durchreichen; rekursiv | §3 |
| 4 | Code-Completion | FIM am Codestral-Endpunkt + eigener Key | §4 |
| 5 | Token sparen | `prompt_cache_key` bei gemeinsamem Praefix (10 % Kosten) | §5 |
| 6 | Rate Limits | Pro Workspace; bei 429 `Retry-After` + Backoff | §6 |
| 7 | SDK-Setup | `RetryConfig`; Stream-Timeouts hoch (~10 min Inaktivitaet) | §7 |
| 8 | Streaming/OCR | `stream:true` im Body; `include_usage`; OCR mit json_schema | §8 |

## 1. Modell-Auswahl & Pinning
- Aktuelle Modelle 2026: **Mistral Large 3** (v25.12, generalistisch/multimodal), **Mistral Medium 3.5** (v26.04, agentic+coding), **Mistral Small 4** (v26.03, hybrid effizient), **Magistral Medium 1.2** (v25.09, Reasoning), **Codestral** (v25.08, Code-Completion), **Devstral 2** (v25.12, SWE-Agenten), **Ministral 3** (v25.12, 3/8/14B). Quelle: https://docs.mistral.ai/getting-started/models/models_overview/ · offiziell
- In Produktion datierte Versionen pinnen (z. B. `mistral-large-2512`), `-latest` nur in Dev — Mistral mustert Modelle aggressiv aus (Small 3.2, Magistral 1.1, alte Devstral durch 2026 sunsetted). Quelle: https://docs.mistral.ai/getting-started/changelog · offiziell

## 2. Structured Output (json_schema bevorzugt)
- `response_format: {"type":"json_schema"}` garantiert valides JSON **nach deinem Schema** — gegenueber `{"type":"json_object"}` (nur valides JSON, kein Schema). Custom Structured Outputs sind zuverlaessiger und werden „whenever possible" empfohlen. Quelle: https://docs.mistral.ai/capabilities/structured_output/custom · offiziell
- Reiner JSON-Mode (`json_object`) verlangt das Wort „JSON" im Prompt, sonst Endlos-Whitespace; `json_schema` ist die robustere Wahl (verfuegbar fuer Chat Completions, Agents, OCR). Quelle: https://docs.mistral.ai/studio-api/conversations/structured-output/json_mode · offiziell

## 3. Function / Tool Calling
- `tool_choice`: `auto` (Default, Modell entscheidet), `any` (erzwingt irgendein Tool), `none` (kein Tool), oder spezifisches Tool-Objekt fuer deterministische Einzel-Calls. `parallel_tool_calls:false` erzwingt sequenzielle Calls. Quelle: https://docs.mistral.ai/capabilities/function_calling/ · offiziell
- Strikte Message-Sequenz: pro `assistant`-`tool_call` genau eine `tool`-Antwort mit identischem `tool_call_id`; vom Modell zurueckgegebene IDs unveraendert durchreichen. Quelle: https://docs.mistral.ai/capabilities/function_calling/ · offiziell
- Tool-Antworten **rekursiv** behandeln: ein Tool-Call kann von weiteren Tool-Calls gefolgt werden — bis finaler Text statt weiterer Calls kommt. Quelle: https://docs.mistral.ai/capabilities/function_calling/ · offiziell

## 4. Codestral / FIM (Code-Completion)
- FIM laeuft am eigenen Codestral-Endpunkt `https://codestral.mistral.ai/v1/fim/completions` mit `prompt` (+ optional `suffix`), Modell `codestral-latest` — eigener Key, getrennt von Platform/Org-Limits. Quelle: https://docs.mistral.ai/api/endpoint/fim · offiziell
- Code-Embeddings (`codestral-embed`): kleine Chunk-Groesse (~3000 Zeichen / ~512 Tokens) mit Overlap (~1000 Zeichen) verbessert RAG-Retrieval deutlich; Dimension via `output_dimension` reduzierbar (bis 3072). Quelle: https://docs.mistral.ai/capabilities/embeddings/code_embeddings · offiziell

## 5. Prompt Caching (Token-Effizienz)
- Gleichen `prompt_cache_key` (stabile App-ID: Conversation-/Session-/Workflow-ID) auf Requests mit gemeinsamem Praefix setzen — cached Tokens kosten nur 10 % des Input-Preises und senken Latenz. Quelle: https://docs.mistral.ai/studio-api/conversations/advanced/prompt-caching · offiziell
- Cache-Bloecke umfassen 64 Tokens; Prompts unter 64 Tokens erzielen keinen Cache-Hit. Ideal fuer Multi-Turn-Chats, geteilte System-Prompts, FIM mit langem Praefix, Agent-Kontext. Quelle: https://docs.mistral.ai/studio-api/conversations/advanced/prompt-caching · offiziell

## 6. Rate Limits & Tiers
- Limits gelten pro **Workspace** (geteilt ueber alle Keys): RPS, Tokens/min, Tokens/Monat — RPS und TPM werden unabhaengig erzwungen. Free/Experiment-Tier nur fuer Eval/Prototyping; via Scale-Plan (Admin›Subscriptions) auf Tier 1+ hochstufen. Quelle: https://docs.mistral.ai/deployment/ai-studio/tier · offiziell
- Bei 429: `Retry-After` respektieren und exponentielles Backoff; aktuelle Limits unter Admin›Limits pruefen. Ueber Tier 4 hinaus erst nach Erreichen von Tier 4 + Billing-Schwelle (> 2000 $/€). Quelle: https://help.mistral.ai/en/articles/698531-why-am-i-hitting-api-rate-limits-and-how-do-i-increase-them · offiziell

## 7. SDK-Konfiguration (Retries, Timeout)
- Python-SDK: `RetryConfig` global oder pro Call setzen (z. B. `RetryConfig("backoff", BackoffStrategy(1, 50, 1.1, 100), False)`); Default-Backoff ist eingebaut, pro Operation ueberschreibbar. Quelle: https://github.com/mistralai/client-python/blob/main/README.md · offiziell
- Timeouts/Proxies/Custom-Header durch eigene HTTP-Client-Instanz beim SDK-Init konfigurieren; fuer Streams hoehere Timeouts (Streams timen nach ~10 min Inaktivitaet aus). Quelle: https://github.com/mistralai/client-python/blob/main/README.md · offiziell

## 8. Streaming & OCR/Document AI
- Streaming: `stream:true` im JSON-Body, `stream_options:{include_usage:true}` fuer Usage; `[DONE]`-Terminator separat behandeln (nicht parsen), Tool-Deltas ueber `index` akkumulieren. Quelle: https://docs.mistral.ai/api/endpoint/chat · offiziell
- OCR: aktuelle Modelle `mistral-ocr-2512` (OCR 3) bzw. `mistral-ocr-2505`; Annotations fuer strukturierte Dokument-Extraktion mit json_schema kombinieren. Quelle: https://docs.mistral.ai/studio-api/document-processing/basic_ocr · offiziell

## 🔗 Bezug zum Bug-Almanach
| Best-Practice | Bug-Abschnitt (`bugs/apis/mistral-api.md`) |
|---|---|
| 1 Modell-Auswahl & Pinning | 22, 23 |
| 2 Structured Output | 14, 15 |
| 3 Function / Tool Calling | 4, 5, 6, 7, 8, 9 |
| 4 Codestral / FIM | 2, 20 |
| 5 Prompt Caching | 12, 13 (Token-Effizienz), Ehrlichkeits-Hinweis (prefix) |
| 6 Rate Limits & Tiers | 21 |
| 7 SDK-Konfiguration | 1, 18 |
| 8 Streaming & OCR/Document AI | 16, 17, 18, 19, 24 |
