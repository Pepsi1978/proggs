# Reverse-Proxy + TLS (Caddy) & Linux-VPS-Betrieb — Best Practices (Stand 2026-06-24)

> **Zweite Seite der Medaille zu `bugs/server/reverse-proxy-tls.md`.** Der Almanach sagt *was schiefgeht
> und wie man es loest*; hier steht, *wie man einen Reverse-Proxy mit Auto-TLS und den laufenden
> Linux-VPS-Betrieb von vornherein richtig aufsetzt und ueber Jahre wartet, damit die Bugs nie entstehen.*
> Beide Kurzchecks VOR der Arbeit lesen (erst Almanach, dann Best Practices). Funktionserhaltend (Direktive #3).
> Bezug: second-brain-VPS (`~/proggs/second-brain-server`, README: geplante Caddy+TLS-Stufe, Offsite-Backup-TODO).
>
> **Abgrenzung:** VPN selbst (wg0.conf, AllowedIPs, Split-Tunnel) → `server/wireguard.md`. Uebergeordneter
> Infra-Ueberblick (VPS-Wahl, Dimensionierung, DSGVO, Gesamt-3-2-1-Konzept) → `best-practices/second-brain/server-infrastruktur.md`.
> Docker/Compose-Mechanik → `server/docker.md`. Qdrant-spezifischer Betrieb/Snapshot → `server/qdrant.md`.
> HIER: Caddy/Proxy/TLS + laufender Server-Betrieb (systemd, SSH/UFW/fail2ban, Auto-Updates, Logs, NTP,
> Swap, Monitoring, Backup-Durchfuehrung).
>
> **Anker:** caddy=2.11.4 (geplant, noch nicht installiert; Auto-TLS LE+ZeroSSL, Host-Header-Auto fuer HTTPS-Upstreams
> ab 2.11, `flush_interval -1` SSE), ubuntu=24.04.4 (systemd 255, cgroup v2, needrestart verschaerft),
> ufw=0.36.2, fail2ban=1.0.2 (backend=systemd noetig). **Live-Befund: Server hat 0 B Swap.**
> Quellen-Rangordnung: offizielle Caddy-/Ubuntu-/systemd-Doku = Grundwahrheit; Community = `extern`.

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Caddy erstmals aufsetzen | ZUERST ACME-Staging (`acme_ca` staging), `email` global, dann Prod | §1 |
| 2 | Zerts persistent | `/data`-Volume (Docker) bzw. `caddy`-User-Owner; Storage NIE als Cache | §1 |
| 3 | reverse_proxy zu lokalem Dienst | an `127.0.0.1`/`10.8.0.1` binden; `handle_path` fuer Subpath; `route {}` wenn Reihenfolge zaehlt | §2 |
| 4 | MCP/SSE durchreichen | `flush_interval -1` + `encode` von der SSE-Route per Matcher ausnehmen | §3 |
| 5 | Echte Client-IP / kein Redirect-Loop | `trusted_proxies` global; uvicorn `--proxy-headers --forwarded-allow-ips="<subnetz>"` (nie `*`) | §4 |
| 6 | systemd-Start-Reihenfolge | `After=/Wants=network-online.target` (+ `wg-quick@wg0`); Docker-Drop-in `After=wg-quick@wg0` | §5 |
| 7 | Dienst nach Reboot | `systemctl enable` (nicht nur start) + `[Install] WantedBy=multi-user.target` | §5 |
| 8 | SSH absichern | Key-only (`PasswordAuthentication no`), `PermitRootLogin prohibit-password`/no, `sshd -t` + 2. Session | §6 |
| 9 | Firewall | nur SSH+80+443+wg-UDP offen; `ufw allow OpenSSH` VOR `enable`; Docker-Ports an 127.0.0.1 binden | §6 |
| 10 | fail2ban | `backend = systemd`, `ignoreip` eigene IP, hinter Caddy/Docker Real-IP + `DOCKER-USER`-Chain | §6 |
| 11 | Auto-Security-Updates | unattended-upgrades NUR Security, needrestart `restart='l'`, Auto-Reboot fix/aus | §7 |
| 12 | Logs/Disk | journald `SystemMaxUse=`, docker `json-file max-size`, `apt autoremove`, `df -i` im Monitoring | §7 |
| 13 | Zeit | `timedatectl set-ntp true`; VMs → chrony (Post-Suspend-Skew) | §7 |
| 14 | Speicher | Swapfile (4 GB) + `vm.swappiness=10`; pro Dienst mem_limit, Reserve fuer Host | §7 |
| 15 | Monitoring | externer Uptime-/Cert-Ablauf-Check + Push-Heartbeat (healthchecks.io-Muster); Disk/RAM-Alarm | §8 |
| 16 | Backup | 3-2-1: lokal + offsite + verschluesselt; Qdrant-Snapshot + Volume; **monatlicher Restore-Test** | §9 |

> **Goldene Ops-Regel:** Bei JEDEM Eingriff an sshd/ufw/fail2ban eine zweite SSH-Session offen halten
> ODER die Provider-VNC/Rescue-Konsole bereit. Erst `sshd -t`/`ufw allow OpenSSH`/`caddy validate`, dann anwenden.

---

## 🔗 Bezugs-Tabelle: Best-Practice ↔ Bug-Almanach

| Best-Practice (diese Datei) | Bug-Gegenpart in `bugs/server/reverse-proxy-tls.md` |
|---|---|
| §1 Caddy Auto-TLS richtig | §1 (Rate-Limit/Staging, Storage, Renewal, Challenges, on-demand, ZeroSSL) |
| §2 reverse_proxy sauber | §2 (Upstream-Start/502, Keepalive, handle_path, Sortierung, Admin-API) |
| §3 Streaming/SSE/MCP | §3 (flush_interval, encode bricht SSE, WS-Reload/Timeout/h2-3) |
| §4 Header/Proxy-Vertrauen | §4 (trusted_proxies, Host-Header, Redirect-Loop, Cloudflare, XFF) |
| §5 systemd richtig | §5 (network-online, wg-Boot-Race, enable, StartLimit, Drop-in, Type) |
| §6 SSH/UFW/fail2ban | §7 (ufw enable, fail2ban Proxy/Docker, StrictModes, cloud-init, backend=systemd) |
| §7 Auto-Updates/Logs/NTP/Swap | §6 (needrestart, Auto-Reboot, OOM/Swap, /boot, inodes, NTP-Drift) |
| §8 Monitoring · §9 Backup | (praeventiv — kein direkter Bug-Gegenpart; verhindert stilles Renewal-/Disk-/Datenversagen) |

---

## §1 Caddy automatisches TLS richtig aufsetzen

- **Staging zuerst, immer.** Beim Einrichten/Testen global `{ acme_ca https://acme-staging-v02.api.letsencrypt.org/directory }` setzen — Staging hat hohe Limits. Erst nach erfolgreichem Test auf Prod umstellen UND das Staging-Zert aus dem Storage entfernen (sonst wird das nicht-vertraute Zert weiter serviert). Schuetzt vor LE-Rate-Limit-Selbstaussperrung (Duplicate 5/Woche, Auth-Fail 5/h).
- **E-Mail global** setzen (`{ email admin@example.com }`) — noetig fuer sauberen ZeroSSL-Fallback (EAB).
- **Storage ist persistent, kein Cache.** Docker: Named Volume auf `/data` (+ `/config`). systemd: `/var/lib/caddy/.local/share/caddy` muss dem `caddy`-User gehoeren — Caddy IMMER ueber denselben Mechanismus starten (systemd ODER root, nie mischen). Storage NIE loeschen (= Zerts weg → Rate-Limit).
- **Challenge passend zur Umgebung:** HTTP-01 (Port 80) / TLS-ALPN-01 (Port 443) brauchen offene Ports; **DNS-01** (keine Ports, EINZIGES fuer Wildcards) braucht einen Caddy-Build mit DNS-Plugin (`xcaddy`). Hinter CDN/NAT → DNS-01.
- **Renewal aktiv ueberwachen** (§8) — Caddy erneuert ~30 Tage vorab, aber Renewal kann still scheitern (Port 80 zu, DNS geaendert). Nicht blind vertrauen.
- **on-demand-TLS NUR mit `ask`-Endpoint** (schneller, konstanter DB-Lookup) — sonst Rate-Limit-DoS. Fuer ein festes Gehirn-Setup mit bekannten Hostnamen ist on-demand gar nicht noetig; statische Site-Bloecke bevorzugen.
- **Quelle:** caddyserver.com/docs/automatic-https, letsencrypt.org/docs/rate-limits (offiziell). Bug-Gegenpart §1.

## §2 reverse_proxy sauber konfigurieren

- **An lokale/VPN-Adresse proxen:** `reverse_proxy 127.0.0.1:8000` bzw. `10.8.0.1:8000`. Die Dienste selbst bleiben an 127.0.0.1/wg gebunden (nicht 0.0.0.0) — Caddy ist das einzige oeffentliche Frontend.
- **Subpath:** `handle_path /app/* { reverse_proxy ... }` (strippt den Prefix); `handle` strippt NICHT. Bei manueller Reihenfolge `route { ... }` (Caddy sortiert Direktiven sonst automatisch nach Matcher-Laenge).
- **Upstream-Start absichern:** Caddy prueft Upstreams nicht beim Start (502 bis der Dienst da ist) → systemd-Reihenfolge (§5) + passive Health-Checks `fail_duration 30s`. Caddy braucht keinen Neustart, wenn der Upstream spaeter hochkommt.
- **Keepalive an den Upstream koppeln:** `transport http { keepalive 3s }` (< Upstream-Idle, z.B. Node 5s) gegen sporadische 502.
- **Timeouts setzen:** `transport http { dial_timeout 5s; response_header_timeout 30s }` (Default kein response-Timeout).
- **HTTPS-Upstream mit eigenem Cert:** `transport http { tls_insecure_skip_verify }` nur intern (WG/localhost); sauberer eigene Trust-CA. Host-Header wird ab Caddy 2.11 fuer HTTPS-Upstreams automatisch gesetzt.
- **Config-Wechsel:** `caddy validate` → `caddy reload`/`systemctl reload caddy` (zero-downtime, Auto-Rollback bei Fehler) — kein Restart.
- **Admin-API lokal lassen** (`localhost:2019`, unauth) — nie public binden; bei untrusted lokalem Code `admin unix//var/run/caddy.sock`.
- **Quelle:** caddyserver.com/docs/caddyfile/directives/reverse_proxy (offiziell). Bug-Gegenpart §2.

## §3 Streaming / SSE / MCP richtig durchreichen

- **Kernregel fuer den MCP-Server hinter Caddy:** `flush_interval -1` im `reverse_proxy`-Block (sofortiges Flushen) UND `encode` (gzip/zstd) von der SSE/MCP-Route per Matcher AUSNEHMEN — Kompression bricht SSE (Header-Flush verzoegert + Framing verschluckt Events).
- **Langlebige Verbindungen:** `stream_close_delay 30s` (sanfter Reload statt sofortigem WS-Kill), `stream_timeout` weglassen (Default unbegrenzt). App-seitig Keep-Alive-Pings (SSE `: keep-alive\n\n` / WS Ping-Pong) gegen NAT/VPN-Idle.
- **WebSocket** macht Caddy v2 automatisch (kein Sonder-Header). Vor Caddy-Upgrades (z.B. 2.8→2.9) WS/SSE gegentesten.
- **Quelle:** caddyserver.com/docs/caddyfile/directives/reverse_proxy, github.com/caddyserver/caddy/issues/6293 (offen). Bug-Gegenpart §3.

### Referenz-Caddyfile (MCP/SSE + FastAPI, an VPN gebunden)
```caddyfile
{
    email admin@example.com
    # acme_ca https://acme-staging-v02.api.letsencrypt.org/directory   # ZUERST testen, dann auskommentieren
    servers {
        trusted_proxies static private_ranges
    }
}

gehirn.example.com {
    encode @notstream gzip zstd
    @notstream not path /mcp* /sse* /events*

    @stream path /mcp* /sse* /events*
    reverse_proxy @stream 127.0.0.1:8001 {
        flush_interval -1
        stream_close_delay 30s
    }

    reverse_proxy 127.0.0.1:8000 {        # brain-api / dashboard
        transport http { keepalive 3s; response_header_timeout 30s }
        header_up X-Real-IP {client_ip}
    }
}
```

## §4 Header / Proxy-Vertrauen

- **`trusted_proxies` global** (`servers { trusted_proxies static private_ranges }`) — aktiviert echtes Client-IP-Parsing fuer alle Handler; sonst sieht das Backend/fail2ban die Proxy-IP.
- **Echte IP ans Backend:** `header_up X-Real-IP {client_ip}` und im Backend NUR diesen Header lesen (nicht die linkste XFF-IP — die kann gespooft sein).
- **FastAPI/uvicorn:** `--proxy-headers --forwarded-allow-ips="<caddy-ip/-subnetz>"` (lokal `127.0.0.1`; NIE `*` bei oeffentlichem Port) — verhindert HTTPS-Redirect-Loop und Secure-/`__Host-`-Cookie-Verlust.
- **Hinter Cloudflare:** `trusted_proxies combine { cloudflare; static private_ranges }` + `client_ip_headers Cf-Connecting-Ip X-Forwarded-For` + `trusted_proxies_strict` (mehrere separate `trusted_proxies` ueberschreiben sich).
- **Quelle:** caddyserver.com/docs/caddyfile/options, uvicorn.dev/settings (offiziell). Bug-Gegenpart §4.

## §5 systemd-Units richtig

- **Netz-/IP-Abhaengigkeit:** `[Unit] After=network-online.target` + `Wants=network-online.target` — `network.target` reicht NICHT (IP noch nicht da). Fuer Dienste/Docker, die an die WireGuard-IP binden, zusaetzlich `After=/Wants=wg-quick@wg0.service` (Docker-Drop-in via `systemctl edit docker.service` → `/etc/systemd/system/docker.service.d/wait-for-wireguard.conf`). Alternativ `FreeBind=yes`/`net.ipv4.ip_nonlocal_bind=1`, um den Boot-Race ganz zu umgehen.
- **Autostart:** `systemctl enable --now <unit>` (nicht nur `start`) + `[Install] WantedBy=multi-user.target`; `docker.service` + `containerd.service` enablen. Pruefen `systemctl is-enabled`.
- **Restart-Policy:** `Restart=on-failure` + `RestartSec=5s`; `[Unit] StartLimitIntervalSec=60s StartLimitBurst=10` gegen "start request repeated too quickly". Root-Cause via `journalctl -u` fixen, Policy nicht als Pflaster.
- **Drop-in statt Hauptdatei aendern:** `systemctl edit <unit>` (reloadt selbst); bei `ExecStart=`-Override erst leere `ExecStart=`-Reset-Zeile. Pruefen `systemctl cat <unit>`.
- **Compose als systemd-Unit:** `Type=oneshot` + `RemainAfterExit=yes` + `WorkingDirectory=/opt/second-brain` + `Requires/After=docker.service`; Boot-Deps (wg/NFS) in `[Unit]`. NICHT zusaetzlich Host-Process-Manager und compose-`restart:` fuer denselben Container mischen.
- **Quelle:** systemd.io/NETWORK_ONLINE, man7.org systemd.service(5) (offiziell). Bug-Gegenpart §5.

## §6 SSH-Hardening · UFW · fail2ban

- **SSH (Key-only):** `PasswordAuthentication no`, `PubkeyAuthentication yes`, `PermitRootLogin prohibit-password` (oder `no` mit sudo-User). **Auf Ubuntu 24.04 autoritativ `sudo sshd -T | grep -E 'passwordauthentication|permitrootlogin'` pruefen** — cloud-init-Drop-ins in `/etc/ssh/sshd_config.d/*.conf` ueberschreiben die Hauptdatei. Key-Permissions: `~/.ssh` 0700, `authorized_keys` 0600, `$HOME` nicht group-writable. IMMER `sshd -t` + zweite Session vor `systemctl reload ssh`.
- **UFW:** Default `deny incoming`/`allow outgoing`. NUR oeffnen, was noetig ist: `ufw allow OpenSSH`, `ufw allow 80,443/tcp`, `ufw allow 51820/udp` (WireGuard). **`ufw allow OpenSSH` IMMER vor `ufw enable`.** Docker umgeht UFW — Dienste an `127.0.0.1`/`10.8.0.1` binden bzw. `DOCKER-USER`-Regeln (Detail `server/docker.md`).
- **fail2ban:** `[DEFAULT] backend = systemd` (Ubuntu 24.04 loggt ins Journal, sonst 0 Bans); `ignoreip = 127.0.0.1/8 ::1 <eigene-IP>`; sshd-Jail bleibt auf `INPUT`. Web-/Docker-Dienste: Real-IP aus Caddy (`X-Real-IP`) + banaction `chain="DOCKER-USER"`. banaction nicht mischen (nftables vs iptables).
- **Quelle:** cherryservers/cyberciti (SSH), linuxize (UFW), github.com/fail2ban/fail2ban/issues/3292. Bug-Gegenpart §7.

## §7 Auto-Updates · Logs · NTP · Swap

- **Unattended-Security-Upgrades ohne Dienst-Bruch:** nur Security-Origin aktiv lassen; needrestart zaehmen (`/etc/needrestart/needrestart.conf`: `$nrconf{restart}='l'`, `$nrconf{kernelhints}=-1`) — sonst startet 24.04 nach glibc-Updates Dienste ungewollt neu. `Dpkg::Options { "--force-confold" }` (manuell geaenderte Configs behalten). Auto-Reboot bewusst: `Automatic-Reboot "false"` ODER feste `Automatic-Reboot-Time "03:00"`.
- **Logs/Disk begrenzen:** journald `SystemMaxUse=500M` (+ `SystemKeepFree`, `MaxRetentionSec`); Docker `json-file` `max-size`/`max-file` (oder `local`-Driver) — siehe `server/docker.md`. `apt autoremove --purge` (alte Kernel/`/boot`), `apt-get clean`. **`df -i` (inodes) mit ins Monitoring** — Platz da, aber keine inodes ist ein klassischer stiller Ausfall.
- **Zeit:** `timedatectl set-ntp true` (synchron halten — falsche Uhr bricht TLS/ACME/JWT). Fuer VMs/VPS mit Suspend/Migration **chrony** statt timesyncd (besser bei grossen Korrekturen).
- **Swap/Reserve (akut: Server hat 0 B Swap):** Swapfile `fallocate -l 4G /swapfile; chmod 600; mkswap; swapon` + fstab-Eintrag; `vm.swappiness=10` (nicht 0). Pro Dienst `mem_limit` (Docker), Summe deutlich < Host-RAM (Reserve fuer Kernel/Page-Cache).
- **Quelle:** unattended-upgrades-README, ubuntu.com chrony, digitalocean swap (offiziell/etabliert). Bug-Gegenpart §6.

## §8 Monitoring / Uptime-Alarm

> Etabliertes Ops-Best-Practice (nicht frisch web-recherchiert — Standardwissen, hier auf den Stack gemuenzt).

- **Externer Uptime-Check** (von ausserhalb des Servers), damit ein Totalausfall ueberhaupt auffaellt: Uptime-Kuma (self-hosted auf einem ZWEITEN Host/Gerat) oder ein Dienst wie healthchecks.io / UptimeRobot. Den Gehirn-`/health`-Endpoint pruefen (ueber VPN bzw. nach der TLS-Stufe oeffentlich).
- **Push-Heartbeat (Dead-Man-Switch):** Cronjobs (Backup, Renewal-Check) pingen nach Erfolg eine healthchecks.io-URL; bleibt der Ping aus → Alarm. Faengt genau die "still gescheitert"-Faelle (Backup lief nicht, Zert nicht erneuert).
- **Zert-Ablauf aktiv pruefen** (gegen stilles Renewal-Versagen, §1): Cron mit `echo | openssl s_client -servername host -connect host:443 2>/dev/null | openssl x509 -noout -enddate` → bei < 21 Tagen alarmieren; zusaetzlich `journalctl -u caddy | grep -i "obtaining certificate\|could not get certificate"`.
- **Ressourcen-Alarm:** Disk (`df -h` UND `df -i`), RAM/Swap, OOM-Ereignisse (`journalctl -k | grep -i oom`), Load. Schwellen: Disk > 85 %, Swap dauerhaft hoch, RAM-Reserve < 10 %.
- **Quelle:** etabliertes Self-Hosting-Best-Practice (uptime-kuma/healthchecks.io-Muster). Praeventiv, kein direkter Bug-Gegenpart.

## §9 Backup 3-2-1 + Qdrant-Snapshot + Restore-Test

> Adressiert das Offsite-Backup-TODO der second-brain-README. Etabliertes Backup-Best-Practice;
> Qdrant-Snapshot-Mechanik im Detail in `server/qdrant.md` §3.

- **3-2-1-Regel:** mind. **3** Kopien, auf **2** verschiedenen Medien/Orten, davon **1 offsite** (ausserhalb des VPS). Konkret: (a) Daten auf dem Server, (b) Snapshot/Kopie lokal auf dem Server, (c) verschluesselte Offsite-Kopie (anderer Cloud-Speicher / Heim-NAS).
- **Was sichern:** Qdrant-Daten (Snapshot + `qdrant-data/`-Volume), `brain-api`-Logs/Daten falls relevant, die `.env`/Secrets (separat, verschluesselt — liegt ohnehin in `~/SK/second-brain/`), die Stack-Configs (compose.yaml/Dockerfiles liegen schon im Repo → Portabilitaets-Versicherung), `/etc`-Server-Config (wg0.conf, Caddyfile, systemd-Drop-ins, sshd/fail2ban/ufw).
- **Qdrant konsistent sichern:** ueber die **Snapshot-API** (konsistenter Punkt) statt das Live-Volume zu kopieren; danach den Snapshot offsite ziehen. Details + Auth/Restore-Fallen: `server/qdrant.md` §3.
- **Verschluesselt + automatisiert:** `restic` oder `borg` nach S3/B2/Storage-Box (Client-seitige Verschluesselung, Dedup, inkrementell). Per systemd-Timer (taeglich); Erfolg an einen Heartbeat (§8) melden.
- **MONATLICHER RESTORE-TEST (der wichtigste Punkt):** Ein Backup, das nie zurueckgespielt wurde, ist kein Backup. Einmal im Monat den Offsite-Snapshot auf einen Wegwerf-Container/zweite Instanz restoren und pruefen, dass Qdrant startet + die Collection + Eintragszahl stimmt. In den Kalender/Heartbeat aufnehmen.
- **Retention:** taeglich 7, woechentlich 4, monatlich 6 (restic `forget --keep-daily 7 --keep-weekly 4 --keep-monthly 6 --prune`).
- **Quelle:** etabliertes Backup-Best-Practice (3-2-1) + restic/borg-Doku; Qdrant-Snapshot → `server/qdrant.md`. Praeventiv.

---

## Pflicht-Checkliste vor "Reverse-Proxy/TLS-Stufe + Dauerbetrieb live"

```
□ Caddy: Staging getestet → Prod + altes Zert weg; email global; Storage persistent; Renewal-Monitoring (§1/§8)
□ reverse_proxy: an 127.0.0.1/wg; handle_path; keepalive/Timeouts; validate vor reload; Admin-API lokal (§2)
□ MCP/SSE: flush_interval -1 + encode-Ausnahme + stream_close_delay + App-Keep-Alive (§3)
□ Header: trusted_proxies global; uvicorn --proxy-headers --forwarded-allow-ips (kein *); X-Real-IP (§4)
□ systemd: network-online + wg-quick-Drop-in; enable; Restart/StartLimit; Compose-Unit (§5)
□ SSH: Key-only + sshd -T autoritativ + 2. Session; UFW SSH-vor-enable + nur noetige Ports; fail2ban backend=systemd + Real-IP/DOCKER-USER (§6)
□ Ops: unattended-upgrades Security + needrestart gezaehmt + Auto-Reboot fix; journald/docker-Logs begrenzt; NTP/chrony; Swap + swappiness=10 (§7)
□ Monitoring: externer Uptime-Check + Heartbeat + Zert-Ablauf-Cron + Disk/RAM/inode-Alarm (§8)
□ Backup: 3-2-1 verschluesselt automatisiert; Qdrant-Snapshot; MONATLICHER Restore-Test terminiert (§9)
□ Bei Eingriffen IMMER zweite SSH-Session/Rescue-Konsole offen
```
