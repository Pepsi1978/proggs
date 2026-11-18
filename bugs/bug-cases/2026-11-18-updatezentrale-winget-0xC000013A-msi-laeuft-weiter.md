# UpdateZentrale: winget endet mit 0xC000013A, das MSI installiert trotzdem fertig (18.11.2026)

> Behoben in UpdateZentrale 1.3.86, nach dem Review geschärft in 1.3.87 (Mutex per `WaitOne(0)` statt nur auf Existenz prüfen, eigene Wartegrenze von 5 min, Listen-Cache nach dem Warten verwerfen, Zeitüberschreitung als klarer Fehler, laufendes MSI als sprachneutrales Signal, Warten auch in `UpdateKette` nach jedem Fehlschlag für alle Arten). macOS ist nicht betroffen: Dort gibt es weder winget noch den Windows Installer als eigenständig weiterlaufenden Dienst.

## Symptom

Karte „PowerShell 7“ (`art: winget`, `Microsoft.PowerShell`, `wingetUpdatePerInstall`, `wingetInstallerTyp: wix`) im Sammellauf: `winget install … --force --installer-type wix --silent` endete nach „Paketinstallation wird gestartet…“ mit **0xC000013A**. Ergebnis „Fehlgeschlagen“, Stand vorher/nachher 7.6.9.0 → 7.6.9.0.

## Ursache

- **Das Update ist angekommen.** Laut Ereignisprotokoll (`MsiInstaller`, Id 1033/11707) lief das MSI von 19:57:27 bis 19:58:49 und endete mit Status 0. Danach war `pwsh.exe` 7.6.10 installiert.
- 0xC000013A ist `STATUS_CONTROL_C_EXIT`: winget selbst wurde beendet (Konsole geschlossen oder Ctrl+C), um 19:58:33. Der Windows-Installer-Dienst (msiexec) läuft unabhängig von winget weiter. Vermutlich hat der Restart Manager beim Ersetzen von pwsh die Konsole beendet.
- `WingetAktualisierer.AktualisierenAsync` wertete jeden Code ≠ 0 sofort als Fehler. `UpdateKette` liest den Stand nach einem Fehler nur einmal und sofort (19:58:34), 15 s bevor das MSI fertig war. Auch die normale Nachprüfung (4 × 3 s) wäre zu kurz gewesen.

## Behebung (ganze Fehlerart)

- `WingetAktualisierer.WaehrendInstallationBeendet`: Code im NTSTATUS-Fehlerbereich `0xC…` (winget starb: Ctrl+C, Konsole zu, Absturz) **und** die Ausgabe enthält „Starting package install“/„Paketinstallation wird gestartet“. Eigene winget-Fehler (`0x8A15…`) und Installer-Codes fallen nicht darunter. Stirbt winget vor dem Installerstart, bleibt es ein Fehler.
- Sprachneutral zählt zusätzlich: Code `0xC…` und gerade läuft eine MSI-Transaktion.
- Dann wartet `Services/WindowsInstaller.AbwartenAsync` (höchstens 5 min), bis keine Installer-Transaktion mehr läuft. Geprüft wird der Mutex `Global\_MSIExecute`: fehlt er oder lässt er sich mit `WaitOne(0)` sofort nehmen, ist der Installer frei. Der ruhende Dienst hält ihn offen, ohne ihn zu besitzen, deshalb reicht „existiert“ nicht. „Zugriff verweigert“ heißt besetzt, das Warten bleibt aber begrenzt. Danach wird der winget-Listen-Cache verworfen, der Provider meldet „durchgelaufen“, und die normale Nachprüfung der Kette entscheidet. Bewegt sich der Stand nicht, bleibt es fail-closed „Nicht verifiziert“, nie „Erfolgreich“. Läuft das MSI nach 5 min noch, gibt es einen klaren Fehler („nicht erneut starten, später prüfen“).
- Für alle Arten: `UpdateKette` wartet nach jedem Fehlschlag auf ein freies MSI, bevor sie „Stand nachher“ liest (nicht nach einem Abbruch durch den Nutzer).

## Erkennen

„winget endete mit Code 0xC…“ nach „Paketinstallation wird gestartet“: Zuerst die installierte Version prüfen (`(Get-Item <exe>).VersionInfo` oder `winget list`) und das Ereignisprotokoll `Application`/`MsiInstaller` lesen (Id 1033 mit Status 0 = installiert). Meist ist nur die Erkennung falsch.
