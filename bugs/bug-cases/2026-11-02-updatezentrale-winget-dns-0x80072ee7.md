# UpdateZentrale: winget scheitert bei kurzem DNS-Aussetzer mit 0x80072EE7 (02.11.2026)

> Behoben in UpdateZentrale 1.3.73 (Wiederholung ab 1.3.71, curl-Download in den winget-Cache ab 1.3.73) und Updater-Zentrale macOS 1.0.43 (nur Wiederholung).
> Der curl-Weg ist am echten Fall bestätigt: `.NET SDK 10.0.403` installierte am 02.11.2026 aus dem so gefüllten Cache („Existing installer file hash matches“).

## Symptom

Im Sammellauf endete `winget upgrade --id Microsoft.DotNet.SDK.10` nach 6 Sekunden mit `InternetOpenUrl() failed.` und `0x80072ee7 : unknown error`. Die Karte zeigte nur „winget endete mit Code 0x80072EE7“. Im winget-Cache lag eine 0-Byte-Datei. Ein zweiter Versuch 8 Minuten später scheiterte genauso, die Updates von github.com liefen im selben Netz normal.

## Ursache

- `0x80072EE7` ist der WinINet-Fehler 12007 `ERROR_INTERNET_NAME_NOT_RESOLVED`.
- Das winget-Diagnoseprotokoll (`…\Microsoft.DesktopAppInstaller_8wekyb3d8bbwe\LocalState\DiagOutputDir`) zeigt: Delivery Optimization und WinINet melden beide sofort diesen Fehler, winget wiederholt selbst nur nach einer halben Sekunde.
- `curl` lud dieselbe Adresse im selben Moment fehlerfrei, `Resolve-DnsName` löste den Namen auf. Es ist also kein Serverausfall und kein allgemeiner DNS-Ausfall, sondern betrifft die Windows-eigenen Download-Wege.
- Im öffentlichen WLAN (Profil „Public WiFi“, DNS-Server 10.36.16.1) lieferte ein direkter WinINet-Test für `www.microsoft.com` ebenfalls 12007, für `download.microsoft.com` kurz danach aber Erfolg. Der Fehler hängt also von Name und Zeitpunkt ab. Warum WinINet und Delivery Optimization hier scheitern und curl nicht, ist nicht geklärt.
- Der winget-Weg hatte weder eine Wiederholung noch einen anderen Download-Weg.

## Behebung

- `WingetAktualisierer.IstNetzfehler`: erkennt die WinINet-HRESULTs `0x80072EE1` bis `0x80072F8F` sowie den Text `InternetOpenUrl() failed` und `InternetReadFile() failed`, aber nicht bei Abbruch, Zeitlimit oder unsauberem Prozessende.
- `AktualisierenAsync`: bis zu 3 Versuche mit 15 und 60 Sekunden Pause. Der fehlgeschlagene Download hatte noch nichts verändert, die Wiederholung ist also gefahrlos.
- `InstallerInDenCacheLadenAsync`: Vor jeder Wiederholung lädt curl den Installer auf den Cache-Pfad von winget, `%TEMP%\WinGet\<Id>.<Version>\<sha256>`. Adresse und Hash stammen aus dem gescheiterten Lauf (Ausgabe und Dateiname). winget meldet dann `Found existing installer file … Verifying file hash`, prüft gegen das Manifest und installiert. Eine falsche Datei löscht winget, statt sie auszuführen. Das korrigiert die ältere Angabe im Fall vom 28.10.: `winget upgrade` nutzt zwar keinen `winget download`-Ordner, aber diesen Cache-Pfad.
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
