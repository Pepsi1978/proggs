# Claude-Code-Profile fuer macOS

Dieser Bereich ist das **macOS-Pendant** zu `Profiles/ClaudeCode/` (Windows). Er wird von der
kuenftigen macOS-Variante des Launchers (Swift/AppKit) als `CLAUDE_CONFIG_DIR` je Profil genutzt.

## Warum getrennt von Windows?

Die Windows-Profile enthalten absolute Pfade (`C:\Users\barwa\...`) und PowerShell-Hooks, die auf
macOS nicht funktionieren. Statt einen bruechigen „kleinsten gemeinsamen Nenner" zu bauen, bekommt
macOS **eigene** Profile mit macOS-Pfaden (`/Users/barwa/...`, `bash`/`zsh` statt `pwsh`).

## Struktur (analog Windows)

- `minimal/`  — regelfrei; Skills koennen per Symlink auf `~/.claude/skills` eingeblendet werden
- `standard/` — versionierte, frei bearbeitbare `skills/ rules/ agents/ commands/` + `settings.json`
- `strict/`   — wie Standard, plus (spaeter) Hook-Aktivierung mit macOS-Pfaden

## Noch zu befuellen (auf macOS)

Dieses Geruest ist bewusst leer. Auf macOS:
1. `skills/ rules/ agents/ commands/` aus dem dortigen `~/.claude` kopieren (portable Inhalte).
2. `settings.json` mit macOS-Pfaden anlegen (Hooks als `bash`/`zsh`, `/Users/...`).
3. Login-Token bleibt lokal (per `.gitignore` nie im Repo).

Die `.gitignore` in jedem Profil haelt Laufzeit/Secrets vom Repo fern und ist schon vorbereitet.
