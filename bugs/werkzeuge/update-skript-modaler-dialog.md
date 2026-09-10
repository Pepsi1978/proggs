# Update-Skript hängt bis zum Timeout: modaler Dialog ohne Terminal

Festgehalten am 10.09.2026, 11:02 Uhr. Betrifft `OpenLauncher/update-launcher.ps1` und `OpenLauncherMac/update-launcher.sh`.

## Falle

Das Update-Skript lief scheinbar endlos, obwohl der Build längst durch war. Der Aufrufer wartete bis in sein Timeout.

## Ursache

Zwei unabhängige Fehler, die sich addierten:

1. **Modaler Dialog ohne Klickmöglichkeit.** Lief noch ein Launcher, zeigte das Skript `[System.Windows.MessageBox]::Show(...)` (macOS: `osascript display dialog`). Wird das Skript aus einem Agenten oder einer Pipeline gestartet, erscheint dieses Fenster irgendwo im Hintergrund — niemand sieht es, niemand klickt, das Skript blockiert bis zum Timeout des Aufrufers.
2. **Keine Aktualitätsprüfung.** Ein Build, der Sekunden zuvor mit demselben Quellstand gelaufen war, wurde bedingungslos wiederholt.

## Vorgehen

**Interaktivität erkennen, statt sie anzunehmen:**

```powershell
$interactive = -not $Force -and -not [System.Console]::IsInputRedirected -and [Environment]::UserInteractive
```

```bash
interaktiv() { [ -t 0 ] && [ "${OPENLAUNCHER_UPDATE_FORCE:-0}" != "1" ]; }
```

Ohne Terminal gilt automatisch „Ja" plus eine Zeile auf der Standardausgabe, damit die Entscheidung im Protokoll steht.

**Aktualität prüfen, bevor gebaut wird:** Version der gebauten Datei gegen die Projektversion, und kein Quellstand neuer als die gebaute Datei. Ist beides erfüllt und der Launcher läuft bereits, meldet das Skript `LAUNCHER_UPDATE_STATUS=already-current` und ist in unter einer Sekunde fertig.

**Wichtiger Fallstrick dabei:** Nur echten Quellcode vergleichen (`.cs`, `.xaml`, `.csproj`). Nimmt man `.json` mit auf, schreibt der Launcher im Betrieb selbst eine Laufzeitdatei (hier `models.json`) neu — der frisch gebaute Stand gilt dann sofort wieder als veraltet, und es wird bei **jedem** Aufruf sinnlos gebaut. Genau dieser Fehler trat beim ersten Anlauf auf und fiel nur durch die Gegenprobe auf.

## Regel

Jeder Dialog in einem Skript, das auch von Werkzeugen aufgerufen wird, braucht einen Weg an sich vorbei. Ein modaler Dialog ohne Terminal ist kein Warten — er ist ein Deadlock.
