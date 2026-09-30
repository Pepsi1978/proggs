---
name: gestartetes-programm-erbt-agenten-pipe
description: "Update-/Startskript meldet \"started\", Agent wartet trotzdem bis zum Timeout – ein langlebiges Kind erbt die Pipe"
metadata:
  node_type: memory
  type: feedback
  originSessionId: 9d29d4c1-5efd-49d4-8134-f9dee93dd32d
  modified: 2026-09-29T10:35:35.759Z
---

Skript meldet schnell Erfolg (z. B. `LAUNCHER_UPDATE_STATUS=started`), das Bash-/PowerShell-Werkzeug läuft aber bis ins 600-s-Timeout oder endet erst beim nächsten Update: Ein gestartetes Programm, das weiterläuft, hat die Ausgabe-Pipe des Agenten geerbt.

**Why:** `[Diagnostics.Process]::Start` mit `UseShellExecute=$false` und `Start-Process -NoNewWindow` rufen `CreateProcess` mit `bInheritHandles=TRUE` auf. Am 29.09.2026 hat das den OpenLauncher-Update-Aufruf bei jedem Lauf hängen lassen. Ursache war ein älterer Fix für die Umgebungsbereinigung, der den ShellExecute-Start ersetzt hatte. Fix in `OpenLauncher/update-launcher.ps1`: `SetHandleInformation` auf die Standard-Handles vor dem Start. Almanach: `bugs/werkzeuge/update-skript-modaler-dialog.md` (Falle 3).

**How to apply:** Bei „fertig, aber Tool wartet“ zuerst nach langlebigen Kindern mit geerbten Handles suchen. Wer in einem agentenseitig aufgerufenen Skript den Startweg ändert, prüft danach, dass der Aufruf mit `| Select-Object -Last 5` sofort zurückkommt. Verwandt: [[openlauncher-deploy-update-skript]], [[overlay-update-bestaetigungsfenster]].
