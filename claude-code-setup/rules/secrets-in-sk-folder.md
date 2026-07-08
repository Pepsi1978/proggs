# Secrets zentral im SK-Ordner (KRITISCH — Poka-Yoke Stufe 3)

> Ausloeser: GitHub Secret Scanning Alert — Firebase-Debug-Key landete im Repo wegen einer
> `.gitignore`-Ausnahme `!app/src/debug/google-services.json` + grobem `git add`.

## Grundprinzip

Alle API-Keys, Signing-Keys, Tokens, Zugangsdaten leben AUSSERHALB aller Repos, zentral pro Rechner:

| Plattform | Pfad |
|-----------|------|
| Windows | `C:\Users\barwa\SK\` |
| macOS | `/Users/barwa/SK/` |
| Variable | `$HOME/SK/` |

Kein Projekt im Repo enthaelt eigene Keys. Alle lesen aus `$HOME/SK/<projekt-name>/`.

## Warum (Poka-Yoke Stufe 3 — Eliminierung)

Liegt ein Secret gar nicht im Projekt-Ordner, kann es konzeptionell nie in einen Commit geraten —
egal welcher Agent/Befehl/Fehler. Ein nicht existierendes File kann nicht committed werden. Staerker
als `.gitignore` (Stufe 1) oder Pre-Commit-Hooks (Stufe 2).

## Struktur

```
$HOME/SK/
├── README.md                         Doku + Backup-Strategie
├── <projekt>/
│   ├── google-services-*.json        Firebase-Configs
│   ├── release.keystore              Signing (KRITISCH — unwiederbringbar)
│   └── keystore.properties           Passwoerter
```

## Wie Projekte SK verwenden (kurz)

- **Android (Gradle):** `syncSecretsFromSk`-Task vor `preBuild`, kopiert aus
  `File(System.getProperty("user.home")).resolve("SK").resolve("<projekt>")`; fehlt SK → klarer GradleException.
- **C#/.NET, Swift, TS/Node:** `.env` an mehreren Pfaden suchen, **SK zuerst** (`userProfile/SK/<projekt>/.env`).

## Pflicht fuer neue Projekte

Unterordner `$HOME/SK/<projekt>/` anlegen · Key dort ablegen · Code liest daraus · `.gitignore`
listet alle Key-Dateinamen **ohne** Ausnahme-Regeln (`!debug/...`) · Template/README ins Repo
(`google-services.json.example`, `ENV.SETUP.md`).

## Secrets im Chat (Frank gibt einen Key direkt)

Sofort: (1) in `$HOME/SK/<projekt>/.env` ablegen, (2) ins Ziel-System (VPS-`.env`), NIE in Repo/Code,
(3) aus allen Repo-Dateien redaktieren (`[REDACTED-...]`). Der Ledger-Hook
`task-ledger-helper.py` (`_redact_secrets`) maskiert bekannte Muster (`tvly-`, `sk-`, `gh[pousr]_`,
`github_pat_`, `AIza`, `glpat-`, `xox[baprs]-`, `fc-`, `nvapi-`, `gsk_`, `r8_`) automatisch. Neue
Formate in `_SECRET_PATTERNS` ergaenzen.

## Backup-Pflicht

`$HOME/SK/` ist NICHT in Git/Cloud-Sync. Manuell woechentlich auf externe Platte / in
verschluesselten Vault. **Release-Keystores** besonders — ihr Verlust = App nie wieder im Play Store aktualisierbar.

## Was NIEMALS passieren darf

- `.gitignore`-Ausnahme `!.../google-services.json` o.ae. (Root Cause des Leaks)
- Keys/Tokens/Passwoerter ins Repo (auch nicht Kommentare/Doku/Test-Fixtures)
- `.env` im Projekt-Ordner nach SK-Migration liegen lassen (kann per `git add -A` zurueck)
- SK-Ordner ins Repo einbinden · SK-Pfad hardcoden (immer `$HOME/SK/`)
