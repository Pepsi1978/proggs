---
name: claude-profile-architektur-launcher
description: OpenLauncher — wie die Claude-Code-Profile (Minimal/Standard/Strikt) und Codex Kontext laden; Skills haben genau eine Quelle (standard/skills)
metadata:
  node_type: memory
  type: project
  originSessionId: 79276bff-93a4-485a-866f-308736932598
  modified: 2026-09-10T11:20:26.241Z
---

OpenLauncher (`~/proggs/OpenLauncher`): **jedes** Claude-Code-Profil hat **eigenen** `CLAUDE_CONFIG_DIR` unter `Profiles/ClaudeCode/<id>` (`Services/InstructionProfileService.cs`, `EnsureClaudeConfigDir`). Geprüft 10.09.2026.

- **Skills – eine Quelle:** `Profiles/ClaudeCode/standard/skills` (versioniert). Junctions darauf: `minimal/skills`, `~/.claude/skills`, `~/.agents/skills` (Codex liest dort). Nur dort bearbeiten, dann committen + pushen (Profilregel 3). `claude-code-setup/skills` am 10.09.2026 gelöscht (nur Git-Historie).
- **Minimal:** regelfrei; ganze `minimal/`-Ebene per eigener `.gitignore` (`*`) **unversioniert** – `git add` dort tut nichts (Skills trotzdem versioniert, Junction auf `standard/skills`).
- **Standard/Strikt:** versionierte `skills/rules/agents/commands` + eigene `settings.json` mit `claudeMdExcludes: ["**/.claude/rules/**"]` (gegen Doppelladung der Rules).
- **Strikt zusätzlich:** `settings.json` aktiviert volle `~/.claude`-Hooks (Pfade auf `~/.claude/hooks`). Standard bewusst ohne Hooks.
- **Codex CLI:** `CODEX_HOME` = `%LOCALAPPDATA%\OpenLauncher\codex-home` (`PrepareCodexHome`); `config.toml` nur angelegt, wenn sie fehlt, seit 1.24.35 mit Statuszeile aus `Statusline-Codex/status-line.toml`. Codex: offizielle npm-Release unter `%APPDATA%\npm` (kein Kosten-Eigenbau mehr).
- **macOS:** `Profiles/ClaudeCodeMac/{minimal,standard,strict}`.
- **Login:** `EnsureLoginToken` kopiert `.credentials.json` bei Bedarf einmalig lokal aus `~/.claude`; per `.gitignore` nie versioniert.
- **Achtung Secret:** `~/.claude/settings.json` enthält `GITHUB_PERSONAL_ACCESS_TOKEN` im Klartext – beim Übernehmen IMMER entfernen (Regel #1).

**Why:** Früher liefen mehrere Skill-Kopien (minimal/skills, claude-code-setup/skills, ~/.claude/skills) auseinander; seit Anfang September 2026 alles auf `standard/skills` verknüpft.

**How to apply:** Profil-Kontext in `Profiles/ClaudeCode/<id>/` bzw. `Profiles/ClaudeCode/sources/<id>.md` ändern. Skills nur in `standard/skills`. Deploy immer via `update-launcher.ps1` ([[openlauncher-deploy-update-skript]]), Version bei jeder Änderung bumpen. Siehe [[repo-branch-und-sync-setup]].
