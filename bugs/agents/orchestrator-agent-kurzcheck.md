# Boss-/Orchestrator-Agent in einem Multi-Agenten-System Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Dieser Kurzcheck ist die Vorab-Pflichtlektüre
> (Stufe A, `Read` mit `limit=80`). Der Volltext darunter ist Pflicht bei JEDEM Fehler in
> diesem Bereich (Stufe B). Der Kurzcheck ersetzt den Volltext nicht.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | Vager/mehrdeutiger Befehl | Plan/Read-back zeigen, bei Unsicherheit nachfragen statt raten | §1.1 |
| 2 | Agent tut etwas anderes als gemeint | Intent + betroffene Dateien vor Ausfuehrung bestaetigen | §1.3 |
| 3 | Agent erweitert Umfang eigenmaechtig | Scope als expliziten Constraint in den Prompt | §1.4 |
| 4 | Schleife/Delegation terminiert nicht | Echte Stop-Bedingung; Limit nur als Sicherheitsnetz | §2.1 |
| 5 | Parallele Worker schreiben denselben State-Key | Reducer `Annotated[list, operator.add]` setzen | §2.5 |
| 6 | Sub-Agent spawnen | Schlanker Prompt + tools-Whitelist; KEIN Voll-Kontext | §3.1 |
| 7 | Viele MCP-Server, Sub crasht bei 0 Token | tools-Whitelist + Tool-Search (deferred Schemas) | §3.2 |
| 8 | Opus 4.8 tool_use kaputt/legacy-XML | Defensiv parsen; interleaved-thinking aus + retry | §4.1 |
| 9 | Opus 4.8 meldet „verified/done" | Build/Test programmatisch verifizieren, nie dem Modell trauen | §4.10 |
| 10 | Opus 4.8 + temperature/top_p/tool_choice | KEINE Sampling-Parameter senden (HTTP 400) | §4.11 |
| 11 | Tool-Args vor Ausfuehrung | Gegen striktes Schema validieren + Retry mit Fehlerkontext | §4.3 |
| 12 | Tool-Auswahl wird schlecht | <20 Tools/Call; progressive disclosure / Tool-Search | §4.7 |
| 13 | Langer Dialog, Agent vergisst/driftet | Persona re-injizieren; Recap+Snowball; Sycophancy meiden | §5.1 |
| 14 | Sub-Agent-Output uebernehmen | Untrusted: Verifier + typed-schema (Error-Amplifikation 17x) | §7.1 |
| 15 | From-scratch Loop: orphaned tool_use → 400 | tool_use/tool_result-Paare zusammen halten/trimmen | §8.1 |
