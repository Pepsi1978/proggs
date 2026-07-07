# Chrome-Shutdown-Sidebar

Eine schlanke Windows-11-Sidebar am **rechten Rand des Haupt-Monitors**. Fährt man mit der
Maus an den rechten Desktoprand, gleitet eine Leiste mit zwei Knöpfen aus: **Neustart** und
**Herunterfahren**.

Beim Klick wird **zuerst Chrome hart geschlossen** (über die Desktop-Verknüpfung
`Chrome beenden.lnk` → `taskkill /F /IM chrome.exe /T`) und **danach** der Neustart bzw. das
Herunterfahren ausgeführt. So ist sichergestellt, dass Chrome vor dem Beenden des Systems
sauber und vollständig geschlossen ist.

## Funktionen

- Erscheint **nur** am rechten Rand des Primär-Monitors (nicht auf den anderen Monitoren).
- Gleitet sanft ein/aus; bleibt sonst komplett unsichtbar.
- **3-Sekunden-Countdown mit Abbrechen** vor jeder Aktion — schützt vor versehentlichem Klick.
- Wartet nach dem Chrome-Beenden, bis wirklich kein `chrome.exe` mehr läuft (max. 6 s), dann
  erst Shutdown/Restart.
- **Immer an**: startet automatisch mit Windows (Autostart-Eintrag).
- Windows-11-Optik: dunkles, abgerundetes Panel mit Akzent-Buttons.

## Technik

- C# / .NET 10 / WPF (kein Python, laut Projekt-Konvention).
- Self-contained Single-File-`.exe` — keine .NET-Installation beim Endnutzer nötig.
- Per-Monitor-V2-DPI, Win32-Topmost-Handling (siehe Bug-Almanach
  `bugs/desktop/dotnet-csharp.md`, §1.5 / §2.1 / §3.3).

## Installation

```powershell
# Im Ordner dieses Projekts ausführen:
powershell -ExecutionPolicy Bypass -File .\build.ps1
```

Das Skript baut die `.exe` nach `%LOCALAPPDATA%\ChromeShutdownSidebar\`, richtet den
Autostart ein und startet die Sidebar sofort.

## Bedienung

1. Maus an den **rechten Rand des Haupt-Monitors** bewegen → Leiste fährt aus.
2. **Neustart** oder **Herunterfahren** anklicken.
3. Countdown läuft (3 s) — bei Bedarf **Abbrechen**.
4. Chrome wird beendet, danach startet Windows neu bzw. fährt herunter.

## Entfernen

```powershell
powershell -ExecutionPolicy Bypass -File .\autostart-entfernen.ps1
```

Beendet die App, löscht den Autostart-Eintrag und das Programm-Verzeichnis.

## Anpassen

- **Anderer Monitor / rechte Randzone**: `MainWindow.xaml.cs` → `PositionWindow()` und
  `TriggerZonePx`.
- **Wartezeit auf Chrome**: `MainWindow.xaml.cs` → `ChromeWaitMs`.
- **Countdown-Dauer**: `MainWindow.xaml.cs` → `StartCountdown()` (`_countdownValue = 3`).
- **Pfad zur Chrome-Verknüpfung**: `MainWindow.xaml.cs` → `ChromeLnk`.
