---
name: commit-nur-mit-pfaden
description: "Parallele Sitzungen haben Dateien vorgemerkt: nur eigene Dateien committen; Teiländerungen über den Index, dann git commit ohne Pfade"
metadata:
  node_type: memory
  type: feedback
  originSessionId: b7db77d8-d4f1-438f-b516-b06f24346713
  modified: 2026-11-13T09:30:02.472Z
---

Im gemeinsamen Repo `~/proggs` laufen oft mehrere Sitzungen gleichzeitig. Nacktes `git commit -a` oder ein Commit nach fremdem `git add` nimmt deren vorgemerkte Dateien mit.

**Why:** Belegt 07.11.2026: `git commit -- <pfad>` committet die **ganze Arbeitskopie** der Datei, nicht den vorgemerkten Stand; so gingen 22 fremde Zeilen in `MEMORY.md` mit. Ursprüngliche Detaildatei fehlte (nie im Git, auf diesem Rechner nicht vorhanden), am 07.11.2026 aus Index-Zeile und nachprüfbaren Quellen neu angelegt.

**How to apply:**
- Datei ohne fremde Änderungen: `git commit -- <pfade>` ist richtig.
- Nie einen Ordner vormerken (`git add <ordner>`), nur einzelne Dateien: Am 11.11.2026 nahm `git add Jarvis/…/faehigkeit` die halbfertige `VictronFaehigkeit.kt` einer parallelen Sitzung mit.
- Datei mit fremden Änderungen: nur den eigenen Teil vormerken (`git hash-object -w` + `git update-index --cacheinfo`), `git diff --cached --name-only` prüfen, dann `git commit` **ohne** Pfade.
- Ersetzungsskript dafür: erst Arbeitskopie und HEAD-Fassung fertig berechnen und prüfen, dann schreiben. `open(p,'w').write(f(x))` leert die Datei schon vor `f` (13.11.2026: Assert in `f` ließ `Einstellungen.kt` leer zurück). Zeilenenden der Quelle übernehmen.

Verwandt: [[parallele-sitzung-nimmt-dateien-mit]].
