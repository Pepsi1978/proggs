# AGENTS.md, Gedächtnis/Memory, Regeln & Kontext Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Wo AGENTS.md liegt | Projekt-Root `AGENTS.md` (ins Git committen) + global `~/.config/opencode/AGENTS.md`; OpenCode liest sie als Rules-Datei direkt in den Kontext | §1 |
| 2 | Kombiniert vs. überschrieben | Projekt- + globale AGENTS.md werden KOMBINIERT (nicht überschrieben); pro Kategorie gewinnt aber die erste Datei → `AGENTS.md` schlägt `CLAUDE.md` | §1, §3 |
| 3 | Verschachtelte AGENTS.md | Unterordner-AGENTS.md werden NICHT automatisch geladen (Issue #7576) → über `instructions`-Glob (`packages/*/AGENTS.md`) einbinden, nicht nur ablegen | §1, §4 |
| 4 | AGENTS.md erzeugen | `/init` scannt das Repo und erstellt/aktualisiert AGENTS.md „in place" (Build/Lint/Test-Befehle, Architektur, Konventionen) | §2 |
| 5 | CLAUDE.md-Fallback | `CLAUDE.md` / `~/.claude/CLAUDE.md` greifen nur, wenn KEIN AGENTS.md existiert; abschaltbar via `OPENCODE_DISABLE_CLAUDE_CODE*` | §3 |
| 6 | Zusatz-Regeldateien | `instructions`-Array in `opencode.json` (Pfade/Globs, Remote-URLs erlaubt) → additiv zu AGENTS.md; bester Weg für Monorepos | §4 |
| 7 | Kein echtes Langzeitgedächtnis | OpenCode hat kein semantisches Memory out-of-the-box — nur persistente Sessions + statische Instruktionsdateien; lernendes Memory nur via Plugins | §5 |
| 8 | Sessions / Storage | Sessions auto-gespeichert in `~/.local/share/opencode/` (Win: `%USERPROFILE%\.local\share\opencode`); KEIN Prune-Befehl, Storage wächst unbegrenzt | §5 |
| 9 | Kontext kompaktieren | `/compact` (`ctrl+x c`); `compaction.auto/prune/reserved` in der Config; `prune:true` bei langen Sessions mit vielen Tool-Outputs | §6 |
| 10 | AGENTS.md kurz halten (WICHTIGSTES) | möglichst < ~150 Zeilen; ab ~200–400 Z. degradiert die Befolgung; jeder Token wird bei JEDER Anfrage geladen | §7 |
| 11 | Damit das Modell folgt | Test-Befehle wirklich auflisten (Agent führt sie aus + fixt Fehler); spezifisch + terse; AGENTS.md als lebende Doku committen | §7 |
