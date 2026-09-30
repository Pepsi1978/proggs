---
name: repo-branch-und-sync-setup
description: "proggs Git-Setup — Branch main, privates Repo, Multi-Maschinen-Sync mit pre-push-Rebase- und auto-sync-Hooks"
metadata: 
  node_type: memory
  type: project
  originSessionId: 9fd40b29-2cd9-4d33-a8a8-8c92992cac2c
---

Das Monorepo `C:\Users\barwa\proggs` (origin: github.com/Pepsi1978/proggs) wird von **mehreren Maschinen/CLIs parallel** bearbeitet (Claude, Gemini, Codex, macOS — git-Autoren `Frank_Laptop`, `Pepsi1978`, `barwa` etc., alle Email `barwandt@gmail.com`).

- **Branch: `main`.** Bis 13.07.2026 hieß der lokale Branch historisch `master` (trackte aber `origin/main`); Pushes via `git push origin master` erzeugten einen divergierenden `origin/master`. Am 13.07.2026 bereinigt: lokal `master → main` umbenannt, `origin/main` fast-forwardet, `origin/master` gelöscht. Kein Skript setzt `master` aktiv — der Name war nur ein Reconnect-Überbleibsel.
- **Repo ist PUBLIC** (bewusst): am 13.07.2026 kurz auf privat gestellt, aber wieder zurück, weil private Repos im GitHub-Free-Plan Actions auf 2.000 Min/Monat (macOS ×10!) deckeln, GitHub Pages sperren und Rulesets verbieten — bei ~395 Pushes/Woche mit macOS+Windows-Builds untragbar. Trade-off: Konkurrenz kann mitlesen. Einziger Collaborator: `Pepsi1978`.
- **Ruleset `no-master-branch`** (id 18897413, active, seit 13.07.2026): GitHub lehnt das Anlegen von `refs/heads/master` zentral ab (GH013) — verhindert für ALLE Rechner, dass `origin/master` wieder entsteht. Funktioniert nur, solange das Repo public bleibt (Free-Plan).
- **Sync-Ökosystem** (in `claude-code-setup/` + `gemini-setup/`): `pre-push`-Hook macht auto fetch+rebase auf `origin/main` vor jedem Push; diverse SessionStart-Hooks (`auto-sync`, `whiteboard-safe-pull`, `self-improve-sync`, je `.ps1`+`.sh`) machen `git pull --rebase`. Installiert von `setup-windows.ps1`.
- **Konsequenz beim Arbeiten:** vor Arbeitsbeginn pullen, nach jeder Aufgabe pushen; NICHT mehr explizit `git push origin master` nutzen (schlichtes `git push` reicht, geht nach `origin/main`). Wegen der parallelen Maschinen bei Push-Ablehnung normal fetchen/rebasen — die Versionszeile in `*/app/build.gradle.kts` ist wegen [[version-bump-regel]] die häufigste Konfliktstelle.
