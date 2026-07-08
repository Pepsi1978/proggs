# Git-Workflow: Alle Regeln fuer Git-Operationen (KRITISCH)

## 1. Fetch + Rebase VOR jedem Push

VOR JEDEM `git push` IMMER `git fetch origin && git rebase origin/main`. Mehrere CLIs (Claude, Codex)
arbeiten am selben Repo — ohne fetch+rebase schlaegt der Push fehl. Bei unstaged Changes:
`git stash` → fetch+rebase → `git stash pop`.

## 2. git add: NUR Repo-Pfade

`git add` nur innerhalb `~/proggs/`, NIEMALS `~/.claude/`-Pfade. Verwechslungsgefahr: `~/proggs/.claude/`
liegt IM Repo (darf committed werden), `~/.claude/` ist Home (NICHT im Repo).

## 3. Rebase-Konflikt-Sicherheit

Bei `git rebase` sind `--ours`/`--theirs` INVERTIERT gegenueber `git merge`: bei rebase = `--ours`
Upstream (main), `--theirs` dein Branch. Konflikt-Dateien IMMER manuell lesen; NIE blind `checkout --ours/--theirs`.

## 4. Lint/Build VOR jedem Commit

Passenden Linter/Build LOKAL: C#/.NET `dotnet build` · TS `tsc --noEmit` · Rust `cargo clippy -- -D
warnings` · Go `go vet ./...` · Kotlin `./gradlew lint` · Python `python3 -m py_compile <file>`.

## 5. Secrets: NIEMALS im Repo (Poka-Yoke Stufe 3)

Secrets leben in `~/.claude/settings.json`/`.local.json`, `.env` (gitignored), OS-Env — NICHT im Repo.
Im Repo steht statt echtem Secret ein Verweis/`REDACTED`. Beim Kopieren von Settings ins Repo SOFORT
danach redaktieren (Python `re.sub` fuer `gho_`/`ghp_`/`sk-`/`AIza`-Muster), DANN `git add`. Vollstaendig:
`secrets-in-sk-folder.md`.

## 6. Edit + Version-Bump atomar

Pro Datei ALLE Aenderungen in einem Edit; mehrere Edits pro Datei sequentiell (Edit 1 → Read → Edit 2),
nicht alle Edit-1 parallel dann alle Edit-2 (Dateien sonst stale).

## 7. Keine untracked Projektdateien liegen lassen

Alles Projekt-Zugehoerige am Aufgabenende committen+pushen. `git status --short` — jeder Eintrag bewusst:
Projektdatei → namentlich `git add` + committen+push; Secret/lokale Config → `.gitignore`/redaktiertes
Template; Build-Artefakt/Cache → loeschen/`.gitignore`; unklar → Frank fragen. NIE `git add .`/`git add -A`
(parallele Sessions). Echte Secrets bleiben draussen — stattdessen sicheres Template ins Repo.
