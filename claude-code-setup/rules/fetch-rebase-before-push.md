# Git: Immer fetch + rebase VOR jedem Push (KRITISCH)

## Regel

Vor JEDEM `git push` MUESSEN diese Befehle in genau dieser Reihenfolge ausgefuehrt werden:

```bash
git fetch origin
git rebase origin/main
git push
```

## Bei unstaged Changes (z.B. Whiteboard-Aenderungen durch Hooks)

```bash
git stash
git fetch origin
git rebase origin/main
git stash pop
git push
```

## Warum

Mehrere CLIs (Claude Code, Codex, Gemini) und Worktree-Agents arbeiten am gleichen Repository.
Ohne vorheriges fetch+rebase schlaegt der Push fehl wenn ein anderer Prozess in der Zwischenzeit
gepusht hat. Das fuehrt zu wiederholten Fehlversuchen und Zeitverlust.

## Was NIEMALS passieren darf

- ❌ Direkt `git push` ohne vorheriges `git fetch origin && git rebase origin/main`
- ❌ `git pull` statt `git fetch + git rebase` (pull kann ungewollte Merge-Commits erzeugen)
- ❌ Push-Fehler ignorieren und einfach nochmal `git push` versuchen
- ❌ `git push --force` um Push-Rejections zu umgehen

## Gilt fuer

- Jeden Commit-Push-Zyklus
- Jeden Plattform (Windows + macOS)
- Jede Session — ausnahmslos
