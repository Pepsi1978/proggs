# Orchestrator-/Boss-Agent Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Kurzcheck = Stufe-A-Pflichtlektüre
> (`Read` mit `limit=80`). Volltext bei Fehlern im Bereich (Stufe B) und vor
> Hochrisiko-Arbeit (Stufe C).

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Boss-Agent neu bauen | Erst einfach (Single-Agent + gute Tools), Multi-Agent nur bei Bedarf | §Grundhaltung |
| 2 | Intent absichern | Plan/Read-back vor Aktion; bei Unsicherheit AskUserQuestion | §1 |
| 3 | Mehrdeutige Anfrage | Staerkstes Modell (Opus) + Scope als Constraint | §1 |
| 4 | Nach >2 Fehlversuchen | STOP statt variieren-und-raten (3-Iterationen-Stop) | §1 |
| 5 | Routing aufsetzen | Typisieren (`Literal[...]`); echte Stop-Bedingung, Limit nur Netz | §2 |
| 6 | Parallele State-Keys | Reducer (`Annotated[list, operator.add]`) gegen Update-Verlust | §2 |
| 7 | Sub-Agent spawnen | tools-Whitelist + schlanker Prompt; nur Pfad+1-2k-Summary zurueck | §3 |
| 8 | Worker-Crash moeglich | Orchestrator-Resume (Subagents haben KEIN Auto-Compact) | §3 |
| 9 | Tool-Calling | Detaillierte Descriptions, <20 Tools/Turn, `strict:true`, Args validieren | §4 |
| 10 | Opus 4.8 | KEINE Sampling-Parameter; robustes Parsen fuer legacy-XML tool_use | §4 |
| 11 | „done"-Meldung des Modells | Tool-Wirkung rules-based verifizieren (Build/Lint/Test) | §4 |
| 12 | Langer Dialog/Voice | Persona re-injizieren; Recap+Snowball; satzweise streamen, Barge-In <60ms | §5 |
| 13 | Reliability/Security | Verifier-Agent + typed-schema; Budget/Circuit-Breaker im Gateway | §6 |
| 14 | Fremd-/Tool-Content | Nur in `tool_result`, als untrusted; MCP Least-Privilege | §6 |
| 15 | From-scratch Loop | `stop_reason` pruefen; max_iterations+Timeout; tool_use/result-Paare halten | §7a |
