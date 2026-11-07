---
name: apps-nie-aus-claude-shell-starten
description: OpenLauncher darf aus Claudes Tool-Shell gestartet werden — er scrubbt die geerbte Agenten-Umgebung selbst; bei anderen Apps gilt die Vorsicht weiter
metadata: 
  node_type: memory
  type: feedback
  originSessionId: e66a1024-ceab-4ae5-ba84-4e7cc6e39d20
  modified: 2026-11-07T13:54:32.567Z
---

Der OpenLauncher entfernt per `OpenLauncherService.cs` (`InheritedAgentEnvScrubScript`) vor jedem Sitzungsstart die geerbte Agenten-Umgebung: `NO_COLOR`, `FORCE_COLOR`, `CLICOLOR`, `CLICOLOR_FORCE`, `AI_AGENT`, `GIT_TERMINAL_PROMPT` und alle `CLAUDE*`-Variablen außer `CLAUDE_CONFIG_DIR`. Daher darf `update-launcher.ps1` aus der Tool-Shell laufen (am 10.08.2026 verifiziert, von Frank bestätigt).

**Why:** 24./25.07.2026 meldete Frank zweimal „die Farben sind kaputt“: eine aus Claudes Shell gestartete App reichte die Umgebung an jedes von ihr geöffnete Terminal weiter (`NO_COLOR=1` schaltet Farben ab, `CLAUDE_CODE_CHILD_SESSION` das Transcript und damit den `ctx`-Wert). Der Launcher behebt das an der Quelle, statt dass der Aufrufer aufpassen muss.

**How to apply:** OpenLauncher normal starten. ANDERE Apps, die ihrerseits Terminals öffnen: alte Regel gilt unverändert, vor `Start-Process` die Variablen mit `Remove-Item Env:<name>` entfernen oder den Benutzer per `! <befehl>` selbst starten lassen. Diagnose: `Get-ChildItem Env:` im Tool zeigt Claudes Unterprozess-Umgebung, NICHT die der Sitzung. Siehe [[repo-branch-und-sync-setup]].
