# OpenAI API (Integration in eigene Software) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Dieser Kurzcheck ist die Vorab-Pflichtlektüre
> (Stufe A, `Read` mit `limit=80`). Der Volltext darunter ist Pflicht bei JEDEM Fehler in
> diesem Bereich (Stufe B). Der Kurzcheck ersetzt den Volltext nicht.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | 404 `model_not_found` | Modellname aus Config, Deprecations-Seite prüfen (4o/4.1/o4-mini retired) | H22 |
| 2 | reasoning-Modell (o-Reihe/gpt-5) | Kein `temperature`/`top_p`; `max_completion_tokens`; `developer`-Rolle | A1, B5, B6 |
| 3 | `status: incomplete` / leere Antwort | Token-Budget großzügig, reasoning-Tokens zählen mit | A2 |
| 4 | Streaming Tool-Args | Pro `index` akkumulieren, erst nach Stream-Ende parsen | C8, C9 |
| 5 | Streaming-Parser crasht | `data: [DONE]` ist kein JSON; abfangen vor Parse | C11 |
| 6 | `usage` ist null beim Streaming | `stream_options.include_usage=true` setzen | C10 |
| 7 | 400 nach `tool_calls` | Für jede `tool_call_id` eine `tool`-Antwort anhängen | D12, D13 |
| 8 | 429 Rate-Limit | `retry-after-ms`/`x-ratelimit-*` lesen, Backoff mit Jitter, RPM≠TPM | F17, F18 |
| 8a | **503 mitten im Mehrschritt-Lauf** ⭐ | Selbst erzeugte Überlast (viele parallele Streams/1 Konto). 3 Fixes zusammen: ≥6 Versuche exponentiell+Jitter · Drossel bei 429/503 auf ~2 · JEDEN teuren Teilschritt persistieren + fortsetzen statt neu | F18a |
| 9 | `strict:true`-Schema | `additionalProperties:false` + alle Keys `required`, `refusal` prüfen | G19, G21 |
