# Parallele Sessions — Commit & Push am geteilten main-Branch (KRITISCH)

> Frank arbeitet oft mit 4-5 gleichzeitig offenen Sessions am selben Repo `Pepsi1978/proggs`, alle pushen
> auf `main`. Diese Regel stellt sicher, dass trotzdem nichts ueberschrieben wird.

## Grundprinzip
IMMER auf `main` committen + nach `origin/main` pushen — keine Feature-Branches. Aber diszipliniert mit
`fetch + rebase + push` vor jedem Push.

## Einmalige Git-Config (beim Session-Start pruefen)
`git config --global pull.rebase true` · `rebase.autoStash true` · `rerere.enabled true`.

## Standard-Ablauf nach jeder Aufgabe
`git commit -m "#NNNN - Text" -- <datei…>` (atomarer Pfad-Commit, NUR eigene Dateien) → `git fetch origin
&& git rebase origin/main && git push`. Nur eigene Dateien namentlich — NIE `git add -A`/`.` (greift
fremde In-Flight-Dateien). Der atomare Pfad-Commit umgeht den geteilten `.git/index`. Nach jedem Commit
`git log --oneline -1` pruefen.

## Bei Push-Rejection
`fetch + rebase + push` wiederholen. 3. Rejection: 5-10 s warten, nochmal. Bleibt haengen: Frank melden.
**Nie** `--force`/`--force-with-lease`/`reset --hard`/neu klonen.

## Fremde Aenderungen im Working Tree
Uncommittete Dateien anderer Sessions NIE mitcommitten. Versehentlich gestaged → `git restore --staged
<datei>`. Blockiert fremdes unstaged den Rebase: `git stash push -u` → fetch+rebase+push → `git stash pop`.

## Pre-Push-Check (PFLICHT)
`git status --short` — jede Zeile bewusst: eigene → committen; fremde Session → ignorieren + in 1 Zeile
melden; lokaler Muell → ignorieren/`.gitignore`.

## Tabus (zerstoeren fremde Arbeit)
`push --force` · `--force-with-lease` · `reset --hard` (ohne Freigabe) · `add -A`/`.` · `commit --amend`
auf Gepushtes · interaktiver Rebase ueber Gepushtes. Ein Commit = ein Zweck (viele kleine > ein Sammel-Commit).
