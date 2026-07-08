# Commit+Push VOR jedem Build (KRITISCH — parallele Sessions)

> Gilt fuer ALLE Build-Tools (Android Gradle, iOS Xcode, .NET, Rust cargo, Bun/Node).

## Grundprinzip

Jede Code-Aenderung wird ZUERST committet und gepusht — DANN gebaut. Reihenfolge IMMER:

1. Code-Edit fertig 2. Version-Bump falls noetig 3. `git add` (nur eigene Dateien namentlich)
4. `git commit` (fortlaufende Nummer) 5. `git fetch origin && git rebase origin/main` 6. `git push`
7. Build (`./gradlew assembleDebug`/`bundleRelease`, `dotnet build`) 8. Install/Deploy 9. Status-Meldung.

## Warum (parallele Sessions)

Frank arbeitet oft mit 4-5 gleichzeitig offenen Sessions am selben Repo, teils am selben AAB-Workflow.
Baut eine Session VOR dem Commit (5 Min Build+Install), pusht Session B zwischenzeitlich → Rebase-Konflikt,
im Worst Case gehen beide Arbeiten verloren; ein hochgeladener AAB stimmt nicht mit dem gepushten Code
ueberein → nicht reproduzierbar/debuggbar. Mit Commit+Push VOR Build: beide Sessions bauen konfliktfrei;
schlaegt Build/Install fehl, ist der Code sicher im Repo.

## Besonders kritisch bei AAB-Builds

Build 1-3 Min (langes Konfliktfenster), oft direkt zur Play Console. Pflicht: Commit+Push ZUERST
(`git add …; git commit; fetch+rebase; git push`), DANN `./gradlew bundleRelease`, DANN Upload.

## Ausnahmen (eng)

Nur lokale Test-Edits die gleich verworfen werden · reine Read-Only-Inspektion (lint ohne Aenderung) ·
Build ohne Code-Bezug (`gradlew tasks`). In ALLEN anderen Faellen: Commit+Push zuerst.

## Was NIEMALS passieren darf

- Build mit uncommitteten Aenderungen · Install vor Commit+Push · AAB hochladen ohne dass der Code im Repo ist
- "Ich committe gleich nach dem Build" (Build kann fehlschlagen/haengen/Konflikt) · Reihenfolge umkehren "spart Zeit"
