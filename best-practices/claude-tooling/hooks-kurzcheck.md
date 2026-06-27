# Hooks Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Schnell-Orientierung der Harness-Best-Practices;
> der Volltext darunter ist die Tiefe. Vor Arbeit am jeweiligen Werkzeug lesen.

| # | Thema / Situation | Kernregel (Kurzform) | Abschnitt |
|---|-------------------|----------------------|-----------|
| 1 | Hook soll blockieren | `exit 2` (nie `exit 1`); Write/Edit via `permissionDecision:"deny"` | Exit-Codes |
| 2 | Kontext an Claude geben | verschachteltes `hookSpecificOutput.{hookEventName,additionalContext}` (PS `-Depth 5`) | JSON-Schema |
| 3 | Side-Effect-Hook (SubagentStop/Stop) | Input-Guard, sonst Phantom-Fires; passive Injection: KEIN Guard | Input-Guard |
| 4 | Stop-Hook | `stop_hook_active` zuerst pruefen (Endlosschleife) | Stop-Loop-Schutz |
| 5 | matcher | exakt `Edit\|Write`, MCP mit `.*`-Suffix | Hook-Events |
| 6 | Standalone-Hook-Ende | `exit 0`; FAIL-OPEN (interner Fehler → `exit 0`) | Hook-Kategorien |
| 7 | Windows/Bash/Cross-Platform | `pwsh`, BOM-frei, LF, `+x`; `.ps1`+`.sh` paritaetisch; dot-sourced libs ohne top-level `exit` | Sicherheit / Kategorien |
| 8 | Tiefen-Praevention | dedizierte Gegenseite: `claude-hooks.md` + Almanach (Stufe C) | — |
