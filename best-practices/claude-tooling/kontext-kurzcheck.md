# Kontext-Management Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Schnell-Orientierung der Harness-Best-Practices;
> der Volltext darunter ist die Tiefe. Vor Arbeit am jeweiligen Werkzeug lesen.

| # | Thema / Situation | Kernregel (Kurzform) | Abschnitt |
|---|-------------------|----------------------|-----------|
| 1 | CLAUDE.md-Groesse | Ziel < 200 Zeilen; Detail in Rules/Skills (Context-Rot ab ~50 % Fuellung) | CLAUDE.md |
| 2 | `CLAUDE_AUTOCOMPACT_PCT_OVERRIDE` | kann die Schwelle nur SENKEN (Math.min), nie erhoehen | OVERRIDE |
| 3 | was Compaction ueberlebt | Root-CLAUDE.md via Disk-Reread; reine Chat-Instruktionen gehen verloren | Was Komprimierung ueberlebt |
| 4 | `MEMORY.md` | nur ~200 Zeilen / 25 KB werden geladen — Index kurz, Detail auslagern | Auto Memory |
| 5 | grosse Daten | Subagents / File-as-Memory statt Hot-Context (lossless) | Subagents fuer Kontext-Schutz |
| 6 | MCP/Skills | deferred / on-demand laden statt Session-Start-Aufblaehung | MCP Tool Definitions |
| 7 | Compact Instructions | in CLAUDE.md pflegen — steuert was die Zusammenfassung behaelt | Compact Instructions |
