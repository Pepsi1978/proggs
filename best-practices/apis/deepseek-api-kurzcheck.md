# DeepSeek API Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

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
