# Gehirn-Laufwerke auf macOS automatisch einbinden

macOS-Pendant zu `../windows/wg-drive-*.ps1`. Bindet die beiden Samba-Freigaben des
Gehirn-Servers dauerhaft im Finder ein (erscheinen unter „Speicherorte"):

| Finder (`/Volumes/…`) | = Windows | Server-Pfad |
|-----------------------|-----------|-------------|
| `gedanken` | Z: | `/srv/samba/gedanken` |
| `daten` | Y: | `/srv/samba/daten` |

Erreichbar nur über den WireGuard-Tunnel (`10.8.0.1`, SMB-Port 445 ist nicht öffentlich).

## Voraussetzungen (einmalig)
- `wg`/`wg-quick` installiert (`brew install wireguard-tools`).
- WireGuard-Konfig liegt in `~/SK/second-brain/wireguard/second-brain.conf`.
  Dieser Mac ist **Peer `10.8.0.6`** (Handy .2, PC .3, Fold6 .4/.5). Der zugehoerige
  `[Peer]`-Block steht in `/etc/wireguard/wg0.conf` auf dem VPS (Kommentar `# Mac (10.8.0.6)`).
  Ein neuer Rechner braucht **immer einen eigenen Peer** — zwei Geraete duerfen sich einen
  Schluessel/eine Tunnel-IP nicht teilen, sonst werfen sie sich gegenseitig raus.
- Samba-Zugangsdaten in `~/SK/second-brain/samba.env` (NICHT im Repo — secrets-in-sk-folder):
  ```
  SAMBA_USER=frank
  SAMBA_PASS=<passwort>
  ```
  Auf dem Server existieren zwei Samba-Nutzer: `frank` (uid 1000) und `frankmac` (uid 1001).
  **Beide** stehen in `valid users` beider Freigaben. Aktuell nutzt der Mac `frank` — damit war
  kein Passwort-Reset auf dem Produktivserver noetig. Wer die Plattformen strikt trennen will,
  setzt `frankmac` ein eigenes Passwort (`smbpasswd -a frankmac`) und traegt es in `samba.env` ein.

## Einrichtung
```bash
# 1) Benutzer-Teil (KEIN sudo): Mount-LaunchAgent (Login + alle 5 Min) + Sofort-Mount
bash ~/proggs/second-brain-server/macos/setup-macos.sh

# 2) Boot-Tunnel (einmalig, mit sudo): WireGuard beim Systemstart hochfahren
sudo bash ~/proggs/second-brain-server/macos/setup-macos.sh --daemon
```

## Bestandteile
| Datei | Zweck | Ebene |
|-------|-------|-------|
| `wg-drive-mount.sh` | mountet gedanken/daten, sobald SMB-445 erreichbar ist (Gate auf 445, Almanach §5); **erkennt tote Mounts und baut sie sofort neu auf** (Stale-Erkennung, siehe unten); mkdir-Lock verhindert Doppelmounts (`/Volumes/gedanken-1`) wenn LaunchAgent und Handaufruf kollidieren | User |
| `de.frank.secondbrain.drivemount.plist` | LaunchAgent: ruft das Mount-Skript bei Login + alle 5 Min | User |
| `wireguard-up.sh` | **Watchdog**: fährt den Tunnel hoch und hält ihn dauerhaft oben (Prüftakt 30 s, baut nach Sleep/Wake, IP-Wechsel oder Prozess-Tod selbst neu auf und stößt danach sofort den Mount-Agent an) | root |
| `de.frank.secondbrain.wireguard.plist` | LaunchDaemon: startet den Watchdog beim Boot, `KeepAlive` hält ihn am Leben | System |
| `nsmb.conf.vorlage` | Vorlage für `/etc/nsmb.conf` (soft mounts → Finder friert bei Aussetzer nicht ein) | System |
| `setup-macos.sh` | Installer für beide Teile | — |
| `install-cortex-ca.sh` | importiert Caddys interne Root-CA in den Login-Schlüsselbund → Chrome zeigt bei `https://10.8.0.1` ein Schloss statt "Nicht sicher" (idempotent, kein sudo) | User |
| `../wg-endpoint-monitor.{sh,service,timer}` | VPS-seitig: protokolliert jeden Endpoint-/IP-Wechsel der Peers (Beweis für Client-IP-Wechsel) | VPS (root) |

## ⭐ Laufwerke waren nach JEDEM Neustart weg (Direktive #3, 2026-08-27)

**Symptom:** Nach jedem Neustart des Macs fehlten `gedanken`/`daten` komplett. Das Mount-Log meldete
im 5-Minuten-Takt nur `SMB 445 (10.8.0.1) nicht erreichbar — Tunnel unten?`, obwohl das Tunnel-Log
kurz nach dem Boot ein sauberes „WireGuard-Tunnel hochgefahren" zeigte.

**Root Cause (aus dem Systemlog bewiesen, Boot 19:47:18):**
```
19:47:45.134  launchd  [system/de.frank.secondbrain.wireguard [578]] exited due to exit(0), ran for 7924ms
19:47:45.136  kernel   utun4 detaching          <- 2 ms nach dem Job-Ende
19:47:45.137  kernel   utun4 detached
```
Der LaunchDaemon war ein **Einmal-Job** (`RunAtLoad`, kein `KeepAlive`): Skript fährt den Tunnel hoch,
`exit 0`. launchd beendet beim Job-Exit aber **alle verbliebenen Prozesse der Prozessgruppe** — und
`wg-quick` startet `wireguard-go` genau dort hinein. Der Tunnel starb also zuverlässig 2 ms nach dem
Start, und die Laufwerke konnten nie mounten. Von Hand aus dem Terminal gestartet trat der Fehler
**nicht** auf (kein launchd-Job) — deshalb wirkte der Fix beim Einrichten jedes Mal „erfolgreich".

**Fix (Fehlerklasse, nicht Symptom — funktionserhaltend, nichts entfernt):**
1. `wireguard-up.sh` läuft als **Dauerschleife** → der Job endet nie → der Prozessgruppen-Kill kann
   gar nicht mehr eintreten (Poka-Yoke Stufe 3).
2. Die Schleife prüft alle 30 s **zwei unabhängige Proben** (ICMP *oder* SMB-Port 445). Erst nach
   4 Fehlschlägen in Folge (~2 Min) wird der Tunnel neu aufgebaut → heilt zusätzlich Sleep/Wake,
   Client-IP-Wechsel und `wireguard-go`-Crash, ohne bei einem einzelnen Aussetzer zu flappen.
3. `KeepAlive=true` in der plist: stirbt das Skript selbst, startet launchd es neu (zweite Schicht).
4. Verwaiste `/var/run/wireguard/<name>.name` (Überbleibsel des alten Bugs) werden vor jedem
   Aufbau entfernt.
5. Kommt der Tunnel hoch, stößt der Watchdog den Mount-LaunchAgent **sofort** an
   (`launchctl kickstart gui/<uid>/de.frank.secondbrain.drivemount`) — die Laufwerke sind in Sekunden
   da statt nach bis zu 5 Minuten.
6. `wg-drive-mount.sh` wartet beim Start **bis zu 90 s** auf Port 445 (Boot-Race: der Agent lief am
   27.08.2026 exakt 3 s vor dem fertigen Tunnel) statt sofort aufzugeben.

**Nicht betroffen (geprüft):** Die schnelle Datenübertragung (`cortex-copy.sh` / rclone, Almanach §14)
ist unverändert — sie hängt nicht am Mount, sondern spricht SMB direkt an. Die übrigen LaunchAgents im
Repo (`drivemount`, Heartbeats) sind echte Einmal-Läufe ohne langlebiges Kind und damit von dieser
Fehlerklasse nicht betroffen.

**Prüfen, ob der Fix aktiv ist:**
```bash
sudo launchctl print system/de.frank.secondbrain.wireguard | grep -E "state|pid"   # muss "running" sein
tail -5 ~/Library/Logs/wg-tunnel.log                                              # "Watchdog gestartet"
ifconfig | grep 10.8.0.6                                                          # Tunnel-IP da?
mount | grep smbfs                                                                # beide Shares
```

## Resilienz gegen IP-Wechsel / Tunnel-Aussetzer (Direktive #3, 2026-06-29)

**Vorfall:** Die Laufwerke verschwanden, weil der WireGuard-Tunnel kurz flappte. Diagnose: Der VPS war
nachweislich kerngesund (`sar`: CPU idle 98 %, eth0 0 Fehler, kein OOM) — die Ursache war ein **Wechsel
der öffentlichen Client-IP** (dynamische Telekom-Leitung; der Endpoint-Monitor belegte einen Wechsel von
`109.41.115.177` auf `62.23.250.218` innerhalb von ~40 Min). Bei so einem Wechsel veraltet der Endpoint
auf dem Server kurz → Paketverlust → macOS wirft die SMB-Mounts ab.

Die externe Ursache (IP-Wechsel) ist nicht abstellbar — also wurde die **Fehlerklasse** unschädlich gemacht:

1. **Stale-Mount-Erkennung** (`wg-drive-mount.sh`): Ein gemounteter Share wird per Leseprobe mit hartem
   Timeout (perl-alarm, 4 s) geprüft. Toter/hängender Mount → `diskutil unmount force` + sofort neu mounten,
   statt blind „bereits gemountet" zu melden. Greift bei jedem LaunchAgent-Lauf.
2. **SMB soft mounts** (`/etc/nsmb.conf`, aus `nsmb.conf.vorlage`): Bei Aussetzer friert der Finder nicht ein;
   ein Zugriff gibt nach `max_resp_timeout` (30 s) auf, statt ewig zu hängen. **Muss als root** angelegt sein —
   die user-`~/Library/Preferences/nsmb.conf` greift NICHT für die automountd-Mounts.
3. **Endpoint-Monitor** (VPS, `wg-endpoint-monitor.*`): loggt alle 30 s jeden Endpoint-Wechsel nach
   `/var/log/wg-endpoint-monitor.log` → der nächste Vorfall ist sofort beweisbar.
4. **`PersistentKeepalive = 15`** (statt 25) in der Client-Konfig → schnellere NAT-Heilung nach IP-Wechsel.

`/etc/nsmb.conf` einrichten (einmalig, root):
```bash
sudo cp ~/proggs/second-brain-server/macos/nsmb.conf.vorlage /etc/nsmb.conf
```

## Troubleshooting
- **Laufwerke fehlen:** ZUERST prüfen, ob der **Tunnel** flappt (häufigste Ursache, nicht der Mac):
  `ping -c 10 10.8.0.1` (Paketverlust?) · `nc -z 10.8.0.1 445` (Port offen?) · `ifconfig | grep 10.8.0.2`
  (WireGuard aktiv?). Internet-Baseline `ping -c 10 1.1.1.1` + `ping` zur public VPS-IP zeigt, ob die
  Strecke oder nur der Tunnel betroffen ist. Verlust NUR im Tunnel + Server gesund = Client-IP-Wechsel.
- **IP-Wechsel beweisen:** auf dem VPS `tail /var/log/wg-endpoint-monitor.log` — zeigt jeden Endpoint-Wechsel
  mit Zeitstempel. Stimmt ein Wechsel zeitlich mit dem „Laufwerke weg" überein → bestätigte Ursache.
- **Logs:** `~/Library/Logs/wg-drive-mount.log` (Mount, inkl. öffentlicher IP bei Aussetzer) ·
  `~/Library/Logs/wg-tunnel.log` (Tunnel).
- **Tunnel manuell:** `sudo wg-quick up ~/SK/second-brain/wireguard/second-brain.conf` /
  `sudo wg-quick down …`.
- **Mount erzwingen:** `bash ~/proggs/second-brain-server/macos/wg-drive-mount.sh`.
- **Gate immer auf Port 445** prüfen, nie auf einen Dienst-Port (brain-api 8000 etc.) — sonst
  blockiert ein Dienst-Neustart das Mapping (siehe `bugs/server/samba-wireguard.md` §5).
