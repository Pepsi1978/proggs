# Bug-Almanach: Reverse-Proxy + automatisches TLS (Caddy) & Linux-VPS-Betrieb

> **PFLICHT-LESEN vor Arbeit an einem Reverse-Proxy/TLS-Setup oder am laufenden Linux-VPS-Betrieb**
> (Caddyfile, nginx-/Caddy-Config, `*.service` systemd-Units, fail2ban/UFW/SSH-Hardening, Auto-Updates).
> Digest-Modell: Kurzcheck (unten, erste ~80 Zeilen) vor der Arbeit; bei einem Fehler im Bereich VOLLTEXT lesen.
>
> **Stand:** recherchiert am 2026-06-24 (7-Opus-Researcher-Schwarm, Issue-Status hart per `gh` geprueft).
> **Anker:** caddy=2.11.4 (geplant, noch NICHT installiert), ubuntu=24.04.4, systemd=255, ufw=0.36.2, fail2ban=1.0.2
> (alle server-/projekt-gebunden → kein lokaler Live-Abgleich). Kontext: second-brain-VPS, Dienste an
> 127.0.0.1/10.8.0.1, geplante Caddy+TLS-Stufe zusaetzlich zur WireGuard-Anbindung. **Live-Befund:
> Server hat 0 B Swap** (→ §6.4 akut).

## 🔗 Abgrenzung zu benachbarten Almanachen (NICHT hier doppeln)

| Thema | Gehoert in |
|-------|-----------|
| VPN-Zugang (wg0.conf, AllowedIPs, Split-Tunnel, ip_forward) | `server/wireguard.md` |
| Security-Architektur (Cloudflare-TLS-Warnung, Container-Sandbox, Lethal Trifecta, Agent-Server) | `server/self-hosted-ai-agent-server.md` |
| Docker-Betrieb (UFW-Bypass-Mechanik, json-file-Logs, OOM/mem_limit, depends_on) | `server/docker.md` |
| **Reverse-Proxy/TLS/Caddy + Linux-Ops-Betriebsfehler** | **DIESE Datei** |

---

## KURZCHECK (Erkennungssignale + Sofort-Regeln)

| Signal | Sofort-Regel | §  |
|--------|--------------|----|
| `acme: error: rateLimited` beim Testen | IMMER zuerst mit Staging `acme_ca https://acme-staging-v02.api.letsencrypt.org/directory` testen | 1.1 |
| Caddy holt bei JEDEM (Docker-)Start neue Zerts → Rate-Limit | Storage persistent: `/data`-Volume mounten; Storage ist KEIN Cache | 1.2 |
| Zert in Prod abgelaufen, kein Fehler im Blick | Renewal scheitert STILL (Port 80/DNS/Permissions) → extern per Cron ueberwachen | 1.3 |
| ⭐ TLS `internal error`/alert 80 bei HTTPS ueber eine **IP** (kein DNS-Name) | Client sendet kein SNI fuer IPs (RFC 6066) → Caddy matcht IP-Cert nicht → global `default_sni <ip>`; geaenderte bind-Caddyfile braucht `restart` (nicht nur `up -d`) | 1.10 |
| on-demand-TLS exponiert → Rate-Limit-DoS | NIE ohne schnellen `ask`-Endpoint betreiben | 1.7 |
| 502 / `connection refused 127.0.0.1:PORT` | Upstream laeuft (noch) nicht; Caddy prueft Upstreams nicht beim Start → systemd `After=` + `fail_duration` | 2.1 |
| Sporadische 502 unter Last (`connection reset`) | Keepalive-Mismatch → `transport http { keepalive 3s }` (< Upstream-Timeout) | 2.2 |
| Backend bekommt vollen Pfad statt gestripptem | `handle` strippt nicht → `handle_path /app/*` | 2.3 |
| Direktiven-Reihenfolge "wird ignoriert" | Caddy sortiert automatisch → `route {}` fuer manuelle Ordnung | 2.4 |
| SSE/MCP-Events kommen erst am Ende / Stream haengt | `flush_interval -1` UND `encode` von der SSE-Route ausnehmen (gzip bricht SSE) | 3.1/3.2 |
| Backend/fail2ban sieht Proxy-IP statt Client-IP | `trusted_proxies` global setzen; im Backend `X-Real-IP {client_ip}` lesen | 4.1 |
| `ERR_TOO_MANY_REDIRECTS` (http↔https-Loop) | uvicorn `--proxy-headers --forwarded-allow-ips="<caddy-subnetz>"` (nie `*`) | 4.4 |
| Dienst failt nur beim Boot (IP noch nicht da) | `After=/Wants=network-online.target` (+ `wg-quick@wg0.service` bei VPN-IP), nie nur `network.target` | 5.1/5.2 |
| Dienst nicht nach Reboot | `systemctl enable` (nicht nur `start`) + `[Install] WantedBy=multi-user.target` | 5.3 |
| journald/Logs/`/boot`/inodes fressen Disk | `SystemMaxUse=`; `apt autoremove --purge`; `df -i` pruefen | 5.7/6.5/6.6 |
| unattended-upgrades killt Dienst (24.04) | `needrestart.conf` (`$nrconf{restart}='l'`, `kernelhints=-1`) | 6.1 |
| Dienst OOM-gekillt (0 B Swap!) | swapfile + `vm.swappiness=10` (nicht 0) | 6.4 |
| TLS "not yet valid"/"expired", ACME scheitert | Systemzeit/NTP: `timedatectl set-ntp true`, VMs → chrony | 6.7 |
| `ufw enable` sperrt SSH aus | IMMER zuerst `ufw allow OpenSSH`, dann `enable`; 2. Session offen halten | 7.1 |
| fail2ban bannt nichts hinter Caddy/Docker | Real-IP-Header in Caddy + banaction `chain="DOCKER-USER"` + `backend = systemd` | 7.2/7.6 |
| `PasswordAuthentication no` greift nicht (24.04) | cloud-init-Drop-in `sshd_config.d/*.conf` ueberschreibt → `sshd -T` autoritativ | 7.5 |

---

## TL;DR — die wichtigsten Regeln

1. **TLS testen IMMER zuerst per ACME-Staging** und Storage persistent halten — sonst sperrt das Let's-Encrypt-Rate-Limit aus. Renewal kann STILL scheitern → extern ueberwachen.
2. **Caddy prueft Upstreams nicht beim Start** → 502 bis der 127.0.0.1/10.8.0.1-Dienst da ist; systemd-Reihenfolge + `unless-stopped`/`fail_duration` als Selbstheilung.
3. **SSE/MCP hinter Caddy:** `flush_interval -1` + `encode` von der Streaming-Route ausnehmen (gzip bricht SSE, #6293 OPEN).
4. **trusted_proxies + uvicorn `--proxy-headers --forwarded-allow-ips`** — sonst falsche Client-IP (fail2ban wirkungslos) und HTTPS-Redirect-Loop.
5. **systemd-Boot:** `network-online.target` (+ `wg-quick@wg0`) statt `network.target`; `enable` nicht vergessen; journald begrenzen.
6. **VPS-Ops:** Swap einrichten (Server hat 0 B!), unattended-upgrades/needrestart zaehmen, NTP sichern, Disk/`/boot`/inodes im Blick.
7. **Hardening NUR mit zweiter offener SSH-Session/Rescue-Konsole** — `ufw allow OpenSSH` vor `enable`, `sshd -t` vor jedem reload.

---

## 1. Caddy automatisches TLS & Let's Encrypt

### 1.1 Rate-Limit-Selbstaussperrung beim Testen [⭐ HAEUFIG]
- **Symptom:** `urn:ietf:params:acme:error:rateLimited` / `too many certificates already issued`; stunden-/tagelang kein Zert.
- **Ursache:** LE-Limits (2026): Duplicate-Cert 5/Woche (Nachfuell 1/34 h); 50 Certs/registered-domain/Woche; Authorization-Failures 5/Identifier/h; New Orders 300/Account/3 h. Beim Wiederholt-Testen reisst man Duplicate/Auth-Fail.
- **FIX:** Vor Prod IMMER Staging: `{ acme_ca https://acme-staging-v02.api.letsencrypt.org/directory }`. Nach Test Zeile entfernen UND altes Staging-Zert aus Storage loeschen (sonst wird das nicht-vertraute Staging-Zert weiter serviert). Renewals zaehlen NICHT gegen die Limits (via ARI ausgenommen) — das Problem ist Neu-Ausstellen beim Testen.
- **Versionen:** alle v2.x (ACME/LE-Politik, per Design). **Quelle:** letsencrypt.org/docs/rate-limits, /staging-environment

### 1.2 Storage nicht persistent (Docker ohne Volume) → Rate-Limit
- **Symptom:** Nach mehreren Container-Restarts Rate-Limit; Caddy zieht bei jedem Start neue Zerts.
- **Ursache:** Datenverzeichnis (Zerts/Keys/ACME-Account) im Container-Schreiblayer geht verloren; Caddy behandelt Storage als persistent, nicht als Cache. Offizielles Image nutzt `/data`.
- **FIX:** Volume auf `/data` (+ `/config`) mounten: `volumes: [caddy_data:/data, caddy_config:/config]`. Bei Swarm: wirklich geteilter persistenter Speicher.
- **Versionen:** alle v2.x im Container ohne Volume (per Design). **Quelle:** hub.docker.com/_/caddy, caddyserver.com/docs/conventions

### 1.3 Renewal scheitert STILL → abgelaufenes Zert in Produktion
- **Symptom:** `NET::ERR_CERT_DATE_INVALID`; kein offensichtlicher Fehler, nur im Log.
- **Ursache:** (a) Port 80 blockiert/belegt (HTTP-01 braucht ihn); (b) DNS zeigt nach IP-Wechsel nicht mehr auf den Host; (c) TLS-ALPN-01 hinter CDN/zweitem :443-Terminator scheitert.
- **FIX:** Port 80 UND 443 von aussen erreichbar halten (Caddy erneuert ~30 Tage vorab mit Backoff). Geht 80 dauerhaft nicht → DNS-01 (`tls { dns <provider> }`, braucht DNS-Plugin-Build). **Aktiv ueberwachen** (eigentlicher Schutz): Cron mit `openssl s_client -connect host:443 | openssl x509 -noout -enddate` + `journalctl -u caddy | grep "obtaining certificate"`.
- **Versionen:** alle v2.x (Umweltproblem, kein Caddy-Bug). **Quelle:** caddy.community/t/expired-certificate-not-renewing-solved/4687

### 1.4 Falscher Challenge-Typ (HTTP-01 vs TLS-ALPN-01 vs DNS-01)
- **Symptom:** Validierung scheitert wiederholt; Wildcard kommt nie; hinter NAT/CDN keine Ausstellung.
- **FIX:** HTTP-01 = eingehender Port 80; TLS-ALPN-01 = Port 443 (scheitert wenn etwas davor terminiert); **DNS-01 = keine offenen Ports noetig + EINZIGE Methode fuer Wildcard** `*.example.com`. DNS-01 explizit setzen; braucht Caddy-Build MIT DNS-Plugin (`xcaddy`), Standard-Binary hat es nicht.
- **Versionen:** alle v2.x (ACME-Design). **Quelle:** caddyserver.com/docs/automatic-https

### 1.5 Port 80/443 belegt (Docker/anderer Dienst)
- **Symptom:** `listen tcp :443: bind: address already in use` ODER Challenge scheitert.
- **FIX:** Belegung finden `sudo ss -tlnp 'sport = :80 or sport = :443'`; Caddy als alleiniges Frontend, andere Dienste auf interne Ports. Muessen Ports belegt bleiben → DNS-01. In Docker nur EIN Container published :80/:443.
- **Versionen:** alle v2.x (Deployment). **Quelle:** caddyserver.com/docs/automatic-https

### 1.6 Storage-Permissions falsch (root vs systemd-`caddy`-User) — #6347 GEFIXT, Falle bleibt
- **Symptom:** `failed to create temp file`/`permission denied`; Renewal scheitert still, obwohl es als root lief.
- **Ursache:** systemd-Caddy laeuft als User `caddy`; ein manueller root-Start legt Storage-Dateien `root:root` an → `caddy` kann nicht erneuern. Storage liegt unter `/var/lib/caddy/.local/share/caddy`.
- **FIX:** `systemctl stop caddy; chown -R caddy:caddy /var/lib/caddy; systemctl start caddy`. Caddy IMMER ueber denselben Mechanismus starten (systemd ODER root, nie mischen). Storage NIE loeschen (wirft Zerts weg → §1.1/1.2).
- **Versionen:** alle v2.x (Deployment). **gh:** caddy#6347 CLOSED/COMPLETED. **Quelle:** caddyserver.com/docs/running

### 1.7 on-demand-TLS ohne `ask`-Endpoint → Rate-Limit-DoS [⭐]
- **Symptom:** Rate-Limit ploetzlich ausgereizt; viele Zert-Versuche fuer fremde/zufaellige Domains.
- **Ursache:** on-demand-TLS holt Zerts WAEHREND des Handshakes fuer beliebige angefragte Domains. Sobald `ask` gesetzt ist, gelten die eingebauten `max_certs`-Limits NICHT mehr → `ask` ist die EINZIGE Schutzschicht.
- **FIX:** NUR mit `ask`-Endpoint (`on_demand_tls { ask http://localhost:9000/check }`), der erlaubte Domains in **Millisekunden** prueft (konstanter DB-Lookup, keine DNS/Netz-Calls — sonst Timeout = "Caddy macht nichts"). Nie ungeschuetzt exponieren.
- **Versionen:** alle v2.x (per Design). **Quelle:** caddyserver.com/docs/automatic-https, /caddyfile/directives/tls

### 1.8 ZeroSSL-Fallback & EAB-Account-Fallen
- **Symptom:** Zerts unerwartet von ZeroSSL; ZeroSSL-Fallback scheitert ohne E-Mail.
- **Ursache:** Caddy aktiviert LE UND ZeroSSL; faellt LE aus → ZeroSSL (braucht EAB, das Caddy ab v2.2 auto generiert — aber nur MIT konfigurierter E-Mail).
- **FIX:** `{ email admin@example.com }` global. Nur-LE bewusst: `{ acme_ca https://acme-v02.api.letsencrypt.org/directory }` (deaktiviert ZeroSSL-Fallback).
- **Versionen:** EAB-auto ab v2.2; Fallback alle v2.x. **gh:** caddy#7084 (kein Fallback fuer API-Domains, offen). **Quelle:** caddyserver.com/docs/caddyfile/options

### 1.9 Cert-Cache-Flush bei Config-Reload
- **Symptom:** Nach `caddy reload` kurze Downtime; `reload --force` re-cached Zerts nicht zuverlaessig.
- **FIX:** Reloads bewusst; Version aktuell halten; Storage nie loeschen. **gh:** caddy#5589 (Cache-Flush) CLOSED/COMPLETED, caddy#6789 (`reload --force` recache) OPEN. **Quelle:** github.com/caddyserver/caddy/issues/5589, /6789

### 1.10 ⭐ HTTPS ueber eine IP-Adresse als Site → TLS `internal error` (alert 80), weil kein SNI
- **Symptom:** Caddy-Site `https://10.8.0.1 { tls internal }` startet sauber, Cert wird ausgestellt
  (`certificate obtained successfully`, `issuer:"local"`), aber JEDER Client (curl `-k`, Browser, wget)
  bekommt `HTTP 000` / `tlsv1 alert internal error` (SSL alert number 80). Der TLS-Handshake bricht ab.
- **Ursache:** SNI (Server Name Indication, RFC 6066) wird vom Client **nur fuer Hostnamen** gesendet,
  **nicht fuer IP-Adressen**. Caddy bekommt also einen ClientHello ohne SNI und kann das auf die IP
  ausgestellte interne Cert nicht zuordnen → es bricht den Handshake mit `internal_error` ab. Betrifft
  jedes IP-only-HTTPS (typisch: privates Dashboard hinter WireGuard, kein DNS-Name).
- **FIX (funktionserhaltend):** Im globalen Optionsblock `default_sni <ip>` setzen — Caddy nimmt dann bei
  fehlendem SNI diesen virtuellen Host und serviert das passende Cert:
  ```
  { admin off
    default_sni 10.8.0.1 }
  https://10.8.0.1 { tls internal
    reverse_proxy dashboard:8003 }
  ```
  Danach `docker compose restart caddy` (ein blosses `up -d` laedt eine geaenderte **bind-gemountete**
  Caddyfile NICHT neu — nur Image-/compose-Aenderungen triggern Recreate; Config-Mount braucht restart
  oder `caddy reload`). Alternative: einen echten Hostnamen statt der IP verwenden (dann sendet der
  Client SNI). **Verifiziert 2026-06-24** (second-brain-Cockpit, Mikrofon-secure-context-Setup).

---

## 2. Caddy `reverse_proxy`-Mechanik & Caddyfile-Fallen

### 2.1 Upstream beim Start nicht erreichbar → 502 [⭐ HAEUFIG, betrifft den Stack]
- **Symptom:** Caddy startet sauber, jeder Request `502`; Log `dial tcp 127.0.0.1:PORT: connect: connection refused`. Direktzugriff geht.
- **Ursache (per Design):** Caddy prueft Upstreams NICHT beim Start, verbindet erst pro Request. Laeuft der Dienst dann nicht (Caddy vor App-Dienst, oder 10.8.0.1/wg0 noch nicht oben) → 502.
- **FIX:** Reihenfolge ueber systemd `After=/Wants=` der Unit (nicht Caddy aendern); Caddy braucht keinen Neustart, naechster Request gelingt automatisch. Optional passive Health-Checks `fail_duration 30s` + `max_fails`. Bei WireGuard: wg0 vor dem Upstream-Dienst sicherstellen.
- **Versionen:** alle v2.x (gewollt). **Quelle:** caddyserver.com/docs/caddyfile/directives/reverse_proxy

### 2.2 Sporadische 502 unter Last: Keepalive-Mismatch — #6452 GEFIXT
- **Symptom:** Sporadische 502 bei Bursts; Log `connection reset by peer`; Dienst gesund.
- **Ursache:** Caddy haelt Keepalive 2 min; viele Upstreams schliessen frueher (Node 5s) → Caddy nutzt eine bereits geschlossene Verbindung.
- **FIX:** `reverse_proxy 127.0.0.1:PORT { transport http { keepalive 3s } }` (< Upstream-Timeout) ODER Upstream-Timeout erhoehen.
- **Versionen:** v2.x. **gh:** caddy#6452 CLOSED/COMPLETED. **Quelle:** github.com/caddyserver/caddy/issues/6452

### 2.3 `handle` statt `handle_path`: voller Pfad ans Backend — #3675 GEFIXT/dokumentiert
- **Symptom:** Subpath `/app/` → 404/falsche Assets; Backend sieht `/app/xyz` statt `/xyz`.
- **Ursache:** `handle /app/*` strippt den Prefix NICHT.
- **FIX:** `handle_path /app/* { reverse_proxy ... }` (= `handle` + `uri strip_prefix /app`). Fallen: `handle_path` nur EIN Matcher, keine named matcher; `*` benutzen; Backend ggf. ueber Subpath informieren (`X-Forwarded-Prefix`/Base-URL).
- **Versionen:** v2.x (per Design). **Quelle:** caddyserver.com/docs/caddyfile/directives/handle_path

### 2.4 Direktiven-Reihenfolge wird "ignoriert" (Auto-Sortierung)
- **Symptom:** `redir`/`rewrite` feuert in falscher Reihenfolge.
- **Ursache (per Design):** Caddyfile sortiert Direktiven automatisch (laengster Pfad-Matcher zuerst), NICHT nach Schreibreihenfolge.
- **FIX:** `route { ... }` erhaelt die Schreibreihenfolge.
- **Versionen:** v2.x. **Quelle:** caddyserver.com/docs/caddyfile/directives

### 2.5 HTTPS-Upstream mit self-signed Cert → 502
- **Symptom:** Proxy zu HTTPS-Backend (selbst-signiert) bricht mit 502 (TLS-Verify).
- **FIX:** `reverse_proxy https://10.8.0.1:PORT { transport http { tls_insecure_skip_verify } }` (Doku: "not in production" — fuer interne WG-/localhost-Upstreams vertretbar; sauberer `tls_trust_pool`). **Ab v2.11.0** setzt Caddy den Host-Header zu HTTPS-Upstreams automatisch (vorher manuell, sonst SNI-Mismatch).
- **Versionen:** Host-auto ab v2.11.0. **Quelle:** caddyserver.com/docs/caddyfile/directives/reverse_proxy

### 2.6 `lb_try_duration` + `lb_retries 0` → CPU-Spin
- **Symptom:** Hohe CPU wenn alle Upstreams down.
- **FIX:** `lb_try_duration` moderat (Start 5s, `dial_timeout` Default 3s); `lb_try_interval` (250ms) nicht auf 0; bei Single-Upstream `lb_try_duration` weglassen.
- **Versionen:** v2.x (Doku-Caveat). **Quelle:** caddyserver.com/docs/caddyfile/directives/reverse_proxy

### 2.7 Kein `response_header_timeout` per Default → haengende Requests
- **FIX:** `transport http { dial_timeout 5s; response_header_timeout 30s }` (Default: dial 3s, aber response_header_timeout = kein Timeout).
- **Versionen:** v2.x (per Design). **Quelle:** caddyserver.com/docs/caddyfile/directives/reverse_proxy

### 2.8 Unix-Socket-Upstream: Adressformat/Host
- **FIX:** HTTP `reverse_proxy unix//var/php.sock`; gRPC/h2c `unix+h2c//var/grpc.sock`; bei TLS+Socket `header_up Host <name>` explizit.
- **Versionen:** v2.x. **Quelle:** caddyserver.com/docs/caddyfile/directives/reverse_proxy

### 2.9 `caddy reload` missverstanden (Restart statt Reload)
- **Fakt:** `caddy reload` tauscht die Config ohne Downtime atomar; bei Fehler **automatischer Rollback** auf die alte Config (Site bleibt oben). Restart ist unnoetig/riskanter.
- **FIX:** `caddy validate --config /etc/caddy/Caddyfile` → dann `caddy reload` / `systemctl reload caddy`. `validate` provisioniert probeweise (faengt Lade-Fehler vorab).
- **Versionen:** v2.x. **Quelle:** caddyserver.com/docs/command-line

### 2.10 Admin-API (`localhost:2019`) unauthentifiziert → SSRF — #5815 GEFIXT/dokumentiert
- **Symptom:** Unauth Zugriff auf die Admin-API; bei SSRF-Luecke kann die Config manipuliert werden.
- **Ursache (per Design):** Admin-API lauscht default `localhost:2019` ohne Auth.
- **FIX:** NIE auf `0.0.0.0`/extern binden. Bei untrusted lokalem Code: `{ admin unix//var/run/caddy.sock }` bzw. `CADDY_ADMIN=unix//...`; komplett aus: `admin off` (kein Live-Reload mehr).
- **Versionen:** v2.x. **gh:** caddy#5815 CLOSED/COMPLETED. **Quelle:** caddyserver.com/docs/caddyfile/options

---

## 3. Streaming / SSE / WebSocket (MCP-relevant)

> **Grundregel SSE/MCP hinter Caddy:** `flush_interval -1` setzen UND `encode` auf der Streaming-Route NICHT anwenden. Beides zusammen loest >90 % der Faelle.

### 3.1 SSE-Antworten werden gepuffert (Events erst am Ende) — #4247 GEFIXT
- **Symptom:** SSE/MCP-Events kommen gebuendelt am Ende; kurze Streams scheinen leer bis zum Verbindungsende.
- **Ursache:** Response-Buffer wird nicht sofort geflusht; sendet der Upstream nur Header (bei SSE valide), gilt der Handshake als nicht etabliert.
- **FIX:** `reverse_proxy localhost:8000 { flush_interval -1 }`. Caddy flusht zwar `Content-Type: text/event-stream` / `Content-Length: -1` automatisch — `flush_interval -1` ist die explizite Absicherung.
- **Versionen:** per Design, `flush_interval` seit v2.x. **gh:** caddy#4247 CLOSED/COMPLETED. **Quelle:** caddyserver.com/docs/caddyfile/directives/reverse_proxy

### 3.2 `encode gzip`/`zstd` bricht SSE — #6293 OFFEN [⭐ KRITISCH fuer MCP]
- **Symptom:** SSE/MCP funktioniert ohne Kompression, bricht/haengt sobald `encode gzip` global aktiv ist.
- **Ursache:** `encode` verzoegert den Flush bis zu den ersten Body-Bytes und sein Framing passt nicht zum SSE-Sofort-Flush; `flush_interval` wird effektiv ausgehebelt.
- **FIX:** `encode` per Matcher von der SSE-Route ausnehmen (Kompression bleibt fuer statische Routen):
  ```caddyfile
  @notstream not path /mcp* /sse* /events*
  encode @notstream gzip zstd
  @stream path /mcp* /sse* /events*
  reverse_proxy @stream localhost:8000 { flush_interval -1 }
  ```
- **Versionen:** v2.x. **gh:** caddy#6293 **OPEN** (Fix diskutiert) → Workaround bleibt aktiv. **Quelle:** github.com/caddyserver/caddy/issues/6293

### 3.3 WebSocket schliesst nach ~9-10 s (KEIN Caddy-Bug)
- **Ursache:** Netzwerkpfad (WireGuard)/Upstream-Idle, NICHT der Caddy-Default (5 min). #6958 als nicht-Caddy-Bug geschlossen.
- **FIX:** Upstream-Read/Idle-Timeout hoch; App-seitige Keep-Alive-Pings (WS Ping/Pong bzw. SSE `: keep-alive\n\n` alle 15-30 s); NAT/VPN-Idle beachten.
- **Versionen:** umgebungsbedingt. **Quelle:** github.com/caddyserver/caddy/issues/6958

### 3.4 WS/SSE bricht bei jedem `caddy reload` — #6420 NOT_PLANNED (per Design)
- **Ursache:** Caddy schliesst beim Reload alle Streaming-Verbindungen (jede haelt eine Referenz auf die alte Config).
- **FIX:** `stream_close_delay 30s` (Karenzzeit) + client-seitiger Reconnect (SSE `EventSource` reconnectet automatisch).
- **Versionen:** v2.x. **gh:** caddy#6420 CLOSED/NOT_PLANNED. **Quelle:** github.com/caddyserver/caddy/issues/6420

### 3.5 `stream_timeout` kappt langlebige WS/SSE
- **FIX:** `stream_timeout` weglassen (Default unbegrenzt) ODER bewusst hoch (`24h`). NICHT mit Keep-Alive-`idle` (5 min, betrifft normale Requests) verwechseln.
- **Versionen:** v2.x. **Quelle:** caddyserver.com/docs/caddyfile/directives/reverse_proxy

### 3.6 HTTP/2- / HTTP/3-Streaming-Fallen (WS) — #6733/#7309 GEFIXT, #6799 Migrationsfalle
- **Symptom:** WS ueber HTTP/1.1 ok, bricht mit HTTP/2/3; h2-WS haengt wenn Client zuerst sendet + `encode` aktiv.
- **FIX:** Auf WS-Routen `encode` deaktivieren; bei anhaltenden Problemen HTTP/1.1 fuer WS erzwingen. **Vor Caddy-Upgrade (z.B. 2.8→2.9) WS/SSE gegentesten** (#6799 brach manche WS-Setups).
- **Versionen:** v2.x. **gh:** caddy#6733/#7309 CLOSED/COMPLETED, #6799 (2.8.4→2.9.1), #5565 (RFC8441). **Quelle:** github.com/caddyserver/caddy/issues/6733

### 3.7 `request_buffers`/`response_buffers` puffern den Stream
- **FIX:** Diese opt-in-Direktiven auf Streaming-Routen NICHT setzen (kein Default-Buffering); falls vorhanden, entfernen.
- **Versionen:** v2.x. **Quelle:** caddyserver.com/docs/caddyfile/directives/reverse_proxy

> **nginx-Referenz** (falls statt Caddy): `proxy_buffering off` + `proxy_http_version 1.1` + `proxy_set_header Connection ""` + `proxy_read_timeout 3600s`; alternativ Upstream-Header `X-Accel-Buffering: no`. Caddy puffert Streaming per Default NICHT (erkennt `text/event-stream`), nginx schon.

---

## 4. Header- / Host- / IP-Weiterleitung (Caddy + FastAPI/uvicorn)

### 4.1 Backend sieht Proxy-IP statt Client-IP (`trusted_proxies` fehlt) [⭐]
- **Symptom:** Access-Log/Rate-Limiter/fail2ban sehen Caddy-IP (127.0.0.1/Docker-Bridge); `{client_ip}` falsch.
- **Ursache:** Ohne `trusted_proxies` ignoriert Caddy eingehende `X-Forwarded-*` (Spoofing-Schutz) und parst keine echte Client-IP → `{client_ip}` = direkter TCP-Peer.
- **FIX:** Global setzen: `{ servers { trusted_proxies static private_ranges } }`. Im Backend echte IP via `header_up X-Real-IP {client_ip}` lesen.
- **Versionen:** alle v2.x; `private_ranges` seit v2.7. **Quelle:** caddyserver.com/docs/caddyfile/options

### 4.2 Vhost-404: Host-Header wird durchgereicht — #993 GEFIXT (v2.11 auto)
- **Symptom:** Upstream liefert 404/falsche Vhost-Seite/Cert-Fehler.
- **Ursache:** Caddy reicht Original-Client-`Host` weiter; routet der Upstream nach Host, passt es nicht.
- **FIX:** `reverse_proxy https://app.intern { header_up Host {upstream_hostport} }`. Gegenrichtung: braucht der Upstream den ORIGINAL-Host (OAuth-Redirects), NICHT ueberschreiben. Ab v2.11 fuer HTTPS-Upstreams automatisch.
- **Versionen:** HTTP-Upstreams weiter manuell; HTTPS auto ab v2.11. **gh:** caddy#993 CLOSED/COMPLETED. **Quelle:** github.com/caddyserver/caddy/issues/993

### 4.3 v2.11-Warnung bei manuellem `header_up Host` (HTTPS-Upstream) — #7584 GEFIXT
- **FIX:** Wenn Auto-Verhalten passt: die explizite `header_up Host`-Zeile entfernen (Warnung weg). Braucht man bewusst einen abweichenden Host: behalten (Warnung ignorierbar).
- **Versionen:** ab v2.11.0. **gh:** caddy#7584 CLOSED/COMPLETED. **Quelle:** github.com/caddyserver/caddy/issues/7584

### 4.4 HTTPS-Redirect-Loop (`ERR_TOO_MANY_REDIRECTS`) [⭐ FastAPI]
- **Symptom:** Endlos-Redirect http↔https; `__Host-`/Secure-Cookies werden verworfen.
- **Ursache:** Caddy sendet `X-Forwarded-Proto: https` korrekt, aber uvicorn vertraut Proxy-Header nur von `127.0.0.1` (`--forwarded-allow-ips`-Default). Aus Docker-Bridge/anderem Host ignoriert uvicorn den Header → App glaubt http → Redirect-Loop.
- **FIX:** `uvicorn app:app --proxy-headers --forwarded-allow-ips="<caddy-ip-oder-subnetz>"` (lokal `127.0.0.1`; NIE `*` bei oeffentlichem Port). In `route`/`handle`-Bloecken keine Forwarding-Header strippen.
- **Versionen:** uvicorn aktuell (Default restriktiv); Caddy XFP seit v2.0. **Quelle:** uvicorn.dev/settings, fastapi.tiangolo.com/advanced/behind-a-proxy

### 4.5 Hinter Cloudflare: echte IP verloren (mehrere `trusted_proxies`)
- **Ursache:** Mehrere separate `trusted_proxies`-Direktiven ueberschreiben sich (nur letzte gilt); CF schuetzt `X-Forwarded-For` nicht vor Spoofing (nur `CF-Connecting-IP`).
- **FIX:** `trusted_proxies combine { cloudflare; static private_ranges }` + `client_ip_headers Cf-Connecting-Ip X-Forwarded-For` + `trusted_proxies_strict`. (`cloudflare`-Modul = custom build.)
- **Versionen:** v2.x. **Quelle:** caddy.community/t/cloudflare-proxy-x-forwarded-for-client-ip-issue/25154

### 4.6 Ganzer XFF wird an Upstream weitergereicht — #6783 OFFEN (kein echter Bug)
- **Ursache (per Design):** `trusted_proxies` steuert nur, welche IP Caddy als `{client_ip}` PARST; der XFF-Header geht augmentiert (inkl. untrusted IPs links) an den Upstream.
- **FIX:** Im Backend NICHT die linkste XFF-IP nehmen, sondern `header_up X-Real-IP {client_ip}` setzen und nur diesen lesen.
- **Versionen:** v2.x. **gh:** caddy#6783 OPEN. **Quelle:** github.com/caddyserver/caddy/issues/6783

### 4.7 Secure-/`__Host-`-Cookie verworfen + CORS
- **FIX:** §4.4-Fix (App erkennt https) → Cookie mit `secure=True, httponly=True`; `__Host-` ohne `Domain`, `Path=/`. CORS mit Credentials: `allow_credentials=True` UND explizite `allow_origins` (kein `*`).
- **Versionen:** Starlette/FastAPI + Browser-Spec. **Quelle:** fastapi.tiangolo.com/tutorial/cors

### 4.8 Forwarding-Header fehlen in `route`/`handle`-Bloecken
- **Ursache:** Caddy setzt X-Forwarded-* nur bei direktem `reverse_proxy` automatisch; in verschachtelten Bloecken mit Header-Manipulation kann das ausgehebelt werden.
- **FIX:** Keine Forwarding-Header strippen; bei Bedarf `header_up X-Forwarded-Proto {scheme}` / `X-Forwarded-Host {host}` explizit.
- **Versionen:** v2.x. **Quelle:** caddyserver.com/docs/caddyfile/directives/reverse_proxy

---

## 5. systemd-Unit-Betrieb

### 5.1 `After=network.target` reicht NICHT (IP noch nicht da) [⭐ HAEUFIG]
- **Symptom:** Dienst failt beim Boot (Bind/Routing), laeuft nach manuellem `start`.
- **Ursache:** `network.target` = Netzwerk-Stack hoch, NICHT "Interface hat routbare IP".
- **FIX:** `[Unit] After=network-online.target` + `Wants=network-online.target` (Wait-Dienst `systemd-networkd-wait-online`/`NetworkManager-wait-online` aktiv).
- **Versionen:** per Design, alle (255). **Quelle:** systemd.io/NETWORK_ONLINE

### 5.2 Dienst an WireGuard-IP (10.8.0.1) failt beim Boot [⭐ betrifft den Stack]
- **Symptom:** "Cannot assign requested address" am Boot; nach `restart` (wg0 oben) ok. Auch: wg-quick bekommt Adresse erst nach Restart (Race).
- **FIX (kombinierbar):** (1) `[Unit] After=wg-quick@wg0.service network-online.target` + `Wants=` (NICHT `Requires`/`BindsTo` ungeprueft — `BindsTo` nur wenn Dienst mit wg0 leben/sterben soll); (2) an nicht-existente IP binden erlauben: `FreeBind=yes` bzw. `sysctl net.ipv4.ip_nonlocal_bind=1` (umgeht den Race ganz).
- **`After`/`Wants`/`Requires`/`BindsTo`/`PartOf`:** After=nur Ordnung; Wants=schwach; Requires=hart (+After noetig fuer Ordnung); BindsTo=stoppt Dienst wenn Ziel zur Laufzeit verschwindet; PartOf=propagiert nur stop/restart.
- **Versionen:** per Design. **Quelle:** ivpn.net/knowledgebase/linux/linux-autostart-wireguard-in-systemd

### 5.3 Dienst startet NICHT nach Reboot (nur `start`ed, nicht `enable`d)
- **FIX:** `[Install] WantedBy=multi-user.target` + `systemctl enable --now <unit>`; pruefen `systemctl is-enabled`. Bei geaenderter `WantedBy=`: `disable` → `daemon-reload` → `enable`. User-Units: `WantedBy=default.target` + `loginctl enable-linger <user>`.
- **Versionen:** per Design (255). **Quelle:** oneuptime.com/blog/post/2026-01-24-systemd-failed-to-start-service/view

### 5.4 "Start request repeated too quickly" → Dienst gibt auf — #30804 OFFEN
- **Ursache:** `Restart=always` ohne `RestartSec` → Crash-Loop reisst `StartLimitBurst` (Default 5/`StartLimitIntervalSec` 10s) → dauerhaft `failed`.
- **FIX:** `[Service] Restart=on-failure; RestartSec=5s` + `[Unit] StartLimitIntervalSec=60s; StartLimitBurst=10` (StartLimit* gehoeren in `[Unit]`!). Root-Cause via `journalctl -u` fixen; Reset `systemctl reset-failed`.
- **Versionen:** per Design. **gh:** systemd#30804 OPEN (Warn-Vorschlag). **Quelle:** github.com/systemd/systemd/issues/30804

### 5.5 Drop-in/Override greift nicht / "more than one ExecStart"
- **Ursache:** (a) handeditiert ohne `daemon-reload`; (b) `ExecStart=`-Override ohne leere Reset-Zeile → systemd haengt an (Mehrfach-ExecStart nur bei `Type=oneshot`).
- **FIX:** `systemctl edit <unit>` (reloadt selbst) → `/etc/systemd/system/<unit>.d/override.conf`; bei ExecStart: erst `ExecStart=` (leer), dann neue Zeile. Pruefen `systemctl cat <unit>`.
- **Versionen:** per Design (255). **Quelle:** baeldung.com/linux/systemd-modify-config

### 5.6 `Type=`-Verwechslung → Dienst gilt faelschlich tot/lebendig
- **Ursache:** `Type=simple` (Default) gilt sofort als up; `Type=forking` ohne `PIDFile=` → systemd findet Hauptprozess nicht.
- **FIX:** fork-Daemon: `Type=forking` + `PIDFile=`; Vordergrund: `Type=simple`/`exec`; readiness: `Type=notify` + `sd_notify READY=1`. Doku rät von `forking` ab.
- **Versionen:** per Design; `Type=exec` seit 240. **Quelle:** man7.org/linux/man-pages/man5/systemd.service.5.html

### 5.7 journald frisst die Disk (`/var/log/journal` ohne Limit)
- **Ursache:** Default 10% des FS (max 4 GB); bei voller Disk raeumt journald NICHT auf, schreibt nur nichts mehr.
- **FIX:** `/etc/systemd/journald.conf.d/size.conf`: `[Journal] SystemMaxUse=500M`, `SystemKeepFree=1G`, `SystemMaxFileSize=50M`, `MaxRetentionSec=2week` → `systemctl restart systemd-journald`. Akut: `journalctl --vacuum-size=500M`.
- **Versionen:** per Design (255). **Quelle:** freedesktop.org/software/systemd/man/latest/journald.conf.html

### 5.8 Dienst braucht ein Mount, das beim Start fehlt
- **FIX:** `[Unit] RequiresMountsFor=/srv/data` (erzeugt Requirement+Ordering zur `*.mount`); Mount-Unit muss existieren (aus fstab generiert).
- **Versionen:** per Design (255). **Quelle:** freedesktop.org/software/systemd/man/latest/systemd.mount.html

### 5.9 Env-Variablen falsch/leer (Quoting) — #36488 GEFIXT/dokumentiert
- **Ursache:** `EnvironmentFile=` ohne Shell-Verarbeitung; Quotes (`"`/`\`) teils still entfernt; keine Command-Substitution.
- **FIX:** `EnvironmentFile=` Format `KEY=value`, quoten nur bei Leerzeichen, keine `$(...)`. `Environment="KEY=wert mit space"`. Pruefen `systemctl show -p Environment <unit>`.
- **Versionen:** langjaehrige Quirks, auch 255. **gh:** systemd#36488 CLOSED/COMPLETED (dokumentiert). **Quelle:** github.com/systemd/systemd/issues/36488

---

## 6. Linux-VPS-Ops (Auto-Updates, Swap, Disk, NTP)

### 6.1 unattended-upgrades + needrestart: ungewollter Dienst-Neustart (24.04) [⭐] — needrestart#270 GEFIXT
- **Symptom:** Nach Auto-Update ist ein Dienst (nginx) tot/neugestartet; "failed to restart while update applied".
- **Ursache:** 24.04: `needrestart` loest nach Library-Updates (typ. glibc) Auto-Restarts via `restart.d/systemd-manager` aus; scheitert der Config-Check beim Restart (z.B. `nginx -t` an temporaerem DNS-Fail), bleibt der Dienst unten. Aggressiver als 22.04.
- **FIX:** `/etc/needrestart/needrestart.conf`: `$nrconf{restart}='l'` (nur listen) und `$nrconf{kernelhints}=-1`; kritischen Dienst per `override_rc`/Blacklist ausnehmen ODER `Unattended-Upgrade::Package-Blacklist` fuers ausloesende Paket. (Der Stack hat unattended-upgrades aktiv.)
- **Versionen:** ab 24.04. **gh:** needrestart#270 CLOSED/COMPLETED. **Quelle:** discourse.ubuntu.com/t/.../67909, github.com/liske/needrestart/issues/270

### 6.2 Auto-Upgrade haengt an interaktivem Prompt
- **FIX:** needrestart non-interaktiv (s. 6.1); `/etc/apt/apt.conf.d/50unattended-upgrades` → `Dpkg::Options { "--force-confdef"; "--force-confold"; }` (`confold` behaelt manuell geaenderte Configs).
- **Versionen:** per Design. **Quelle:** github.com/mvo5/unattended-upgrades/blob/master/README.md

### 6.3 Automatischer Reboot mitten im Betrieb
- **FIX:** `Unattended-Upgrade::Automatic-Reboot "false";` (Default false; Cloud-Images weichen ab) ODER feste `Automatic-Reboot-Time "03:00";` + `Automatic-Reboot-WithUsers "false";`. Reboot-Bedarf: `/var/run/reboot-required`.
- **Versionen:** konfigurierbar. **Quelle:** github.com/mvo5/unattended-upgrades

### 6.4 OOM-Killer killt Dienste — KEIN Swap [⭐ AKUT: Server hat 0 B Swap]
- **Symptom:** Bei Speicherspitze killt der OOM-Killer einen Dienst; `dmesg | grep -i oom` "Killed process".
- **Ursache:** 0 B Swap → kein Puffer bei voller RAM → sofort OOM.
- **FIX:** `fallocate -l 4G /swapfile; chmod 600 /swapfile; mkswap /swapfile; swapon /swapfile; echo '/swapfile none swap sw 0 0' >> /etc/fstab`. `vm.swappiness=10` (in `/etc/sysctl.d/99-swap.conf`; NICHT 0 — `=0` swapt erst bei voller RAM → Slowdown/OOM). zram als RAM-only-Alternative.
- **Versionen:** Konfiguration. **Quelle:** digitalocean.com/community/tutorials/how-to-add-swap-space-on-ubuntu-20-04

### 6.5 `/boot` voll → `apt upgrade` scheitert
- **Ursache:** Alte Kernel-Images akkumulieren in der kleinen `/boot`-Partition.
- **FIX:** `apt autoremove --purge` (laesst aktiven + Fallback); blockiert APT: aktiven Kernel `uname -r` ermitteln (NIE loeschen), alte gezielt `apt remove --purge linux-image-X` + `apt-get -f install`. Praevention: `COMPRESS=xz` in initramfs.conf.
- **Versionen:** per Design. **Quelle:** help.ubuntu.com/community/RemoveOldKernels

### 6.6 inode-Erschoepfung: Platz da, Schreiben scheitert
- **Symptom:** "No space left" obwohl `df -h` Platz zeigt.
- **FIX:** IMMER auch `df -i` (IUse% 100%); Verursacher `du -sh --inodes /pfad/*`; Kleinst-Dateien (Sessions/Caches/Queue) aufraeumen; logrotate. ext4 hat fixe inode-Zahl ab Format.
- **Versionen:** ext4-Design. **Quelle:** penguin-gym-linux.com/en/articles/troubleshooting/inode-exhaustion

### 6.7 Zeit/NTP-Drift bricht TLS/ACME/JWT
- **Symptom:** "certificate not yet valid"/"expired"; ACME-Renewal scheitert; JWT abgelehnt. Nach VM-Suspend/Migration.
- **FIX:** `timedatectl` pruefen ("synchronized: yes"); `timedatectl set-ntp true`. **Fuer VMs/Post-Suspend-Skew chrony** (`apt install chrony`, deaktiviert timesyncd). Akut stark verstellt: einmal hart setzen, dann sync. (Server-Live-Check: NTP aktiv/synchron — gut.)
- **Versionen:** Konfiguration. **Quelle:** ubuntu.com/server/docs/how-to/networking/chrony-client

### 6.8 Phased Updates "kept back"/"deferred" (KEIN Bug)
- **FIX:** Nichts tun, warten (kommt automatisch in der Rollout-Welle). Security-Updates sind NIE phased. Nur bei Bedarf gezielt `apt install <paket>` (nicht pauschal erzwingen).
- **Versionen:** per Design. **Quelle:** ubuntu.com/server/docs/explanation/software/about-apt-upgrade-and-phased-updates

### 6.9 Disk-Fueller: /var/log, /var/cache/apt, /tmp
- **FIX:** journald `SystemMaxUse=` (s. 5.7); `apt-get clean`/`autoclean` + u-u `Remove-Unused-Dependencies "true"`/`Remove-Unused-Kernel-Packages "true"`; `/tmp` via `systemd-tmpfiles` (Achtung: kann tmpfs/RAM sein).
- **Versionen:** Konfiguration. **Quelle:** github.com/mvo5/unattended-upgrades

---

## 7. Ops-Sicherheit (fail2ban / UFW / SSH)

> **Goldene Regel bei ALLEM Hardening: IMMER eine zweite SSH-Session offen halten ODER Rescue/VNC-Konsole bereit, bevor sshd/ufw/fail2ban neu geladen wird.**

### 7.1 `ufw enable` sperrt die aktive SSH-Verbindung aus [⭐]
- **Ursache (per Design):** UFW default `deny incoming` greift sofort; ohne vorherige SSH-Regel wird der eigene Zugang gedroppt.
- **FIX:** `sudo ufw allow OpenSSH` (bzw. eigener Port) ZUERST, dann `sudo ufw enable`. Jedes Mal. Recovery nur ueber Provider-VNC.
- **Versionen:** alle (0.36.2). **Quelle:** linuxize.com/post/how-to-setup-a-firewall-with-ufw-on-ubuntu-24-04

### 7.2 fail2ban wirkungslos hinter Caddy/Docker (Proxy-IP) [⭐ betrifft den Stack]
- **Ursache (drei verflochten):** (1) Backend loggt Proxy-IP 127.0.0.1 (XFF nicht ausgewertet); (2) Docker-Ports laufen ueber `DOCKER-USER`, NICHT `INPUT` (wo fail2ban default droppt); (3) 24.04 default nftables vs Docker-iptables = getrennte Regelsaetze.
- **FIX:** Caddy `header_up X-Real-IP {remote_host}` + Backend loggt echte IP; banaction `iptables-multiport[chain="DOCKER-USER", ...]`; Backend nicht mischen; verifizieren `iptables -L DOCKER-USER -n -v | grep DROP` (Counter >0). **SSH-Jail bleibt auf `INPUT` korrekt** (sshd laeuft nicht durch Docker).
- **Versionen:** per Design, fail2ban 1.0.2. **Quelle:** serverspan.com/.../fixing-fail2ban-...-behind-caddy, vaarlion.com/blog/how-to-use-fail2ban-when-there-is-a-proxy-in-the-way

### 7.3 Selbst-Aussperrung (fail2ban bannt eigene IP)
- **FIX:** `/etc/fail2ban/jail.local` `[DEFAULT] ignoreip = 127.0.0.1/8 ::1 <eigene-IP/CIDR>`. Recovery von 2. IP: `fail2ban-client set sshd unbanip <IP>` UND `set recidive unbanip <IP>` (jedes Jail einzeln; recidive hat lange bantime).
- **Versionen:** per Design. **Quelle:** spinupwp.com/doc/how-to-unban-and-whitelist-ip-addresses-in-fail2ban

### 7.4 sshd ignoriert den Key STILL (StrictModes/Permissions)
- **Symptom:** Key-Login scheitert; Auth-Log "bad ownership or modes".
- **FIX:** `chmod go-w ~/`; `chmod 700 ~/.ssh`; `chmod 600 ~/.ssh/authorized_keys`; `chown -R $USER:$USER ~/.ssh`. `StrictModes off` ist der FALSCHE Weg.
- **Versionen:** alle OpenSSH. **Quelle:** cyberciti.biz/faq/openssh-server-authentication-refused-bad-ownership-or-modes

### 7.5 `PasswordAuthentication no` greift nicht — cloud-init-Drop-in (24.04) [⭐]
- **Ursache:** `/etc/ssh/sshd_config.d/*.conf` (z.B. `50-cloud-init.conf`) erzwingt `PasswordAuthentication yes`; Drop-ins werden zuerst geladen und gewinnen (erster Wert gilt).
- **FIX:** Autoritativ pruefen `sudo sshd -T | grep -E 'passwordauthentication|permitrootlogin'`; wo gesetzt `grep -R PasswordAuthentication /etc/ssh/sshd_config*`; eigenen `99-hardening.conf` ODER den cloud-init-Wert korrigieren (nur EIN aktiver Wert je Direktive). `sshd -t && systemctl reload ssh` (2. Session offen).
- **Versionen:** Ubuntu 22.04/24.04 cloud-init. **Quelle:** progressiverobot.com/.../ubuntu-24-04-lts-ssh-password-authentication-suddenly-disabled

### 7.6 fail2ban bannt 0 IPs auf 24.04 (journald-Backend) — #3292 GEFIXT
- **Ursache:** sshd loggt ins Journal, nicht `/var/log/auth.log`; `backend = auto` ohne rsyslog matcht nichts.
- **FIX:** `/etc/fail2ban/jail.local` `[DEFAULT] backend = systemd`; SSH `LogLevel` mind. INFO.
- **Versionen:** 22.04/24.04 journald-only. **gh:** fail2ban#3292 CLOSED/COMPLETED. **Quelle:** github.com/fail2ban/fail2ban/issues/3292

### 7.7 fail2ban-Ban wirkungslos: UFW-allow VOR Ban-Regel
- **Ursache:** Steht UFW-`allow` vor der fail2ban-DROP-Regel, gewinnt das allow.
- **FIX:** banaction `ufw` (setzt per `ufw insert 1 deny` an Position 1); Ketten-Reihenfolge `iptables -L -n` pruefen.
- **Versionen:** per Design. **Quelle:** github.com/fail2ban/fail2ban/discussions/3781

### 7.8 nftables/iptables-Backend-Mismatch ("iptables: not found")
- **Ursache:** 24.04 default banaction nftables, Setup erwartet teils iptables; Mischbetrieb (Docker=iptables, f2b=nftables) = zwei Regelsaetze.
- **FIX:** banaction durchgaengig an das tatsaechliche Backend (`fail2ban-client get sshd banaction`); nicht mischen.
- **Versionen:** 24.04, fail2ban 1.0.2. **Quelle:** dev1galaxy.org/viewtopic.php?id=5605

### 7.9 sshd-Config-Syntaxfehler bricht sshd beim Restart
- **FIX (Pflicht-Ablauf):** `sudo sshd -t` (Syntax-Test) VOR `systemctl reload ssh` (reload trennt Session nicht, restart schon). Zweite Session offen halten. Port-Wechsel: ZUERST `ufw allow <port>/tcp`. `AllowUsers`/`AllowGroups` exakt (`sshd -T | grep allow`) — ein Tippfehler sperrt alle aus.
- **Versionen:** alle OpenSSH. **Quelle:** cherryservers.com/knowledge/docs/compute/how-to/enable-ssh-authentication-on-linux

### 7.10 `ufw reset` loescht ALLES (inkl. SSH-Regel)
- **FIX:** Nach jedem `ufw reset` SOFORT `ufw allow OpenSSH` + `ufw enable`. Backup unter `/etc/ufw/*.rules.<datum>`.
- **Versionen:** per Design (0.36.2). **Quelle:** linuxize.com/post/how-to-list-and-delete-ufw-firewall-rules

### 7.11 UFW: IPv6-Regeln separat
- **Symptom:** Regel per Nummer geloescht, Dienst ueber IPv6 weiter erreichbar.
- **FIX:** `ufw status numbered` (zeigt v4 + v6 mit `(v6)`); beide Nummern einzeln loeschen (von unten nach oben). Kein IPv6: `/etc/default/ufw IPV6=no` (bei oeffentlichem v6-VPS lieber absichern statt abschalten).
- **Versionen:** per Design (0.36.2). **Quelle:** linuxize.com/post/how-to-list-and-delete-ufw-firewall-rules

> **Querverweis:** Docker umgeht UFW komplett (published Ports nicht in `ufw status`, aber offen) → Detail in `server/docker.md` §1; Stichwort `DOCKER-USER` + `-p 127.0.0.1:PORT:PORT`/Host-IP-Bind.

---

## Fix-Status (hart per `gh` am 2026-06-24)

| Frueherer Bug | Issue | gh-Status | Bezug |
|---------------|-------|-----------|-------|
| Cert-Cache-Flush bei Config-Load | caddy#5589 | CLOSED COMPLETED | 1.9 |
| Permission denied fresh install | caddy#6347 | CLOSED COMPLETED | 1.6 |
| Keepalive-502 | caddy#6452 | CLOSED COMPLETED | 2.2 |
| handle_path Prefix-Strip | caddy#3675 | CLOSED COMPLETED | 2.3 |
| Admin-API-Sicherheit | caddy#5815 | CLOSED COMPLETED | 2.10 |
| SSE-Flush | caddy#4247 | CLOSED COMPLETED | 3.1 |
| h2-WS + encode / http2-3-WS | caddy#6733 / #7309 | CLOSED COMPLETED | 3.6 |
| Host-Header HTTPS-Upstream / v2.11-Warnung | caddy#993 / #7584 | CLOSED COMPLETED | 4.2 / 4.3 |
| Env-Quoting dokumentiert | systemd#36488 | CLOSED COMPLETED | 5.9 |
| needrestart auto-restart | needrestart#270 | CLOSED COMPLETED | 6.1 |
| fail2ban journald-Backend | fail2ban#3292 | CLOSED COMPLETED | 7.6 |

### Noch NICHT gefixt (Workaround bleibt aktiv)
- **caddy#6293** (OPEN) — `encode` bricht SSE → §3.2-Workaround (encode von SSE-Route ausnehmen) bleibt.
- **caddy#6789** (OPEN) — `reload --force` re-cached Zerts nicht.
- **caddy#6783** (OPEN) — ganzer XFF an Upstream → `X-Real-IP {client_ip}` lesen (§4.6).
- **caddy#6420** (NOT_PLANNED) — WS/SSE-Reload-Kill per Design → `stream_close_delay` (§3.4).
- **caddy#7084** — kein ZeroSSL-Fallback fuer API-Domains.
- **systemd#30804** (OPEN) — keine Warnung vor Restart-Loop → StartLimit selbst setzen (§5.4).
- **Alle "per Design"-Fallen** (UFW-Bypass/enable-Aussperrung, network.target, Caddy-kein-Start-Check, on-demand ohne ask, 0-Swap-OOM, NTP-Drift, cloud-init-Drop-in, Direktiven-Sortierung) bleiben dauerhaft — der "Fix" ist die richtige Konfiguration.

> **Ehrlichkeit:** "gefixt" nur wo `gh` CLOSED/COMPLETED bzw. eine Version den Fix nennt. NOT_PLANNED/OPEN = Workaround bleibt aktiv. Caddy noch nicht installiert → Anker = aktuellste stabile v2.11.4 (vor Installation gegenpruefen).

---

## Pflicht-Checkliste vor "Reverse-Proxy/TLS-Stufe live"

- [ ] **TLS:** Erst Staging (`acme_ca` staging) getestet, dann Prod + altes Zert entfernt? Storage persistent (`/data`-Volume / `caddy`-User-Owner)? Renewal-Monitoring per Cron?
- [ ] **on-demand-TLS** (falls genutzt): `ask`-Endpoint mit schnellem DB-Lookup?
- [ ] **reverse_proxy:** systemd-Reihenfolge zum Upstream (kein 502 beim Boot)? Keepalive < Upstream-Timeout? `handle_path` statt `handle` bei Subpath? `route {}` wo Reihenfolge zaehlt?
- [ ] **SSE/MCP:** `flush_interval -1` + `encode` von der SSE-Route ausgenommen? `stream_close_delay`? App-Keep-Alive-Pings?
- [ ] **Header:** `trusted_proxies` global? uvicorn `--proxy-headers --forwarded-allow-ips="<subnetz>"` (nie `*`)? Im Backend `X-Real-IP {client_ip}`?
- [ ] **systemd:** `network-online.target` (+ `wg-quick@wg0`)? `enable`d? journald `SystemMaxUse=`? `sshd -t` vor reload?
- [ ] **VPS-Ops:** Swap eingerichtet (Server hat 0 B!)? needrestart/Auto-Reboot gezaehmt? NTP/chrony? `/boot`/inodes/`df -i` im Blick?
- [ ] **Security:** `ufw allow OpenSSH` vor `enable`? zweite SSH-Session offen? fail2ban `backend=systemd` + (hinter Caddy/Docker) Real-IP + `DOCKER-USER`-Chain? `sshd -T` autoritativ (cloud-init-Drop-in)?
- [ ] **Nach Fehler:** VOLLTEXT dieses Almanachs lesen (Stufe B); neuen Bug hier ergaenzen.

---

## 🔗 Bezug zu Best-Practices (Praevention — "wie macht man es richtig")

Gegenseite: [`best-practices/server/reverse-proxy-tls.md`](../../best-practices/server/reverse-proxy-tls.md)
— dort steht, wie man Proxy/TLS + den laufenden Server-Betrieb von vornherein richtig aufsetzt,
damit diese Bugs gar nicht entstehen (erst Almanach lesen, dann Best Practices).

| Bug-Abschnitt (diese Datei) | Best-Practice-Abschnitt in `best-practices/server/reverse-proxy-tls.md` |
|-----------------------------|------------------------------------------------------------------------|
| §1 Caddy Auto-TLS / Let's Encrypt | §1 Caddy Auto-TLS richtig aufsetzen |
| §2 reverse_proxy-Mechanik | §2 reverse_proxy sauber konfigurieren |
| §3 Streaming/SSE/WebSocket | §3 Streaming/SSE/MCP richtig durchreichen |
| §4 Header/Host/IP-Weiterleitung | §4 Header/Proxy-Vertrauen |
| §5 systemd-Unit-Betrieb | §5 systemd-Units richtig |
| §6 Linux-VPS-Ops (Updates/Swap/Disk/NTP) | §7 Auto-Updates/Logs/NTP/Swap |
| §7 Ops-Sicherheit (fail2ban/UFW/SSH) | §6 SSH-Hardening/UFW/fail2ban |
| (praeventiv, kein direkter Bug-Gegenpart) | §8 Monitoring/Uptime-Alarm · §9 Backup 3-2-1 + Restore-Test |
