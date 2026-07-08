# Git-Workflow: Regeln fuer Git-Operationen (KRITISCH)

## 1. Fetch + Rebase VOR jedem Push
VOR JEDEM `git push` IMMER `git fetch origin && git rebase origin/main` (mehrere CLIs am selben Repo).
Bei unstaged Changes: `git stash` → fetch+rebase → `git stash pop`.

## 2. git add: NUR Repo-Pfade
`git add` nur in `~/proggs/`, NIEMALS `~/.claude/`-Pfade. `~/proggs/.claude/` liegt IM Repo, `~/.claude/` ist Home.

## 3. Rebase-Konflikt-Sicherheit
Bei `git rebase` sind `--ours`/`--theirs` INVERTIERT (rebase = `--ours` Upstream/main, `--theirs` dein
Branch). Konflikt-Dateien IMMER manuell lesen; NIE blind `checkout --ours/--theirs`.

## 4. Lint/Build VOR jedem Commit
Passenden Linter/Build lokal: C# `dotnet build` · TS `tsc --noEmit` · Rust `cargo clippy -- -D warnings`
· Go `go vet ./...` · Kotlin `./gradlew lint` · Python `python3 -m py_compile`.

## 5. Secrets: NIEMALS im Repo (Poka-Yoke Stufe 3)
Secrets in `~/.claude/settings.json`/`.local.json`, `.env` (gitignored), OS-Env — NICHT im Repo. Im Repo
Verweis/`REDACTED`. Beim Kopieren SOFORT redaktieren (`re.sub` fuer `gho_`/`ghp_`/`sk-`/`AIza`). Voll: `secrets-in-sk-folder.md`.

## 6. Edit + Version-Bump atomar
Pro Datei ALLE Aenderungen in einem Edit; mehrere Edits sequentiell (Edit 1 → Read → Edit 2).

## 7. Keine untracked Projektdateien liegen lassen
Alles Projekt-Zugehoerige am Aufgabenende committen+pushen. `git status --short` — jeder Eintrag bewusst.
NIE `git add .`/`-A` (parallele Sessions). Echte Secrets draussen, stattdessen sicheres Template ins Repo.
