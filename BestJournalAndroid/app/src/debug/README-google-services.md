# google-services.json fehlt?

Die Datei `google-services.json` wird **nicht mehr im Repo gespeichert** — alle Secrets
liegen zentral in `$HOME/SK/BestJournalAndroid/`.

## Einmalige Einrichtung

1. Stelle sicher dass der Ordner existiert:
   - Windows: `C:\Users\<USER>\SK\BestJournalAndroid\`
   - macOS:   `/Users/<USER>/SK/BestJournalAndroid/`

2. Lege folgende Dateien dort ab (aus Backup oder Firebase Console):
   - `google-services-debug.json`
   - `google-services-release.json`
   - `debug-shared.keystore`
   - `release.keystore`
   - `keystore.properties`

3. Baue die App — der Gradle-Task `syncSecretsFromSk` kopiert automatisch alle
   Secrets an die erwarteten Pfade vor dem Build.

Siehe `$HOME/SK/README.md` fuer vollstaendige Doku.
