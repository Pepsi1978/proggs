# Regel-Archiv: Entfernung von Termux
- **Datum:** 2026-03-25
- **Grund:** Benutzer nutzt Termux nicht mehr. Fokus liegt auf Windows + macOS.
- **Status:** Geloescht (archiviert)
- **Stale-Patterns:** "termux", "com.termux", "termux-notification", "termux-toast", "aarch64", "/data/data/com.termux"

## Urspruengliche Regel (Auszug aus ~/.claude/rules/termux.md)
- Platform: Android/Termux on aarch64 (no root, no sudo, no systemd)
- Package manager: pkg (not apt, brew, or winget)
- NEVER use #!/usr/bin/env — it doesn't exist on Termux
- ALWAYS use #!/data/data/com.termux/files/usr/bin/bash

## Betroffene Artefakte
- `~/.claude/rules/termux.md` (geloescht am 2026-03-25)
- War nie im Setup-Repo (nur lokal)

## Warum archiviert statt nur geloescht
Verhindert dass ein zukuenftiger /self-improve oder Agent versucht Termux-Support wieder einzufuehren
weil er "fehlende Plattform-Unterstuetzung" erkennt. Die Entscheidung war bewusst vom Benutzer.
