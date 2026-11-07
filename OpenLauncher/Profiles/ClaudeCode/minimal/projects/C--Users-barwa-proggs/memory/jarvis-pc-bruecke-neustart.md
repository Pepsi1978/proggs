---
name: jarvis-pc-bruecke-neustart
description: "Jarvis-PC-Brücke neu starten - Stop-ScheduledTask allein macht sie kaputt (pwsh verliert conhost, curl -1073741502)"
metadata:
  node_type: memory
  type: reference
  originSessionId: d2bd53fd-6005-445c-a60c-7fa6a13389b6
  modified: 2026-11-07T13:54:40.774Z
---

Windows-PC-Brücke = geplante Aufgabe `JarvisPcBruecke` (`conhost.exe --headless pwsh … jarvis-pc-bruecke.ps1`, Wächter jede Minute, IgnoreNew). `Stop-ScheduledTask` beendet nur den conhost; pwsh läuft verwaist weiter, jeder curl-Aufruf scheitert dann mit „Kein Prozess ist am anderen Ende der Pipe“ bzw. `curl -1073741502`, und die Aufgabe startet keinen neuen, solange er lebt.

**How to apply:**
- Neustart: pwsh-Prozess mit `bruecke` in der CommandLine per `Stop-Process -Force` beenden, dann `Start-ScheduledTask -TaskName JarvisPcBruecke`.
- Prüfen: `%LOCALAPPDATA%\JarvisPcBruecke\bruecke.log` zeigt „Brücke gestartet“, Relay-Log (`docker logs jarvis-relay`) zeigt `GET /pc/auftrag … ua=curl`.

Token-Rollen seit 18.10.2026 (1.8.154): `relay.properties`-Token = Rolle pc+signal; Handy-Token in `~/SK/Jarvis/handy-token.txt`, kommt per `Jarvis/werkzeuge/zugang-aufs-handy.ps1 -Geraet <adb>` aufs Handy (Details `~/SK/README.md`). Siehe [[jarvis-relay-und-chatgpt-plugin]].
