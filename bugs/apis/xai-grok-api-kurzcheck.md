# xAI Grok API (Integration) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Dieser Kurzcheck ist die Vorab-Pflichtlektüre
> (Stufe A, `Read` mit `limit=80`). Der Volltext darunter ist Pflicht bei JEDEM Fehler in
> diesem Bereich (Stufe B). Der Kurzcheck ersetzt den Volltext nicht.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | Alter Slug läuft fehlerfrei ⭐ | 8 Slugs still auf `grok-4.3` redirected — explizit pinnen, Kosten/Qualität prüfen | §A |
| 2 | `reasoning_effort` setzen | Bewusst `none/low/medium/high`; bei grok-4 nur korrekt für 4.3 | §A |
| 3 | Tools / structured outputs ⭐ | Streaming kann das nicht → `stream=false` | §B/§C |
| 4 | Schema für Tools/Output | Strict-Regeln (kein leeres `enum`/`anyOf`, `prefixItems` statt Array-`items`) | §C |
| 5 | 429 trotz Restbudget ⭐ | Eigenes Exp-Backoff, keine Retry-Header; Reasoning-Tokens in TPM | §D |
| 6 | Endpunkt-Wahl | OpenAI-kompatibler `/v1`, nicht der deprecatete Anthropic-Pfad | §E |
| 7 | Live/Web-Search | Nur EIN Domain-Filter (≤5 Domains), Verbrauch überwachen | §F |
