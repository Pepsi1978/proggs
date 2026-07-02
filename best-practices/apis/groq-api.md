# Groq API — Best Practices (Stand 2026-07-02)

> Gegenstueck zu `bugs/apis/groq-api.md`. Offiziell empfohlen (Quellen). (Researcher-Recherche 2026-06-09, Re-Recherche 2026-07-02.)
> Update 2026-07-02: Modell-IDs noch strenger dynamisch behandeln; mehrere bisherige Kernmodelle haben Shutdown-Daten im Juli/August 2026. Rate-Limits immer per Response-Header/Dashboard auswerten, nicht aus statischen Tabellen hardcoden.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Kurzcheck = Stufe-A-Pflichtlektüre
> (`Read` mit `limit=80`). Volltext bei Fehlern im Bereich (Stufe B) und vor
> Hochrisiko-Arbeit (Stufe C).

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | OpenAI-Drop-in | `base_url=.../openai/v1`; nicht unterstützte Felder gar nicht senden | §1 |
| 2 | Modell/Output-Tokens | Dynamisch von `/models`; `max_completion_tokens` ans Output-Limit | §2 |
| 3 | Rate-Limits | Alle Header überwachen, token-bewusst pre-throttlen; `retry-after` zuerst | §3 |
| 4 | Tool/Structured Output | Striktes Schema; strict-Mode nur gpt-oss-20b/-120b, sonst Repair-Loop | §4/§5 |
| 5 | Streaming/Chat | `stream=true`; `temperature` bewusst >0 (echte 0 → `1e-8`) | §6 |
| 6 | Whisper | 16 kHz Mono FLAC, chunken, `verbose_json`; min. 10 s Abrechnung | §7 |
| 7 | Massen-Workloads | Batch API (50 % Rabatt); native SDKs mit Retries nutzen | §8 |

## 1. OpenAI-Kompatibilitaetslayer richtig nutzen
- `base_url=https://api.groq.com/openai/v1` setzen, API-Key per `api_key`-Parameter — der Drop-in-Migrationspfad ist offiziell unterstuetzt. Quelle: https://console.groq.com/docs/openai · offiziell
- Nicht unterstuetzte Felder erst gar nicht senden: `logprobs`, `logit_bias`, `top_logprobs`, `messages[].name`, `n>1` sowie `vtt`/`srt` als Audio-Format (alles 400). Quelle: https://console.groq.com/docs/openai · offiziell
- Fuer volle Feature-Abdeckung die nativen Groq-SDKs (Python/TypeScript) statt der OpenAI-Clients verwenden — offiziell empfohlen. Quelle: https://console.groq.com/docs/openai · offiziell

## 2. Modelle dynamisch beziehen, Output-Tokens pro Modell setzen
- Aktuelle Produktionsmodelle (2026): `llama-3.1-8b-instant` (131k Kontext / 131k Output, ~560 T/s), `llama-3.3-70b-versatile` (131k / 32k Output, ~280 T/s), `openai/gpt-oss-120b` (131k / 65k, ~500 T/s) und `-20b` (131k / 65k, ~1000 T/s). Quelle: https://console.groq.com/docs/models · offiziell
- `max_completion_tokens` an das modellspezifische Output-Limit koppeln, NICHT ans Kontextfenster — z. B. 70b nur 32k Output trotz 131k Kontext. Quelle: https://console.groq.com/docs/models · offiziell
- Modell-IDs nie hartkodieren — dynamisch vom `/models`-Endpoint ziehen und Deprecations-Seite vor jedem Release pruefen (Groq mustert schnell aus). Quelle: https://console.groq.com/docs/deprecations · offiziell

## 3. Rate-Limits proaktiv steuern (RPM/TPM/RPD/TPD + ITPM/OTPM)
- Alle Limit-Header ueberwachen (`x-ratelimit-remaining-requests`/`-tokens`, `-reset-*`) und token-bewusst pre-throttlen, statt erst auf 429 zu reagieren — Limits gelten organisationsweit. Quelle: https://console.groq.com/docs/rate-limits · offiziell
- Bei 429 zuerst den `retry-after`-Header befolgen, sonst exponentielles Backoff mit Jitter. Quelle: https://console.groq.com/docs/rate-limits · offiziell
- Gecachte Tokens zaehlen NICHT auf das Rate-Limit — Prompt-Caching senkt also auch die TPM-Last. Quelle: https://console.groq.com/docs/rate-limits · offiziell

## 4. Tool/Function Calling
- Striktes OpenAI-Tool-Schema verwenden (`tools[].function.{name,description,parameters}`), aussagekraeftige `description` + `enum`-Werte fuer zuverlaessige Tool-Auswahl. Quelle: https://console.groq.com/docs/tool-use · offiziell
- Modelle mit starker Tool-Faehigkeit waehlen (z. B. `llama-3.3-70b-versatile`, `qwen/qwen3-32b`) und parallele Tool-Calls nutzen, um Multi-Step-Latenz zu senken. Quelle: https://console.groq.com/docs/tool-use · offiziell
- Niedrige `temperature` (0.0–0.5) fuer stabileres Tool-Calling; leere/null-Tool-Antworten als Fehlerfall behandeln und Retry. Quelle: https://console.groq.com/docs/tool-use · offiziell

## 5. Structured Outputs / JSON Mode
- Strict Mode (`strict: true`, constrained decoding) garantiert Schema-Treue, aber nur bei `openai/gpt-oss-20b/-120b` — alle Felder `required` + `additionalProperties: false`, optionale Felder als Union `["string","null"]`. Quelle: https://console.groq.com/docs/structured-outputs · offiziell
- Best-effort (`strict: false`) bei breiteren Modellen → Retry-/Repair-Loop einplanen; reines `{"type":"json_object"}` nur als Fallback ohne Schema-Zwang. Quelle: https://console.groq.com/docs/structured-outputs · offiziell
- Schemata mit Pydantic/Zod definieren (Typsicherheit + Auto-JSON-Schema); bei Validierungsfehlern Prompt schaerfen statt nur das Schema. Quelle: https://console.groq.com/docs/structured-outputs · offiziell

## 6. Streaming & Chat-Parameter
- `stream=true` fuer responsives UI; bei Python `AsyncGroq`/asyncio fuer nebenlaeufige nicht-blockierende Calls. Quelle: https://console.groq.com/docs/text-chat · offiziell
- `temperature` bewusst > 0 setzen (echte 0 wird intern zu `1e-8`); `top_p` und `stop`-Sequenzen gezielt zur Ausgabesteuerung nutzen. Quelle: https://console.groq.com/docs/text-chat · offiziell

## 7. Speech-to-Text (Whisper)
- Modell nach Bedarf: `whisper-large-v3-turbo` (guenstig, mehrsprachig) vs. `whisper-large-v3` (hoechste Genauigkeit). Quelle: https://console.groq.com/docs/speech-to-text · offiziell
- Audio vorab auf 16 kHz Mono FLAC downmixen (`ffmpeg -i <file> -ar 16000 -ac 1 -map 0:a -c:a flac out.flac`); grosse Dateien chunken (Limit 25 MB free / 100 MB dev), `verbose_json` fuer Segment-/Wort-Timestamps. Quelle: https://console.groq.com/docs/speech-to-text · offiziell
- Mindest-Abrechnung 10 s pro Request → kurze Clips client-seitig buendeln; nur `json`/`verbose_json`/`text` (kein vtt/srt). Quelle: https://console.groq.com/docs/speech-to-text · offiziell

## 8. Batch API & SDK-Konfiguration
- Batch API fuer nicht-zeitkritische Massen-Workloads: 50 % Rabatt, eigene hoehere Limits, Fenster 24 h–7 Tage, JSONL bis 50.000 Zeilen/200 MB (Chat, Vision, Audio). Rabatt stapelt NICHT mit Prompt-Caching. Quelle: https://console.groq.com/docs/batch · offiziell
- Native SDKs nutzen (`pip install groq` / `npm i groq-sdk`), Key per `GROQ_API_KEY`-Env; SDKs bringen Retries und sync/async-Clients mit — Timeout/`max_retries` pro Request bzw. Client setzen. Quelle: https://console.groq.com/docs/libraries · offiziell

## 🔗 Bezug zum Bug-Almanach
| Best-Practice | Bug-Abschnitt (`bugs/apis/groq-api.md`) |
|---|---|
| 1 OpenAI-Kompatibilitaetslayer | 1, 2, 3 (Abschn. 1) |
| 2 Modelle dynamisch / Output-Tokens | 9, 10 (Abschn. 3), 20 (Abschn. 6) |
| 3 Rate-Limits proaktiv | 5, 6, 7, 8 (Abschn. 2) |
| 4 Tool/Function Calling | 17, 18, 19 (Abschn. 5) |
| 5 Structured Outputs / JSON | 4 (Abschn. 1) |
| 6 Streaming & Chat-Parameter | 3 (Abschn. 1), 20 (Abschn. 6) |
| 7 Speech-to-Text (Whisper) | 11, 12, 13, 14, 15, 16 (Abschn. 4) |
| 8 Batch API & SDK-Konfiguration | 21 (Abschn. 7), 8 (Abschn. 2) |
