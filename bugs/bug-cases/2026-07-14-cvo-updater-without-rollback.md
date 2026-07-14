# CVO: Updater löscht Backup vor dem Gesundheitscheck

## Symptom

Ein CVO-Update konnte eine neue EXE installieren und Erfolg suggerieren, obwohl der neue Prozess
nicht stabil startete. Das alte Binary war zu diesem Zeitpunkt bereits gelöscht; ein automatischer
Rollback war nicht mehr möglich.

## Ursache

`update.ps1` beendete Prozesse nur nach Namen, kopierte direkt auf den Zielpfad, löschte das
Backup vor dem Starttest und wertete einen fehlenden Prozess lediglich als Warnung. Der externe
Watcher wurde nicht pfadgenau angehalten und parallele Deployments waren nicht serialisiert.

## Fix

- In ein laufbezogenes Temp-Verzeichnis bauen und das Artefakt per SHA-256 prüfen.
- Nur Watcher und Overlay-Prozesse am kanonischen CVO-Pfad stoppen.
- Deployments mit `Local\ClaudeVoiceOverlay.Deployment` serialisieren.
- Alte EXE unter eindeutigem Namen behalten und die gestagte Datei per Rename einsetzen.
- Neue PID drei Sekunden lang prüfen; jeder Startfehler löst Rollback aus.
- Backup erst nach erfolgreichem Stabilitätstest entfernen.
- Watcher in `finally` wiederherstellen und bei jedem Fehlschlag Exit-Code 1 liefern.

## Verifikation

- PowerShell-AST-Parse und CVO-Build mit `TreatWarningsAsErrors=true` bestanden.
- `update.ps1` real ausgeführt: Build, Hash-Staging, Swap und Stabilitätstest erfolgreich.
- Danach zwei CVO-Prozesse und genau ein pfadgenauer Watcher aktiv; keine `.new`-, `.old-*`-
  oder Temp-Artefakte übrig.
- Finaler Deploy aus Commit `b1746e02b` als CVO `2.1.65` verifiziert.

## Referenz

- Fix-Commit: `b1746e02b #47914 - Make CVO updates atomic and recoverable`
- TVO-Vorbild: `0b1ab03be #47894 - Make TVO updates atomic and recoverable`
