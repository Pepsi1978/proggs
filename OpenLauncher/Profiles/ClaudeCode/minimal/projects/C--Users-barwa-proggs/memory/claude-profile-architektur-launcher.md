---
name: claude-profile-architektur-launcher
description: OpenLauncher — wie die Claude-Code-Profile (Minimal/Standard/Strikt) und Codex Kontext laden; Skills haben genau eine Quelle (standard/skills)
metadata:
  node_type: memory
  type: project
  originSessionId: 79276bff-93a4-485a-866f-308736932598
  modified: 2026-09-10T11:20:26.241Z
---

Im OpenLauncher (`~/proggs/OpenLauncher`) hat **jedes** Claude-Code-Profil seinen **eigenen** `CLAUDE_CONFIG_DIR`-Ordner im Repo unter `Profiles/ClaudeCode/<id>` (Logik in `Services/InstructionProfileService.cs`, `EnsureClaudeConfigDir`). Stand 10.09.2026 geprüft.

- **Skills — eine einzige Quelle**: `Profiles/ClaudeCode/standard/skills` (versioniert). Verknüpft (Junction) darauf: `minimal/skills`, `~/.claude/skills`, `~/.agents/skills` (Codex liest dort). Skills nur in `standard/skills` bearbeiten, danach committen und pushen. Der alte Ordner `claude-code-setup/skills` wurde am 10.09.2026 gelöscht (nur noch in der Git-Historie).
- **Minimal**: regelfrei; die gesamte `minimal/`-Ebene ist per eigener `.gitignore` (`*`) **unversioniert** — `git add` dort tut nichts (die Skills sind trotzdem versioniert, weil die Junction auf `standard/skills` zeigt).
- **Standard/Strikt**: versionierte `skills/rules/agents/commands` im Repo + eigene `settings.json` mit `claudeMdExcludes: ["**/.claude/rules/**"]` (verhindert Doppelladung der Rules).
- **Strikt zusätzlich**: `settings.json` aktiviert die vollen `~/.claude`-Hooks (Hook-Pfade zeigen auf `~/.claude/hooks`). Standard bleibt bewusst ohne Hooks.
- **Codex CLI**: eigenes `CODEX_HOME` = `%LOCALAPPDATA%\OpenLauncher\codex-home` (`PrepareCodexHome`); `config.toml` wird nur angelegt, wenn sie fehlt, und enthält seit 1.24.35 die Statuszeile aus `Statusline-Codex/status-line.toml`. Codex selbst: offizielle npm-Release unter `%APPDATA%\npm` (kein Kosten-Eigenbau mehr).
- **macOS**: eigener Bereich `Profiles/ClaudeCodeMac/{minimal,standard,strict}`.
- **Login**: `EnsureLoginToken` kopiert `.credentials.json` bei Bedarf einmalig lokal aus `~/.claude`; per `.gitignore` nie versioniert.
- **Achtung Secret**: `~/.claude/settings.json` enthält `GITHUB_PERSONAL_ACCESS_TOKEN` im Klartext — beim Übernehmen IMMER entfernen (Regel #1).

**Why:** Früher gab es mehrere Skill-Kopien (minimal/skills, claude-code-setup/skills, ~/.claude/skills), die auseinanderliefen. Seit Anfang September 2026 ist alles auf `standard/skills` verknüpft.

**How to apply:** Profil-Kontext in `Profiles/ClaudeCode/<id>/` bzw. `Profiles/ClaudeCode/sources/<id>.md` ändern. Skills nur in `standard/skills`. Deploy immer via `update-launcher.ps1` ([[openlauncher-deploy-update-skript]]), Version bei jeder Änderung bumpen. Siehe [[repo-branch-und-sync-setup]].
