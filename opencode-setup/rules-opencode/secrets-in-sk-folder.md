Secrets zentral im SK-Ordner (KRITISCH — Poka-Yoke Stufe 3)

Alle API-Keys, Signing-Keys, Tokens und vertraulichen Zugangsdaten leben AUSSERHALB aller Repos — in einem zentralen Ordner pro Rechner: `$HOME/SK/` (Windows `C:\Users\barwa\SK\`, macOS `/Users/barwa/SK/`). Kein Projekt im Repo enthaelt eigene Keys; jedes Projekt liest seine Keys aus `$HOME/SK/<projekt-name>/`.

## Warum (Poka-Yoke Stufe 3 — Eliminierung)
Was gar nicht erst im Projekt-Ordner liegt, kann nie in einen Commit geraten — egal welcher Agent/Befehl/Fehler. Ein nicht existierendes File kann nicht committed werden. Das ist staerker als `.gitignore` (Stufe 1) oder Pre-Commit-Hooks (Stufe 2).

## Pflicht fuer neue Projekte
1. Unterordner `$HOME/SK/<projekt>/` anlegen, Key-Datei(en) dort ablegen.
2. Build-Task/Start-Code liest aus `$HOME/SK/<projekt>/` (z.B. Gradle-Sync-Task, oder .env-Suche mit SK als erster Prioritaet).
3. `.gitignore` listet alle Key-Dateinamen — OHNE Ausnahme-Regeln wie `!debug/...`.
4. Template/README ins Repo (`google-services.json.example`, `ENV.SETUP.md`).

## Secret taucht im Chat auf (Frank gibt einen Key direkt)
SOFORT: (1) in den SK-Ordner ablegen, (2) ins Ziel-System eintragen (z.B. VPS-`.env`, nie in eine Repo-Datei/Code — nur `os.getenv(...)`-Verweise), (3) aus allen Repo-Dateien redaktieren, falls er irgendwo gelandet ist (Key durch `[REDACTED-...]` ersetzen vor Commit).

## Backup
`$HOME/SK/` ist nicht im Git und nicht im Cloud-Sync. Manuell sichern (extern/verschluesselt). Release-Keystores sind unwiederbringbar — ihr Verlust = App nie wieder im Play Store aktualisierbar.

## NIEMALS
- `.gitignore`-Ausnahme `!app/src/debug/google-services.json` o.ae. (war die Leak-Ursache).
- API-Keys/Tokens/Passwoerter ins Repo committen (auch nicht in Kommentaren, Doku, Test-Fixtures).
- `.env`-Dateien im Projekt-Ordner liegen lassen nach Migration zu SK.
- SK-Ordner ins Repo einbinden (Submodul/Symlink/Tracking).
- SK-Pfad hardcoden — immer `$HOME/SK/` (plattformuebergreifend).
- Release-Keystore ohne externes Backup.
