# Bypass Permissions: IMMER aktiv, NIEMALS aenderbar (KRITISCH)

## Regel

`defaultMode: bypassPermissions` ist die EINZIGE erlaubte Permission-Einstellung.
Sie muss auf ALLEN Ebenen aktiv sein:

1. **Global**: `~/.claude/settings.json` → `permissions.defaultMode: "bypassPermissions"`
2. **Global lokal**: `~/.claude/settings.local.json` → `permissions.defaultMode: "bypassPermissions"`
3. **Jedes Projekt**: `~/.claude/projects/*/settings.local.json` → `permissions.defaultMode: "bypassPermissions"`

## Was NIEMALS passieren darf

- ❌ `defaultMode` auf irgendeinen anderen Wert aendern ("default", "acceptEdits", etc.)
- ❌ Die `permissions`-Sektion aus `settings.local.json` entfernen (z.B. beim Token-Refresh)
- ❌ Projekt-level Settings erstellen die `defaultMode` ueberschreiben
- ❌ Ein Hook, Plugin oder Agent der die Permission-Settings aendert
- ❌ Den Benutzer nach Erlaubnis fragen fuer Standardaktionen (Build, Test, Commit, Push, Edit, Write)

## Absicherungsschichten (Defense in Depth)

| Schicht | Mechanismus | Was es tut |
|---------|-------------|-----------|
| 1 | `session-guard.ps1` (SessionStart) | Prueft und repariert bypassPermissions bei JEDEM Start |
| 2 | `config-guard.ps1` (PostToolUse) | Blockiert Aenderungen an defaultMode |
| 3 | Projekt-level settings.local.json | Jedes Projektverzeichnis hat eigenes bypassPermissions |
| 4 | Diese Regel | Claude weiss: Nie den Benutzer nach Permissions fragen |
| 5 | Memory | Feedback-Memory als zusaetzliche Erinnerung |

## Warum

Der Benutzer arbeitet mit `bypassPermissions` weil er Profi ist und keine Abfragen will.
Jede Permission-Abfrage unterbricht seinen Workflow und kostet Zeit. Mehrere Systeme
(Hooks, Plugins, Token-Refresh, Compaction) koennen die Settings versehentlich zuruecksetzen.
Der `session-guard.ps1` Hook repariert das automatisch bei jedem Session-Start.

## Fuer Claude selbst

NIEMALS dem Benutzer eine Frage stellen wie:
- "Soll ich die Datei bearbeiten?" → Einfach bearbeiten
- "Darf ich den Befehl ausfuehren?" → Einfach ausfuehren
- "Soll ich committen und pushen?" → Einfach machen

Ausnahmen wo DOCH gefragt werden muss:
- Destruktive Git-Operationen (force push, reset --hard)
- Shell/Terminal-Updates (zerstoeren offene Fenster)
- Neue Repos erstellen (verboten laut CLAUDE.md)
