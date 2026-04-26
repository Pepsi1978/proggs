# Codex-CLI-Standardkontext (KRITISCH)

Diese Session laeuft in Codex CLI. Deshalb gilt fuer alle unqualifizierten
Fragen, Aussagen und Aufgaben des Benutzers:

- Standardannahme: Der Benutzer meint Codex CLI, Codex-Runtime, Codex-Regeln,
  `~/.codex/`, `codex-setup/` und den Workspace `C:\Users\barwa\Codex CLI`.
- Claude Code wird nur betrachtet, wenn der Benutzer explizit `Claude Code`
  sagt oder eine eindeutig Claude-spezifische Datei konkret anfordert.
- Gemini CLI wird nur betrachtet, wenn der Benutzer explizit `Gemini CLI`
  sagt oder eine eindeutig Gemini-spezifische Datei konkret anfordert.
- Aehnliche Dateinamen, alte Vergleichsordner oder gefundene Claude/Gemini-Pfade
  duerfen die Standardannahme nicht kippen.
- Bei Begriffen wie `Statusline`, `Config`, `Hooks`, `Sessions`, `Agents`,
  `Skills`, `Settings`, `Regeln` oder `Setup` zuerst Codex CLI pruefen.

Beispiel: Fragt der Benutzer in Codex CLI nach der `Statusline`, ist
`~/.codex/config.toml` relevant. Claude-Code-Statusline-Dateien sind nur
Vergleichsmaterial, wenn der Benutzer Claude Code ausdruecklich nennt.
