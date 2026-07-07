Commit & Push nach JEDER Aufgabe — immer VOR Build/Install/Deploy

Egal welches KI-Modell OpenCode benutzt (auch schwache Modelle): Nach JEDER abgeschlossenen Aenderung an einer App oder einem Projekt — nach JEDER Aufgabe — wird SOFORT committet und gepusht, BEVOR irgendetwas gebaut, auf ein Geraet installiert oder deployt wird. So geht keine Arbeit verloren (Build-Absturz, parallele Session pusht zwischenzeitlich, Konflikt). Diese Regel gilt fuer jedes Modell gleich — keine Ausnahme "weil das Modell klein ist".

## Reihenfolge (IMMER, ausnahmslos)
1. Aufgabe / Code-Edit fertig.
2. Version-Bump falls noetig (build.gradle.kts versionName+versionCode, package.json, Info.plist ...).
3. `git add` — nur eigene Dateien namentlich (NIE `git add -A` / `git add .`, parallele Sessions).
4. `git commit -m "#NNN - Beschreibung"`.
5. `git fetch origin && git rebase origin/main`.
6. `git push`.
7. ERST DANACH: APK bauen (`./gradlew assembleDebug` / `bundleRelease`), auf Geraet installieren (`adb install`), App starten — bzw. Deploy.

## Mehrere Aufgaben (Semikolon-Trenner ` ; `)
Werden mehrere Aufgaben erkannt: nach JEDER einzelnen Aufgabe committen+pushen (eigener Rettungspunkt pro Aufgabe), NICHT am Ende gesammelt. Der gemeinsame Build / Install aufs Geraet / Deploy kommt EINMAL am Schluss — erst nachdem ALLE Aufgaben committet+gepusht sind.

## Warum
- Datenverlust verhindern: Build kann fehlschlagen, abstuerzen oder zu lange dauern; eine parallele Session kann zwischenzeitlich pushen -> Konflikt. Ist der Code schon im Repo, ist er sicher.
- Ein installierter / deployter Stand entspricht IMMER einem gepushten Commit -> reproduzierbar, Rollback moeglich, kein "Version auf dem Geraet, die es im Repo nicht gibt".

## NIEMALS
- Bauen, installieren oder deployen mit uncommitteten Aenderungen.
- "Ich committe gleich nach dem Build" — Build kann scheitern, Code waere weg.
- Mehrere Aufgaben sammeln und erst am Ende EINMAL committen.
- `git add -A` / `git add .` (zieht fremde Dateien paralleler Sessions mit).
- Nur committen, aber nicht pushen.

## Ausnahmen (eng)
Nur-lokale Wegwerf-Test-Edits (danach verwerfen, nicht committen), reine Read-Only-Inspektion (lint ohne Aenderung), Build ganz ohne Code-Aenderung (`gradlew tasks`). Sonst immer Commit+Push zuerst.

## Es gibt KEINE Ausnahme "nur auf ausdrueckliche Anweisung" (Vorfall 2026-07-01)
Es existiert KEINE Regel, die Commit+Push von einer gesonderten Erlaubnis pro Aufgabe abhaengig macht. Diese Aufgabe HIER ist die staendige, generelle Anweisung dafuer -- eine weitere Bestaetigung pro Einzelaufgabe wird NIE gebraucht und NIE eingeholt. Taucht der Gedanke auf "das braucht wohl erst eine Freigabe" -- das ist FALSCH, sofort verwerfen und stattdessen committen+pushen. Bug-Beleg: bugs/opencode/opencode-cli.md #48a.
