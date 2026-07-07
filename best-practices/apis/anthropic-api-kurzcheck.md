# Anthropic Claude API (Messages API) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Kurzcheck = Stufe-A-Pflichtlektüre
> (`Read` mit `limit=80`). Volltext bei Fehlern im Bereich (Stufe B) und vor
> Hochrisiko-Arbeit (Stufe C).

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Prompt Caching | `cache_control` auf letzten STATISCHEN Block, Mindest-Tokens beachten | Prompt Caching |
| 2 | Tool Use | Detaillierte Descriptions (3–4 Sätze), `strict:true`, Tools namespacen | Tool Use |
| 3 | Tool-Fehler | Via `is_error:true` + lehrreiche Meldung (Claude retryt 2–3×) | Tool Use |
| 4 | Extended Thinking (Opus 4.8/4.7) | Nur Adaptive Thinking + `effort`; thinking-Blöcke 1:1 in History | Extended Thinking |
| 5 | Message-Struktur | Keine `tool`/`function`-Rollen; `system` Top-Level (Array für Caching) | Message-Struktur & Streaming |
| 6 | Streaming bei hohem max_tokens | SDK `.stream()` + `get_final_message()` (vermeidet Timeouts) | Message-Struktur & Streaming |
| 7 | Async / Kostenersparnis | Batch API = 50 %; mit 1h-Cache kombinieren | Batch API & Token-Counting |
