# Updater-Zentrale macOS: Android-Studio-Update bricht mit „Zeitlimit überschritten“ ab (28.10.2026)

> Behoben in Updater-Zentrale macOS 1.0.22.

## Symptom

`brew upgrade --cask android-studio` lief 45 Minuten und wurde dann mit SIGTERM beendet. Das DMG (1,5 GB) war zu diesem Zeitpunkt schon vollständig geladen und hatte die richtige Prüfsumme.

## Ursache

- Das Zeitlimit galt fest für den ganzen Prozess. Ein langsamer, aber laufender Download war davon nicht von einem hängenden zu unterscheiden.
- Ohne Terminal gibt brew während des Downloads keinen Fortschritt aus, „Fetching downloads“ erscheint erst am Ende. Die Ausgabe allein taugt deshalb nicht als Lebenszeichen.
- Android Studio wurde vor dem Download geschlossen und blieb so die ganze Downloadzeit zu.

## Behebung

- `Kommandozeile.ausfuehren(…, lebenszeichen:)`: Das Zeitlimit zählt jetzt als Ruhezeit. Es läuft erst ab, wenn weder Ausgabe kommt noch der Homebrew-Ordner `downloads` wächst. Insgesamt gilt eine Obergrenze von 8 Stunden.
- `Aktualisierer.vorbereiten`: Für Casks läuft `brew fetch --cask` vor der Frage, ob das Programm beendet werden darf. `brew upgrade` installiert danach aus dem Cache.
- Fehlgeschlagene Updates gehen automatisch an Claude Code. Je Programm geschieht das höchstens einmal in 6 Stunden. Der Auftrag lautet: Ursache finden, Update zu Ende bringen, die Fehlerart dauerhaft vorbeugen und den Fall hier festhalten.

## Dateien

- `UpdaterZentrale-macOS/UpdaterZentrale/Services/Kommandozeile.swift`
- `UpdaterZentrale-macOS/UpdaterZentrale/Providers/BrewAktualisierer.swift`
- `UpdaterZentrale-macOS/UpdaterZentrale/Providers/Aktualisierer.swift`
- `UpdaterZentrale-macOS/UpdaterZentrale/ViewModels/ProgrammViewModel.swift`
- `UpdaterZentrale-macOS/UpdaterZentrale/Services/ClaudeUebergabe.swift`
