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

## Hostinger-Server: offen für die nächste Prüfrunde

Schon geklärt (19.11.2026 vom Mac): Zeitzone Europe/Berlin (der Stand in eintrag.py stimmt), Python 3.12.3, bash 5.2,
lsof und fuser vorhanden, repo-status ok.
1. M26: Ein Gradle-Bau im Vordergrund wird geblockt. Mit run_in_background muss er durchlaufen; danach liest minute.py die
   Ausgabedatei (Schreiber per lsof, Hülle = bash-Schnappschuss).
2. Vorwärm-Sperre: Die flock-Umschreibung beim reinen Bau (Bypass) wartet auf ein laufendes Vorwärmen, kein zweiter Daemon.
3. Deploy-Wächter unter Linux nutzt die Windows-Regeln (dotnet build mit OpenLauncher wird geblockt). Laut Mac-Gedächtnis
   ist das auf dem Server ein Fehlalarm-Auslöser. Klären, ob es dort überhaupt etwas zu schützen gibt.
4. analyse.py mit `~/.openlauncher/claude/minimal` samt Geschwistern; Maßnahmen mit `plattform: linux` an echten Server-Verläufen.
5. jarvis-install.sh und auf-beide.sh über die Tunnel 10.8.0.7/10.8.0.8 (ADB_VENDOR_KEYS); Lauf-Wächter meldet dort ÜBERTRÄGT.
