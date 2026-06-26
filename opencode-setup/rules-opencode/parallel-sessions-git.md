Parallele Sessions — Commit & Push am geteilten main-Branch

Frank arbeitet oft mit 4-5 gleichzeitig offenen Sessions am selben Repo `Pepsi1978/proggs`. Alle pushen auf `main`. Es wird IMMER auf `main` committet und nach `origin/main` gepusht — keine Feature-Branches, keine Worktrees, aber diszipliniert mit fetch+rebase+push.

## Einmalige Git-Config (pro Rechner)
- `git config --global pull.rebase true`
- `git config --global rebase.autoStash true`
- `git config --global rerere.enabled true`

## Standard-Ablauf nach jeder Aufgabe
1. Nur EIGENE Dateien namentlich — NIE `git add -A` / `git add .`.
2. Atomarer Pfad-Commit (umgeht den geteilten Index): `git commit -m "#NNNN - Beschreibung" -- pfad1 pfad2`
3. `git fetch origin && git rebase origin/main`
4. `git push`

Nach dem Commit kurz `git log --oneline -1` pruefen (Hintergrund-Commits koennen Duplikate erzeugen).

## Bei Push-Rejection
`git fetch origin && git rebase origin/main && git push` einfach wiederholen. Bei der 3. Rejection 5-10 Sekunden warten, dann erneut. Hilft das nicht: Frank melden, nicht stumm weiterprobieren.

## Fremde Aenderungen im Working Tree
Gehoeren anderen Sessions -> NIEMALS mitcommitten. Bei Rebase-Blockade ("cannot rebase: unstaged changes"): `git stash push -u -m hold` -> fetch+rebase+push -> `git stash pop`.

## Pre-Push-Check (PFLICHT)
Vor jedem `git push`: `git status --short`. Jede Zeile bewusst zuordnen — eigene Datei (committen), fremde Session (ignorieren + dem Benutzer 1 Zeile melden), Muell (ignorieren/gitignore). Eigene Dateien nie unstaged liegen lassen.

## Absolute Tabus (zerstoeren fremde Arbeit)
`git push --force`, `--force-with-lease`, `git reset --hard` (ohne Freigabe), `git add -A`/`.`, `git commit --amend` auf bereits Gepushtes, `git rebase -i` ueber Gepushtes.

## Commit-Granularitaet
Ein Commit = ein abgeschlossener Zweck. Lieber 5 kleine Commits als 1 grosser (jeder ist ein Rettungspunkt). Keine Sammel-Commits mit mehreren unabhaengigen Aufgaben.
