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
- Der Agent darf das Skript nicht selbst abbrechen, solange der Dialog wartet. Deshalb startet er es mit dem höchsten erlaubten Zeitlimit statt mit dem kurzen Standard (Claude Code: 2 Minuten). Den vollständigen Ablauf beschreibt Regel 13 in allen Profilen (Minimal, Standard, Strikt; Windows und Mac).

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

Den neuen Launcher darf der Aufrufer nie über eine vererbte Pipe festhalten, siehe Falle 3. Das Skript endet mit `exit 0`.

## Falle 3 – Timeout durch den neu gestarteten Launcher selbst (29.09.2026, 12:35 Uhr)

Gleiches Symptom wie Falle 2, obwohl die Build-Server abgeschaltet sind. Der Aufruf endet erst, wenn der neue Launcher wieder geschlossen wird, also meist beim nächsten Update.

**Ursache:** Ein späterer Fix gegen die geerbte Farbabschaltung (`NO_COLOR`) startet den Launcher über `[Diagnostics.Process]::Start` mit `UseShellExecute=$false`, weil nur so die Umgebung des Kindes bereinigt werden kann. `CreateProcess` läuft dabei mit `bInheritHandles=TRUE`. Das Kind erbt alle vererbbaren Handles, auch stdout/stderr des Skripts, also die Pipe des Agenten. Der Launcher lebt weiter, die Pipe bleibt offen. Die Anweisung aus Falle 2 (ShellExecute) wurde so still aufgehoben: eine Regression durch einen Fix.

**Richtig:** Gemeinsamer Starthelfer `Werkzeuge/start-ohne-pipe/Start-OhnePipe.ps1` (`Start-ProzessOhnePipe`). Er markiert vor `Process.Start` die eigenen Standard-Handles (-10/-11/-12) mit `SetHandleInformation(h, HANDLE_FLAG_INHERIT, 0)` als nicht vererbbar markieren. Schlägt das fehl, meldet das Skript es als `LAUNCHER_UPDATE_INFO` und bricht nicht ab. Die Umgebungsbereinigung bleibt erhalten.

**Erkennen:** Die Ausgabe zeigt `started` nach wenigen Sekunden, das Werkzeug meldet aber erst nach Minuten „completed“ oder läuft ins Timeout. Dann erbt ein langlebiges Kind die Pipe.

## Aktualitätsprüfung (bleibt gültig)

Version der gebauten Datei gegen die Projektversion vergleichen. Außerdem darf keine Quelldatei (`.cs`, `.xaml`, `.csproj`) neuer sein als die Exe. Sind beide Bedingungen erfüllt und läuft der Launcher bereits, meldet das Skript ohne Dialog `already-current` und endet sofort. Laufzeitdateien wie `models.json` nicht mitprüfen, sonst gilt der Build sofort wieder als veraltet.

## Regeln

1. Eine Freigabe-Rückfrage darf nie still zu „Ja“ werden. Fehlt die Antwort, gilt „Nein“.
2. Ein Dialog, der „hängt“, ist meist unsichtbar und nicht blockiert. Er muss nach vorne geholt und zeitlich begrenzt werden, statt entfernt zu werden.
3. Wer aus einem Skript heraus `dotnet build` aufruft, das ein Agent startet, schaltet die Build-Server ab. Sonst hängt der Agent an der geerbten Pipe.
4. Startet ein Skript, das ein Agent aufruft, ein Programm, das weiterlaufen soll, darf dieses die Pipe des Aufrufers nicht erben: ShellExecute nutzen oder die Standard-Handles vorher auf nicht vererbbar setzen. Wer den Startweg ändert (z. B. für die Umgebungsbereinigung), prüft danach, dass der Aufruf sofort endet.
