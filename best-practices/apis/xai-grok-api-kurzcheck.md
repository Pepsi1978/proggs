# xAI Grok API Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Kurzcheck = Stufe-A-Pflichtlektüre
> (`Read` mit `limit=80`). Volltext bei Fehlern im Bereich (Stufe B) und vor
> Hochrisiko-Arbeit (Stufe C).

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Modellwahl | `grok-4.3` explizit pinnen, nie retired Slugs | §1 |
| 2 | Denktiefe | `reasoning_effort` gilt jetzt auch für 4.3 (`none/low/medium/high`) | §2 |
| 3 | Structured Outputs | `json_schema`; Limits 2048 Zeichen/256 Items/64 Props, kein zirkulärer `$ref` | §3 |
| 4 | Tools | Tool-Args immer strict → Schema einfach halten; `messages` pflegen | §4 |
| 5 | Live Search | Nur EIN Domain-Filter (≤5); Verbrauch überwachen | §5 |
| 6 | Streaming | Bei Reasoning Timeout hochsetzen; für Tools/Output `stream=false` | §6 |
| 7 | Rate-Limits | Eigenes Exp-Backoff (kein Retry-Header); Reasoning-Tokens in TPM | §7 |
| 8 | SDK & Auth | `xai-sdk`/OpenAI-`/v1`/`@ai-sdk/xai`; Anthropic-Pfad deprecatet | §8 |
