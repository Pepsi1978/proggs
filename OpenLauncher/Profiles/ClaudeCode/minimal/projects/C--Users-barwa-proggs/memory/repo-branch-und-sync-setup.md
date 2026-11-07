---
name: repo-branch-und-sync-setup
description: "proggs Git-Setup — Branch main, öffentliches Repo, Multi-Maschinen-Sync mit pre-push-Rebase- und auto-sync-Hooks"
metadata:
  node_type: memory
  type: project
  originSessionId: 9fd40b29-2cd9-4d33-a8a8-8c92992cac2c
  modified: 2026-11-07T13:54:08.854Z
---

Monorepo `C:\Users\barwa\proggs` (origin: github.com/Pepsi1978/proggs) wird von **mehreren Maschinen/CLIs parallel** bearbeitet (Claude, Gemini, Codex, macOS; git-Autoren `Frank_Laptop`, `Pepsi1978`, `barwa` etc., alle Email `barwandt@gmail.com`).

- **Branch `main`.** Bis 13.07.2026 hieß der lokale Branch historisch `master` (trackte `origin/main`); `git push origin master` erzeugte einen divergierenden `origin/master`. 13.07.2026 bereinigt: lokal `master → main` umbenannt, `origin/main` fast-forwardet, `origin/master` gelöscht. Kein Skript setzt `master` aktiv (nur Reconnect-Überbleibsel).
- **Repo ist PUBLIC** (bewusst): 13.07.2026 kurz privat, wieder zurück, weil private Repos im GitHub-Free-Plan Actions auf 2.000 Min/Monat (macOS ×10!) deckeln, GitHub Pages sperren und Rulesets verbieten; bei ~395 Pushes/Woche mit macOS+Windows-Builds untragbar. Trade-off: Konkurrenz kann mitlesen. Einziger Collaborator: `Pepsi1978`.
- **Ruleset `no-master-branch`** (id 18897413, active, seit 13.07.2026): GitHub lehnt `refs/heads/master` zentral ab (GH013), verhindert für ALLE Rechner ein neues `origin/master`. Funktioniert nur, solange das Repo public bleibt (Free-Plan).
- **Sync** (in `claude-code-setup/` + `gemini-setup/`, installiert von `setup-windows.ps1`): `pre-push`-Hook macht auto fetch+rebase auf `origin/main` vor jedem Push; SessionStart-Hooks (`auto-sync`, `whiteboard-safe-pull`, `self-improve-sync`, je `.ps1`+`.sh`) machen `git pull --rebase`.
- **Beim Arbeiten:** vor Arbeitsbeginn pullen, nach jeder Aufgabe pushen; nie mehr `git push origin master` (schlichtes `git push` geht nach `origin/main`). Bei Push-Ablehnung normal fetchen/rebasen; häufigste Konfliktstelle ist die Versionszeile in `*/app/build.gradle.kts` wegen [[version-bump-regel]].
