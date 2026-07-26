# Commit+Push VOR jedem Build (KRITISCH — parallele Sessions)

> Gilt fuer ALLE Build-Tools (Android Gradle, iOS Xcode, .NET, Rust cargo, Bun/Node).

## Grundprinzip

Jede Code-Aenderung ZUERST committen und pushen, DANN bauen. Reihenfolge IMMER:
1. Code-Edit fertig 2. Version-Bump falls noetig 3. `git add` (nur eigene Dateien namentlich)
4. `git commit` (fortlaufende Nummer) 5. `git fetch origin && git rebase origin/main` 6. `git push`
7. Build (`./gradlew assembleDebug`/`bundleRelease`, `dotnet build`) 8. Install/Deploy 9. Status-Meldung.

## Warum

Bei 4-5 parallelen Sessions: baut eine VOR dem Commit und pusht B zwischenzeitlich → Rebase-Konflikt,
evtl. beide Arbeiten verloren; AAB passt nicht zum gepushten Code. Commit+Push VOR Build → konfliktfrei;
bei Build-Fehler Code sicher im Repo.

## Besonders kritisch bei AAB-Builds

Build 1-3 Min, oft direkt zur Play Console. Pflicht: Commit+Push ZUERST
(`git add …; git commit; fetch+rebase; git push`), DANN `./gradlew bundleRelease`, DANN Upload.

## Ausnahmen (eng)

Nur lokale Test-Edits die gleich verworfen werden · reine Read-Only-Inspektion (lint ohne Aenderung) ·
Build ohne Code-Bezug (`gradlew tasks`). Sonst IMMER Commit+Push zuerst.

## Was NIEMALS passieren darf
- Build mit uncommitteten Aenderungen · Install vor Commit+Push · AAB hochladen ohne dass der Code im Repo ist
- "Ich committe gleich nach dem Build" (Build kann fehlschlagen/haengen/Konflikt) · Reihenfolge umkehren "spart Zeit"
