# TVO/CVO: Update scheitert mit „Build-Artefakt passt nicht zum Projekt“ (01.11.2026)

> Behoben in TVO 1.11.35, CVO 2.4.31, rebuild-overlay.ps1 1.8.0, UpdateZentrale 1.3.60, Updater-Zentrale macOS 1.0.32; nach Code-Review nachgeschärft in TVO 1.11.36, CVO 2.4.32, rebuild-overlay.ps1 1.8.1, UpdateZentrale 1.3.62, macOS 1.0.34. Erster Vorfall: 22.10.2026.

## Symptom

`rebuild-overlay.ps1 TVO` baut erfolgreich und endet trotzdem mit `OVERLAY_UPDATE_STATUS=failed`: „erwartet 1.11.34, gefunden 1.11.33 (28.10.2026, 22:25 Uhr)+26ce29007…“. Die UpdateZentrale meldete dazu „Stand nachher: 1.11.33.0 vom 29.10.“, obwohl die exe neu gebaut war.

## Ursache

- Die csproj führte die Version doppelt: `<Version>` und `<InformationalVersion>` mit eigener Versionsnummer. Commit 0ba3d2b67 hob in TVO und CVO nur `<Version>` an.
- Die Produktversion der exe stammt aus `InformationalVersion`, `Test-BuiltArtifact` vergleicht sie aber mit `Version`. Der Widerspruch fiel erst nach Beenden und Bauen auf.
- Die Merkregel vom 22.10. („beide Zeilen ändern“) lag nur im Gedächtnis und hat den Fehler nicht verhindert.
- Falscher „Stand nachher“: `UpdateKette` las den Fingerabdruck bei einem Fehler nicht neu und meldete den Startwert.

## Vorbeugung

- Eine Quelle: `<InformationalVersion>$(Version) ($(VersionStand))</InformationalVersion>`. `VersionStand` entsteht beim Bauen aus der Systemzeit, von Hand wird nur noch `Version` geändert.
- `rebuild-overlay.ps1` wertet beide Properties vor dem Ja/Nein-Fenster und nach dem Klick mit `dotnet msbuild -getProperty` aus (Zeitgrenze 60 s). Bei Widerspruch oder unauswertbarem Projekt läuft das Overlay weiter und die Rückmeldung ist `refused`; bei `Both` wird das andere Ziel trotzdem aktualisiert. Antwortet MSBuild nicht, warnt das Skript sichtbar und läuft weiter.
- `UpdateKette` liest den Stand auch im Fehler- und Abbruchzweig neu (Windows und macOS). Die Lesung kann das Urteil nicht ersetzen und meldet `(nicht lesbar)`, wenn sie nichts liefert. Unter Windows ist sie auf 20 s begrenzt, auf macOS ohne eigene Grenze.

## Regel für die Fehlerart

Eine Versionsnummer steht an genau einer Stelle, alles andere wird daraus abgeleitet. Prüfungen, die ohne Eingriff möglich sind, laufen vor dem Beenden eines Programms.

## Dateien

- `TerminalVoiceOverlay-Windows/TerminalVoiceOverlay.csproj`
- `ClaudeVoiceOverlay-Windows/ClaudeVoiceOverlay.csproj`
- `rebuild-overlay.ps1`
- `UpdateZentrale/Services/UpdateKette.cs`
- `UpdaterZentrale-macOS/UpdaterZentrale/Services/UpdateKette.swift`
