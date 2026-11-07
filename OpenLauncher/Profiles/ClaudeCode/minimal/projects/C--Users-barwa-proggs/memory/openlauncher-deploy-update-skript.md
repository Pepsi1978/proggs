---
name: openlauncher-deploy-update-skript
description: "Änderungen am OpenLauncher immer über update-launcher.ps1 ausliefern, nie per dotnet build + Stop-Process von Hand"
metadata: 
  node_type: memory
  type: feedback
  originSessionId: c141fd8e-3121-402a-8b95-8f925a8b4d5d
  modified: 2026-08-12T20:10:16.806Z
---

Jede Code-Änderung am OpenLauncher per `pwsh ~/proggs/OpenLauncher/update-launcher.ps1` ausliefern (Ablauf: Profilregel 13). Das Skript fragt per Dialog, schließt den Launcher über `CloseMainWindow()`, baut Release, startet die neue Version. Nie stattdessen von Hand `Stop-Process -Force` + `dotnet build` + `Start-Process`.

**Why:** (von Frank am 12.08.2026 bestätigt) Vor dem Skript wurde wiederholt die ALTE EXE gestartet und als neue gemeldet; Frank sah seine Verbesserungen nicht, Claude behauptete „das ist die neue“ und merkte erst nach langem Suchen, dass es die alte war. Das Skript macht das unmöglich: `$build.ExitCode -ne 0` bricht bei rotem Build ab (statt die alte EXE zu starten), `Test-Path $launcherExe` prüft, dass überhaupt eine neue Datei entstand, `$newLauncher.HasExited` erkennt Sofortabsturz → ein fehlgeschlagener Deploy ist als solcher sichtbar. Sekundär: kein hartes Kill (`CloseMainWindow()` + Rückfragedialog), Frank sieht den Neustart. Der Sperr-Hinweis in `SETUP.md` (Schritt 5/6, `MSB3027`/`MSB3021`) ist nur Anlass, nicht Grund – beendet werden muss die App ohnehin.

**How to apply:** Frank den Bestätigungsdialog quittieren lassen. Nacktes `dotnet build` nur beim Erstbau oder bei geschlossener App. Nach jedem Deploy laufende Version gegenprüfen (`Get-Process OpenLauncher | Select-Object @{n='V';e={$_.MainModule.FileVersionInfo.FileVersion}}`) und die Nummer nennen – genau daran scheiterte es damals. Farben/Statusline sind unabhängig, die repariert der Launcher selbst: [[apps-nie-aus-claude-shell-starten]]. Siehe [[claude-profile-architektur-launcher]].
