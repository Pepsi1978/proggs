# Prüfrunden je Rechner (Bedarfsdatei, nur bei einer Prüfrunde lesen)

## macOS, 19.11.2026 17:50 (Commits 456da5d32, fd68c55a6)

Umgebung: macOS 27.2.1, Bash-Werkzeug läuft in **zsh** (`zsh -c 'source …snapshot… && eval …'`), Python 3.15.2
(Homebrew, `python3`) und 3.9.6 (`/usr/bin/python3`), `/bin/bash` 3.2, Homebrew-bash 5.3, `/usr/bin/timeout` vorhanden.

Mit echten Eingaben geprüft, beide Python-Fassungen und bash 3.2:
- Deploy-Wächter: Die Mac-App heißt `OpenLauncher.app` (Prozess `OpenLauncher`, Bundle `de.frank.OpenLauncher`), nicht
  OpenLauncherMac. Geblockt: killall/pkill, osascript quit, open …app/-a/-b (auch -na), Binärdatei direkt. Frei: build.sh,
  dotnet build zur Probe, open -R, Lesen, Commit-Texte, Heredoc-Daten, OpenLauncherServer.
- Bau-Wächter: Bump-, Kill-, Warte- und adb-Teil an einem Test-Repo; Heredoc-Daten und Text in Anführungszeichen frei,
  adb-Pfad in Anführungszeichen geprüft (auch PowerShell `& "…adb.exe"`). Alias-Umschreibung (M35) entfernt.
- Lauf-Wächter mit echten Hintergrundläufen: WARTET (osascript, Overlay-Wartezeile), HÄNGER? (stiller Prozess),
  LÄUFT (kurze stille Schritte mit 25 s CPU, früher HÄNGER?), adb push zum Tablet. zsh-Hülle zählt nicht mehr mit.
- Versionslog-Eintrag (BOM, Umlaute, Konfigurations-Cache), analyse.py über alle Mac-Verläufe (0,7 s).
- Mac-Maßnahmen: M31 und M35 ohne Treffer; M32 Ursache behoben (alle 28 gradlew 100755); M36 wirkt (nonomatch im
  Schnappschuss, jetzt messbar); M40 nur der Ursprungsfall.

Begründet offen:
- eintrag.py schreibt nicht atomar. Das Fenster ist winzig, und `os.replace` scheitert unter Windows, wenn die Datei offen ist.
- `update-launcher.sh; open -a OpenLauncher` bleibt frei: Ein echter Skriptaufruf gibt den ganzen Befehl frei, unter Windows genauso.
- `sleep` in einer Warteschleife heißt WARTET: Regel 24 definiert das so, und endlose Prozess-Warteschleifen deckt M34 ab.

## Hostinger-Server, 19.11.2026 17:57 (Jarvis-Auftrag X9VQZ)

Umgebung: Ubuntu 24.04, Bash-Werkzeug in **bash** (`/bin/bash -c 'source …snapshot… && eval …'`), Python 3.12.3, procps 4.0.4,
lsof und fuser, TZ=Europe/Berlin (auch ohne TZ-Variable Berliner Zeit), adb unter `~/android-sdk/platform-tools` (PATH und
ANDROID_HOME aus `~/.profile`), Handy 10.8.0.7 und Tablet 10.8.0.8 gleichzeitig im Tunnel.

Die fünf offenen Punkte vom Mac:
1. M26 überholt: Eine 80-s-Schleife und ein echter Gradle-Bau (UpdateStation, `--rerun-tasks`) im Vordergrund wurden nach
   60 s in den Hintergrund verschoben, nicht abgebrochen. Sperre entfernt; minute.py liest die Ausgabedatei (Schreiber per lsof).
2. Vorwärm-Sperre: Die flock-Umschreibung greift jetzt bei jedem Befehl mit echtem Bau (auch `eintrag.py … && gradlew`,
   `git pull && gradlew`), nicht bei gradlew nur als Text (Anführungszeichen, Heredoc, `python3 -c`). Das Warten hinter
   `flock -w 3600 9` meldete der Lauf-Wächter als HÄNGER?; jetzt WARTET (M43).
3. Deploy-Wächter: Auf dem Server läuft kein OpenLauncher, `dotnet build -p:EnableWindowsTargeting=true` ist dort die erlaubte
   WPF-Prüfung und wurde mit Windows-Pfad als Hinweis geblockt. Unter Linux jetzt sofort `exit 0`.
4. analyse.py: 26 Server-Verläufe in 2,2 s, Eigenaufwand nach der Mac-Kappung plausibel (Mittel 32 s statt 50 min).
   Replay aller 720 Bash-Befehle durch den Bau-Wächter: 6 Sperren, alle echte Selbstbeendigungen (5 endeten damals mit Exit 144).
5. auf-beide.sh lehnte auf dem Server **jede** APK ab (`*[Jj]arvis*.apk` traf `/home/jarvis/…`); jetzt App-Ordner oder Dateiname.
   apk-install.sh nahm das erste Netzgerät; jetzt die Seriennummer (`--ziel`, Vorgabe handy). ÜBERTRÄGT bei adb push über den
   Tunnel geprüft, apk-install.sh zählt jetzt mit.

Außerdem: Kill-Schutz übersah `pkill -u jarvis -f X` (Schalter mit Wert, Exit 144 in d6a0f83e); Python-Heredocs mit pkill-Text
sind frei; der Kill-Hinweis rät auf dem Server nicht mehr zu `gradlew --stop`. Lauf-Wächter: CPU unter Linux aus `/proc`
(procps nur ganze Sekunden), Overlay-Wartezeile höchstens 10 min WARTET.

## Offen für die nächste Prüfrunde am Mac und am Windows-PC

1. Kill-Schutz: Die neue PKILL-Regex (Schalter mit Wert) und `nur_shell` statt `ohne_heredoc` in selbstmord() gelten überall.
   Windows: PowerShell-Befehle mit `Stop-Process`/CommandLine-Filter und Git-Bash-`pkill` gegenprüfen; Mac: zsh-Hülle.
2. Lauf-Wächter: Die 10-min-Grenze für die Overlay-Wartezeile (Alter der Ausgabedatei) unter Windows prüfen (mtime der
   Ausgabedatei des PowerShell-Werkzeugs); am Mac bleibt die CPU aus ps (`proc_cpu` gilt nur unter Linux).
3. apk-install.sh wählt jetzt per Seriennummer: am Mac mit Handy und Tablet gleichzeitig verbunden einmal installieren.
4. auf-beide.sh: Jarvis-Erkennung am Dateinamen auch mit Windows-Pfaden (`C:\…\Jarvis\app\…`, Git Bash) prüfen.
5. Regel 24 in den Profiltexten nennt noch „auf dem Server Gradle mit run_in_background“: weiter richtig (spart die 60 s
   Wartezeit), aber keine Pflicht mehr. Profiltexte nur auf Franks Wunsch ändern.

## Hostinger-Server: Liste vom Mac (erledigt am 19.11.2026, siehe oben)

Schon geklärt (19.11.2026 vom Mac): Zeitzone Europe/Berlin (der Stand in eintrag.py stimmt), Python 3.12.3, bash 5.2,
lsof und fuser vorhanden, repo-status ok.
1. M26: Ein Gradle-Bau im Vordergrund wird geblockt. Mit run_in_background muss er durchlaufen; danach liest minute.py die
   Ausgabedatei (Schreiber per lsof, Hülle = bash-Schnappschuss).
2. Vorwärm-Sperre: Die flock-Umschreibung beim reinen Bau (Bypass) wartet auf ein laufendes Vorwärmen, kein zweiter Daemon.
3. Deploy-Wächter unter Linux nutzt die Windows-Regeln (dotnet build mit OpenLauncher wird geblockt). Laut Mac-Gedächtnis
   ist das auf dem Server ein Fehlalarm-Auslöser. Klären, ob es dort überhaupt etwas zu schützen gibt.
4. analyse.py mit `~/.openlauncher/claude/minimal` samt Geschwistern; Maßnahmen mit `plattform: linux` an echten Server-Verläufen.
5. jarvis-install.sh und auf-beide.sh über die Tunnel 10.8.0.7/10.8.0.8 (ADB_VENDOR_KEYS); Lauf-Wächter meldet dort ÜBERTRÄGT.
