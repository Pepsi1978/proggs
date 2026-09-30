---
name: apps-nie-aus-claude-shell-starten
description: OpenLauncher darf aus Claudes Tool-Shell gestartet werden — er scrubbt die geerbte Agenten-Umgebung selbst; bei anderen Apps gilt die Vorsicht weiter
metadata: 
  node_type: memory
  type: feedback
  originSessionId: e66a1024-ceab-4ae5-ba84-4e7cc6e39d20
  modified: 2026-08-10T19:16:17.532Z
---

Der OpenLauncher entfernt seit `OpenLauncherService.cs` (`InheritedAgentEnvScrubScript`) vor jedem Sitzungsstart die geerbte Agenten-Umgebung: `NO_COLOR`, `FORCE_COLOR`, `CLICOLOR`, `CLICOLOR_FORCE`, `AI_AGENT`, `GIT_TERMINAL_PROMPT` sowie alle `CLAUDE*`-Variablen ausser `CLAUDE_CONFIG_DIR`. `update-launcher.ps1` darf deshalb aus der Tool-Shell laufen — am 10.08.2026 verifiziert und von Frank bestaetigt.

**Why:** Ursprünglich (24./25.07.2026) meldete Frank zweimal „die Farben sind kaputt", weil eine aus Claudes Shell gestartete App die Umgebung an jedes von ihr geoeffnete Terminal weiterreichte: `NO_COLOR=1` schaltet Farben ab, `CLAUDE_CODE_CHILD_SESSION` das Transcript und damit den `ctx`-Wert. Der Launcher behebt das jetzt an der Quelle, statt dass der Aufrufer aufpassen muss.

**How to apply:** OpenLauncher normal starten. Bei ANDEREN Apps, die ihrerseits Terminals oeffnen, gilt die alte Regel unveraendert — dort vor `Start-Process` die Variablen mit `Remove-Item Env:<name>` entfernen oder den Benutzer per `! <befehl>` selbst starten lassen. Beim Diagnostizieren: `Get-ChildItem Env:` im Tool zeigt Claudes Unterprozess-Umgebung, NICHT die der Sitzung. Siehe [[repo-branch-und-sync-setup]].
