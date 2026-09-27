---
name: apk-update-cloud
description: >
  Veröffentlicht in einer Claude-Code-CLOUD-Sitzung eine geänderte Android-App vollautomatisch aufs
  Handy: Versionslog-Eintrag anhängen, committen, Pull Request öffnen, Codex-Review abwarten und
  Befunde fixen, selbst mergen, GitHub Actions
  baut und signiert mit dem geteilten Key und legt APK + update.json nach Google Drive
  "Dokumente/Updates/<Projekt>/", UpdateStation zeigt das Update. Nutze diesen Skill IMMER in einer
  Cloud-Sitzung (Umgebungsvariable CLAUDE_CODE_REMOTE=true, kein C:\Users\barwa\SK), sobald eine
  Änderung an einer Android-App fertig ist, auch ohne dass Frank etwas sagt, und wenn er sagt:
  "apk update", "APK-Update", "bau die App", "aufs Handy", "Update bereitstellen". NICHT am Windows-PC,
  dort gilt der Skill apk-update mit dem Skript.
---

# APK-Update aus der Cloud (Verweis)

Die vollständige Anleitung steht zentral im Skill-Ordner:
`OpenLauncher/Profiles/ClaudeCode/standard/skills/apk-update-cloud/SKILL.md`

**Lies diese Datei jetzt vollständig und folge ihr.** Dieser Ordner ist absichtlich kein Link: Claude Code
findet Skill-Ordner, die Symlinks sind, nicht zuverlässig (Bug #14836, siehe
`bugs/claude-tooling/claude-code-cloud.md` §B1). Änderungen nur an der zentralen Datei vornehmen.
