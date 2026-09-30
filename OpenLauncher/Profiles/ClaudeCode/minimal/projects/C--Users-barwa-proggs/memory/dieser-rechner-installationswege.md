---
name: dieser-rechner-installationswege
description: "Auf diesem Windows-Rechner liegen LM Studio, Claude/Codex CLI anders als im Katalog-Standard, Stream Deck fehlt ganz"
metadata: 
  node_type: memory
  type: project
  originSessionId: 0ba21f96-de3c-4b6f-afd4-d0826569f8d6
  modified: 2026-09-21T12:29:51.607Z
---

Dieser Rechner (barwa, Windows 11) weicht bei den Werkzeug-Installationen vom anderen PC ab:
LM Studio liegt unter `C:\Program Files\LM Studio\`, nicht unter `%LOCALAPPDATA%\Programs`.
Claude Code CLI und Codex CLI sind per npm global installiert (`%APPDATA%\npm\claude.cmd`
bzw. `codex.cmd`), nicht über den nativen Installer unter `~/.local/bin` bzw. `~/.codex/packages`.
Stream Deck ist gar nicht installiert und wird hier auch nicht benutzt.

**Warum:** Die geteilte `UpdateZentrale/programs.json` gilt für beide Rechner; ohne dieses Wissen
wirken die Karten fälschlich als „Nicht installiert".

**How to apply:** Rechnerunterschiede nie durch Gabeln des Katalogs lösen, sondern über
`exePfadAlternativen` (erster existierender Pfad gewinnt) und `ausblendenWennFehlt`.
Siehe [[updatezentrale-eingerichtet]].
