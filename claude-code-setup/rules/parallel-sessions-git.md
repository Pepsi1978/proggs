# Parallele Sessions — Commit & Push am geteilten main-Branch (KRITISCH)

> Gilt in JEDER Session, auf allen Plattformen. Frank arbeitet oft mit 4-5 gleichzeitig offenen
> Claude-Code-Sessions am selben Repo (`Pepsi1978/proggs`), alle pushen auf `main`. Diese Regel
> stellt sicher, dass trotzdem nichts ueberschrieben wird.

---

## Grundprinzip

IMMER auf `main` committen und nach `origin/main` pushen — keine Feature-Branches, kein Merge-Dance.
Aber diszipliniert mit `fetch + rebase + push` vor jedem Push.

## Einmalige Git-Config (MUSS gesetzt sein — beim Session-Start pruefen)

```bash
git config --global pull.rebase true        # Pull nutzt immer rebase
git config --global rebase.autoStash true   # lokale Aenderungen automatisch stashen/poppen
git config --global rerere.enabled true     # geloeste Konflikte merken
```

## Standard-Ablauf nach jeder Aufgabe

```bash
git commit -m "#NNNN - Beschreibung" -- <datei1> <datei2>   # atomarer Pfad-Commit, NUR eigene Dateien
git fetch origin && git rebase origin/main && git push
```

- **Nur eigene Dateien namentlich** — NIE `git add -A`/`git add .` (greift fremde In-Flight-Dateien).
- **Atomarer Pfad-Commit** (`git commit -- <pfade>`): umgeht den geteilten `.git/index`. Fuehrt eine
  andere Session zwischen deinem `add` und `commit` ein `add`/`reset` aus, kann sie deinen Index leeren.
  Pfade direkt an `commit` uebergeben committet genau diese Dateien, egal was am Index passiert.
- Nach jedem Commit kurz `git log --oneline -1` pruefen (Hintergrund-Commit-Duplikat-Falle).

## Bei Push-Rejection

`git fetch origin && git rebase origin/main && git push` einfach wiederholen. Bei 3. Rejection:
5-10 s warten, nochmal. Bleibt es haengen: Frank melden. **Nie** `--force`/`--force-with-lease`/`reset --hard`/neu klonen.

## Fremde Aenderungen im Working Tree

Uncommittete Dateien anderer Sessions NIE mitcommitten. Nur eigene namentlich stagen; versehentlich
gestaged → `git restore --staged <datei>`. Blockiert fremdes unstaged den Rebase:
`git stash push -u -m hold` → fetch+rebase+push → `git stash pop`.

## Pre-Push-Check (PFLICHT vor jedem Push)

`git status --short` — jede Zeile bewusst zuordnen: eigene Aufgabe → committen; fremde Session →
ignorieren + in 1 Zeile melden ("X Dateien unstaged, gehoeren nicht zu dieser Aufgabe"); lokaler
Muell → ignorieren/`.gitignore`. Leere Ausgabe → sauber.

## Tabus (zerstoeren fremde Arbeit)

`push --force` · `--force-with-lease` · `reset --hard` (ohne Freigabe) · `add -A`/`add .` ·
`commit --amend` auf Gepushtes · interaktiver Rebase ueber Gepushtes.

## Commit-Granularitaet

Ein Commit = ein Zweck. Lieber viele kleine (jeder ist ein Rettungspunkt + weniger Konflikt) als
ein grosser Sammel-Commit.
