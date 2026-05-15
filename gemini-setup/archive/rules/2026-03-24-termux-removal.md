# Regel-Archiv: Entfernung von Termux
- **Datum:** 2024-03-24
- **Grund:** Benutzer nutzt Termux nicht mehr; Fokus auf Windows/macOS-Paritaet.
- **Status:** Gel├Âscht (Archiviert)
- **Stale-Patterns:** "termux", "com.termux", "termux-notification", "termux-toast"

## Urspr├╝ngliche Regel (Auszug aus AGENTS.md/gemini-setup/rules/termux.md)
- Platform: Android/Termux on aarch64
- NEVER use `#!/usr/bin/env` ÔÇö it doesn't exist on Termux
- ALWAYS use `#!/data/data/com.termux/files/usr/bin/bash`

## Betroffene Artefakte
- `gemini-setup/rules/termux.md` (gel├Âscht)
- `AGENTS.md` (Referenzen entfernt)
- `gemini-setup/skills/self-improve/SKILL.md` (Referenzen entfernt)

---
*Zweck des Archivs: Verhindert, dass k├╝nftige Agenten versehentlich versuchen, Termux-Support wieder einzuf├╝hren.*

