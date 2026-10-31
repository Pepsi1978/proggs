# OpenLauncher Server

Programmieraufträge von Jarvis auf dem Hostinger-Server: dieselben CLIs, Modelle, Profile und Modi wie auf Mac und Windows, ohne Oberfläche, in tmux. Entstehung und Entscheidungen: `PLAN.md`.

| Teil | Datei | Läuft als |
|---|---|---|
| Launcher-Kern (Profile, Modi, Startskripte, Sitzungen) | `kern/launcher.py` | Bibliothek und `launcher.py starten/liste/schliessen/abgleich` |
| Brücke zum Jarvis-Relay (`X-Jarvis-Rang: server`) | `bruecke/server_bruecke.py` | `jarvis-server-bruecke.service` |
| Server-Konsole (nur im Tunnel, `http://10.8.0.1:8090`) | `web/` | `jarvis-konsole.service` |
| CLI-Updates alle 3 Stunden, nur wenn die CLI nicht läuft | `kern/clis.py` | `jarvis-clis.timer` |
| Repo vorspulen alle 5 Minuten; Dienste starten bei neuer Fassung selbst neu | `kern/launcher.py abgleich` | `jarvis-repo.timer` |
| tmux mit Speicher- und CPU-Deckel | `systemd/jarvis-tmux.service` | enthält alle Sitzungen |
| Regeln für Sitzungen auf dem Server (gehen dem Profil vor) | `server-zusatz.md` | wird an Profil und Modus gehängt |

**Einrichten:** Vom Mac `~/SK/Jarvis/relay.properties`, `~/SK/Jarvis/fernsteuerung-token.txt` und `~/SK/Android/debug-shared.keystore` nach `/home/jarvis/SK/` kopieren, dann `installieren.sh` als root ausführen (beliebig oft ausführbar). Meldet es `DEPLOY_KEY_FEHLT`, den genannten Schlüssel mit `gh repo deploy-key add --allow-write` eintragen und neu starten. Danach in der Konsole unter System anmelden.

**Fallen auf dem Server:**
- `/etc/hosts` zeigte den eigenen Namen auf 127.0.1.1, die Brücke kam nicht zum Relay. Geändert in `/etc/hosts` und `/etc/cloud/templates/hosts.debian.tmpl`.
- tmux meldet für ein laufendes Startskript immer `bash`. Das Ende einer CLI steht deshalb in `~/.openlauncher/sitzungen/<name>.fertig`.
- Mehrere Sitzungen teilen sich `~/proggs` und die eine `AGENTS.md` (im Repo ignoriert), wie am Mac: Startet eine andere CLI mit anderem Profil, gilt für eine neu lesende Sitzung dessen Fassung.

**Bei einer Neuinstallation:** Kimi fragt beim ersten Start „Trust this folder?“. Einmal von Hand bestätigen, gemerkt wird das in `~/.kimi-code/workspace-trust/`; sonst bleibt jeder Kimi-Auftrag dort stehen.

**Version:** `VERSION`, Form `0.1.1`, angezeigt im Reiter System; pro Commit an diesem Ordner einmal erhöhen.
