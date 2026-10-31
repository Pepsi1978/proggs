# OpenLauncher Server: Bauplan

Stand: 31.10.2026, 09:45 Uhr. Beraten mit Fable. Noch nichts davon ist gebaut.

## Ziel

Jarvis schickt Programmieraufträge auch an den Hostinger-Server. Dort läuft dieselbe CLI-Sitzung wie am Mac, mit Franks
Abos, Profilen, Modi, Modellen und dem Repo. Frank sieht und bedient jede Sitzung live vom Handy aus, schließt fertige
Sitzungen und startet neue über eine Server-Fassung des OpenLaunchers.

## Was am Bestand entscheidend ist

| Befund | Folge |
|---|---|
| Server: Ubuntu 24.04, 2 vCPU, 7,8 GB RAM (5,5 GB frei), **kein Swap**, 86 GB Platte; Werft Studio, Second Brain und der Jarvis-Relay laufen dort | Höchstens 2 Sitzungen gleichzeitig, Speicherdeckel und Swap sind Pflicht, keine Android-Builds auf dem Server |
| Der Relay kennt genau zwei Plätze (`haupt`, `ersatz`, Schalter `mac: bool`); `sol` nur `haupt`, `katalog` nur `ersatz` | Relay muss auf eine Liste benannter Rechner mit Fähigkeiten umgestellt werden; alte Brücken müssen weiter funktionieren |
| `jarvis_pc_bruecke.py` hat mit dem „Notweg“ schon alles für einen Start ohne Oberfläche | Server-Brücke entsteht daraus; `open -a Terminal` wird `tmux new-session -d` |
| `jarvis_hook.py` läuft auf Linux fast unverändert, **aber** `pc_ruhe_s()` liefert dort 0, also „Frank sitzt am Rechner“ | Für den Server fest auf „niemand da“ setzen, sonst bleiben Rückfragen am Server hängen, statt aufs Handy zu kommen |
| `claude-login-sync.py` ruft außerhalb von Windows den macOS-Schlüsselbund (`security`) | Braucht einen Linux-Zweig (Datei `.credentials.json` wie unter Windows) |
| Fernwartung ist H.264 von einem echten Bildschirm (`FernVerbindung`, Port 47800) | Passt nicht: Der Server hat keinen Bildschirm. Für ihn kommt ein Web-Terminal |
| Die App vertraut der privaten Cortex-CA nicht und hat keine `network_security_config` | Konsole über HTTP nur im WireGuard-Tunnel, Ausnahme in der App nur für `10.8.0.1` |
| `apk-update-cloud` und `android-cloud-build.yml` bauen und signieren über GitHub Actions, aber nur nach PR-Merge | Das ist der Android-Weg vom Server; der Skill erkennt bisher nur `CLAUDE_CODE_REMOTE=true` und braucht ein eigenes Server-Merkmal |

## Grenze, die Frank kennen muss

„Alles wie am Mac“ geht nicht vollständig. Auf Linux gibt es kein `swiftc`-App-Bundle und kein WPF: Mac- und Windows-Apps
lassen sich dort bearbeiten und pushen, gebaut und installiert werden sie weiter am jeweiligen Rechner. Dazu gehören auch
die Updateskripte mit Ja/Nein-Fenster (OpenLauncher, TVO, CVO). Android-Apps gehen vollständig, nur der letzte Schritt
ändert sich: GitHub Actions baut, UpdateStation installiert, kein direktes `adb install`. Jarvis selbst fragt dabei nicht
nach dem Zeitpunkt, denn UpdateStation stellt diese Frage bewusst nicht.

## Architektur

```
Handy (Jarvis) ── POST /geraet/pc-auftrag ──► Relay ◄── Long-Polling ── server-bruecke.py (Benutzer "jarvis")
      │                                                                      │ tmux new-session -d
      │ WireGuard 10.8.0.1:8090                                              ▼
      └──── WebView ──► serverlauncher (aiohttp + xterm.js) ── pty ──► tmux: claude / codex / opencode / kimi / agy
                                                                             │ jarvis_hook.py → Relay (Schritte, Rückfragen, Nachträge)
                                                                             ▼
                                                                  ~/proggs ⇄ GitHub ⇄ Actions → Drive → UpdateStation
```

### Entscheidungen

1. **Reihenfolge: Mac bzw. Windows (wie eingestellt) → Server → Cloud.** Stünde der immer laufende Server vorn, bekämen
   Mac und PC nie wieder einen Auftrag, und die Direktinstallation per WLAN fiele weg. Die Reihenfolge wird in Jarvis
   einstellbar, und „auf dem Server“ im Auftrag wählt ihn direkt (neuer Wert `ziel=server`). Neue Einstellung auch in den
   `EinstellungsKatalog`.
2. **Eigener Benutzer `jarvis`, nie root, nicht in der Gruppe `docker`.** Die Sitzungen laufen mit
   `--dangerously-skip-permissions` auf einem Server mit Gesundheits- und Gedächtnisdaten; Docker-Zugriff wäre root-Zugriff.
3. **Ressourcenschutz:** 4 GB Swap, systemd-Slice `jarvis-sitzungen` mit `MemoryMax` und `CPUQuota`, damit eine Sitzung
   nie Relay oder Second Brain abschießt. Sind 2 Sitzungen aktiv, meldet die Brücke beim Abfragen „belegt“, und der Relay
   gibt ihr gar nicht erst einen Auftrag. Er geht an den nächsten Rechner oder in die Cloud. Ablehnen nach dem Abholen wäre
   falsch: Mit `ok=false` springt das Handy aus, obwohl der Auftrag schon zugestellt ist.
4. **Server-Brücke in Python** (`OpenLauncherServer/bruecke/`), Dienst als systemd-User-Unit, nur ausgehende Verbindung.
   Sie meldet `X-Jarvis-Rang: server` und ihre Fähigkeiten (`opus, sol, katalog`).
5. **OpenLauncher Server portiert die Logik, nicht die Oberfläche.** Kern in Python: Profil → `CLAUDE_CONFIG_DIR`, Modus →
   aktive `CLAUDE.md`/`AGENTS.md`, Modell, Effort und CLI → Startbefehl. Gelesen werden dieselben Dateien wie am Mac:
   `models.json`, `model-defaults.json`, `WorkModes/*.md`. Vorlage ist `OpenLauncherService.swift`, Skripte in bash.
6. **Standards wie auf Mac und Windows:** Opus 5.5 · Minimal · Schnellmodus · High, bei OpenAI GPT 6.1 Sol · Minimal ·
   Schnellmodus · High. Neu ist nur, dass Sol und das Katalog-Angebot auch auf dem Server laufen.
7. **Eigener Profilsatz `Profiles/ClaudeCodeServer/`** mit eigenen Regeln: Installation über GitHub Actions statt adb,
   kein Emulator, keine Updateskripte. Merkmal `JARVIS_SERVER=1` steuert Skills wie `apk-update-cloud`. Nicht
   `CLAUDE_CODE_REMOTE` setzen, das könnte das Verhalten von Claude Code selbst ändern.
8. **Oberfläche:** eine Web-App im Tunnel unter `http://10.8.0.1:8090`, mit Zugangstoken, für Handy gebaut und auch am
   Mac-Browser nutzbar. Vier Reiter:
   - **Sitzungen:** Liste mit Auftrag, CLI, Modell, Stand; antippen öffnet das Terminal, Schließen beendet sie.
   - **Terminal:** echte CLI mit Eingabezeile (xterm.js), Sondertasten für Esc, Strg+C, Pfeile.
   - **OpenLauncher:** Modell, Profil, Modus, Effort und CLI wählen, starten.
   - **System:** CLI-Versionen, Anmeldestände, Speicher, „Jetzt aktualisieren“.
   Eigener Dienst statt `ttyd` (im Ubuntu-Archiv 1.7.4): ein Port und ein Token für alle Sitzungen, ohne Prozess je Sitzung.
   Weil der Dienst nativ läuft, braucht er eine UFW-Regel `8090/tcp on wg0`.
9. **In Jarvis:** neuer Eintrag „Server“ im Fernwartungssymbol, öffnet eine WebView statt `FernVerbindung`. `Fernziel` und
   `RechnerStatus` bekommen dafür eine eigene Erreichbarkeitsprüfung, weil sie heute das Fernsteuerungsprotokoll erwarten.
10. **CLI-Updates:** systemd-Timer alle 3 Stunden. Eine CLI wird nur aktualisiert, wenn gerade keine Sitzung sie benutzt;
    sonst folgt sie beim nächsten Lauf. Fehlschläge erscheinen im Reiter System. Die Liste kommt aus UpdaterZentrale.
11. **Shell: bash für alle Skripte.** PowerShell wird zusätzlich installiert und ist als Shell im Terminal wählbar, aber
    keine Abhängigkeit. Die aktuelle Version wird bei der Installation abgelesen; 7.6.9 ist unbestätigt.
12. **Repo:** ein Klon in `/home/jarvis/proggs` mit LFS, Sitzungen teilen ihn wie am Mac. Vor jedem Start
    `git pull --rebase --autostash`.
13. **Geheimnisse nur nach Bedarf:** `relay.properties` und ein eigener GitHub-Schreibzugang für den Server, keine Kopie von
    `~/SK`. Keine Signaturschlüssel, die liegen in GitHub Actions. Abo-Anmeldungen erfolgen einmalig im Web-Terminal.

## Änderungen am Bestand

| Datei | Änderung |
|---|---|
| `Jarvis/server/relay/relay.py` | Rechnerliste statt zwei Plätze, Fähigkeiten und Reihenfolge je Auftrag; `vorrang`, `haupt` und `ersatz` bleiben gültig |
| `ProgrammierFaehigkeit.kt` | `ziel=server`, Reihenfolge, Texte in `pc_status`; `rechnerDa()` prüft Fähigkeit statt Rang |
| `Einstellungen.kt`, `EinstellungsKatalog` | Rechner-Reihenfolge |
| `fernsteuerung/` plus neue `ServerKonsoleActivity` | WebView, `network_security_config` nur für `10.8.0.1` |
| `auftragstext()` (in der neuen Server-Brücke) | Eigener Abschnitt VORGEHEN: GitHub Actions und `apk-update-cloud` statt „Installation per WLAN“, sonst versucht die erste Sitzung trotz Profil `adb` |
| `jarvis_hook.py` | Bei `JARVIS_SERVER=1` nie „am Rechner“ |
| `claude-login-sync.py` | Linux-Zweig |
| Skill `apk-update-cloud` | Merkmal `JARVIS_SERVER=1` zulassen |
| JarvisMac | Später: dieselbe Konsole, nur Link oder WebView |

Neu: `OpenLauncherServer/` mit `kern/`, `bruecke/`, `web/`, `installieren.sh`, `systemd/`, eigener `CLAUDE.md` und
`AGENTS.md`. Version steht in `OpenLauncherServer/VERSION`, Form `0.1.0`, und wird im Reiter System angezeigt.

## Bauabschnitte

**Phase 1: Auftrag läuft durch.** Benutzer, Swap, Slice, Repo, Claude Code, Server-Brücke, Relay-Umbau, Profilsatz,
Hook- und Login-Anpassung, `ziel=server` in Jarvis. Anmeldung vorläufig per `ssh` und `tmux attach`.
*Fertig, wenn:* ein Opus-Auftrag vom Handy auf dem Server läuft, Schritte und eine Rückfrage in Jarvis erscheinen und der
Commit auf GitHub landet, während Mac und Windows-PC ihre Aufträge weiter wie bisher bekommen.

**Phase 2: Sehen und bedienen.** Web-App mit Sitzungen und Terminal, `ServerKonsoleActivity`, UFW-Regel.
*Fertig, wenn:* Frank die laufende CLI am Handy sieht, tippt und die Sitzung schließt.

**Phase 3: Alle Modelle.** Codex, OpenCode, Kimi, Antigravity samt Anmeldung, Reiter OpenLauncher, Sol und Katalog auf dem
Server, Android-Abschluss über GitHub Actions.
*Fertig, wenn:* GPT 6.1 Sol vom Handy startet und eine Android-Änderung in UpdateStation ankommt.

**Phase 4: Dauerbetrieb.** Update-Timer, Reiter System, PowerShell, Aufräumen alter Sitzungen, JarvisMac-Anbindung.

## Bei der Installation zu prüfen

Diese Punkte sind nicht bestätigt und blockieren nichts vorab. Was dafür ins Web muss, läuft über den Skill `research`.

- Anmeldeablauf ohne Browser für Claude Code, Codex, Kimi und Antigravity.
- Ob `--remote-control` auf dem Server die Sitzung auch in der Claude-App zeigt.
- Linux-Installation von `agy` (`antigravity.google/cli/install.sh`) und Kimi.
- Ob die Abo-Bedingungen der Anbieter den Betrieb auf einem eigenen Server abdecken.
- Ob der Android-Build von `android-cloud-build.yml` alle Apps abdeckt und wie der PR-Weg mit dem direkten Arbeiten auf
  `main` zusammenpasst.

## Risiken

- **Mehr Angriffsfläche:** Abo-Anmeldungen und ein GitHub-Schreibzugang liegen auf einem Server im Internet, und die
  Sitzungen laufen ohne Rückfragen. Abfederung: eigener Benutzer, kein Docker, Konsole nur im Tunnel. Wer ganz trennen
  will, nimmt später einen zweiten VPS; die Brücke läuft dort unverändert.
- **Wenig Leistung:** Mit 2 vCPU und ohne Reserve werden Sitzungen spürbar langsamer als am Mac, wenn Werft-Studio-Worker
  gleichzeitig arbeiten.
- **Gemeinsamer Klon:** Parallele Sitzungen im selben Arbeitsbaum können sich stören, wie schon heute am Mac.
- **Relay-Umbau:** Der Relay ist die zentrale Stelle für alle Aufträge. Darum Rückwärtsverträglichkeit zuerst und Test gegen
  beide bestehenden Brücken vor dem Deploy.
- **Reihenfolge beim Ausrollen ist zwingend: erst Relay, dann Brücke.** Der heutige Relay führt jeden Rang außer `ersatz` als
  Windows-PC. Eine Server-Brücke bekäme dort Sol-Aufträge, die für den PC gedacht sind. Darum nennt der neue Relay seine
  Fähigkeit in einer Antwort-Kopfzeile, und die Brücke holt ohne sie keine Aufträge ab.
