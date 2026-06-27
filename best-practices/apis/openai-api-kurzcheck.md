# OpenAI API Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Kurzcheck = Stufe-A-Pflichtlektüre
> (`Read` mit `limit=80`). Volltext bei Fehlern im Bereich (Stufe B) und vor
> Hochrisiko-Arbeit (Stufe C).

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Neues Projekt | Responses API statt Chat Completions (besseres Caching, stateful) | §1 |
| 2 | Structured Output | `strict:true`, `additionalProperties:false`, alle Felder `required` | §2 |
| 3 | Tool Calling | 0..n Tool-Calls behandeln; `parallel_tool_calls` gezielt steuern | §3 |
| 4 | reasoning-Modell | `reasoning_effort`-Knopf, `developer`-Rolle, Token-Budget großzügig | §4 |
| 5 | Token-Effizienz | Caching ab 1024 Tok (Statisches nach vorne); Batch API = 50 % | §5 |
| 6 | Streaming | `stream_options.include_usage:true` für Usage im Finalchunk | §6 |
| 7 | SDK-Konfiguration | Default `max_retries=2` nicht eigene Retry-Schleife darüber stapeln | §7 |
