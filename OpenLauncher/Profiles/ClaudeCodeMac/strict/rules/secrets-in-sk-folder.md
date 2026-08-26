# Secrets zentral im SK-Ordner (KRITISCH — Poka-Yoke Stufe 3)

> Ausloeser: GitHub Secret Scanning Alert — Firebase-Key im Repo wegen `.gitignore`-Ausnahme.

## Grundprinzip
Alle Keys, Signing-Keys, Tokens, Zugangsdaten leben AUSSERHALB aller Repos, zentral in `$HOME/SK/`
(Windows `/Users/frank/SK/`, macOS `/Users/barwa/SK/`). Kein Projekt enthaelt eigene Keys; alle lesen
aus `$HOME/SK/<projekt>/`. Liegt ein Secret nicht im Projekt, kann es nie in einen Commit geraten (Stufe 3
Eliminierung, staerker als `.gitignore`/Pre-Commit-Hooks).

## Wie Projekte SK verwenden
- **Android (Gradle):** `syncSecretsFromSk`-Task vor `preBuild`, kopiert aus `user.home/SK/<projekt>`; fehlt SK → GradleException.
- **C#/.NET, Swift, TS/Node:** `.env` mehrere Pfade, **SK zuerst** (`$HOME/SK/<projekt>/.env`).

## Pflicht + Chat-Secrets
Neues Projekt: `$HOME/SK/<projekt>/` anlegen · `.gitignore` listet Key-Dateinamen **ohne** Ausnahme-Regeln
· Template ins Repo. Key im Chat: sofort in `$HOME/SK/<projekt>/.env`, NIE in Repo/Code, aus Repo-Dateien
redaktieren (`task-ledger-helper.py` maskiert). Backup: `$HOME/SK/` NICHT in Git — manuell (Keystores kritisch).

## Was NIEMALS
- `.gitignore`-Ausnahme `!.../google-services.json` · Keys/Tokens ins Repo · `.env` im Projekt nach
  SK-Migration lassen · SK-Pfad hardcoden.
