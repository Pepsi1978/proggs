# Bypass Permissions: IMMER aktiv, NIEMALS aenderbar (KRITISCH)

## Regel
`defaultMode: bypassPermissions` ist die EINZIGE erlaubte Permission-Einstellung, auf ALLEN Ebenen:
global (`~/.claude/settings.json` + `settings.local.json`), jedes Projekt
(`~/.claude/projects/*/settings.local.json`).

## Was NIEMALS passieren darf
- `defaultMode` auf einen anderen Wert aendern ("default"/"acceptEdits"/...) - `permissions`-Sektion
  entfernen (z.B. Token-Refresh) - Projekt-Settings die `defaultMode` ueberschreiben - Hook/Plugin/Agent
  der die Permission-Settings aendert - `allow`-Liste in `permissions` (wirkt als Whitelist-Blocker bei
  bypassPermissions!)
- Benutzer nach Erlaubnis fragen fuer Standardaktionen (Build, Test, Commit, Push, Edit, Write)

## Absicherung (Defense in Depth)
`session-guard.ps1` (SessionStart) prueft+repariert bypassPermissions UND entfernt die allow-Liste bei
JEDEM Start - `config-guard.ps1` (PostToolUse) blockiert Aenderungen an `defaultMode` UND allow-Listen -
Projekt-Settings, diese Regel + Feedback-Memory als Schichten.

## Fuer Claude selbst
NIE fragen "Soll ich bearbeiten / ausfuehren / committen+pushen?" -> einfach MACHEN. Ausnahmen wo DOCH
gefragt wird: destruktive Git-Ops (force push, `reset --hard`), Shell/Terminal-Updates, neue Repos
(verboten laut CLAUDE.md).

## Warum
Hooks/Token-Refresh/Compaction koennen die Settings versehentlich zuruecksetzen; `session-guard`
repariert das bei jedem Start.
