# DeepSeek API — Best Practices (Stand 2026-07-02)

> Gegenstueck zu `bugs/apis/deepseek-api.md`. Offiziell empfohlen (Quellen). (Researcher-Recherche 2026-06-09, Re-Recherche 2026-07-02.)
>
> WICHTIGE AKTUALISIERUNG: `deepseek-v4-flash` / `deepseek-v4-pro` sind jetzt die aktiven Modelle
> (1M Kontext, bis 384K Output). Die alten Namen `deepseek-chat` / `deepseek-reasoner` werden am
> 2026-07-24 15:59 UTC deprecated — fuer Neubau direkt V4 verwenden.
> Update 2026-07-02: V4 direkt pinnen; bei Frameworks wie LiteLLM explizit pruefen, ob `reasoning_content` fuer V4-Thinking-Multiturn/Tool-Calls erhalten bleibt.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Kurzcheck = Stufe-A-Pflichtlektüre
> (`Read` mit `limit=80`). Volltext bei Fehlern im Bereich (Stufe B) und vor
> Hochrisiko-Arbeit (Stufe C).

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Modellwahl | Neu `deepseek-v4-flash`/`-pro` (1M Kontext); alte Namen ab 2026-07-24 weg | §1 |
| 2 | SDK/Auth | OpenAI-kompatibel, `base_url=.../com`; ein Key für `/v1` und `/anthropic` | §2 |
| 3 | reasoning_content | R1: strippen; V3.2/V4-Thinking+Tools: zurückgeben (sonst 400) | §3 |
| 4 | Reasoner-Params | `temperature`/`top_p`/penalties weglassen; `logprobs` wirft Error | §4 |
| 5 | Caching | Stabilen Prefix vorn halten; `prompt_cache_hit_tokens` messen | §5 |
| 6 | JSON Mode | `json_object` + Wort "json" im Prompt + Beispiel; `max_tokens` hoch | §6 |
| 7 | Function Calling | Über non-thinking/`deepseek-chat`; Beta-`strict`-Mode für Schema | §7 |
| 8 | Limits/Streaming | V4 Concurrency-Limits → Backoff; `reasoning_content`+`content` getrennt | §8 |

## 1. Modelle & Kontext
- Fuer NEUE Integrationen `deepseek-v4-flash` (guenstig, hoher Durchsatz) oder `deepseek-v4-pro` (staerker) verwenden — nicht mehr die deprecateten `deepseek-chat`/`deepseek-reasoner`. Quelle: https://api-docs.deepseek.com/ · offiziell
- Beide V4-Modelle: 1M Token Kontextfenster, max. 384K Output. Thinking-Mode loest das alte reasoner-Modell ab (non-thinking = chat). Quelle: https://api-docs.deepseek.com/quick_start/pricing · offiziell

## 2. OpenAI-/Anthropic-Kompatibilitaet & SDK-Konfiguration
- DeepSeek ist OpenAI- und Anthropic-kompatibel: einfach `base_url="https://api.deepseek.com"` setzen und das OpenAI-SDK weiterverwenden (`/v1` ist optional, `v1` ist KEINE Modellversion). Quelle: https://api-docs.deepseek.com/ · offiziell
- Auth ueber Standard `Authorization: Bearer <KEY>`; Key von platform.deepseek.com/api_keys. Gleicher Key gilt fuer `/v1`- und `/anthropic`-Endpoint. Quelle: https://api-docs.deepseek.com/ · offiziell

## 3. reasoning_content / Thinking-Mode
- Beim klassischen Reasoner NUR `content` in die Folge-History zurueckgeben — `reasoning_content` MUSS gestrippt werden, sonst 400. Lokal anzeigen/loggen ist OK. Quelle: https://api-docs.deepseek.com/guides/reasoning_model · offiziell
- Achtung Umkehr je Generation: V3.2/V4-Thinking + Tool-Use verlangen das `reasoning_content` der assistant-Messages ZURUECK (sonst 400). Der Code muss die Generation kennen (siehe Bug-Almanach 1 vs. 2). Quelle: https://api-docs.deepseek.com/guides/reasoning_model · offiziell

## 4. Unsupported Parameters beim Reasoner / Thinking
- `temperature`, `top_p`, `presence_penalty`, `frequency_penalty` werden beim Reasoner ignoriert (kein Fehler, aber wirkungslos) — nicht senden, um Verwirrung zu vermeiden. Quelle: https://api-docs.deepseek.com/guides/reasoning_model · offiziell
- `logprobs` / `top_logprobs` loesen beim Reasoner einen echten Fehler aus — strikt vermeiden. Sampling-Params nur bei non-thinking/`deepseek-chat`-Modus nutzen. Quelle: https://api-docs.deepseek.com/guides/reasoning_model · offiziell

## 5. Context/Disk Caching (automatisch)
- Caching laeuft automatisch auf Disk und greift nur bei exaktem Prefix-Match — stabilen Teil (System-Prompt, lange Dokumente) VORNE halten, Variables ans Ende. Quelle: https://api-docs.deepseek.com/guides/kv_cache · offiziell
- Cache-Effizienz ueber `prompt_cache_hit_tokens` / `prompt_cache_miss_tokens` in `usage` messen. Best-effort, kein 100%-Hit; Cache-Aufbau braucht Sekunden. Cache-Hit-Preis ist ~50x guenstiger als Miss. Quelle: https://api-docs.deepseek.com/quick_start/pricing · offiziell

## 6. JSON Mode / response_format
- `response_format={'type':'json_object'}` setzen UND das Wort "json" im System-/User-Prompt nennen plus ein Beispiel des gewuenschten JSON-Formats liefern — sonst greift der Mode nicht zuverlaessig. Quelle: https://api-docs.deepseek.com/guides/json_mode · offiziell
- `max_tokens` ausreichend hoch setzen (Abschneiden vermeidet leeres/kaputtes JSON); bei gelegentlich leerem `content` Prompt anpassen — bekanntes, offiziell eingeraeumtes Verhalten. Quelle: https://api-docs.deepseek.com/guides/json_mode · offiziell

## 7. Function Calling
- Function Calling ueber den non-thinking/`deepseek-chat`-Modus fahren — der Reasoner unterstuetzt `tool_choice` nicht (400, siehe Bug-Almanach 4). Tools im `tools`-Array uebergeben, Funktion selbst clientseitig ausfuehren. Quelle: https://api-docs.deepseek.com/guides/function_calling · offiziell
- Fuer garantiertes Schema den Beta-`strict`-Mode nutzen (`base_url=.../beta`, `"strict":true`): nur object/string/number/integer/boolean/array/enum/anyOf, alle Properties `required`, `additionalProperties:false`, KEIN `minLength`/`maxLength` (stattdessen `pattern`). Quelle: https://api-docs.deepseek.com/guides/function_calling · offiziell

## 8. Rate-Limits, Streaming & max_tokens
- V4 hat jetzt HARTE Concurrency-Limits (account-weit, key-unabhaengig): v4-pro 500, v4-flash 2.500 gleichzeitige Requests. Bei Ueberschreitung HTTP 429 → exponentielles Backoff; bei dauerhaftem Bedarf kostenlosen Capacity-Expansion-Request stellen. Quelle: https://api-docs.deepseek.com/quick_start/rate_limit · offiziell
- Streaming: `delta.reasoning_content` und `delta.content` getrennt akkumulieren (beide koennen null sein), SSE-Keep-alive-Kommentare (`: keep-alive`) ignorieren; Server schliesst nach 10 min ohne Inferenz-Start. `max_tokens` explizit setzen und auf `finish_reason:"length"` pruefen (Defaults schneiden sonst ab). Quelle: https://api-docs.deepseek.com/quick_start/rate_limit · offiziell

## 🔗 Bezug zum Bug-Almanach
| Best-Practice | Bug-Abschnitt (`bugs/apis/deepseek-api.md`) |
|---|---|
| 1 Modelle & Kontext | 8, 9, Fix-Status (Deprecation 2026-07-24) |
| 2 OpenAI-Kompatibilitaet & SDK | 11, Zusatz (Endpoints) |
| 3 reasoning_content / Thinking | A.1, A.2, A.3 (TL;DR 1) |
| 4 Unsupported Parameters | C.7 (TL;DR 3) |
| 5 Context/Disk Caching | F.14 |
| 6 JSON Mode / response_format | B.5, B.6 |
| 7 Function Calling | B.4, B.5, B.6 (TL;DR 2) |
| 8 Rate-Limits, Streaming & max_tokens | D.8, D.9, E.10, E.12, F.13 (TL;DR 4) |
