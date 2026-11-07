---
name: commit-nur-mit-pfaden
description: "Parallele Sitzungen haben Dateien vorgemerkt: nur eigene Dateien committen; Teiländerungen über den Index, dann git commit ohne Pfade"
metadata:
  node_type: memory
  type: feedback
  originSessionId: b7db77d8-d4f1-438f-b516-b06f24346713
  modified: 2026-11-07T13:55:37.143Z
---

Im gemeinsamen Repo `~/proggs` laufen oft mehrere Sitzungen gleichzeitig. Nacktes `git commit -a` oder ein Commit nach fremdem `git add` nimmt deren vorgemerkte Dateien mit.

**Why:** Belegt 07.11.2026: `git commit -- <pfad>` committet die **ganze Arbeitskopie** der Datei, nicht den vorgemerkten Stand; so gingen 22 fremde Zeilen in `MEMORY.md` mit. Ursprüngliche Detaildatei fehlte (nie im Git, auf diesem Rechner nicht vorhanden), am 07.11.2026 aus Index-Zeile und nachprüfbaren Quellen neu angelegt.

**How to apply:**
- Datei ohne fremde Änderungen: `git commit -- <pfade>` ist richtig.
- Datei mit fremden Änderungen: nur den eigenen Teil vormerken (`git hash-object -w` + `git update-index --cacheinfo`), `git diff --cached --name-only` prüfen, dann `git commit` **ohne** Pfade.

Verwandt: [[parallele-sitzung-nimmt-dateien-mit]].
