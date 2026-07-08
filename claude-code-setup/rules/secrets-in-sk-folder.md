# Secrets zentral im SK-Ordner (KRITISCH — Poka-Yoke Stufe 3)

> Ausloeser: GitHub Secret Scanning Alert — Firebase-Debug-Key im Repo wegen einer `.gitignore`-Ausnahme.

## Grundprinzip

Alle API-Keys, Signing-Keys, Tokens, Zugangsdaten leben AUSSERHALB aller Repos, zentral in `$HOME/SK/`
(Windows `C:\Users\barwa\SK\`, macOS `/Users/barwa/SK/`). Kein Projekt enthaelt eigene Keys; alle lesen
aus `$HOME/SK/<projekt>/`.

## Warum (Stufe 3 — Eliminierung)

Liegt ein Secret gar nicht im Projekt-Ordner, kann es nie in einen Commit geraten — egal welcher
Agent/Befehl/Fehler. Staerker als `.gitignore` (Stufe 1) oder Pre-Commit-Hooks (Stufe 2).

## Wie Projekte SK verwenden

- **Android (Gradle):** `syncSecretsFromSk`-Task vor `preBuild`, kopiert aus
  `user.home/SK/<projekt>`; fehlt SK → klarer GradleException.
- **C#/.NET, Swift, TS/Node:** `.env` an mehreren Pfaden suchen, **SK zuerst** (`$HOME/SK/<projekt>/.env`).

## Pflicht fuer neue Projekte

`$HOME/SK/<projekt>/` anlegen · Key dort ablegen · Code liest daraus · `.gitignore` listet alle
Key-Dateinamen **ohne** Ausnahme-Regeln (`!debug/...`) · Template/README ins Repo.

## Secrets im Chat (Frank gibt einen Key)

Sofort: (1) in `$HOME/SK/<projekt>/.env`, (2) ins Ziel-System (VPS-`.env`), NIE in Repo/Code, (3) aus
allen Repo-Dateien redaktieren. Der Ledger-Hook `task-ledger-helper.py` (`_redact_secrets`) maskiert
bekannte Muster (`tvly-`, `sk-`, `gh[pousr]_`, `AIza`, `glpat-`, …) automatisch; neue Formate ergaenzen.

## Backup-Pflicht

`$HOME/SK/` ist NICHT in Git/Cloud-Sync — manuell woechentlich sichern. Release-Keystores besonders
(ihr Verlust = App nie wieder im Play Store aktualisierbar).

## Was NIEMALS

- `.gitignore`-Ausnahme `!.../google-services.json` (Root Cause des Leaks) · Keys/Tokens/Passwoerter ins
  Repo (auch nicht in Doku/Test-Fixtures) · `.env` im Projekt-Ordner nach SK-Migration lassen · SK-Pfad
  hardcoden (immer `$HOME/SK/`).
