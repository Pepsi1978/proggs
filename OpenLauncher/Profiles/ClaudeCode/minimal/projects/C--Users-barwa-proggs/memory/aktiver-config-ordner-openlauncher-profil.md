---
name: aktiver-config-ordner-openlauncher-profil
description: Claude Code läuft mit CLAUDE_CONFIG_DIR auf dem OpenLauncher-Profil; Settings, Verläufe und Gedächtnis liegen nicht in ~/.claude
metadata:
  type: reference
---

Unter Windows setzt der OpenLauncher `CLAUDE_CONFIG_DIR` auf
`~/proggs/OpenLauncher/Profiles/ClaudeCode/<profil>` (meist `minimal`). Dort liegen `settings.json`,
`projects/` (Sitzungsverläufe) und dieses Gedächtnis. Mac: `Profiles/ClaudeCodeMac/<profil>`,
Hostinger-Server: `~/.openlauncher/claude/<profil>`.

**Why:** Die ursprüngliche Detaildatei fehlte (nie im Git, auf diesem Rechner nicht vorhanden). Am 07.11.2026 aus der Index-Zeile und nachprüfbaren Quellen neu angelegt. Am 07.11.2026 per `$env:CLAUDE_CONFIG_DIR` geprüft.

**How to apply:** Einstellungen und Hooks im Profilordner ändern, nicht in `~/.claude`. Skills sind die
Ausnahme: eine Quelle `Profiles/ClaudeCode/standard/skills`, überall nur verlinkt.
