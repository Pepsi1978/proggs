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
- Fehlgeschlagene Updates gehen automatisch an Claude Code, „nicht verifiziert“ zählt nicht dazu. Ein Sammellauf übergibt alle Fehler erst am Ende in einer einzigen Sitzung. Die Sperrfrist je Programm wird gespeichert und übersteht so auch einen Neustart (damals 6 Stunden, Stand heute siehe „Erweiterung der Übergabe“). Der Auftrag lautet: Ursache finden, Update zu Ende bringen, die Fehlerart dauerhaft vorbeugen und den Fall hier festhalten.

## Windows (UpdateZentrale 1.3.45)

- Die automatische Übergabe an Claude Code funktioniert unter Windows genauso: nur bei „Fehlgeschlagen“, eine Sitzung am Ende eines Sammellaufs, eine gespeicherte Sperrfrist (`AutomatischUebergebenAm`).
- `Kommandozeile.AusfuehrenAsync(…, lebenszeichen:)`: Das Zeitlimit zählt als Ruhezeit. winget wächst `%TEMP%\WinGet`, der Hersteller-Download per curl wächst die Zieldatei.
- Einen Vorab-Download gibt es unter Windows nicht. `winget upgrade` nutzt einen vorher mit `winget download` geladenen Installer nicht und würde ihn ein zweites Mal laden.

## Erweiterung der Übergabe (01.11.2026)

- Ablauf der Sitzung jetzt fest: Ursache finden, dauerhaft reparieren, Almanach, Update zu Ende bringen, Standard-Code-Review (`REVIEW: ja`, ohne Rückfrage), UpdateZentrale als allerletzten Schritt neu bauen. Windows und macOS.
- Die Sperrfrist beträgt nur noch 1 Stunde statt 6: Sie soll nur eine zweite Sitzung auf denselben Fehler verhindern, solange die erste läuft. Scheitert ein Programm nach der Reparatur erneut, muss das schnell wieder an eine Sitzung gehen. Antwortet der Launcher nicht, bleibt die Marke stehen, weil die Sitzung dann meist doch läuft. Auch ein Klick auf „An Claude Code übergeben“ setzt die Marke.
- Windows: Ein Update, das mit einer Ausnahme abbrach, schrieb keinen Bericht und erreichte die Übergabe nie. `ProgrammViewModel.AusnahmeBerichten` schließt diese Lücke, außer beim Beenden der App. Eine gesperrte Übergabe steht auf der Karte bzw. in der Sammelzeile, nie in einem blockierenden Fenster.
- Leitplanken im Auftrag: Programme mit CLI-Risiko laufen erst nach Review und Neubau und abgekoppelt, die UpdateZentrale selbst nur über Schritt 6, Beenden nur nach Rückfrage, Versionsnummer direkt vor dem Bump frisch lesen. Der Neubau entfällt, wenn sich im Projektordner nichts geändert hat.
- `update-zentrale.sh` fragt jetzt selbst nach, wenn es von außen gestartet wird und die App läuft; beide Skripte warnen, dass ein laufendes Update abbricht.
- Weiter ausgenommen: „nicht verifiziert“ und unter Windows die `LangsamesNetzMeldung`, weil eine Sitzung beides nicht beheben kann.
- Bekannte Grenze: Die Frist steht stellvertretend für „Sitzung läuft noch“. Eine Sitzung über eine Stunde oder Sitzungen zu verschiedenen Programmen können parallel laufen. Eine Sperrdatei je Sitzung bräuchte eine Warteschlange, damit kein Fehler verloren geht.

## Dateien

- `UpdaterZentrale-macOS/UpdaterZentrale/Services/Kommandozeile.swift`
- `UpdaterZentrale-macOS/UpdaterZentrale/Providers/BrewAktualisierer.swift`
- `UpdaterZentrale-macOS/UpdaterZentrale/Providers/Aktualisierer.swift`
- `UpdaterZentrale-macOS/UpdaterZentrale/ViewModels/ProgrammViewModel.swift`
- `UpdaterZentrale-macOS/UpdaterZentrale/Services/ClaudeUebergabe.swift`
