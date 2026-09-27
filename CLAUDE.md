# proggs

Claude-Regeln kommen nicht aus dieser Datei:
- Am PC aus dem gewählten OpenLauncher-Profil (`OpenLauncher/Profiles/ClaudeCode/sources/<profil>.md`).
- In Cloud-Sitzungen aus `OpenLauncher/Profiles/ClaudeCode/sources/cloud.md`, eingeblendet vom SessionStart-Hook in
  `.claude/settings.json`.

**Cloud-Sitzung (`CLAUDE_CODE_REMOTE=true`) ohne „Cloud-Regeln“ im Kontext?** Dann lies sofort
`OpenLauncher/Profiles/ClaudeCode/sources/cloud.md` vollständig und befolge sie (der Start-Hook lief nicht,
z. B. weil mehrere Repos angehängt sind). Am PC gilt dieser Absatz nicht.

Diese Datei ist absichtlich kurz. Sie verhindert, dass Claude Code die `AGENTS.md` im Wurzelordner liest: Die schreibt
OpenLauncher bei jedem OpenCode- oder Codex-Start mit dem dort gewählten Profil neu.
