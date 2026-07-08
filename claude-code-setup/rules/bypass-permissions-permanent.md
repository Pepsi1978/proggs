# Bypass Permissions: IMMER aktiv, NIEMALS aenderbar (KRITISCH)

## Regel

`defaultMode: bypassPermissions` ist die EINZIGE erlaubte Permission-Einstellung, auf ALLEN Ebenen:
global (`~/.claude/settings.json` + `settings.local.json`), jedes Projekt
(`~/.claude/projects/*/settings.local.json`).

## Was NIEMALS passieren darf

- `defaultMode` auf einen anderen Wert aendern ("default"/"acceptEdits"/…) · die `permissions`-Sektion
  entfernen (z.B. beim Token-Refresh) · Projekt-Settings die `defaultMode` ueberschreiben · ein
  Hook/Plugin/Agent der die Permission-Settings aendert · eine `allow`-Liste in `permissions` (wirkt
  als Whitelist-Blocker bei bypassPermissions!)
- Den Benutzer nach Erlaubnis fragen fuer Standardaktionen (Build, Test, Commit, Push, Edit, Write)

## Absicherung (Defense in Depth)

`session-guard.ps1` (SessionStart) prueft+repariert bypassPermissions UND entfernt die allow-Liste bei
JEDEM Start · `config-guard.ps1` (PostToolUse) blockiert Aenderungen an `defaultMode` UND allow-Listen ·
Projekt-Settings, diese Regel und ein Feedback-Memory als weitere Schichten.

## Fuer Claude selbst

NIE fragen "Soll ich die Datei bearbeiten / den Befehl ausfuehren / committen+pushen?" → einfach MACHEN.
Ausnahmen wo DOCH gefragt wird: destruktive Git-Ops (force push, `reset --hard`), Shell/Terminal-Updates
(zerstoeren offene Fenster), neue Repos erstellen (verboten laut CLAUDE.md).

## Warum

Frank arbeitet mit bypassPermissions weil er Profi ist und keine Abfragen will — jede Abfrage
unterbricht den Workflow. Mehrere Systeme (Hooks, Token-Refresh, Compaction) koennen die Settings
versehentlich zuruecksetzen; `session-guard` repariert das automatisch bei jedem Start.
