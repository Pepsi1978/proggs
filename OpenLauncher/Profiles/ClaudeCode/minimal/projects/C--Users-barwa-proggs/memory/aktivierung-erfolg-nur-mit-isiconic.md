---
name: aktivierung-erfolg-nur-mit-isiconic
description: "Fenster-Aktivierung gilt nur als erfolgreich, wenn Vordergrund UND nicht minimiert; Fehlerzustand zuerst live messen"
metadata:
  type: project
---

Bei Windows-Fenster-Fehlern ("Klick auf die Taskleiste zeigt kein Fenster") ist
`GetForegroundWindow() == hwnd` allein KEIN Erfolgsmass — ein minimiertes Fenster kann den
Vordergrund-Status halten. Immer `IsIconic` mitprüfen.

**Why:** Im OpenLauncher meldete das Log am 20.09.2026 acht Mal `activated=true`, während das
Fenster nativ minimiert bei -16000/-16000 hing. Die falsche Erfolgsmeldung übersprang jeden
Fallback und tarnte den Fehler als Erfolg — deshalb wurde derselbe Defekt am 12.07., 18.07. und
24.07.2026 dreimal falsch eingeordnet.

**How to apply:** Meldet der Benutzer so einen Zustand und die App läuft noch: ZUERST live
messen (EnumWindows über die PID: `IsIconic`, Rect, Cloaked, Foreground-HWND), bevor irgendetwas
neu gestartet wird — der Zustand ist Minuten später verschwunden. Danach das Laufzeitlog gegen
die Messung stellen. Annahmen über Shell-Nachrichten (etwa "die Shell sendet beim
Taskleisten-Klick kein SC_RESTORE") verfallen mit dem Windows-Build und müssen am Log
gegengeprüft werden. Siehe `bugs`-Eintrag vom 20.09.2026 und
`OpenLauncher/tests/window-activation-source-guard.ps1`.
