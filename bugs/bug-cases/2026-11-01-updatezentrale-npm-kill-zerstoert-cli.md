# UpdateZentrale: Zeitlimit-Kill während `npm install -g` zerstört die CLI (01.11.2026)

> Behoben in UpdateZentrale 1.3.68 und Updater-Zentrale macOS 1.0.40. Geschwisterfall: `2026-10-28-updaterzentrale-brew-zeitlimit-langsamer-download.md`.

## Symptom

`codex.cmd update` lief im Sammellauf 15 Minuten, dann beendete das Zeitlimit den Prozessbaum. Danach meldete die Karte: „Codex CLI fehlt nach dem Update (`…\Programs\OpenAI\Codex\bin\codex.exe`) – bitte neu installieren.“ Stand nachher: nicht lesbar.

## Ursache

1. **npm räumt zuerst weg und lädt danach.** Im npm-Debug-Log steht `reify mark retired` vor dem Abruf der Tarball. Stirbt npm in dieser Lücke hart, läuft sein Rollback nicht. `codex.cmd` und das Paket waren weg.
2. **Der Download war langsam, aber er lief.** In `npm-cache\_cacache\tmp` blieb eine 118 MB große Teildatei mit dem Schreibzeitpunkt 16:21:08 liegen, genau beim Kill. Die Tarball ist `@openai/codex-win32-x64` (npm-Log eines späteren Laufs: bei freiem Netz 35 s). Im selben Sammellauf brach auch der Download von Antigravity mit `context deadline exceeded` ab.
3. **Festes Zeitlimit ohne Lebenszeichen.** `codex` puffert seine Ausgabe ohne Terminal, die einzige Zeile erschien erst beim Kill. Der Lauf sah also tot aus.
4. **Falscher Pfad in der Meldung.** Ohne vorhandenen Kandidaten fällt `ExePfadWirksam` auf den Katalog-Pfad zurück. Die Meldung nannte deshalb eine `codex.exe`, die auf diesem Rechner nie existierte.

Das Update selbst hat sieben Mal vorher geklappt. Es scheitert nur, wenn der Download länger als das Zeitlimit dauert. Die Fehlerart betrifft jede global per npm installierte CLI, auch `claude update` und direkte `npm install -g`-Einträge.

## Vorbeugung

- `Providers/NpmGlobal`: Liegt die Programmdatei im globalen npm-Ordner und ist `npmPaket` gesetzt, lädt `npm install --prefix <temp> <paket>@latest --ignore-scripts` die neue Version zuerst in den npm-Cache. Ein Abbruch dort zerstört nichts. Das eigentliche Update tauscht danach aus dem Cache.
- Das Zeitlimit des Vorab-Downloads zählt als Ruhezeit: Es läuft ab, wenn `_cacache\tmp` nicht mehr wächst.
- Scheitert der Vorab-Download am Netz, endet der Lauf mit der `LangsamesNetzMeldung`: installierte Version unverändert, keine Übergabe an Claude.
- Fehlt die Programmdatei nach dem Lauf trotzdem, stellt `npm install -g <paket>@latest` sie sofort wieder her, auch nach einem Klick auf Abbrechen.
- `Programmpruefung.Vollstaendig` prüft den vor dem Lauf wirksamen Pfad und nennt damit den richtigen Ort.
- macOS hat dieselbe npm-Reihenfolge und bekommt deshalb dasselbe, erkannt über einen Symlink nach `node_modules`. Die Meldung ist dort eine normale Fehlermeldung, die Übergabe bleibt also aktiv.

## Nebenbefunde

- Ein Test mit einer schon gecachten Version zeigt keinen Download. Für Messungen eine Version nehmen, die noch nie installiert war.
- Abgebrochene npm-Läufe lassen ihre Teildateien dauerhaft in `_cacache\tmp` liegen. Hier waren es 194 MB, die npm nicht selbst aufräumt. Das Lebenszeichen zählt deshalb nur Wachstum über den bisher höchsten Wert.
- npm hält nur 10 Debug-Logs (`logs-max`). Das Log des gescheiterten Laufs war schon weg, die Teildatei war der Beleg.

## Dateien

- `UpdateZentrale/Providers/NpmGlobal.cs`, `Providers/CliAktualisierer.cs`, `Services/Programmpruefung.cs`, `ViewModels/ProgrammViewModel.cs`
- `UpdaterZentrale-macOS/UpdaterZentrale/Providers/NpmGlobal.swift`, `Providers/CliAktualisierer.swift`, `Services/Programmpruefung.swift`, `ViewModels/ProgrammViewModel.swift`, `build.sh`
