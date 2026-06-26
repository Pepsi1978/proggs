Commit+Push VOR jedem Build

Jede Code-Aenderung wird ZUERST committet und gepusht — DANN gebaut. Niemals umgekehrt.

## Reihenfolge (IMMER)
1. Code-Edit fertig.
2. Version-Bump falls noetig (build.gradle.kts, package.json, Info.plist, ...).
3. `git add` nur eigene Dateien namentlich.
4. `git commit` (#NNN).
5. `git fetch origin && git rebase origin/main`.
6. `git push`.
7. Build (`./gradlew assembleDebug` / `bundleRelease` / `dotnet build` ...).
8. Install/Deploy.
9. Status-Meldung.

## Warum (parallele Sessions)
Frank baut oft mit 4-5 Sessions am selben Repo/Build. Wer baut BEVOR er committet, riskiert: andere Session pusht zwischenzeitlich -> Rebase-Konflikt -> Arbeit weg; oder ein AAB landet im Play Store, dessen Code-Stand NICHT im Repo ist (kein Debug/Rollback moeglich). Mit Commit+Push zuerst ist der Code sicher, selbst wenn Build/Install fehlschlaegt.

## Besonders kritisch bei AAB-Releases
Erst committen+pushen (add + commit + fetch+rebase + push), DANN `./gradlew bundleRelease`, ERST DANACH zur Play Console hochladen.

## NIEMALS
- Build mit uncommitteten Aenderungen ("ich teste erstmal lokal").
- Install-Befehl bevor Commit+Push durch ist.
- AAB hochladen ohne dass der Code-Stand im Repo ist.
- "Ich committe gleich nach dem Build" — Build kann fehlschlagen/Konflikt erzeugen, Code waere weg.

## Ausnahmen (eng)
Nur-lokale Wegwerf-Test-Edits (danach NICHT committen, nur verwerfen), reine Read-Only-Inspektion (lint ohne Aenderung), Build ohne Code-Aenderung (`gradlew tasks`). Sonst immer Commit+Push zuerst.
