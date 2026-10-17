# Jarvis auf dem Mac einrichten – Prompt für Claude Code (macOS)

**So benutzen:** Auf dem Mac im Terminal `claude` im Ordner `~/proggs` starten (über den OpenLauncher, Profil Minimal) und
alles ab der Linie unten einfügen. Vorher einmal den Ordner `~/SK/Jarvis/` vom Windows-PC auf den Mac kopieren
(USB-Stick oder AirDrop, **nicht** über Git oder eine Cloud-Freigabe): darin liegen `relay.properties`, `routine.properties`
und `plugin-url.txt`. Ohne diese Dateien bricht der Prompt mit einer Meldung ab.

Stand der Windows-Einrichtung, die hier nachgebaut wird: 17.10.2026 (Jarvis 1.8.117, OpenLauncher 1.24.95).

---

Richte auf diesem Mac die komplette Zusammenarbeit mit Jarvis so ein, wie sie auf meinem Windows-PC läuft. Jarvis ist mein
Sprachassistent auf dem Handy (Android-App `~/proggs/Jarvis`, Paket `de.frank.jarvis`). Ziel: Alles, was unter Windows mit
Jarvis funktioniert, funktioniert hier genauso schnell, egal ob mein Handy im WLAN oder im Mobilfunknetz ist.

## 0. Rahmen

- Der Schnellmodus gilt für diesen Auftrag **nicht**: Jeder Teil wird nach dem Einrichten wirklich geprüft (Befehl laufen
  lassen, Log lesen, Antwort vom Relay sehen). Erledigt ist nur, was eine Prüfung bestätigt.
- Vor der ersten Änderung `git -C ~/proggs fetch --quiet` und `git -C ~/proggs status -sb`; bei `behind` erst
  `git -C ~/proggs pull --rebase --autostash`. Bei Konflikten stoppen und melden.
- Zugangsdaten nur aus `~/SK/`. Nie ein Geheimnis (Plugin-Adresse, Token, Schlüssel) in eine Datei im Repo, in eine
  Commit-Nachricht oder in deine Ausgabe schreiben. In Ausgaben die Plugin-Adresse nur gekürzt zeigen (`…/j/M2JP…/mcp`).
- Code, den du für den Mac neu schreibst, kommt ins Repo neben die Windows-Fassung (Ordner `Jarvis/werkzeuge/pc-bruecke/`,
  `Werkzeuge/adb-wlan/`) und wird nach den Profilregeln committet und gepusht. Die Windows-Dateien müssen danach unverändert
  weiter funktionieren.
- Lies zuerst und vollständig: `Jarvis/README.md` (vor allem „Aufbau“, „Zugangsschutz“, „Programmieren per Sprache“,
  „Fortschritt und Rückfragen“, „Nachträge in dieselbe Sitzung“), dann die Windows-Quellen, die du portierst:
  `Jarvis/werkzeuge/pc-bruecke/jarvis-pc-bruecke.ps1`, `einrichten.ps1`, `jarvis_hook.py`,
  `OpenLauncher/Services/JarvisAuftrag.cs` und in `OpenLauncher/Services/OpenLauncherService.cs` die Funktionen
  `BuildClaudeCodeSessionSettings` und `JarvisHooks()`. Lies außerdem den Skill `cross-platform` sowie
  `best-practices/android/adb-wlan-debugging.md` und `bugs/android/adb-wlan-debugging.md`.

## 1. Bestandsaufnahme (erst prüfen, dann ändern)

Ermittle und zeig mir als Tabelle (vorhanden / fehlt / Version):
- `~/SK/Jarvis/relay.properties` (Schlüssel `host`, `token`), `routine.properties`, `plugin-url.txt`. **Fehlt eine davon:
  sofort stoppen** und mir sagen, welche Datei ich vom Windows-PC kopieren muss. Nie raten, nie neu erzeugen.
- `~/SK/Android/debug-shared.keystore`, `~/SK/second-brain/id_ed25519` (Server-Zugang), `~/SK/rclone/`.
- Werkzeuge: `claude` (Pfad, Version), `python3` (mindestens 3.10), `curl`, `adb` (Android-SDK, `ANDROID_HOME`),
  JDK (`/usr/libexec/java_home`), `node`/`npx`, `codex`, `opencode`, `pwsh`, Homebrew, WireGuard (App oder `wg-quick`).
- Apps: Claude Desktop, Codex Desktop, ChatGPT Desktop, Google Drive für den Desktop (Pfad des Ordners
  `Meine Ablage/Dokumente/Updates`, meist unter `~/Library/CloudStorage/GoogleDrive-…/`).
- OpenLauncherMac (`~/proggs/OpenLauncherMac`, installiert in `/Applications`?): Wie startet er eine Claude-Code-Sitzung,
  wie schreibt er die aktive `CLAUDE.md` mit dem Arbeitsmodus (Gegenstück zu `EnsureClaudeConfigDir` unter Windows), welchen
  `CLAUDE_CONFIG_DIR` setzt er? Kennt er schon `--jarvis-auftrag`? (Stand 17.10.2026: nein.)
- Relay erreichbar: `curl -4 -s -o /dev/null -w "%{http_code}" https://<host>/` muss `404` liefern.

Fehlt ein Werkzeug, installiere es per Homebrew, sofern es kein neues Konto bei einem Fremdanbieter braucht. Braucht es eins
oder eine Eingabe von mir (Anmeldung, Systemerlaubnis), sag mir genau, was ich tun soll (`! <befehl>` im Prompt), und mach
mit dem Rest weiter.

## 2. Jarvis-Plugin (MCP) in allen Programmen

Jarvis ist ein MCP-Server, der auf dem Handy läuft; die öffentliche Gegenstelle ist der Relay auf meinem Hostinger-Server.
Alle Programme sprechen dieselbe Adresse an: den Inhalt von `~/SK/Jarvis/plugin-url.txt` (Form
`https://<host>/j/<Geheimnis>/mcp`, ohne Anmeldung). Die Werkzeugliste (Stand: 68 Werkzeuge) kommt live vom Handy, deshalb
muss nirgends eine Liste gepflegt werden; ein Programm sieht neue Werkzeuge nach seinem nächsten Neustart.

Trage Jarvis ein (bestehende Einträge anderer Server unangetastet lassen, vorher eine Sicherung nach
`~/SK/Jarvis/<programm>-mac-<datum>.bak` legen):
1. **Claude Code:** als HTTP-Server `jarvis` im Benutzerbereich (`claude mcp add --transport http --scope user jarvis <url>`)
   – und zwar für **jeden** Config-Ordner, den der OpenLauncher auf dem Mac benutzt (`CLAUDE_CONFIG_DIR` der Profile
   minimal, standard, strict unter `~/proggs/OpenLauncher/Profiles/ClaudeCode/<profil>`, dazu der Standardordner `~/.claude`).
   Die Datei `.claude.json` dieser Ordner ist nicht im Git, deshalb fehlt der Eintrag auf dem Mac bis jetzt.
   Unter Windows sieht der Eintrag so aus: `"jarvis": { "type": "http", "url": "<url>" }`.
2. **Codex CLI und Codex Desktop** (beide lesen `~/.codex/config.toml`):
   ```toml
   [mcp_servers.jarvis]
   startup_timeout_sec = 30
   tool_timeout_sec = 120
   url = "<url>"
   ```
   Benutzt der OpenLauncher für Codex eigene Profil-Ordner (`CODEX_HOME`), dort ebenso.
3. **OpenCode** (`~/.config/opencode/opencode.jsonc`, dazu eventuelle OpenLauncher-Profile), unter `"mcp"`:
   `"jarvis": { "type": "remote", "url": "<url>", "enabled": true, "oauth": false, "timeout": 120000 }`.
4. **Claude Desktop** (`~/Library/Application Support/Claude/claude_desktop_config.json`), als stdio-Brücke:
   `"jarvis": { "command": "npx", "args": ["-y", "mcp-remote", "<url>"], "env": { "NODE_OPTIONS": "--dns-result-order=ipv4first" } }`.
   Den absoluten Pfad zu `npx` eintragen, falls Claude Desktop ihn ohne Shell-PATH nicht findet. Danach Claude Desktop
   sauber beenden und neu starten (nur die Desktop-App, nie eine laufende CLI-Sitzung).
5. **ChatGPT (Web, Desktop, Handy):** nichts tun. Der Konnektor hängt an meinem ChatGPT-Konto, nicht am Rechner, und ist
   schon eingerichtet.

Prüfen: in jedem Programm die Werkzeugliste von `jarvis` abrufen (Claude Code: `claude mcp list` bzw. `/mcp`; Codex:
`codex mcp list`; Relay direkt: `curl -4` mit `{"jsonrpc":"2.0","id":1,"method":"tools/list"}` an die URL) und einen
harmlosen Lese-Aufruf machen (`jarvis_status`). Ändert sich später die Plugin-Adresse (in Jarvis „Plugin-Adresse erneuern“),
müssen alle diese Einträge und `plugin-url.txt` nachgezogen werden – schreib dafür ein kleines Skript
`Jarvis/werkzeuge/plugin-adresse-setzen.sh`, das die Adresse aus `plugin-url.txt` in alle oben genannten Dateien einträgt.

## 3. Mac-Brücke für Programmieraufträge

So läuft es unter Windows: „Jarvis, programmiere in App … ein, dass …“ → Jarvis schickt den Auftrag an den Relay
(`POST /geraet/pc-auftrag`). Die PC-Brücke fragt den Relay in einer Dauerschleife ab (`GET /pc/auftrag`, Long-Polling bis
25 s, Header `X-Jarvis-Token` und `X-Jarvis-Rechner`), startet für einen Auftrag sofort eine Claude-Code-Sitzung mit Remote
Control „Jarvis: <App>“ (Profil Minimal, Schnellmodus, `claude-opus-5-5[1m]`, Effort `high`, Auftrag als erste Nachricht)
und quittiert binnen 30 s über `POST /pc/quittung`. Fragt keine Brücke ab (45 s), antwortet der Relay 409 und das Handy nimmt
die Cloud-Routine – dafür ist am Mac nichts zu tun.

Baue die Mac-Fassung **in Python 3** (eine Sprache mit dem Hook, `python3` ist vorhanden):
- `Jarvis/werkzeuge/pc-bruecke/jarvis_pc_bruecke.py`: gleiche Schleife, gleiche Endpunkte, gleiche Wartezeiten, gleicher
  Auftragstext (Abschnitt „VORGEHEN“ 1:1 aus der .ps1 übernehmen), gleiche Quittung mit drei Versuchen. Netz **nur über
  IPv4** (wie `curl -4` unter Windows; im Hook gibt es dafür schon `_nur_ipv4`). `X-Jarvis-Rechner` aus
  `scutil --get ComputerName`. Zugang bei jedem Durchlauf frisch aus `~/SK/Jarvis/relay.properties`. Nur eine Brücke je
  Benutzer (Sperre per `fcntl.flock` auf eine Datei). Log `~/Library/Logs/JarvisPcBruecke/bruecke.log`, ab 1 MB rotieren.
  Vor jedem Auftrag `JAVA_HOME` (`/usr/libexec/java_home`) und `PATH` frisch bestimmen, wie es die Windows-Brücke aus
  demselben Grund tut (lange laufender Dienst, veraltete Umgebung nach einem JDK-Update). launchd startet mit einem sehr
  kurzen PATH: Homebrew (`/opt/homebrew/bin`), `~/.local/bin`, Android-SDK `platform-tools` und den Ordner von `claude`
  ausdrücklich ergänzen.
- **Sitzungsstart:** Bevorzugt über den OpenLauncherMac, genau wie der Startknopf, damit Profil, aktive `CLAUDE.md` mit
  Schnellmodus, Anmeldung und Terminal-Farbe stimmen. Hat er keinen Startbefehl dafür, füge ihm einen hinzu (Gegenstück zu
  `--jarvis-auftrag <datei> --name … --profil … --modus … --modell … --effort … --auftrag-id …` aus `JarvisAuftrag.cs`),
  bau ihn mit `bash ~/proggs/OpenLauncherMac/update-launcher.sh` (nie mit `OPENLAUNCHER_UPDATE_FORCE=1`, mit dem höchsten
  Zeitlimit warten, bis ich im Fenster geklickt habe) und bump seine Version. Die Sitzung muss in einem **sichtbaren**
  Terminal-Fenster laufen (Terminal.app oder das Terminal, das der OpenLauncherMac sonst nimmt), damit ich sie am Mac sehe;
  per Remote Control steht sie zusätzlich in der Claude-App unter Code.
  Notweg, falls der Launcher-Umbau scheitert: Die Brücke startet `claude` selbst mit `CLAUDE_CONFIG_DIR` des Profils
  minimal, `--dangerously-skip-permissions --model 'claude-opus-5-5[1m]' --effort high --remote-control 'Jarvis: <App>'`,
  dem Auftrag als erster Nachricht und `--settings <temp.json>` mit den Hooks (unten). Dann die aktive `CLAUDE.md` mit dem
  Schnellmodus so schreiben, wie der OpenLauncherMac es tut, nicht nachbauen.
- **Hooks (wichtig, sonst kein Fortschritt und keine Rückfragen im Jarvis-Chat):** Nur wenn der Auftrag eine `nummer` hat,
  setzt der Start `JARVIS_AUFTRAG_ID=<nummer>` und hängt per `--settings` dieselben Hooks an wie `JarvisHooks()` unter
  Windows: `PostToolUse` (`*`, `post`, async, 15 s), `PostToolUseFailure` (`Bash`, `post`, async), `PreToolUse`
  (`AskUserQuestion` → `frage`, 660 s; `*` → `puls`, async, 10 s), `Stop` (`stop`, 660 s, dazu der Warter), `SessionStart`
  (Warter). Warter = `warte`, Timeout 43200, `asyncRewake: true`, `rewakeSummary: "Nachtrag von Frank über Jarvis"`. Befehl
  `python3 "<voller Pfad>/jarvis_hook.py" <art>`. Struktur 1:1 aus `OpenLauncherService.cs` übernehmen, nichts weglassen.
- **`jarvis_hook.py` macOS-fähig machen** (eine Datei für beide Systeme, Windows-Zweig unverändert):
  - Log- und Zustandsordner: ohne `LOCALAPPDATA` nach `~/Library/Logs/JarvisPcBruecke/` statt ins Home.
  - `claude_prozess()` und `lebt()` liefern außerhalb von Windows bisher nichts Brauchbares; dadurch merkt der Warter das
    Ende der Claude-Sitzung nicht und wartet bis zu 12 Stunden weiter. POSIX-Zweig ergänzen: Elternkette per
    `ps -o ppid=,comm= -p <pid>` bis zum `claude`-Prozess, Lebenszeichen per `os.kill(pid, 0)`.
  - Alle anderen Windows-Annahmen im Hook suchen und gleichziehen (Pfadtrenner, `python` statt `python3`, Kodierung).
- **Autostart:** LaunchAgent `~/Library/LaunchAgents/de.frank.jarvis.pc-bruecke.plist` mit `RunAtLoad` und `KeepAlive`
  (ersetzt den Minuten-Wächter der Windows-Aufgabe), Ausgaben ins Log, laden per `launchctl bootstrap gui/$(id -u) …`.
  Dazu `Jarvis/werkzeuge/pc-bruecke/einrichten.sh` als Gegenstück zu `einrichten.ps1` (mehrfach ausführbar, ohne sudo).
- **Ruhezustand:** Schläft der Mac, fragt die Brücke nicht ab und Aufträge gehen nach 45 s in die Cloud – das ist gewollt.
  Prüfe, ob macOS die Brücke bei zugeklapptem Gerät am Netzteil weiterlaufen lässt (App Nap, Energieeinstellungen), und
  sag mir, welche Einstellung ich ändern müsste, damit der Mac wie der PC erreichbar bleibt. Nicht selbst umstellen.

**Wichtige Grenze, zuerst mit mir klären (AskUserQuestion):** Der Relay kennt genau **eine** Brücke (eine
Warteschlange, ein Zeitstempel, ein Rechnername in `PcBruecke` in `Jarvis/server/relay/relay.py`). Laufen Windows-PC und Mac
gleichzeitig, bekommt den Auftrag zufällig der, der gerade abfragt. Frag mich, welche Lösung ich will:
(A) Relay erweitern: jede Brücke meldet sich mit ihrem Rechnernamen, Vorrang hat der Rechner, an dem ich zuletzt gearbeitet
habe (Brücke meldet die Leerlaufzeit: macOS `ioreg -c IOHIDSystem` → `HIDIdleTime`, Windows `GetLastInputInfo`), und
„Jarvis, programmiere auf dem Mac“ wählt ausdrücklich; `pc_status` zeigt alle Rechner. Das ändert Relay, Windows-Brücke und
Jarvis-App gemeinsam; Relay-Ausrollen nach `Jarvis/README.md` „Server ausrollen“.
(B) Fester Vorrang Windows, Mac nur, wenn der PC nicht abfragt. (C) Erst einmal nichts ändern, ich lasse immer nur eine
Brücke laufen. Ohne Antwort: (C), und die Grenze in der Abschlussmeldung nennen.

**Prüfen:** `curl -4` mit Geräte-Token auf `GET /geraet/pc` (oder Jarvis-Werkzeug `pc_status`) zeigt den Mac als verbunden; danach bitte mich, über
Jarvis einen Mini-Auftrag zu schicken („Jarvis, programmiere in Jarvis: nur eine Zeile ins README unter ‚Noch offen‘, dass
der Mac-Test lief“), und verfolge im Brücken- und Hook-Log, dass Start, Quittung, Fortschritt, Rückfrage und Abschluss im
Jarvis-Chat ankommen.

## 4. Aufs Handy installieren – im WLAN und im Mobilfunknetz

Unter Windows kommen Apps per adb über TCP/IP aufs Handy (Galaxy Fold 8), selbstheilend:
`Werkzeuge/adb-wlan/adb-wlan.ps1` plus Wachhund-Aufgabe alle 2 Minuten. Unterwegs läuft adb über den WireGuard-Tunnel des
Second-Brain-Netzes an `10.8.0.7:5555` (Handy-Adresse; Hub ist der VPS `10.8.0.1`).

- `Werkzeuge/adb-wlan/adb-wlan.sh` gibt es schon, ihm fehlt aber der WireGuard-Schritt. Bring ihn auf denselben Stand wie
  die .ps1 (Reihenfolge der Schritte, Serial-Bindung, Lebenszeichen, TLS-Port → 5555, Tunnel-Adresse aus derselben Datei
  wie die .ps1, Log `~/Library/Logs/adb-wlan/adb-wlan.log`). Der Windows-Schritt „Direktverbindung über den PC-Hotspot“ hat
  kein einfaches Mac-Gegenstück: nicht erfinden, als Lücke melden.
- Wachhund als zweiter LaunchAgent `de.frank.adb-wlan.wachhund` (`StartInterval` 120, bei Anmeldung). Regeln aus der
  Windows-Fassung übernehmen: nie den TLS-Port speichern, im Wachhund nie `adb kill-server`.
- **WireGuard für den Mac:** Der Mac braucht einen **eigenen** Peer im Second-Brain-Netz. Die Windows-Datei
  `~/SK/second-brain/wireguard/pc.conf` nicht kopieren (gleicher Schlüssel an zwei Geräten verdrängt sich gegenseitig).
  Vorgehen: freie Adresse im Netz `10.8.0.0/24` aus `server-wg0.conf` bzw. auf dem Server (`wg show`) ermitteln,
  Schlüsselpaar auf dem Mac erzeugen, Peer auf dem VPS eintragen (Zugang: `~/SK/second-brain/vps-access.txt`,
  `id_ed25519`; vorher `wg0.conf` sichern), Client-Konfiguration als `~/SK/second-brain/wireguard/mac.conf` ablegen und in
  der WireGuard-App importieren (dafür bin ich nötig – sag mir, was ich klicken muss). Vor dem Eintragen auf dem Server
  frag mich kurz mit Ja/Nein, weil das den laufenden Server ändert.
- Prüfen: mit Handy im WLAN `adb devices` zeigt `IP:5555`; dann bitte mich, WLAN am Handy auszuschalten (Mobilfunk an,
  WireGuard an Handy und Mac an) und prüf `adb connect 10.8.0.7:5555` und eine Testinstallation (`adb install -r` einer
  vorhandenen Debug-APK, z. B. UpdateStation). Meldet `adb connect` „No route to host“ trotz Ping: `adb kill-server` und neu.

## 5. Bauen, signieren, APK-Update

- Android-Signierung: Der OpenLauncherMac legt `~/SK/Android/debug-shared.keystore` nach `~/.android/debug.keystore`; prüfe,
  dass das passiert und der SHA-256 mit `F7:82:13:1C` beginnt. Nie einen neuen Keystore erzeugen.
- Ein Bau einer Android-App muss am Mac durchlaufen: `./gradlew :app:assembleDebug` in `~/proggs/Jarvis` (JDK und SDK
  aus Abschnitt 1). Achtung: Jarvis backt beim Bau Server und Token aus `~/SK/Jarvis/relay.properties` ein – genau deshalb
  muss die Datei auf dem Mac liegen.
- Skill `apk-update`: Das Skript `skills/apk-update/scripts/apk-update.ps1` ist PowerShell und hat Windows-Pfade (Drive-Ordner
  `C:\Users\barwa\Meine Ablage\Dokumente\Updates\<Projekt>`). Mach es auf dem Mac lauffähig – bevorzugt mit `pwsh` per
  Homebrew und einem plattformabhängigen Drive-Pfad (den echten Pfad aus Abschnitt 1 ermitteln, nicht hart setzen). Skills
  nur im Repo-Ordner `OpenLauncher/Profiles/ClaudeCode/standard/skills` ändern, Windows-Verhalten unverändert lassen.
  Prüfen mit einem echten Lauf für Jarvis (neuer versionCode, `update.json` im Drive-Ordner).

## 6. Was der Mac nicht braucht

- Den Relay selbst (läuft auf dem Server) und die Cloud-Routine „Jarvis: Programmieren“ (läuft in der Claude-Cloud) – außer
  du setzt in Abschnitt 3 Lösung (A) um.
- Den ChatGPT-Konnektor (Konto, nicht Rechner).
- Die Jarvis-Hooks in normalen Sitzungen: Sie hängen nur an Sitzungen mit `JARVIS_AUFTRAG_ID`.

## 7. Abschluss

- Alles, was du am Repo geändert hast, nach den Profilregeln abschließen: Version des jeweiligen Projekts genau einmal je
  Commit bumpen (echte Systemzeit per `date "+%d.%m.%Y %H:%M"`), Commit-Nachrichten Deutsch mit echten Umlauten, direkt auf
  `main`, vor dem Push rebasen, pushen. Bei geänderter Jarvis-App: bauen, per WLAN installieren, `apk-update`.
- `Jarvis/README.md` um einen Abschnitt „Mac“ ergänzen (Brücke, LaunchAgents, Logs, Plugin-Einträge, WireGuard-Peer).
- Abschlussmeldung als Tabelle je Teil (Plugin je Programm, Brücke, Hooks, Mini-Auftrag, adb WLAN, adb Mobilfunk,
  Signierung, Bau, apk-update): eingerichtet ja/nein, geprüft womit, was noch an mir hängt.
