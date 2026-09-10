# Update-Skript: Rückfrage fehlt, Aufrufer hängt bis zum Timeout

Festgehalten am 10.09.2026, 11:02 Uhr. Berichtigt am 10.09.2026, 11:40 Uhr. Betrifft `OpenLauncher/update-launcher.ps1` und `OpenLauncherMac/update-launcher.sh`.

## Falle 1 – Update ohne Freigabe

Ein Agent (Claude Code, Codex, OpenCode) ruft das Update-Skript auf. Es erscheint kein Ja/Nein-Fenster, der Launcher wird ohne Rückfrage geschlossen und neu gebaut.

**Ursache:** Der erste Fix (Stand 11:02) hat den Dialog abgeschaltet, sobald die Standardeingabe umgeleitet war (`[Console]::IsInputRedirected` bzw. `[ -t 0 ]`). Bei Agenten-Aufrufen ist sie das immer. Es galt dann still „Ja“. Das war eine falsche Schlussfolgerung: Der Dialog hing damals nicht, weil kein Terminal da war. Er hing, weil die WPF-`MessageBox` eines Hintergrundprozesses hinter anderen Fenstern landete, wo sie niemand sah.

**Richtig:**

- Die Rückfrage kommt immer. Überspringen nur mit `-Force` bzw. `OPENLAUNCHER_UPDATE_FORCE=1` und nur auf ausdrückliche Ansage des Benutzers.
- Windows: `MessageBoxTimeoutW` aus user32 mit `MB_SYSTEMMODAL | MB_SETFOREGROUND | MB_TOPMOST`. Das Fenster liegt garantiert ganz oben. Fallback ist `WScript.Shell.Popup` mit denselben Flags.
- macOS: `display dialog … giving up after N` innerhalb von `tell application "System Events" to activate`.
- Zeitlimit von 240 s. Kommt kein Klick, gilt „Nein“ (`LAUNCHER_UPDATE_STATUS=no-answer`), nie „Ja“. Damit ist ein Deadlock ausgeschlossen, ohne dass die Freigabe verloren geht.
- Den Aufruf aus dem Agenten mit 10 Minuten Tool-Timeout starten. Das steht als Regel 13 in allen Profilen.

## Falle 2 – Timeout, obwohl die neue Version längst läuft

Das Skript meldet `LAUNCHER_UPDATE_STATUS=started` und ist fertig. Das Werkzeug des Agenten wartet trotzdem, bis sein Timeout abläuft.

**Ursache:** `dotnet build` startet Build-Server: MSBuild-Knoten (node reuse), den MSBuild-Server und den Compiler-Server `VBCSCompiler`. Sie laufen nach dem Build noch minutenlang weiter. Mit `Start-Process -NoNewWindow` erben sie die Ausgabe-Pipe des Aufrufers. Solange einer davon lebt, sieht der Agent kein Dateiende und wartet.

**Richtig:** Keine Build-Server, doppelt abgesichert.

```powershell
$env:MSBUILDDISABLENODEREUSE = '1'
$env:DOTNET_CLI_USE_MSBUILD_SERVER = '0'
$env:UseSharedCompilation = 'false'
dotnet build … -nodeReuse:false -p:UseSharedCompilation=false -p:UseRazorBuildServer=false
```

Den neuen Launcher mit `Start-Process` **ohne** `-NoNewWindow` starten (ShellExecute, keine Handle-Vererbung). Das Skript endet mit `exit 0`.

## Aktualitätsprüfung (bleibt gültig)

Version der gebauten Datei gegen die Projektversion vergleichen. Außerdem darf keine Quelldatei (`.cs`, `.xaml`, `.csproj`) neuer sein als die Exe. Sind beide Bedingungen erfüllt und läuft der Launcher bereits, meldet das Skript ohne Dialog `already-current` und endet sofort. Laufzeitdateien wie `models.json` nicht mitprüfen, sonst gilt der Build sofort wieder als veraltet.

## Regeln

1. Eine Freigabe-Rückfrage darf nie still zu „Ja“ werden. Fehlt die Antwort, gilt „Nein“.
2. Ein Dialog, der „hängt“, ist meist unsichtbar und nicht blockiert. Er muss nach vorne geholt und zeitlich begrenzt werden, statt entfernt zu werden.
3. Wer aus einem Skript heraus `dotnet build` aufruft, das ein Agent startet, schaltet die Build-Server ab. Sonst hängt der Agent an der geerbten Pipe.
