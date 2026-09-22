# Windows-CLIs über WSL/tmux starten

Stand: 22.09.2026, WSL 2.7.10, Ubuntu, tmux 3.6, PowerShell 7.6.6. Gegenstück zum
Almanach [`bugs/desktop/openlauncher-windows-tmux.md`](../../bugs/desktop/openlauncher-windows-tmux.md).
Alle Punkte sind lokal mit echten Prozessen belegt, nicht aus Web-Recherche.

## 1. Argumente getrennt übergeben
`wsl.exe -d <distro> --exec <programm> <arg>…` statt `wsl -- …` oder `sh -c "…"`.
In .NET `ProcessStartInfo.ArgumentList`, in PowerShell 7 `& $wsl @array`. tmux-Befehlsfolgen
mit `';'` als eigenem Argument. Für `Start-Process` nur Tokens ohne Leerzeichen/Quotes
zulassen und das vorher prüfen.

## 2. Windows-Programme im Pane über den Instanz-Interop starten
Ein Pane erbt `WSL_INTEROP` vom `wsl.exe`, das Server oder Sitzung erzeugt hat. Endet dieses
`wsl.exe` (z. B. `new-session -d`) vor oder nach dem Start, scheitert der Start bzw. stirbt
das Programm mit. Deshalb pro Fenster `-e WSL_INTEROP=/run/WSL/1_interop` setzen und das
Vorhandensein beim Start mit `wsl --exec test -e /run/WSL/1_interop` prüfen.

## 3. WSL während des Starts aktiv halten
Während des fehlerhaften Pane-Starts kann die Distro etwa 15–25 s später herunterfahren,
wenn keine lebende tmux-Sitzung mehr übrig ist. Einen versteckten Halteprozess nutzen, der
die Sitzung anlegt und mit `tmux wait-for <kanal>` blockiert, bis der Start bestätigt ist.
Danach mit `wait-for -S <kanal>` freigeben und nie einen verwaisten Halteprozess
zurücklassen (`finally`, notfalls `Stop-Process`). Eine lebende, abgehängte tmux-Sitzung
hält WSL selbst aktiv; ein dauerhaft laufender Halteprozess ist dafür nicht nötig.

## 4. Erfolg am echten Zielprozess messen
Nicht `has-session`, nicht die äußere `-NoExit`-Shell, nicht den Exitcode von `new-session`.
CIM auf pwsh/powershell mit exakt dem inneren Skriptnamen als letztem Pfadglied
(`[\\/]name(?=["'\s]|$)`), dazu eine kurze Stabilitätsprüfung. Erst danach anhängen.

## 5. Kontrolliert wiederholen
Scheitert die Bestätigung: Zielsitzung beenden, Backoff (2 s, 4 s), höchstens 3 Versuche,
dann sichtbarer Fehler. Ein reines Linux-Bootstrap-Fenster hält die Sitzung während der
Prüfung stabil und wird nach Erfolg entfernt, damit CLI-Ende weiterhin die Sitzung beendet.

## 6. Tests auf frischem tmux-Socket
Ein bereits laufender Server liefert seinen alten, eventuell noch gültigen Interop-Socket
und verdeckt Fehler. Testsitzungen auf eigenem `-L <eindeutig>` und am Ende `kill-server`.
Jeden Schutz mit einer Mutationsprobe gegenprüfen.

## 🔗 Bezug ↔ Bug-Almanach

Was konkret schiefging: [`bugs/desktop/openlauncher-windows-tmux.md`](../../bugs/desktop/openlauncher-windows-tmux.md).

| Best Practice (hier) | Bug-Almanach (→ bugs/desktop/openlauncher-windows-tmux.md) |
|----------------------|-------------------------------------------------------------|
| §1 Argumente getrennt | Windows-Pfade bei WSL-Aufrufen |
| §2 Instanz-Interop, §3 Halteprozess, §5 Wiederholen, §6 Test-Socket | Interop-Wettlauf beim Pane-Start |
| §4 Zielprozess messen | Store-PowerShell aus WSL gesperrt |
