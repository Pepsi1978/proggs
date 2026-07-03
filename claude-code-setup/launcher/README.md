# CLI-Launcher (Windows Terminal) — Repo-Spiegel

Aktiver Ort: `C:\Users\barwa\` (Home-Verzeichnis, NICHT im Repo). Diese Kopien sind der
Spiegel für andere Rechner. Bei Änderungen IMMER beide Orte aktualisieren.

| Datei | Zweck |
|-------|-------|
| `start-wt-common.ps1` | Gemeinsame Robust-Start-Funktion `Start-WtCliRobust` — Zombie-Cleanup, Nach-Start-Verifikation, Auto-Retry (`-w new`), Konsolen-Fallback |
| `start-claude-wt.ps1` | Claude-Code-Launcher (Tab mit Zufallsfarbe, innerer `/color`-Match) |
| `start-opencode-wt.ps1` | OpenCode-Launcher |
| `start-codex-wt.ps1` | Codex-Launcher |
| `start-gemini-wt.ps1` | Gemini-Launcher |

Die Desktop-/Taskleisten-Verknüpfungen zeigen auf
`pwsh.exe -NoProfile -WindowStyle Hidden -ExecutionPolicy Bypass -File "C:\Users\barwa\start-<cli>-wt.ps1"`.

**Warum die Robust-Funktion existiert:** Windows-Terminal-Kaltstart-Race (Monarch-Handshake)
ließ Verknüpfungen nach jedem Windows-Neustart 5-6x scheitern (Fenster blitzt auf und
schließt sofort). Vorab-Heuristiken können das nicht lösen — nur Verifikation nach dem
Start + automatische Wiederholung. Volle Fall-Doku:
`bugs/claude-tooling/claude-code-desktop-vs-cli.md` §O1 (Diagnose + Log-Beweis 2026-07-03).
