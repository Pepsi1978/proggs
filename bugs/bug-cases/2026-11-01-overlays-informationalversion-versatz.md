# TVO/CVO: Update scheitert mit „Build-Artefakt passt nicht zum Projekt“ (01.11.2026)

> Behoben in TVO 1.11.35, CVO 2.4.31, rebuild-overlay.ps1 1.8.0, UpdateZentrale 1.3.60, Updater-Zentrale macOS 1.0.32. Erster Vorfall: 22.10.2026.

## Symptom

`rebuild-overlay.ps1 TVO` baut erfolgreich und endet trotzdem mit `OVERLAY_UPDATE_STATUS=failed`: „erwartet 1.11.34, gefunden 1.11.33 (28.10.2026, 22:25 Uhr)+26ce29007…“. Die UpdateZentrale meldete dazu „Stand nachher: 1.11.33.0 vom 29.10.“, obwohl die exe neu gebaut war.

## Ursache

- Die csproj führte die Version doppelt: `<Version>` und `<InformationalVersion>` mit eigener Versionsnummer. Commit 0ba3d2b67 hob in TVO und CVO nur `<Version>` an.
- Die Produktversion der exe stammt aus `InformationalVersion`, `Test-BuiltArtifact` vergleicht sie aber mit `Version`. Der Widerspruch fiel erst nach Beenden und Bauen auf.
- Die Merkregel vom 22.10. („beide Zeilen ändern“) lag nur im Gedächtnis und hat den Fehler nicht verhindert.
- Falscher „Stand nachher“: `UpdateKette` las den Fingerabdruck bei einem Fehler nicht neu und meldete den Startwert.

## Vorbeugung

- Eine Quelle: `<InformationalVersion>$(Version) ($(VersionStand))</InformationalVersion>`. Von Hand werden nur noch `Version` und `VersionStand` geändert.
- `rebuild-overlay.ps1` wertet beide Properties vor dem Ja/Nein-Fenster mit `dotnet msbuild -getProperty` aus. Passen sie nicht zusammen, endet es mit `failed`, und das Overlay läuft weiter.
- `UpdateKette` liest den Fingerabdruck auch im Fehlerzweig neu (Windows und macOS).

## Regel für die Fehlerart

Eine Versionsnummer steht an genau einer Stelle, alles andere wird daraus abgeleitet. Prüfungen, die ohne Eingriff möglich sind, laufen vor dem Beenden eines Programms.

## Dateien

- `TerminalVoiceOverlay-Windows/TerminalVoiceOverlay.csproj`
- `ClaudeVoiceOverlay-Windows/ClaudeVoiceOverlay.csproj`
- `rebuild-overlay.ps1`
- `UpdateZentrale/Services/UpdateKette.cs`
- `UpdaterZentrale-macOS/UpdaterZentrale/Services/UpdateKette.swift`
