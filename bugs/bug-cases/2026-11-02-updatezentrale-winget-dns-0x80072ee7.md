# UpdateZentrale: winget scheitert bei kurzem DNS-Aussetzer mit 0x80072EE7 (02.11.2026)

> Behoben in UpdateZentrale 1.3.71 und Updater-Zentrale macOS 1.0.43.

## Symptom

Im Sammellauf endete `winget upgrade --id Microsoft.DotNet.SDK.10` nach 6 Sekunden mit `InternetOpenUrl() failed.` und `0x80072ee7 : unknown error`. Die Karte zeigte nur „winget endete mit Code 0x80072EE7“. Im winget-Cache lag eine 0-Byte-Datei. Das Update davor (GitHub CLI, 10 Sekunden früher) und das danach liefen im selben Netz normal.

## Ursache

- `0x80072EE7` ist der WinINet-Fehler 12007 `ERROR_INTERNET_NAME_NOT_RESOLVED`: Der Name des Download-Servers (`download.microsoft.com`) ließ sich in diesem Moment nicht auflösen. Zwei Minuten später löste er wieder auf.
- Der Rechner hing in einem öffentlichen WLAN (Profil „Public WiFi“) mit dem DNS-Server des WLANs, die Netzprofile hatten sich zuvor mehrfach geändert. Einzelne DNS-Aussetzer sind dort normal.
- Der CLI- und der npm-Weg wiederholen bei Netzfehlern schon seit Längerem (`CliAktualisierer.IstNetzfehler`). Der winget-Weg hatte keine Wiederholung, ein einzelner Aussetzer wurde sofort zum roten Fehler und startete eine Claude-Sitzung.

## Behebung

- `WingetAktualisierer.IstNetzfehler`: erkennt die WinINet-HRESULTs `0x80072EE1` bis `0x80072F8F` sowie den Text `InternetOpenUrl() failed` und `InternetReadFile() failed`, aber nicht bei Abbruch, Zeitlimit oder unsauberem Prozessende.
- `AktualisierenAsync`: bis zu 3 Versuche mit 15 und 60 Sekunden Pause. Der fehlgeschlagene Download hatte noch nichts verändert, die Wiederholung ist also gefahrlos.
- Bleibt das Netz über alle Versuche gestört, beginnt die Meldung mit `Netz gestört: ` (`CliAktualisierer.NetzGestoertMeldung`). `ClaudeUebergabe.AutomatischRelevant` lässt diese Meldung aus, denn eine Sitzung kann das Netz nicht reparieren.
- macOS: dieselbe Fehlerart gibt es bei brew (`curl: (6) Could not resolve host`). `BrewAktualisierer.mitNetzWiederholung` umschließt `brew fetch` und `brew upgrade`/`brew install` mit denselben Versuchen, `fehlergrundText` und `ClaudeUebergabe.automatischRelevant` entsprechen Windows.

## Erkennen

Auf der Karte oder im Tagesprotokoll steht `0x80072EE7` oder ein anderer Code mit `0x80072E`/`0x80072F`, oder es steht `Netz gestört:` am Anfang der Meldung. Das ist ein Netzproblem des Rechners, kein Fehler im Paket oder im Update-Weg. Also das Update später erneut starten, nicht den Update-Weg ändern.

## Dateien

- `UpdateZentrale/Providers/WingetAktualisierer.cs`
- `UpdateZentrale/Providers/CliAktualisierer.cs`
- `UpdateZentrale/Services/ClaudeUebergabe.cs`
- `UpdateZentrale/Tests/ProzessendeTests.cs`
- `UpdaterZentrale-macOS/UpdaterZentrale/Providers/BrewAktualisierer.swift`
- `UpdaterZentrale-macOS/UpdaterZentrale/Services/ClaudeUebergabe.swift`

## Nebenfund: `HEAD` Monate hinter dem Arbeitsbaum

Am selben Abend stand `HEAD` von `~/proggs` noch auf dem Commit vom 30.06.2026 (3715 hinter `origin/main`), die Dateien im Arbeitsbaum entsprachen aber bis auf 6 Stück `origin/main`. Dadurch zeigte `git status` rund 3800 Änderungen, `git pull --ff-only` scheiterte, und TVO und CVO meldeten „git pull --ff-only ist fehlgeschlagen“.

`git pull --rebase --autostash` ist in diesem Zustand der falsche Weg, denn der Stash würde tausende Dateien zurücksetzen und wieder einspielen. Vorher prüfen, ob die Dateien wirklich `origin/main` entsprechen (temporärer Index: `GIT_INDEX_FILE=<datei> git read-tree origin/main`, dann `git update-index -q --refresh` und `git diff-files --name-status`). Passt das, zieht `git reset --mixed <geprüfter Commit>` nur `HEAD` und Index nach, ohne eine Datei anzufassen.
