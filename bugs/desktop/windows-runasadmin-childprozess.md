# Win32-Fehler 740 beim Start eines Kindprozesses (RUNASADMIN-Kompatibilitätsflag)

**Stand:** 20.09.2026 · Windows 11 · .NET 10 · betrifft TVO/CVO

## Symptom
Die App meldet „Aufnahme nicht möglich" bzw. der Watchdog startet das Overlay nicht mehr.
Im Log steht:
`System.ComponentModel.Win32Exception (740): An error occurred trying to start process
'…\publish\X.exe'. Der angeforderte Vorgang erfordert erhöhte Rechte.`

## Ursache
Die exe trägt das AppCompat-Flag `RUNASADMIN` unter
`HKCU\Software\Microsoft\Windows NT\CurrentVersion\AppCompatFlags\Layers`
(gesetzt über die Explorer-Eigenschaften „Als Administrator ausführen" oder über die
UpdateZentrale). Startet ein **nicht erhöhter** Prozess dieselbe exe per
`Process.Start` mit `UseShellExecute = false` (nötig für Stdout/Stderr-Redirect),
verlangt Windows Elevation, kann aber ohne Shell keinen UAC-Dialog zeigen →
`ERROR_ELEVATION_REQUIRED` (740). Das Manifest steht dabei korrekt auf `asInvoker` —
das Flag kommt von außen und ist im Code nicht sichtbar.

## Lösung (Poka-Yoke)
Für Kindprozesse, die keine erhöhten Rechte brauchen, die Elevation-Anforderung im
Umgebungsblock des Kindes aushebeln:

```csharp
psi.Environment["__COMPAT_LAYER"] = "RunAsInvoker";
```

Wirkt unabhängig davon, ob das Flag gesetzt, entfernt oder später erneut gesetzt wird —
kein Registry-Schreiben, die bewusste Benutzer-Einstellung bleibt erhalten.

Zusätzlich `Win32Exception.NativeErrorCode == 740` gesondert fangen und mit einem
verständlichen deutschen Text melden statt der rohen englischen Meldung.

## Fundstellen
- `TerminalVoiceOverlay-Windows/Services/AudioRecorder.cs` (Capture-Worker)
- `TerminalVoiceOverlay-Windows/App.xaml.cs` → `StartOverlayProcess` (Watchdog)
- gleiche Stellen in `ClaudeVoiceOverlay-Windows`
- Fix-Commit: `dd9655544` (20.09.2026)
