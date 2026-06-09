# Lokale OpenAI-kompatible Server — Best Practices (Stand 2026-06-09)

> Gegenstueck zu `bugs/apis/local-openai-compatible.md`. Offiziell empfohlen (Quellen). (Researcher-Recherche 2026-06-09.)

## 1. base_url & `/v1`-Endpunkt-Konventionen
- `base_url` IMMER mit `/v1`-Suffix setzen — alle Server routen die OpenAI-Kompatibilitaet unter `/v1` (Ollama `:11434/v1`, LM Studio `:1234/v1`, llama.cpp `:8080/v1`, vLLM `:8000/v1`). Quelle: https://ollama.com/blog/openai-compatibility · offiziell
- Offizielle Empfehlung: nur die `base_url`-Property eines bestehenden OpenAI-Clients (Python/JS/C#) umbiegen, Code sonst unveraendert lassen — drop-in-Ersatz. Quelle: https://lmstudio.ai/docs/app/api/endpoints/openai · offiziell
- Fuer LAN-Zugriff Server bewusst auf `0.0.0.0` binden (vLLM/llama.cpp `--host 0.0.0.0`, Ollama `OLLAMA_HOST=0.0.0.0:11434`); lokal `127.0.0.1` statt `localhost` (IPv6-Falle). Quelle: https://ollama.com/blog/openai-compatibility · offiziell

## 2. Dummy-API-Key
- API-Key IMMER setzen, auch wenn der Server ihn ignoriert — Ollama dokumentiert ausdruecklich den Platzhalter `"ollama"`; LM Studio empfiehlt einen beliebigen nicht-leeren String. Niemals leer (crasht neuere openai-python-SDKs). Quelle: https://ollama.com/blog/openai-compatibility · offiziell
- vLLM/llama.cpp koennen echte Auth erzwingen (`--api-key`); dann muss der Client-Key exakt passen. Quelle: https://docs.vllm.ai/en/latest/serving/openai_compatible_server.html · offiziell

## 3. Modell-Laden, Keep-Alive & Auto-Unload
- LM Studio: JIT-Loading nutzen — Modell muss nicht vorab geladen werden; Idle-TTL steuert das Entladen (Default 60 Min, pro Request via `ttl`-Feld setzbar), Auto-Evict entlaedt das alte Modell vor dem Laden des neuen (Default an). Quelle: https://lmstudio.ai/docs/app/api/ttl-and-auto-evict · offiziell
- Ollama: `keep_alive` bewusst steuern (Default `5m`, `-1` = dauerhaft geladen, `0` = sofort entladen); Status via `:11434/api/ps`. Quelle: https://ollama.com/blog/openai-compatibility · offiziell
- vLLM: Modell wird beim Serverstart einmalig in den VRAM geladen und bleibt resident — kein TTL/Auto-Unload; ein vLLM-Prozess pro Modell. Quelle: https://docs.vllm.ai/en/latest/serving/openai_compatible_server.html · offiziell
- Modell-Alias setzen und im Client verwenden: vLLM `--served-model-name <alias>`, mit `/v1/models` verifizieren. Quelle: https://docs.vllm.ai/en/latest/serving/openai_compatible_server.html · offiziell

## 4. Kontextfenster setzen (sonst stilles Truncaten)
- Ollama: Kontext NICHT ueber den `/v1`-Body — pro Modell ein Modelfile mit `PARAMETER num_ctx <wert>` ODER Server-Default `OLLAMA_CONTEXT_LENGTH` setzen, sonst stille Truncation. Quelle: https://ollama.com/blog/openai-compatibility · offiziell
- llama.cpp: `--ctx-size` (alias `-c`) explizit setzen (Default niedrig, oft 512/2048) — bei Concurrency wird der Kontext auf Slots aufgeteilt, also `ctx_size = parallel × gewuenschter_kontext`. Quelle: https://github.com/ggml-org/llama.cpp/blob/master/tools/server/README.md · offiziell
- vLLM: `--max-model-len` setzen; zu hoher Wert → CUDA OOM, dann senken oder `--gpu-memory-utilization` anpassen. Quelle: https://docs.vllm.ai/en/latest/serving/openai_compatible_server.html · offiziell

## 5. Tool / Function Calling (Server- und Modell-abhaengig)
- vLLM: ZWEI Flags noetig — `--enable-auto-tool-choice` UND `--tool-call-parser <parser>` passend zum Modell (`llama3_json` fuer Llama 3.1/3.2, `hermes` fuer Qwen2.5/Hermes, `mistral`, `deepseek_v3`, `llama4_pythonic`); ggf. `--chat-template`. Quelle: https://docs.vllm.ai/en/latest/features/tool_calling/ · offiziell
- vLLM: `tool_choice="required"`/benannte Funktion erzwingt schema-valides JSON (structured outputs); `tool_choice="auto"` haengt von Modellqualitaet ab (kein strict-mode), defensiv parsen + Retry. Quelle: https://docs.vllm.ai/en/latest/features/tool_calling/ · offiziell
- llama.cpp: `--jinja` mit korrektem Chat-Template aktivieren; nur Modelle mit Tool-Template verwenden, Argumente client-seitig defensiv parsen. Quelle: https://github.com/ggml-org/llama.cpp/blob/master/tools/server/README.md · offiziell

## 6. Structured Output / JSON / Grammar
- LM Studio: `response_format` mit `{"type":"json_schema","json_schema":{...}}` — folgt 1:1 dem OpenAI-Structured-Output-Format, funktioniert ueber die OpenAI-SDKs. Quelle: https://lmstudio.ai/docs/developer/openai-compat/structured-output · offiziell
- vLLM: ueber `response_format` (json_schema) ODER `extra_body={"structured_outputs":{...}}` (choice/regex/json/grammar/EBNF); Backend via `--structured-outputs-config.backend` (Default `auto`). Quelle: https://docs.vllm.ai/en/latest/features/structured_outputs.html · offiziell
- llama.cpp: ENTWEDER `response_format` (json_schema, intern in Grammar uebersetzt) ODER GBNF-`grammar` angeben — nie beides gleichzeitig (Konflikt-Fehler). Quelle: https://github.com/ggml-org/llama.cpp/blob/master/tools/server/README.md · offiziell

## 7. Streaming, Concurrency & Embeddings
- vLLM ist fuer hohe Parallelitaet gebaut (continuous batching, ein Server bedient viele gleichzeitige Requests); Ollama/llama.cpp serialisieren bzw. brauchen explizite Slots — llama.cpp `--parallel <n>` (+ `--cont-batching`) fuer mehrere Slots. Quelle: https://docs.vllm.ai/en/latest/serving/openai_compatible_server.html · offiziell
- Embeddings ueber `/v1/embeddings`: llama.cpp braucht `--embeddings`-Flag (eigene Server-Instanz empfehlenswert, da andere Modell-Optionen); LM Studio/Ollama bieten `/v1/embeddings` mit Embedding-Modell. Quelle: https://lmstudio.ai/docs/app/api/endpoints/openai · offiziell
- Streaming: Stream-Parser nur auf garantierte Felder stuetzen und am `[DONE]`/letzten Chunk abschliessen (Struktur weicht von Non-Streaming ab; Tool-Call-Deltas teils unvollstaendig). Quelle: https://docs.vllm.ai/en/latest/serving/openai_compatible_server.html · offiziell

## 8. Timeouts & Versions-Drift der Compat-Layer
- First-Request-Timeout grosszuegig (≥ 60 s): bei vLLM/llama.cpp laedt das Modell beim Start, bei Ollama/LM-Studio-JIT erst beim ersten Request (Cold-Start) — Folge-Requests sind schnell. Quelle: https://lmstudio.ai/docs/app/api/ttl-and-auto-evict · offiziell
- Compat-Layer ist „best-effort": nicht jeder Parameter wird umgesetzt (`logprobs`/`n` koennen still verworfen werden). Feature-Set pro Server-Version gegen die Doku der genutzten Version pruefen, nicht auf vollstaendige OpenAI-Paritaet verlassen. Quelle: https://docs.vllm.ai/en/latest/serving/openai_compatible_server.html · offiziell

## 🔗 Bezug zum Bug-Almanach
| Best-Practice | Bug-Abschnitt (`bugs/apis/local-openai-compatible.md`) |
|---|---|
| 1 base_url & /v1 | A1, A2, A3 |
| 2 Dummy-API-Key | B4 |
| 3 Modell-Laden/Keep-Alive | D7, D8, H18 |
| 4 Kontextfenster | C5, C6, H17 |
| 5 Tool/Function Calling | E9, E10, E11, E12 |
| 6 Structured Output/Grammar | F13 |
| 7 Streaming/Concurrency/Embeddings | C6, G16, I19, I20 |
| 8 Timeouts & Versions-Drift | D8, G14, G15 |
