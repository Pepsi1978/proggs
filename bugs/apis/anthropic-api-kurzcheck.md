# Anthropic Claude API (Messages API, Integration) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Dieser Kurzcheck ist die Vorab-Pflichtlektüre
> (Stufe A, `Read` mit `limit=80`). Der Volltext darunter ist Pflicht bei JEDEM Fehler in
> diesem Bereich (Stufe B). Der Kurzcheck ersetzt den Volltext nicht.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | Direkter HTTP-Call | `x-api-key` (nicht Bearer) + `anthropic-version: 2023-06-01` | §1, §2 |
| 2 | 400 Messages-Struktur | `system` Top-Level, `max_tokens` Pflicht, Rollen alternieren | §3, §4, §5 |
| 3 | 400 tool_use/tool_result | Paarweise + ID-gematcht; beim Compacten nie nur eine Seite | §7 |
| 4 | Streaming Tool-Args | `input_json_delta` erst bei `content_block_stop` parsen | §9 |
| 5 | Fehler nach HTTP-200 | SSE-`error`-Event behandeln, auf `message_stop` warten | §10, §11 |
| 6 | Prompt Caching greift nicht | TTL 5 Min, Mindest-Tokens, byte-identischer Prefix | §14, §15, §16 |
| 7 | Multi-Turn mit thinking | thinking-Blocks unverändert 1:1 in History zurück | §20 |
| 8 | 429/529 | `anthropic-ratelimit-*`+`retry-after` lesen, 529≠500≠504 | §21, §22 |
| 9 | Doppelte Retries / Hangs | SDK `max_retries=2`; bei eigener Retry-Logik `max_retries=0` | §26 |
