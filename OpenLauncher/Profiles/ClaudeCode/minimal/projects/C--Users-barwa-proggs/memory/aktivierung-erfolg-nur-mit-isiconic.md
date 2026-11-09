---
name: aktivierung-erfolg-nur-mit-isiconic
description: "Fenster-Aktivierung gilt nur als erfolgreich, wenn Vordergrund UND nicht minimiert; Fehlerzustand zuerst live messen"
metadata:
  type: project
---

Windows-Fenster-Fehler ("Klick auf die Taskleiste zeigt kein Fenster"): `GetForegroundWindow() == hwnd` allein ist KEIN Erfolgsmaß – ein minimiertes Fenster kann den Vordergrund halten. Immer `IsIconic` mitprüfen.

**Why:** OpenLauncher-Log am 20.09.2026: acht Mal `activated=true`, während das Fenster nativ minimiert bei -16000/-16000 hing. Die falsche Erfolgsmeldung übersprang jeden Fallback und tarnte den Fehler – derselbe Defekt wurde am 12.07., 18.07. und 24.07.2026 dreimal falsch eingeordnet.

**How to apply:**
- Läuft die App noch: ZUERST live messen (EnumWindows über die PID: `IsIconic`, Rect, Cloaked, Foreground-HWND), bevor irgendetwas neu gestartet wird – der Zustand ist Minuten später weg. Danach Laufzeitlog gegen die Messung stellen.
- Annahmen über Shell-Nachrichten (etwa "die Shell sendet beim Taskleisten-Klick kein SC_RESTORE") verfallen mit dem Windows-Build, am Log gegenprüfen.
- Siehe `bugs`-Eintrag vom 20.09.2026 und `OpenLauncher/tests/window-activation-source-guard.ps1`.

**Nachtrag 30.09.2026:** Neuer Fall ohne jede Logzeile: minimiert + systemweit KEIN Vordergrundfenster (`GetForegroundWindow()=0`). Taskleisten-Klick lieferte kein SC_RESTORE; ein direkt gesendetes `PostMessage(SC_RESTORE)` stellte sofort her. Messskript immer mit `SetProcessDPIAware` (sonst -16000 statt -32000). Ab 1.24.80 schreibt der WndProc alle Aktivierungsnachrichten mit (Ringpuffer im Fehlerlog) — beim nächsten Mal zuerst `TraceActivationMessage`-Zeilen im Log lesen. Almanach `bugs/desktop/dotnet-csharp.md` §5.9.
