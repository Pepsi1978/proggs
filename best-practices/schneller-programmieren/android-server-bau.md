# Schneller programmieren: Android-Aufträge auf dem Hostinger-Server

> Stand: 13.11.2026 10:58. Entstanden aus einem Jarvis-Auftrag (DWD-Wetter), der 22,5 Minuten statt 3–4 Minuten
> brauchte, und der anschließenden Messung und Beratung mit dem Advisor. Gegenstück zu den Fallen:
> `bugs/schneller-programmieren/android-server-bau.md`. Alle Zahlen sind **Einzelmessungen** auf dem Server
> (2 vCPU AMD EPYC, 8 GB RAM, Gradle 8.11.1, AGP 8.7, Kotlin 2.1), keine Mittelwerte.

## ⚡ Kurzcheck (vor jedem Android-Auftrag auf dem Server)

| # | Situation | Regel | Abschnitt |
|---|-----------|-------|-----------|
| 1 | Bau starten | Immer `run_in_background`, nie `timeout` davor, nie im Vordergrund | §4 |
| 2 | Ablauf in der Sitzung | Code ändern → Versionslog → Bau sofort im Hintergrund → Doku, Commit, Push währenddessen | §4 |
| 3 | Gradle-Daemon „hängt“ | Nie `gradlew --stop`, nie `kill`/`pkill` auf Gradle: Ein warmer Daemon ist der größte Zeitgewinn | §2, §3 |
| 4 | Andere CLI ohne Claude-Hook | Bau selbst sperren: `exec 9>>~/.openlauncher/vorwaermen/gradle-bau.lock; flock -w 3600 9; bash gradlew assembleDebug` | §3 |
| 5 | Ist die App vorgewärmt? | `python3 ~/proggs/OpenLauncherServer/kern/vorwaermen.py stand` | §3 |
| 6 | Tablet/Handy `unauthorized` | Nicht warten: melden, am Gerät freigeben lassen | §5 |

## 1. Warum der Server langsamer ist als der PC (das Grundproblem)

- **Git überträgt nur Quelltext, nicht das Gebaute.** Der `build`-Ordner (übersetzte Klassen, Dex, Kotlin-Inkrementalcaches)
  ist pro Rechner und nicht im Repo. Der PC baut fast jeden Auftrag selbst, sein `build`-Ordner ist daher fast immer aktuell.
- **Kotlin übersetzt inkrementell**, aber nur gegen den eigenen letzten Stand. Liegen dazwischen viele fremde Commits
  (hier 44 Commits, 169 von 332 Dateien), werden die geänderten Dateien **und alle, die sie benutzen**, neu übersetzt:
  praktisch ein Vollbau (Jarvis: rund 92 000 Zeilen, 4,5–6 min nur Kotlin auf 2 vCPU).
- **Der Gradle-Daemon lebt nur 15 Minuten** (`org.gradle.daemon.idletimeout=900000`). Danach kostet jeder Bau einen kalten
  Start mit Konfiguration: gemessen 1:00–1:45 min, bevor überhaupt etwas übersetzt wird.

## 2. Messwerte (Jarvis, Einzelmessungen)

| Fall | Dauer |
|------|-------|
| Kalter Daemon, `build`-Ordner 5 Tage alt (44 fremde Commits) | ~11 min (hochgerechnet; Bau 2 allein 7:34) |
| Kalter Daemon, `build`-Ordner aktuell, nichts geändert | 163 s |
| Warmer Daemon, nichts geändert (`--profile`) | 3,3 s (Konfiguration 0,3 s, Tasks 0,9 s) |
| Warmer Daemon, eine Kotlin-Datei geändert | **37 s** |
| Installation aufs Handy über den Tunnel (Delta-Upload, Overlay „Sofort“) | 69 s |
| Vorwärmen je App aus altem Stand | 90–453 s (Aufgaben 90, Epigenetik 218, GenialeIdeen 230, GenialerWecker 297, StackLabor 453) |

Die Sitzungsarbeit selbst (Auftrag lesen, Code finden, ändern) lag bei 2:00 min. Ziel „2–3 Minuten bis zur App am Handy“
heißt daher: Bau muss warm sein (≈ 40 s), Installation ≈ 1 min, Agent-Arbeit ≈ 2 min. Für Jarvis (111-MB-APK) sind
eher 3,5–4 min realistisch, für kleine Apps 2,5–3 min.

## 3. Vorwärmen (umgesetzt: `OpenLauncherServer/kern/vorwaermen.py`)

- **Auslöser 1, Repo-Abgleich** (`jarvis-repo.timer`, alle 5 min): `launcher.py abgleich` ruft `vorwaermen.py start`.
  Pro App wird der Baum-Hash `git rev-parse HEAD:<App>` mit dem zuletzt gebauten verglichen. Der Diff eines Fetches würde
  Neustarts, abgebrochene Läufe und eigene Commits übersehen.
- **Auslöser 2, Auftragsstart** (`server_bruecke.py`): die Auftrags-App sofort mit `--immer` bauen. Das wärmt den Daemon,
  während die Sitzung noch liest und ändert (≈ 2 min), und schließt Restlücken im `build`-Ordner.
- **Welche Apps:** alle Ordner mit `gradlew` + `app/build.gradle.kts`, deren `applicationId` am Handy installiert ist
  (`adb shell pm list packages -3`, letzte Liste wird gemerkt). Ausgenommen: Apps, die hier nie installiert werden
  (GenialerWeckerAndroid, BestJournalAndroid, EntropieReductor). Reihenfolge: Jarvis zuerst, dann zuletzt geänderte.
- **Ein Bau zur Zeit:** gemeinsame Sperre `~/.openlauncher/vorwaermen/gradle-bau.lock`. Der Claude-Bau-Hook
  (`build-ablauf-guard.py`) setzt vor jeden Gradle-Bau `touch pause; exec 9>>lock; flock -w 3600 9;`. Die Datei `pause`
  lässt das Vorwärmen nach der laufenden App 20 Minuten ruhen. Zwei Bauten gleichzeitig = zwei Daemons (je 2 GB + Kotlin
  1,5 GB) bei 8 GB RAM.
- **Apps mit offenen Änderungen** (eine Sitzung arbeitet daran) werden beim Timer-Lauf übersprungen.
- **Fehlschläge:** derselbe Stand höchstens zweimal versuchen, danach bis zum nächsten Commit Ruhe (sonst Dauerbau).
- **Losgelöst starten:** über `tmux -L jarvis run-shell -b`. Ein Kindprozess eines oneshot-systemd-Dienstes stirbt mit
  dem Dienstende bzw. an `TimeoutStartSec`.
- **Priorität:** keine eigene `nice`/`ionice`. Die Daemons werden geteilt und erben sie (siehe Almanach B3). Alles im
  tmux-Server läuft ohnehin mit Nice 5, genau wie die Sitzungen.
- **Log und Stand:** `~/.openlauncher/vorwaermen/log.txt`, `stand.json`; Übersicht mit `vorwaermen.py stand`.

## 4. Ablauf in der Sitzung (Agent-Verhalten)

1. Versionslog-Eintrag **vor** dem Bau (Hook-Regel M2/M16), danach den Bau **sofort** mit `run_in_background` starten.
2. Während der Bau läuft: README/Funktionsdatei, Commit, `git pull --rebase`, Push. Das spart die Dauer dieser Schritte
   (diesmal 20 s Commit/Push parallel).
3. Nie `timeout N bash gradlew …` im Vordergrund. Das Bash-Werkzeug bricht bei 10 min ab, und mit dem Client stirbt der
   Daemon samt halber Übersetzung (Almanach B1). Hook M26 sperrt das auf dem Server.
4. Nach der Bau-Meldung direkt installieren (Jarvis über `jarvis-install.sh`, sonst `apk-install.sh`, beide per Delta).
5. Große MCP-Antworten (Tagesauswertung ≈ 34 k Zeichen) kosten Kontext, aber wenig Zeit (< 1 s). Sie gehören zu Franks
   Regel 21 und bleiben.

## 5. Installation

- `jarvis-install.sh` lädt die APK schon während das Overlay auf Franks Tipp wartet und überträgt nur die geänderten Teile
  (`apk-delta`). Die 69 s waren Upload plus Tippen; weiter kürzen ginge nur, wenn Frank für Server-Aufträge „Sofort ohne
  Overlay“ erlaubt (offene Entscheidung, §6).
- Handy und Tablet parallel installieren (`auf-beide.sh`), nicht nacheinander.
- `unauthorized` am Tunnel-Gerät: `adb-wlan.sh` meldet es jetzt sofort und verbindet im Hintergrund neu (stößt die
  Freigabe-Abfrage am Gerät wieder an), statt 45–100 s zu warten.

## 5b. Daemon rund um die Uhr warm (umgesetzt 13.11.2026)

- `org.gradle.daemon.idletimeout=86400000` (24 h) in `~/.gradle/gradle.properties` und in der Vorlage in `installieren.sh`.
- `vorwaermen.py` (`daemon_pflege`, nach jedem Abgleich-Lauf): fehlt der Daemon, startet ein Jarvis-Bau ihn. Ist er älter
  als 24 Stunden, wird er unter der Bau-Sperre beendet (samt Kotlin-Daemon) und frisch gestartet. So gibt es keine
  „verwitterten“ Daemons nach Wochen Laufzeit.
- Speicher: Gradle-Daemon ≈ 2,25 GB, Kotlin-Daemon 0,8–1,5 GB. Beide liegen im cgroup von `jarvis-tmux.service`
  (seit 13.11.2026 MemoryHigh 6,5 GB, MemoryMax 7 GB; vorher 4,5/5,5 GB) zusammen mit den Sitzungen. Den Deckel
  kann nur root ändern (`systemctl set-property jarvis-tmux.service …`, ohne tmux-Neustart).

## 6. Weitere Hebel (nicht umgesetzt, Franks Entscheidung)

| Hebel | Gewinn | Preis |
|-------|--------|-------|
| Overlay bei Server-Aufträgen überspringen | bis zu Franks Reaktionszeit | Laufende Sprachaufnahme kann abbrechen |
| Configuration-Cache | warm nur 0,3 s Konfiguration, also kaum Gewinn | EntropieReductor hat ihn bewusst aus; der Versionslog-Bump macht ihn ohnehin vor jedem Bau ungültig (Vermutung) |
| Mehr vCPU beim Hostinger-Tarif | Kotlin-Vollbau skaliert fast linear | Kosten |

## 7. Auf andere CLIs übertragen (Codex, OpenCode, Kimi)

- Vorwärmen läuft serverseitig und gilt für jede CLI automatisch.
- Ohne Claude-Hooks fehlen Bau-Sperre und M26: In Skills oder Prompts den gesperrten Bauaufruf aus dem Kurzcheck (#4)
  vorgeben und „Bau im Hintergrund, nie mit Zeitlimit“ verlangen. Die Regel steht in `OpenLauncherServer/server-zusatz.md`,
  das alle CLIs auf dem Server lesen.
- Prüfen nach dem Übertragen: Während eines Sitzungsbaus muss `flock -n ~/.openlauncher/vorwaermen/gradle-bau.lock true`
  scheitern.

## 8. Windows-PC (umgesetzt 13.11.2026, Mac folgt mit demselben Skript)

Dasselbe Grundproblem gilt am PC: Nach vielen Commits auf Server, Mac oder in der Cloud passt der `build`-Ordner nicht mehr.
Umgesetzt in `Werkzeuge/vorwaermen/` (Python, plattformneutral; Einrichtung `einrichten.ps1`):

| Server-Maßnahme | PC |
|---|---|
| Repo-Abgleich alle 5 min + Vorbau geänderter Apps (Baum-Hash) | Aufgabe `Vorwaermen`: Anmeldung (+2 min) und alle 10 min, `fetch` + `merge --ff-only` wie `RepoSync.cs`, dann Vorbau |
| Auftragsstart-Vorbau | PC-Brücke `Starte-Sitzung` startet `vorwaermen.py lauf --app <App> --immer` |
| Daemon 24 h warm | `org.gradle.daemon.idletimeout=86400000` in `~/.gradle/gradle.properties`; Anmelde-Auslöser nach 30 s startet ihn gleich nach dem Windows-Start (Jarvis-Bau, sonst eine saubere App mit denselben jvmargs); älter als 24 h → unter der Bau-Sperre `--stop` + Kotlin-Daemon beenden und frisch starten (`daemon_pflege`, wie am Server) |
| Repo-Abgleich beim CLI-Start | OpenLauncher `RepoSync.PullAsync` stößt nach einem neuen Stand `schtasks /run /tn Vorwaermen` an (ab 1.24.121) |
| — | Kein Vorbau, solange ein Steam-Spiel läuft (`HKCU\Software\Valve\Steam\RunningAppID` ≠ 0) oder ein Call-of-Duty-Prozess (`cod.exe`, `cod24…`; Muster nie `cod*`, das trifft `codex.exe`) |
| Bau-Sperre über Hook | `build-ablauf-guard.py` setzt unter Windows `python …/vorwaermen.py warte;` vor jeden Gradle-Bau (Bash und PowerShell), `apk-update.ps1` ruft es selbst: pausiert 20 min, wartet auf einen laufenden Vorbau. Zusätzlich setzt das Skript aus, solange ein Gradle-Client läuft (`gradle-wrapper.jar` in der Befehlszeile), und überspringt Apps mit offenen Änderungen oder Commit jünger als 10 min |
| Vorrang für Sitzungen (alle CLIs) | Der Vorbau prüft während des Bauens alle 2 s die Datei `pause` und alle 10 s fremde Gradle-Clients. Startet eine Sitzung (Claude, Codex, OpenCode, Kimi, Antigravity) einen Bau, beendet er **nur seinen Client-Prozess** (Kind von `cmd`, nie `taskkill /T`: der Daemon ist Kind des Clients). Der Daemon bricht den Bau ab und bleibt warm (gemessen: nach 5 s frei, Daemon danach IDLE). Sitzungen warten nie aufeinander; parallele Bauten bekommen je einen eigenen Daemon, die Pflege beendet Leerlauf-Daemons über die 3 jüngsten hinaus |
| `gradlew.bat` aus Agenten-Shells | Claude Code setzt `NoDefaultCurrentDirectoryInExePath=1`: `cmd /c gradlew.bat` findet die Datei im cwd nicht. Immer den absoluten Pfad nehmen |
| Ausgaben | Nie über Pipes (Temp-Datei): Ein dabei gestarteter Daemon oder adb-Server erbte sonst die Pipe, und unter Windows hängt `subprocess.run` dann auch nach dem Zeitlimit |
| Apps vom Handy | Pakete von **allen** verbundenen Geräten (Handy + Tablet), keine Ausschlussliste (am PC bauen auch Wecker und BestJournal) |
| `nur-arm64`, `workers.max=2`, cgroup, M26 | nicht übernommen: Emulator ist x86_64, PC hat 8 Kerne/31 GB; Vorbau nur mit `--max-workers=<Kerne/2>` |
| — | Im Akkubetrieb kein Vorwärmen |

- Daemons sind am PC **nicht** für alle Apps gemeinsam: verschiedene `org.gradle.jvmargs` (2048m/4096m/Metaspace) ergeben je
  einen eigenen Daemon. Dauerhaft warm bleibt deshalb nur die Jarvis-Gruppe; andere Apps baut der Takt-Lauf mit
  `--no-daemon` vor (der `build`-Ordner wird trotzdem aktuell), nur der Auftragsstart-Vorbau wärmt auch ihren Daemon.
  Sonst lägen mehrere 2–4-GB-Daemons 24 Stunden im Speicher.
- Ersten Lauf nie aus einer Agenten-Shell starten (`Start-ScheduledTask Vorwaermen`), sonst erbt der Daemon die Pipe.
- Log und Stand: `~/.openlauncher/vorwaermen/log.txt`, `python Werkzeuge/vorwaermen/vorwaermen.py stand`.
