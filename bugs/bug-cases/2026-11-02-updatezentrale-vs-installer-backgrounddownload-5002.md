# UpdateZentrale: Visual-Studio-Update scheitert an der eigenen Hintergrundaufgabe (02.11.2026)

> Behoben in UpdateZentrale 1.3.74. macOS nicht betroffen: Dort gibt es kein Visual Studio, und ein laufendes Programm sperrt seine Dateien nicht.

## Symptom

`winget upgrade --id Microsoft.VisualStudio.Community` (18.6.3 → 18.11.3) endete nach gut 3 Minuten mit `0x8A150115`, „Fehler bei der Installation des benutzerdefinierten Installationsprogramms“ und „Installation fehlgeschlagen mit Exitcode: 5002“. Die winget-Ausgabe nennt keinen Grund. Der steht nur in `%TEMP%\dd_bootstrapper_*.log`:

```
Comparing client version '4.7.25...' to autoSelfUpdateMinVersion '4.11.24...'
Warning: Unable to move directory from: ...\Microsoft Visual Studio\Installer ... Zugriff verweigert (5 Versuche in 18 s)
Error 0x80131500: ... Der Ordner ... Installer ... ist durch ein anderes Programm gesperrt.
```

## Ursache

1. **Der Bootstrapper muss zuerst sich selbst ersetzen.** Ist der installierte Installer zu alt, benennt er den Ordner `C:\Program Files (x86)\Microsoft Visual Studio\Installer` um. Das gelingt nur, wenn nichts daraus läuft.
2. **Visual Studio läuft selbst daraus.** Die geplante Aufgabe `\Microsoft\VisualStudio\Updates\BackgroundDownload` startet alle 3 Stunden als SYSTEM `Installer\resources\app\ServiceHub\Services\Microsoft.VisualStudio.Setup.Service\BackgroundDownload.exe`. Sie lief seit 20:54:53, das Update kam um 21:02.
3. **Der Zusammenstoß ist systematisch.** Die Aufgabe lädt lange, gerade wenn ein Update offen ist. Sie darf bis zu einen Tag laufen und lässt sich ohne Administratorrechte nicht beenden.

**Ausgeschlossen:** Fehlende Rechte. `ConsentPromptBehaviorAdmin` ist 0, die Dateien des Bootstrappers gehören den Administratoren. Visual Studio selbst lief nicht.

## Vorbeugung

Die Reparatur gilt für jede fremde Sperre, die nicht beendet werden darf:

- **Katalogfeld `warteAufProzesse`** (dazu `warteMinuten`, Vorgabe 90): Prozessnamen, auf die ein Update wartet. Abgleich nur über den Namen, weil ein SYSTEM-Prozess seinen Pfad vor einem normalen Benutzer verbirgt.
- **`Services/FremdeSperre` in `UpdateKette`:** Prüft vor jedem Durchlauf alle 30 Sekunden, für alle Update-Arten. Die Karte zeigt „Wartet auf …“. Nach dem Limit meldet das Update den Prozess als Grund und beendet ihn nie.
- **Ein zweiter Versuch:** Scheitert ein Lauf, während die Sperre läuft (die Aufgabe kann mitten im Lauf starten), wartet die Kette und versucht es einmal neu.
- **vs2026** trägt `"warteAufProzesse": ["BackgroundDownload"]`.
- **Bewusst nicht:** Prozess beenden oder Aufgabe abschalten. Dafür fehlen die Rechte, und der fertige Download verkürzt das Update.

## Vorgehen, wenn es wieder auftritt

```powershell
Get-Content (Get-ChildItem $env:TEMP -Filter dd_bootstrapper_* | Sort LastWriteTime | Select -Last 1).FullName -Tail 30
Get-ScheduledTask -TaskName BackgroundDownload | Get-ScheduledTaskInfo
Get-CimInstance Win32_Process -Filter "Name='BackgroundDownload.exe'" | Select ProcessId,ReadTransferCount,WriteTransferCount
```

Wachsen die Zähler, lädt die Aufgabe noch: abwarten. Bleiben sie über längere Zeit stehen, hängt sie. Dann muss sie jemand mit Administratorrechten beenden.
