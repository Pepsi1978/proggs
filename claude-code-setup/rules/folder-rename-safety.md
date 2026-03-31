# Ordner-Umbenennung: Sichere Reihenfolge (KRITISCH)

## Regel: VOR dem Umbenennen IMMER externe Prozesse beenden

Wenn ein Projektordner umbenannt wird der von externen Prozessen genutzt wird
(Android-Projekte, Node-Projekte, Rust-Projekte, etc.), MUSS vorher ALLES gestoppt werden.

## Pflicht-Ablauf (in exakt dieser Reihenfolge)

```
1. ./gradlew --stop          # Gradle-Daemon beenden (haelt .kotlin/, build/ offen)
2. adb kill-server            # ADB beenden (haelt Dateien im Projektordner offen!)
3. [Andere Daemons stoppen]   # z.B. cargo, node, tsc --watch, etc.
4. cmd.exe //c "ren Alt Neu"  # Windows-natives Rename (NICHT git mv — das schlaegt fehl!)
5. .gitattributes anpassen    # SOFORT alle LFS-Regeln auf neuen Pfad aendern
6. git lfs status             # Pruefen: alle LFS-Dateien muessen "LFS → LFS" zeigen
7. Falls "Git:" statt "LFS:": git rm --cached + git add (erzwingt LFS-Filter)
8. git add -A && commit && push
```

## Warum NICHT `git mv` auf Windows

`git mv` macht intern `mv` (POSIX) → das schlaegt auf Windows fehl wenn IRGENDEIN
Prozess eine Datei im Ordner offen hat ("Permission denied" / "Device or resource busy").
`cmd.exe //c "ren"` verwendet die Windows-API die robuster mit File-Handles umgeht.

## Warum .gitattributes SOFORT anpassen

Git-LFS-Regeln in `.gitattributes` sind pfadabhaengig. Wenn der Pfad sich aendert aber
die Regel nicht, werden grosse Dateien als regulaere Git-Objekte behandelt statt als
LFS-Pointer. GitHub rejected dann den Push wegen >100MB File-Size-Limit.

## Was NIEMALS passieren darf

- ❌ `git mv` ohne vorher ADB + Gradle zu stoppen
- ❌ Ordner umbenennen ohne .gitattributes LFS-Pfade anzupassen
- ❌ Mehrere Retry-Versuche bei "Permission denied" ohne Root Cause zu suchen
- ❌ Push ohne `git lfs status` zu pruefen nach Pfadaenderungen

## Warum

Am 2026-03-30 hat eine Ordner-Umbenennung (EntropyJournal → BestJournalFrank)
6+ Minuten Fehlersuche verursacht:
- 4x "Permission denied" Retries statt sofort ADB zu stoppen
- Push-Rejection wegen 124MB .onnx-Datei (LFS-Regel nicht aktualisiert)
- Soft-Reset + Re-Stage + Re-Commit noetig
