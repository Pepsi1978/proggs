Git-Workflow: Alle Regeln fuer Git-Operationen

## 1. Fetch + Rebase VOR jedem Push
`git fetch origin && git rebase origin/main`, dann push. Sonst schlaegt der Push fehl, wenn ein anderer Prozess schon gepusht hat. Bei unstaged Changes: `git stash` -> fetch+rebase -> `git stash pop`.

## 2. git add: NUR Repo-Pfade
Nur Pfade in `~/proggs/`, NIEMALS `~/.claude/`. Achtung: `~/proggs/.claude/` liegt IM Repo (darf committed werden), `~/.claude/` ist das Home-Verzeichnis (NICHT im Repo).

## 3. Rebase-Konflikt-Sicherheit
Bei `git rebase` sind `--ours`/`--theirs` INVERTIERT gegenueber `git merge`: bei rebase ist `--ours` = Upstream (main), `--theirs` = dein Branch. Konflikt-Dateien IMMER manuell lesen, nie blind `checkout --ours/--theirs`.

## 4. Lint/Build VOR jedem Commit
Lokal ausfuehren: C#/.NET `dotnet build`, TypeScript `tsc --noEmit`, Rust `cargo clippy -- -D warnings`, Go `go vet ./...`, Kotlin/Android `./gradlew lint`, Python `python3 -m py_compile <file>`.

## 5. Secrets NIEMALS im Repo
Secrets leben in `~/.claude/settings.json`, `settings.local.json`, `.env` (gitignored), Umgebungsvariablen — NIE im Repo. Beim Kopieren von Settings ins Repo: SOFORT redaktieren (Token-Muster wie `gho_/ghp_/sk-/AIza...` durch "REDACTED" ersetzen) BEVOR staged/committed wird.

## 6. Edit + Version-Bump atomar
Edit und Version-Bump als zusammengehoerigen Schritt planen. Mehrere Edits pro Datei sequentiell (Edit 1 -> Read -> Edit 2), nicht alle parallel (sonst werden Dateien "stale").

## 7. Keine untracked Projektdateien liegen lassen
Am Ende jeder Aufgabe `git status --short`. Jeden Eintrag einordnen: Projektdatei -> namentlich committen+pushen; Secret/lokale Config -> gitignore oder redaktiertes Template; Build-Artefakt/Cache -> loeschen oder gitignore. Nie `git add .`/`git add -A` (zieht fremde Dateien paralleler Sessions mit).
