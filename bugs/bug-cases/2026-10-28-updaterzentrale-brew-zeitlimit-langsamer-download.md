# Updater-Zentrale macOS: Android-Studio-Update bricht mit „Zeitlimit überschritten“ ab (28.10.2026)

> Behoben in Updater-Zentrale macOS 1.0.22, nachgeschärft in 1.0.24.

## Symptom

`brew upgrade --cask android-studio` lief 45 Minuten und wurde dann mit SIGTERM beendet. Das DMG (1,5 GB) war zu diesem Zeitpunkt schon vollständig geladen und hatte die richtige Prüfsumme.

## Ursache

- Das Zeitlimit galt fest für den ganzen Prozess. Ein langsamer, aber laufender Download war davon nicht von einem hängenden zu unterscheiden.
- Ohne Terminal gibt brew während des Downloads keinen Fortschritt aus, „Fetching downloads“ erscheint erst am Ende. Die Ausgabe allein taugt deshalb nicht als Lebenszeichen.
- Android Studio wurde vor dem Download geschlossen und blieb so die ganze Downloadzeit zu.

## Behebung

- `Kommandozeile.ausfuehren(…, lebenszeichen:)`: Das Zeitlimit zählt jetzt als Ruhezeit. Es läuft erst ab, wenn weder Ausgabe kommt noch der Homebrew-Ordner `downloads` wächst. Insgesamt gilt eine Obergrenze von 8 Stunden.
- `Aktualisierer.vorbereiten`: Für Casks läuft `brew fetch --cask` nach dem Ja zum Beenden, aber bevor das Programm geschlossen wird. Hängt oder scheitert der Download, bleibt das Programm offen und das Update endet mit einer Fehlermeldung. Klappt er, installiert `brew upgrade` danach aus dem Cache.
- Fehlgeschlagene Updates gehen automatisch an Claude Code, „nicht verifiziert“ zählt nicht dazu. Ein Sammellauf übergibt alle Fehler erst am Ende in einer einzigen Sitzung. Die Sperrfrist von 6 Stunden je Programm wird gespeichert und übersteht so auch einen Neustart; sie zählt erst ab einem gelungenen Start der Sitzung. Der Auftrag lautet: Ursache finden, Update zu Ende bringen, die Fehlerart dauerhaft vorbeugen und den Fall hier festhalten.

## Dateien

- `UpdaterZentrale-macOS/UpdaterZentrale/Services/Kommandozeile.swift`
- `UpdaterZentrale-macOS/UpdaterZentrale/Providers/BrewAktualisierer.swift`
- `UpdaterZentrale-macOS/UpdaterZentrale/Providers/Aktualisierer.swift`
- `UpdaterZentrale-macOS/UpdaterZentrale/ViewModels/ProgrammViewModel.swift`
- `UpdaterZentrale-macOS/UpdaterZentrale/Services/ClaudeUebergabe.swift`
