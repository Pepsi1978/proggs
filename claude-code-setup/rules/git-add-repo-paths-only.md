# Git Add: NUR Repo-Pfade, NIEMALS Home-Verzeichnis-Pfade (KRITISCH)

## Regel

`git add` darf AUSSCHLIESSLICH Pfade verwenden die INNERHALB von `~/proggs/` liegen.
NIEMALS Pfade aus `~/.claude/` oder anderen Verzeichnissen ausserhalb des Repos.

## Warum

Das Git-Repository ist `~/proggs/`. Das Verzeichnis `~/.claude/` liegt im Home-Verzeichnis
und ist NICHT Teil des Repos. `git add ~/.claude/...` schlaegt IMMER fehl mit:
`fatal: pathspec '.claude/hooks/...' did not match any files`

## Haeufiger Fehler (2026-03-28)

Hook-Dateien werden an ZWEI Stellen erstellt:
1. `~/.claude/hooks/datei.ps1` — aktiv, wird von Claude Code ausgefuehrt (NICHT im Repo)
2. `~/proggs/claude-code-setup/hooks/datei.ps1` — Backup/Sync-Kopie (IM Repo)

Beim Commit darf NUR Pfad 2 verwendet werden:
```bash
# FALSCH — schlaegt fehl:
git add .claude/hooks/mcp-auth-check.ps1

# RICHTIG — funktioniert:
git add claude-code-setup/hooks/mcp-auth-check.ps1
```

## Allgemeine Regel

Vor JEDEM `git add` mental pruefen: Liegt dieser Pfad in `~/proggs/`?
- `claude-code-setup/...` → JA, im Repo ✅
- `.claude/agent-memory/...` → JA, im Repo (~/proggs/.claude/) ✅
- `TerminalVoiceOverlay-Windows/...` → JA, im Repo ✅
- `~/.claude/hooks/...` → NEIN, Home-Verzeichnis ❌
- `~/.claude/settings.json` → NEIN, Home-Verzeichnis ❌
- `~/.claude/agents/...` → NEIN, Home-Verzeichnis ❌

## Verwechslungsgefahr

Es gibt ZWEI `.claude/`-Verzeichnisse:
| Pfad | Im Repo? | Zweck |
|------|----------|-------|
| `~/proggs/.claude/` | JA ✅ | Projekt-spezifisch (agent-memory, Whiteboard) |
| `~/.claude/` | NEIN ❌ | User-global (hooks, settings, agents, rules) |
