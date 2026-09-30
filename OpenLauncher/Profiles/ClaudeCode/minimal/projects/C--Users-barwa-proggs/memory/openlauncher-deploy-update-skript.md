---
name: openlauncher-deploy-update-skript
description: "Änderungen am OpenLauncher immer über update-launcher.ps1 ausliefern, nie per dotnet build + Stop-Process von Hand"
metadata: 
  node_type: memory
  type: feedback
  originSessionId: c141fd8e-3121-402a-8b95-8f925a8b4d5d
  modified: 2026-08-12T20:10:16.806Z
---

Jede Code-Änderung am OpenLauncher wird mit `pwsh ~/proggs/OpenLauncher/update-launcher.ps1` ausgeliefert. Das Skript fragt per Dialog nach, schließt den laufenden Launcher über `CloseMainWindow()`, baut Release und startet die neue Version. Niemals stattdessen von Hand `Stop-Process -Force` + `dotnet build` + `Start-Process`.

**Why:** Der eigentliche Anlass (von Frank am 12.08.2026 bestätigt): Vor Einführung des Skripts wurde mehrfach die ALTE EXE gestartet und als die neue Version gemeldet. Frank sah seine Verbesserungen nicht, Claude behauptete "das ist die neue" und stellte erst nach längerem Suchen fest, dass es die alte war — das passierte wiederholt. Das Skript macht diesen Fehler unmöglich: `$build.ExitCode -ne 0` bricht bei rotem Build ab (statt die alte EXE zu starten), `Test-Path $launcherExe` prüft, dass überhaupt eine neue Datei entstand, `$newLauncher.HasExited` erkennt einen sofortigen Absturz der neuen Version. Ein fehlgeschlagener Deploy wird dadurch als fehlgeschlagen sichtbar, statt vom Erfolg auszugehen. Sekundär: kein hartes Kill (`CloseMainWindow()` + Rückfragedialog), und Frank sieht den Neustart überhaupt. Der Sperr-Hinweis in `SETUP.md` (Schritt 5/6, `MSB3027`/`MSB3021`) ist nur der Anlass, nicht der Grund — beendet werden muss die App ohnehin.

**How to apply:** Beim Deploy `update-launcher.ps1` aufrufen und Frank den Bestätigungsdialog quittieren lassen. Nacktes `dotnet build` nur beim Erstbau oder bei geschlossener App. Nach jedem Deploy die laufende Version gegenprüfen (`Get-Process OpenLauncher | Select-Object @{n='V';e={$_.MainModule.FileVersionInfo.FileVersion}}`) und die Nummer nennen — genau daran ist es damals gescheitert. Farben/Statusline sind davon unabhängig, die repariert der Launcher selbst: [[apps-nie-aus-claude-shell-starten]]. Siehe auch [[claude-profile-architektur-launcher]].
