# Claude Code — Desktop-App vs. CLI Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Drei Tabs im Desktop | **Chat** (Gespräche), **Cowork** (Dispatch/lange Agenten-Arbeit), **Code** (Programmieren). Zum Coden → Tab **Code**. | §1 |
| 2 | Gleiche Engine | Desktop und CLI nutzen **dieselbe Engine** + dieselben Configs (CLAUDE.md, MCP, Hooks, Skills, settings.json). Parallel auf demselben Projekt erlaubt. | §2 |
| 3 | Stärke Desktop | Parallele Sessions in EINEM Fenster, automatische **Git-Worktrees**, visuelle Diffs, Vorschau-Browser, PR-Monitoring, Panes nebeneinander. | §3 |
| 4 | Stärke CLI | **Scripting & Automatisierung**: `--print`, `--output-format`, Agent SDK / Headless, Cron/CI, Agent-Teams, `dontAsk`-Modus. | §4 |
| 5 | NICHT im Desktop | Headless/Scripting, Agent-Teams (die sich Nachrichten schicken), `/agents` `/doctor` `/config` `/permissions`, Inline-Code-Vorschläge, Linux, Drittanbieter (Bedrock/Vertex/Foundry) standardmäßig. | §4 |
| 6 | CLI → Desktop wechseln | Im Terminal `/desktop` tippen → Session öffnet im Desktop, CLI beendet sich. (Nur mit Abo-Login, nicht mit API-Key/Bedrock/Vertex.) | §2 |
| 7 | Frank-spezifisch | Seine Hook-/Agent-Team-/Headless-Workflows sind **CLI-Kernland**. Im Desktop verschwinden Agent-Teams + Headless; Hooks/Skills/Subagenten bleiben. | §5 |
