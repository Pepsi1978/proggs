# Parallele Sessions -- Commit & Push am geteilten main-Branch (KRITISCH)

## Grundprinzip
IMMER auf `main` committen + nach `origin/main` pushen -- keine Feature-Branches. `fetch + rebase + push`
vor jedem Push. (4-5 parallele Sessions/Repo.)

## Einmalige Git-Config (Session-Start pruefen)
`git config --global pull.rebase true`; `rebase.autoStash true`; `rerere.enabled true`.

## Standard-Ablauf nach jeder Aufgabe
`git commit -m "#NNNN - Text" -- <datei...>` (atomarer Pfad-Commit, NUR eigene Dateien) -> `git fetch
origin && git rebase origin/main && git push`. NIE `git add -A`/`.` (greift fremde In-Flight-Dateien).
Nach jedem Commit `git log --oneline -1`.

## Bei Push-Rejection
`fetch + rebase + push` wiederholen; 3. Rejection: 5-10 s warten, nochmal; haengt: Frank melden.
**Nie** `--force`/`reset --hard`/neu klonen.

## Fremde Aenderungen im Working Tree
Fremde uncommittete Dateien NIE mitcommitten. Versehentlich gestaged -> `git restore --staged <datei>`.
Blockiert fremdes unstaged den Rebase: `git stash push -u` -> fetch+rebase+push -> `git stash pop`.

## Pre-Push-Check (PFLICHT)
`git status --short` -- jede Zeile bewusst: eigene -> committen; fremde Session -> ignorieren; lokaler
Muell -> `.gitignore`.

## Tabus (zerstoeren fremde Arbeit)
`push --force`; `--force-with-lease`; `reset --hard` (ohne Freigabe); `add -A`/`.`; `commit --amend` auf
Gepushtes; interaktiver Rebase ueber Gepushtes. Ein Commit = ein Zweck.
