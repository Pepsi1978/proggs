# Agents, Custom Agents, Modes, Subagents & Permissions Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Modes sind deprecated | „Modes" (Plan/Build) sind ins Agent-System überführt; alte `mode`-Config + `modes/`-Ordner deprecated → alles über `agent`-Block + Frontmatter-Key `mode` (`primary`/`subagent`/`all`) | Kopf, §7 |
| 2 | Eingebaute Agents | Primary: `build` (Vollzugriff, Default), `plan` (read-only, Edits/Bash auf `ask`); Subagents: `general`, `explore` (read-only), `scout` (externe Docs) | §1 |
| 3 | Agent umschalten | `Tab` (rückwärts `shift+tab`) wechselt den Primary-Agent; `default_agent` muss Primary sein, sonst Fallback `build` | §2 |
| 4 | Custom Agent — Syntax-Falle | KORREKT: Objekt `"agent": { "<name>": {...} }`, Key `prompt` (NICHT `systemPrompt`), Permission-Keys `edit`/`bash`/`webfetch`. Blog-Array `"agents":[...]` ist FALSCH | §3 |
| 5 | Markdown-Agent | `~/.config/opencode/agents/*.md` (Plural!) bzw. `.opencode/agents/*.md`; Dateiname = Agent-Name; YAML-Frontmatter = Config, Body = System-Prompt | §3 |
| 6 | Subagent aufrufen | automatisch (Task-Tool) ODER `@agent-name`; Nutzer kann jeden Subagent per `@` aufrufen, auch wenn `task`-Permission die AUTO-Delegation sperrt | §4 |
| 7 | Modell pro Agent (Token-Spar) | Subagents ERBEN das Primary-Modell ohne `model`-Angabe → günstige Subagents brauchen explizites `model`; `small_model` für Leichtgewicht-Tasks; `steps` deckelt Iterationen | §5 |
| 8 | Permissions | Werte `allow`/`ask`/`deny`; granular gewinnt die LETZTE passende Regel (Catch-all `"*"` zuerst); `edit` deckt write/edit/patch; Wildcard `mymcp_*` sperrt ganze MCP-Server | §6 |
| 9 | Best-Practice-Regeln | richtiger Agent pro Aufgabe (nicht alles mit `build`); restriktive Permissions per Default; niedrige Temp (0.0–0.2) für technische Tasks; ein Primary + viele Subagents; Agents nach Erstellung testen | §8 |
