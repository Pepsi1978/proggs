---
name: profilregeln-aus-sources-generiert
description: Die aktive CLAUDE.md/AGENTS.md schreibt der OpenLauncher bei jedem Start aus Profiles/<CLI>/sources neu
metadata:
  type: reference
---

Die aktive `CLAUDE.md` im Profilordner ist eine Laufzeitdatei: Der OpenLauncher schreibt sie bei jedem CLI-Start neu aus `OpenLauncher/Profiles/ClaudeCode/sources/<profil>.md` (Mac: `ClaudeCodeMac/sources`; Hostinger-Server ebenfalls `ClaudeCodeMac/sources` + `OpenLauncherServer/server-zusatz.md`; OpenCode: `Profiles/OpenCode(Mac)/<profil>/AGENTS.md`). Belege: `OpenLauncher/SETUP.md`, `OpenLauncher/Services/InstructionProfileService.cs`, `OpenLauncherServer/kern/launcher.py`.

Herkunft: Originaldatei fehlte (nie im Git, auf diesem Rechner nicht vorhanden); am 07.11.2026 aus der Index-Zeile und nachprüfbaren Quellen neu angelegt.

**How to apply:** Regeln immer in den Quellen ändern (alle 12: ClaudeCode, ClaudeCodeMac, OpenCode, OpenCodeMac je minimal/standard/strict), nie in der aktiven CLAUDE.md. Fehlt in einer Sitzung eine Regel: Repo veraltet oder Sitzung lief vor dem Pull an, kein Launcher-Fehler.
