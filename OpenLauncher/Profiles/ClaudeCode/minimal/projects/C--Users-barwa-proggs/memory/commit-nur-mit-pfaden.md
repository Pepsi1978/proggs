---
name: commit-nur-mit-pfaden
description: Parallele Sitzungen haben Dateien vorgemerkt: nur eigene Dateien committen; Teiländerungen über den Index, dann git commit ohne Pfade
metadata:
  type: feedback
---

Im gemeinsamen Repo `~/proggs` laufen oft mehrere Sitzungen gleichzeitig. Ein nacktes `git commit -a`
oder ein Commit nach fremdem `git add` nimmt deren vorgemerkte Dateien mit.

**Why:** Die ursprüngliche Detaildatei fehlte (nie im Git, auf diesem Rechner nicht vorhanden). Am 07.11.2026 aus der Index-Zeile und nachprüfbaren Quellen neu angelegt. Belegt am 07.11.2026: `git commit -- <pfad>` committet die **ganze Arbeitskopie**
der genannten Datei, nicht den vorgemerkten Stand. So gingen 22 fremde Zeilen in `MEMORY.md` mit.

**How to apply:**
- Datei ohne fremde Änderungen: `git commit -- <pfade>` ist richtig.
- Datei mit fremden Änderungen: nur den eigenen Teil vormerken (`git hash-object -w` +
  `git update-index --cacheinfo`), `git diff --cached --name-only` prüfen, dann `git commit` **ohne** Pfade.
Verwandt: [[parallele-sitzung-nimmt-dateien-mit]].
