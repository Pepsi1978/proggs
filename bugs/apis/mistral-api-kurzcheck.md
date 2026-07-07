# Mistral AI API (Integration) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Dieser Kurzcheck ist die Vorab-Pflichtlektüre
> (Stufe A, `Read` mit `limit=80`). Der Volltext darunter ist Pflicht bei JEDEM Fehler in
> diesem Bereich (Stufe B). Der Kurzcheck ersetzt den Volltext nicht.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | Tool-Calling (400/422) ⭐ | `tool_call_id` = exakt 9 Zeichen `[a-zA-Z0-9]`, unveraendert durchreichen | §4 |
| 2 | "number of function calls" ⭐ | Pro `tool_call` genau eine `tool`-Antwort, gleiche Anzahl | §5 |
| 3 | JSON-Mode haengt ⭐ | Wort „JSON" muss im Prompt stehen, sonst Whitespace-Stream | §14 |
| 4 | Codestral / FIM (404) | Eigener Endpunkt+Key, FIM unter `/v1/fim/completions` | §2, §20 |
| 5 | Token-Count / 400 | `mistral-common`-Tokenizer; Kontext-Ueberschreitung = hartes 400 | §12, §13 |
| 6 | Modell „not found" (400/404) ⭐ | Datierte Version pinnen, Aliase zentral; aggressive Deprecations | §22, §23 |
| 7 | 401 bei Auth | Nur nackten Key; `Bearer` genau einmal; Key zum Endpunkt | §1, §2 |
| 8 | 429 trotz normaler Last | Workspace-weite Limits; `Retry-After`/`X-RateLimit-Remaining` | §21 |
