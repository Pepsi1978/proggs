# Bekannte Bugs: Electron-/MSIX-Apps mit Administratorrechten starten (Windows)

> Stand: 2026-09-20. Belegt am lebenden Objekt (Codex Desktop `OpenAI.Codex 26.915.4065.0`,
> Windows 11 Build 26200). Umsetzung und Doku: `Werkzeuge/codex-autostart/`.
> Gilt für **jede** Electron-App, die als MSIX/Store-Paket ausgeliefert wird —
> Codex Desktop, ChatGPT Desktop, viele Store-Ports.

## ⚡ Kurzcheck (vor der Arbeit lesen)

| # | Signal / Situation | Sofort-Regel |
|---|---|---|
| 1 | Launcher meldet "der erhöhte Prozess wurde beendet", App läuft danach trotzdem — nur unerhöht | Chromium **de-elevatet sich selbst**. Beim Start `--do-not-de-elevate` mitgeben. Ohne den Schalter ist ein erhöhter Start unmöglich. |
| 2 | Start über `shell:AppsFolder\<Paket>!App` mit `-Verb RunAs` ergibt trotzdem normale Rechte | MSIX-Paketaktivierung läuft **immer** im Medium-Integritätslevel, solange das Manifest kein `<rescap:Capability Name="allowElevation"/>` deklariert. `runFullTrust` reicht **nicht**. Direkt die `.exe` im Paketordner starten. |
| 3 | `Process.Start` mit `UseShellExecute = $true` verliert die Rechte | ShellExecute reicht den Start an die Shell weiter, die das Paket normal aktiviert. Nur `UseShellExecute = $false` (CreateProcess) vererbt das erhöhte Token. |
| 4 | Prozesssuche über die Kommandozeile findet nichts (`--type=` herausfiltern) | Die **Kommandozeile erhöhter Prozesse ist für normale Prozesse nicht lesbar** — `Win32_Process.CommandLine` kommt leer zurück, der Filter greift ins Leere. Hauptprozess stattdessen daran erkennen, dass sein Elternprozess nicht dieselbe `.exe` ist. |
| 5 | Zweiter Start stirbt sofort, obwohl die Prüfung "läuft nicht" sagte | Electrons Single-Instance-Lock. Erst prüfen, ob eine Instanz läuft (Punkt 4), unerhöhte vor dem erhöhten Start beenden und auf das Verschwinden **warten**. |
| 6 | Klick auf die Verknüpfung tut scheinbar nichts, App liegt im Tray | `Process.MainWindowHandle` liefert nur **sichtbare** Fenster und ist bei Tray-Apps 0. Fenster per `EnumWindows` selbst suchen (Owner-Fenster und `WS_EX_TOOLWINDOW` überspringen). |
| 7 | Aktivierung meldet Erfolg, Benutzer sieht nichts | Rückgabewert von `SetForegroundWindow` ist kein Erfolgsmaß. Immer `IsWindowVisible` **und** `IsIconic` nachprüfen. (Gleiche Lehre wie `openlauncher-terminalfarben`-Umfeld, siehe OpenLauncher-Bug-Case vom 20.09.2026.) |
| 8 | Nach dem Umstieg auf erhöht: Drag & Drop und Automatisierung funktionieren nicht mehr | **UIPI**: unerhöhte Prozesse dürfen erhöhten Fenstern keine Eingaben schicken. Betrifft Explorer-Drag&Drop, Overlays, Sendkeys-Werkzeuge, Autohotkey. Kein Bug — Designentscheidung von Windows. |
| 9 | PowerShell: `$liste = Funktion-Die-Ein-Array-Liefert` und `.Count` ist leer | **Nicht Electron-spezifisch, sondern die teuerste Falle hier.** PowerShell entrollt Arrays beim Funktionsrückgabewert; bei genau einem Treffer bleibt ein einzelnes Objekt übrig. Bei `CimInstance` existiert `.Count` nicht → `$null` → `if ($liste.Count)` ist still `false`. Immer `@(Funktion)` am **Aufruf**. |

## §1 Die De-Elevation von Chromium

Chromium beendet unter Windows den Browser-Prozess, wenn er erhöht gestartet wurde, und
startet sich sofort unerhöht neu. Der aufrufende Launcher sieht nur den sterbenden Prozess.

Typisches Fehlerbild in einem selbstgebauten Launcher:

```
Der erhöhte Codex-Prozess wurde beendet.
Möglicherweise hat eine andere Instanz den Start übernommen.
```

Die Meldung ist korrekt — die "andere Instanz" ist die App selbst, unerhöht neu gestartet.
Erkennbar daran, dass die laufende Instanz einen bereits beendeten Elternprozess hat und
`--do-not-de-elevate` in ihrer eigenen Kommandozeile trägt.

Gegenmittel: den Schalter beim Start selbst setzen.

```powershell
$info = New-Object Diagnostics.ProcessStartInfo
$info.FileName        = $exe          # app\ChatGPT.exe im Paketordner
$info.Arguments       = '--do-not-de-elevate'
$info.UseShellExecute = $false        # CreateProcess, vererbt das erhöhte Token
[Diagnostics.Process]::Start($info)
```

## §2 MSIX-Paketaktivierung kann nicht erhöht werden

Prüfen, was das Manifest deklariert:

```powershell
$p = (Get-AppxPackage -Name <Name>).InstallLocation
Select-String -Path (Join-Path $p 'AppxManifest.xml') -Pattern 'Capability Name'
```

Steht dort kein `allowElevation`, ist der Weg über `shell:AppsFolder` endgültig zu.
Der direkte `.exe`-Start ist dann der einzige Weg. Nebenwirkung: Der Prozess läuft **ohne
Paket-Identität**. In der Praxis unkritisch, kann aber Auto-Update und Protokollhandler
(`app://`, `codex://`) betreffen — aktualisiert sich die App irgendwann nicht mehr selbst,
liegt es daran.

Der Paketpfad enthält die Version und ändert sich bei jedem Update. Immer zur Laufzeit über
`Get-AppxPackage` ermitteln, nie fest verdrahten.

## §3 Prüfen, ob es wirklich erhöht läuft

Nicht auf Selbstauskunft verlassen. Zwei belastbare Proben:

```powershell
# 1. Kommandozeile: bei erhöhten Prozessen aus einem normalen Prozess heraus LEER
Get-CimInstance Win32_Process -Filter "Name='ChatGPT.exe'" | Select-Object ProcessId, CommandLine

# 2. Token direkt: OpenProcess/GetTokenInformation(TokenElevation).
#    "Zugriff verweigert" aus einem normalen Prozess ist selbst schon der Beweis.
```

## §4 Autostart ohne UAC-Abfrage

Ein erhöhter Autostart gehört in die **Aufgabenplanung**, nicht in `HKCU\...\Run` und nicht
in den Startup-Ordner — beide können nicht erhöht starten.

```powershell
$prinzipal = New-ScheduledTaskPrincipal -UserId <SID> -LogonType Interactive -RunLevel Highest
```

`RunLevel Highest` + `LogonType Interactive` startet beim Anmelden erhöht **ohne** UAC-Abfrage.
Eine Verzögerung von ~20 s einplanen, sonst konkurriert der Start mit dem Hochfahren.

Fallstrick: Einen Task im Wurzelordner **anzulegen oder zu ändern** braucht selbst
Administratorrechte (`Register-ScheduledTask` → "Zugriff verweigert"), ihn zu **starten** und
zu **löschen** dagegen nicht. Zum Testen eines erhöhten Launchers ist `Start-ScheduledTask`
auf einer bestehenden Aufgabe deshalb der bequemste Weg — er umgeht die UAC-Abfrage.

## §5 Sonderzeichen in Task-Namen

Ein Task namens `Codex Desktop – Start im System-Tray` (Gedankenstrich U+2013) lässt sich aus
einem `.ps1` heraus nicht zuverlässig per Literal ansprechen: Windows PowerShell 5.1 liest
Skriptdateien ohne BOM als ANSI, der Gedankenstrich wird verstümmelt, `Unregister-ScheduledTask`
findet nichts und legt die Aufgabe doppelt an. Alte Aufgaben über ein Muster suchen:

```powershell
Get-ScheduledTask | Where-Object { $_.TaskName -match '^Codex Desktop .+Start im System-Tray$' }
```

Besser: in neuen Namen nur ASCII verwenden.
