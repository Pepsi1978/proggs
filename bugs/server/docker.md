# Bug-Almanach: Docker & Docker-Compose im Self-Hosting-Betrieb

> **PFLICHT-LESEN vor Arbeit an einem Docker-/Compose-Stack** (compose.yaml/.yml, docker-compose.*,
> Dockerfile, `docker compose`-Betrieb auf einem Linux-Server/VPS). Digest-Modell: Kurzcheck (unten,
> erste ~80 Zeilen) vor jeder Arbeit; bei einem Fehler im Bereich den VOLLTEXT lesen.
>
> **Stand:** recherchiert am 2026-06-24 fuer Docker Engine 29.6.0 (API 1.55), Docker Compose v5.1.4
> (Compose-v2-Linie), Ubuntu 24.04.4 LTS, Kernel 6.8 (cgroup v2 unified). 7-Researcher-Schwarm +
> harte `gh`-Issue-Status-Pruefung. Reine Betriebs-Bugsuche (Best-Practices/Stil laufen getrennt).
> **Anker:** docker=29.6.0, compose=5.1.4

## 🔗 Abgrenzung zu benachbarten Almanachen (NICHT hier doppeln)

| Thema | Gehoert in |
|-------|-----------|
| Container als Sicherheitsgrenze, Sandbox, UFW-Konzept fuer Agent-Server, Lethal Trifecta | `server/self-hosted-ai-agent-server.md` |
| WireGuard-VPN selbst (wg0.conf, AllowedIPs, Split-Tunnel, ip_forward) | `server/wireguard.md` |
| Anbieter-/Server-Wahl (Shared vs. VPS, RAM-Dimensionierung) | `server/vps-hosting.md` |
| Qdrant-**spezifischer** Docker-Betrieb (on_disk, Snapshot, Collection-Tuning) | `server/qdrant.md` |
| **Allgemeine Docker/Compose-Betriebsfehler (jeder Dienst)** | **DIESE Datei** |

## 🔗 Best-Practices-Gegenseite (so macht man es von vornherein richtig)

> Zweite Seite der Medaille: `best-practices/server/docker.md` (Stand 2026-06-24). Der Almanach sagt
> *was schiefgeht und wie man es loest*; die Best-Practices sagen *wie man es von vornherein richtig
> baut*. Beide Kurzchecks VOR der Arbeit lesen (erst Almanach, dann Best Practices).

| Bug-Abschnitt (hier) | Best-Practice (`best-practices/server/docker.md`) |
|----------------------|---------------------------------------------------|
| §1 Firewall/Netzwerk (UFW-Bypass, Binding) | §7 Netze & Port-Binding |
| §2 Memory/OOM/Limits | §8 Memory & Resource-Limits |
| §3 healthcheck & depends_on | §5 Healthchecks · §6 depends_on/restart/Boot |
| §4 Volume-/Bind-Mount-Permissions | §9 Volumes/Logging/Pinning/TZ |
| §5 restart-Policy & Boot-Reihenfolge | §6 depends_on/restart/Boot |
| §6 Logs/Disk + .env-Fallen | §4 Secrets & ENV · §9 Logging |
| §7 Zeitzone/Locale · Pinning · Build-Cache | §1 Image-Bau · §2 BuildKit · §3 .dockerignore · §9 Pinning/TZ |

---

## KURZCHECK (Erkennungssignale + Sofort-Regeln)

| Signal | Sofort-Regel |
|--------|--------------|
| `-p 8000:8000` / `ports:` → Port aus dem Internet offen TROTZ `ufw deny` | Docker umgeht UFW (DNAT in PREROUTING/FORWARD, nie INPUT). Bind an `127.0.0.1:` ODER spezifische Host-IP (`10.8.0.1:8000:8000`) statt `0.0.0.0`; Firewall via `ufw-docker` / DOCKER-USER (§1) |
| App crasht beim Boot mit "connection refused" zur DB/Qdrant | `depends_on: [x]` (Kurzform) wartet nur auf START, nicht "healthy" → Langform `condition: service_healthy` + healthcheck (§3) |
| Healthcheck dauerhaft `unhealthy`, App laeuft aber | curl/wget fehlt in `*-slim`/alpine/distroless → image-internen Check (Python-`urllib`, `pg_isready`, BusyBox-`wget`) (§3) |
| Exit 137 | NICHT automatisch "OOM" — `docker inspect --format '{{.State.OOMKilled}}'` + `dmesg \| grep -i oom`; oft SIGTERM-Timeout beim Shutdown (§2) |
| Container ohne Limit auf kleinem VPS | JEDEN Service mit `mem_limit` **oder** `deploy.resources.limits.memory` deckeln; `deploy.resources.reservations` wirkt OHNE Swarm NICHT → `mem_reservation` (§2) |
| "Permission denied" beim Schreiben in bind-gemounteten Ordner (non-root) | Bind-Mount = Host-uid/gid gilt direkt → Host-Ordner auf Container-uid `chown` ODER Container-uid == Host-uid. NIE `chmod 777` (§4) |
| `bind: cannot assign requested address` beim Boot | Daemon bindet an IP (WireGuard `10.8.0.1`), die beim Boot noch fehlt → systemd-Drop-in `After=wg-quick@wg0.service` + `restart: unless-stopped` als Selbstheilung (§5) |
| Root-Disk laeuft voll, Container/Host sterben | `json-file` (Default) rotiert OHNE `max-size`/`max-file` NICHT → `daemon.json` `log-opts` ODER `local`-Driver; danach Container NEU erstellen (§6) |
| Disk voll, aber Images/Volumes klein — `/var/lib/containerd` riesig | BUILD-CACHE waechst bei jedem `--build`-Deploy UNBEGRENZT (keine GC-Policy per Default) → `docker system df` zeigt "Build Cache … RECLAIMABLE"; Fix: `builder.gc` in `daemon.json` + Cron `docker builder prune -f --max-used-space=10GB` (§6.11) |
| Variable in `.env`, aber im Container leer | Auto-`.env` speist nur `${VAR}`-Interpolation der compose.yaml, NICHT den Container → `env_file:`/`environment:` nutzen (§6) |
| Logs in UTC trotz `TZ=Europe/Berlin` | `tzdata` fehlt im slim/alpine-Image → `tzdata` installieren ODER `/etc/localtime`+`/etc/timezone` `:ro` mounten (§7) |
| `:latest` driftet / `compose up` zieht nicht neu | Per Tag/`@sha256:`-Digest pinnen; `docker compose pull` / `pull_policy: always` (§7) |
| `restart: always` startet manuell gestoppten Container nach Reboot wieder | Fuer Dienste `unless-stopped` (merkt sich manuelles Stop ueber Reboot) (§5) |

---

## TL;DR — die wichtigsten Regeln

1. **Firewall:** Docker umgeht UFW per Design. Dienste nie blanko `0.0.0.0` veroeffentlichen — an `127.0.0.1`/spezifische Host-IP binden, Custom-Regeln in **DOCKER-USER** (`-I`, conntrack-Original-Felder), nie `-A FORWARD`.
2. **Memory:** Auf kleinem VPS jeden Dienst deckeln. `deploy.resources.limits.memory` wirkt in Compose v2/v5 ohne Swarm (alte "wird ignoriert"-Weisheit gilt nur v1); `deploy.resources.reservations` aber NICHT → `mem_reservation`.
3. **Start-Reihenfolge:** `depends_on` Kurzform wartet nur auf Start. Fuer DB-Abhaengige → Langform `condition: service_healthy` + healthcheck mit image-internem Tool.
4. **Mounts:** Bind-Mount = Host-Permissions gelten direkt; non-root-Container braucht passenden uid-Owner am Host-Ordner. Nie `chmod 777`.
5. **Boot/Restart:** `unless-stopped` statt `always`; Boot-Abhaengigkeiten (WireGuard-IP, NFS) ueber systemd-Drop-in `After=`/`RequiresMountsFor=`; `docker.service` muss `enable`d sein.
6. **Disk:** `json-file` rotiert nicht von allein → Log-Limits setzen, sonst laeuft die Disk voll. `env_file:` ≠ Auto-`.env`.
7. **Reproduzierbarkeit:** Images pinnen (Tag/Digest), `tzdata`+Locale im slim-Image, Dockerfile-Reihenfolge (Manifest vor Code), `.dockerignore` Pflicht.

---

## 1. Firewall / Netzwerk — Docker umgeht UFW/iptables

> **Mechanik (Grundlage):** Bei `-p`/`ports:` legt Docker NAT- + Filter-Regeln an. PREROUTING→DOCKER
> macht **DNAT** auf die Container-IP, BEVOR die Firewall greift; veroeffentlichte Ports laufen ueber
> die **FORWARD**-Kette (geroutet), NICHT ueber **INPUT** — wo UFW filtert. Folge: `ufw deny <port>`
> ist fuer Container-Ports wirkungslos. Quelle: docs.docker.com/engine/network/packet-filtering-firewalls/

### 1.1 Veroeffentlichte Ports umgehen UFW komplett (Kern-Bug)
- **Symptom:** `ufw enable` + `ufw deny 8000` aktiv, aber `-p 8000:8000`-Container ist aus dem Internet erreichbar.
- **Ursache:** DNAT in PREROUTING, Traffic ueber FORWARD, nie INPUT. Per Design.
- **FIX (funktionserhaltend):**
  - **A:** Nur lokal/hinter Reverse-Proxy → an Loopback binden: `127.0.0.1:8000:8000`.
  - **B:** Nur ueber VPN → an WireGuard-IP binden: `10.8.0.1:8000:8000` (genau das Muster des second-brain-Stacks).
  - **C:** `ufw-docker` installieren (verdrahtet UFW mit DOCKER-USER, §1.4).
  - **D (Docker 29):** nftables-Backend (§1.7) — integriert sich sauber mit Host-Firewall.
- **Versionen:** alle mit iptables-Backend (per Design); mit nftables-Backend (Docker 29, opt-in) sauber geloest.
- **Quelle:** docs.docker.com/engine/network/packet-filtering-firewalls/ · github.com/chaifeng/ufw-docker

### 1.2 `127.0.0.1`-gebundene Ports waren im LAN erreichbar (vor 28.0) — GEFIXT ab 28.0.0
- **Symptom:** `-p 127.0.0.1:8080:80` war von anderen Hosts im selben L2-Segment erreichbar.
- **Ursache:** NAT-DNAT-Regel matchte `-d 127.0.0.1` auf JEDEM Interface (ohne `-i lo`).
- **FIX:** Auf Docker ≥ 28.0.0 (auf 29.6.0 bereits behoben). Status: gefixt.
- **Versionen:** betroffen < 28.0.0, gefixt ab 28.0.0.
- **Quelle:** github.com/moby/moby/issues/41872 · /45610

### 1.3 Swarm-Mode: `127.0.0.1`-Ports auf `0.0.0.0` exponiert — OFFEN (#32299)
- **Symptom:** In Swarm wird ein als `127.0.0.1:PORT` deklarierter Port trotzdem auf allen Interfaces veroeffentlicht.
- **Ursache:** Routing-Mesh/Ingress ignoriert die Bind-Adresse.
- **FIX:** Port im `mode: host` veroeffentlichen statt Ingress-Routing-Mesh. (Betrifft den second-brain-Stack NICHT — kein Swarm.)
- **Versionen:** langlebig; **gh-Status: OPEN** (moby/moby#32299, geprueft 2026-06-24).
- **Quelle:** github.com/moby/moby/issues/32299

### 1.4 Korrekte Loesung: `ufw-docker` (DOCKER-USER-Block in after.rules)
- **Was:** Fuegt einen Block in `/etc/ufw/after.rules`, der DOCKER-USER mit UFW verdrahtet (RFC1918 erlaubt, Rest per `ufw-docker allow` explizit).
- **Install:** `wget -O /usr/local/bin/ufw-docker https://github.com/chaifeng/ufw-docker/raw/master/ufw-docker && chmod +x /usr/local/bin/ufw-docker && ufw-docker install`, dann `systemctl restart ufw`.
- **Port freischalten:** `ufw-docker allow httpd 80` (oeffnet den **Container-internen** Port).
- **WICHTIG:** ufw-docker schreibt in die iptables-DOCKER-USER-Kette — beim **Docker-29-nftables-Backend** existiert KEINE DOCKER-USER-Kette mehr (§1.7) → ufw-docker greift dann NICHT wie gewohnt (nicht offiziell bestaetigt; separat testen).
- **Quelle:** github.com/chaifeng/ufw-docker

### 1.5 Eigene FORWARD-Regeln werden ignoriert (Rule-Ordering)
- **Symptom:** `-A FORWARD ...`-Regeln greifen nicht fuer Container-Traffic.
- **Ursache:** Docker springt aus FORWARD frueh in DOCKER-USER/DOCKER-FORWARD/DOCKER; dort akzeptierte/verworfene Pakete erreichen spaeter angehaengte FORWARD-Regeln nie.
- **FIX:** IMMER in **DOCKER-USER** mit `-I` (nicht `-A`): z.B. `iptables -I DOCKER-USER -i ext_if ! -s 192.0.2.0/24 -j DROP`.
- **Versionen:** per Design (iptables-Backend).
- **Quelle:** docs.docker.com/engine/network/firewall-iptables/

### 1.6 Nach DNAT veraenderte Ziel-IP/Port → falsche Match-Regeln
- **Symptom:** `-d <host-ip> --dport <host-port>` in DOCKER-USER matcht nicht (Paket schon DNAT'd auf Container-IP:Port).
- **FIX:** conntrack-Original-Felder nutzen: `--ctorigdst <host-ip> --ctorigdstport <host-port>`; interface-basiert (`-i ext_if`) statt subnetz-basiert filtern.
- **Versionen:** per Design.
- **Quelle:** docs.docker.com/engine/network/firewall-iptables/

### 1.7 Docker 29: iptables-Aenderungen + nftables-Backend (BREAKING)
- **Symptom:** Nach Upgrade auf 29.x verhalten sich eigene iptables/ufw-docker-Regeln anders; bei nftables-Backend fehlt DOCKER-USER ganz.
- **Ursache:** Docker 29 aendert die iptables-Regelverwaltung **auch ohne** nftables-Opt-in. Das experimentelle nftables-Backend (seit 29.0.0) hat **keine DOCKER-USER-Kette**, aktiviert IP-Forwarding **nicht selbst**, geht **nicht in Swarm**.
- **FIX (funktionserhaltend):**
  - Aktivieren: `daemon.json` → `{"firewall-backend": "nftables"}` — loest den UFW-Bypass sauber.
  - IP-Forwarding manuell: `sysctl net.ipv4.ip_forward=1` (+ ipv6 forwarding) bzw. `"ip-forward": false`.
  - FORWARD-Policy pruefen (`iptables -L FORWARD`), Custom-Skripte die Docker-Ketten flushen anpassen, interface-basierte Regeln (`-i docker0`) statt Subnetz-Hardcoding.
- **Versionen:** ab 29.0.0 (29.6.0 betroffen). Migration bewusst planen.
- **Quelle:** docs.docker.com/engine/network/firewall-nftables/ · blog.canadianwebhosting.com/docker-v29-migration-guide-self-hosters/

### 1.8 `iptables: false` als "Firewall-Fix" = Footgun
- **Symptom:** `{"iptables": false}` gesetzt → Container ohne Internet (kein Masquerading) UND alle Ports im LAN offen.
- **FIX:** NIEMALS als Firewall-Loesung. Stattdessen ufw-docker (§1.4) oder nftables-Backend (§1.7).
- **Versionen:** per Design, alle.
- **Quelle:** docs.docker.com/engine/network/packet-filtering-firewalls/

### 1.9 IPv6-Bypass: ip6tables filtert nicht wie IPv4 — firewalld-Divergenz OFFEN (#50694)
- **Symptom:** IPv4-Port korrekt gefiltert, derselbe Port ueber IPv6 offen / abweichendes Verhalten mit firewalld.
- **FIX:** `daemon.json` → `{"ip6tables": true}` (in neueren Versionen Default-on, pruefen); IPv6-Regeln in ip6tables/DOCKER-USER(v6) spiegeln; wenn IPv6 nicht gebraucht, im Netz deaktiviert lassen.
- **Versionen:** langjaehrig; **gh-Status: OPEN** (moby/moby#50694, geprueft 2026-06-24).
- **Quelle:** github.com/moby/moby/issues/50694

### 1.10 `127.0.0.1`-Bind schuetzt NICHT vor anderen Containern im selben Netz
- **Symptom:** Dienst an `127.0.0.1:5432:5432` — extern blockiert, aber andere Container im selben Netz erreichen ihn ueber die Container-IP.
- **Ursache:** Loopback-Bind betrifft nur die HOST-veroeffentlichte Adresse; Container im selben User-Defined-Network sprechen direkt ueber die Bridge-IP/DNS.
- **FIX:** Echte Isolation ueber getrennte Netze / `internal: true`-Netze; Inter-Container-Verkehr ueber gemeinsames internes Netz statt `-p`.
- **Versionen:** per Design.
- **Quelle:** docs.docker.com/engine/network/

### 1.11 IP-Forwarding-Default-DROP blockiert Container-Internet
- **Symptom:** Kein ausgehender Container-Traffic, FORWARD-Policy auf DROP.
- **FIX:** `daemon.json` → `{"ip-forward-no-drop": true}` ODER eigene ACCEPT-Regel in DOCKER-USER.
- **Quelle:** docs.docker.com/engine/network/packet-filtering-firewalls/

### 1.12 firewalld: Docker-`docker`-Zone mit target ACCEPT (RHEL/Fedora)
- **Symptom:** Container-Ports offen trotz restriktivem firewalld.
- **FIX:** Zonen-Konfiguration anpassen statt loeschen (`docker`-Zone loeschen bricht Container-Netzwerk); Docker-29-nftables-Backend integriert sich sauberer. (Ubuntu nutzt UFW/iptables, nicht firewalld — fuer den second-brain-Stack nachrangig.)
- **Quelle:** docs.docker.com/engine/network/packet-filtering-firewalls/

---

## 2. Memory / OOM-Kill / Limits

### 2.1 (KRITISCH) `deploy.resources.limits.memory` wird in Compose v2/v5 OHNE Swarm DOCH durchgesetzt
- **Symptom:** Verwirrung/Fehlkonfiguration. Alte Anleitungen sagen "`deploy` wird von `docker compose up` ignoriert, nutze `mem_limit`". Das stimmte nur in Compose **v1**.
- **Ursache:** Compose v2 (Go-Rewrite) wendet die `limits`-Unterschluessel von `deploy.resources` lokal an. Der Hinweis "deploy only takes effect in a Swarm" gilt fuer die ECHTEN Swarm-Keys (`replicas`, `placement`, `update_config` …), NICHT mehr pauschal fuer `resources.limits`.
- **FIX (funktionserhaltend, beide Wege gueltig in v5):** Top-Level `mem_limit`/`cpus`/`mem_reservation` (eindeutigster Weg, immer ohne Swarm) ODER `deploy.resources.limits.memory`/`.cpus` (wirkt in v2/v5 ebenfalls). **Empfehlung 8-GB-Server:** Top-Level `mem_limit` + `mem_reservation` (kein Doku-Mehrdeutigkeit). Der second-brain-Stack nutzt `deploy.resources.limits` → das WIRKT.
- **Versionen:** ignoriert in Compose v1, durchgesetzt ab v2 (alle 2.x) inkl. v5.1.4.
- **Quelle:** github.com/docker/compose/issues/7307 · docs.docker.com/reference/compose-file/deploy/

### 2.2 `deploy.resources.reservations.memory` wirkt OHNE Swarm NICHT (limits schon)
- **Symptom:** `limits` greift, `reservations` (soft) hat keinen Effekt im Single-Host-`compose up`.
- **Ursache:** Asymmetrie in Compose v2 — nur `limits` wird lokal uebersetzt, `reservations` nicht auf cgroup gemappt.
- **FIX:** Statt `deploy.resources.reservations.memory` das Top-Level `mem_reservation:` verwenden (setzt cgroup `memory.low` auch ohne Swarm). **Direkt relevant fuer den second-brain-Stack:** die `reservations.memory`-Eintraege (Qdrant 512M, brain-api 256M usw.) sind dort aktuell **wirkungslos** — fuer echte Soft-Reservierung auf `mem_reservation` umstellen.
- **Versionen:** Compose v2.x inkl. v5.1.4 (per Design ohne Swarm).
- **Quelle:** github.com/docker/compose/issues/10046

### 2.3 (KRITISCH) Exit 137 ist NICHT immer OOM
- **Symptom:** Container endet mit 137; Reflex "OOM" — aber `docker inspect` zeigt `State.OOMKilled: false`.
- **Ursache:** 137 = 128+9 = SIGKILL. Quellen: cgroup-OOM (`OOMKilled: true`), Host-OOM (`false` moeglich), `docker stop`/`compose down` bei App ohne SIGTERM-Handling innerhalb `stop_grace_period` (Default 10s) → SIGKILL → 137 mit `OOMKilled: false`, manuelles `kill -9`.
- **FIX (Diagnose zuerst):** `docker inspect <c> --format '{{.State.ExitCode}} {{.State.OOMKilled}}'`; `dmesg | grep -i "oom\|killed process"`. Bei echtem OOM → Limit hoch / Heap runter (§2.4). Bei 137 beim Shutdown → `stop_grace_period: 60s` + App-SIGTERM-Handling (`init: true` / korrektes Signal-Forwarding).
- **Versionen:** alle (Exit-Code-Semantik).
- **Quelle:** oneuptime.com/blog/post/2026-02-08-how-to-fix-docker-container-immediately-exiting-with-code-137/view

### 2.4 (KRITISCH) Container "sieht" Host-RAM statt Limit → falsches Heap-Sizing → OOM
- **Symptom:** App mit `mem_limit: 512m` wird OOM-gekillt; JVM/Runtime allokiert Heap nach 8 GB Host-RAM statt 512 MB.
- **Ursache:** Tools, die `/proc/meminfo` lesen, sehen Host-RAM; cgroup-Limit wird dort nicht reflektiert. Container-aware Runtimes lesen das Limit nur bei passender Version (JVM `UseContainerSupport` ab Java 10; JDK <15 versteht cgroup v2 nicht; JDK <21.0.10 bei Kernel 6.12+ buggy).
- **FIX (Heap an Limit koppeln):** JVM `-Xmx384m` (sicher unter 512m) oder `-XX:MaxRAMPercentage=60.0` (JDK 17/21+); Node `NODE_OPTIONS=--max-old-space-size=384`; Python: Worker/Pool-Anzahl manuell am Limit ausrichten. (Der second-brain-Stack ist Python/FastAPI ohne grossen Heap — primaer fuer kuenftige JVM/Node-Dienste relevant.)
- **Versionen:** JDK <15 / <21.0.10@Kernel6.12+; per Design fuer nicht-container-aware Tools.
- **Quelle:** netdata.cloud/guides/docker/docker-jvm-memory-tuning/

### 2.5 Swap-Accounting / `memswap_limit` auf Ubuntu 24.04 (cgroup v2)
- **Symptom:** `docker info` zeigt "WARNING: No swap limit support"; `memswap_limit`/`--memory-swap` wirkungslos; Container OOM-killt strikt.
- **Ursache:** Swap-Accounting per cgroup auf Debian/Ubuntu standardmaessig nicht aktiv; Kernel 6.x (cgroup v2) erzwingt Memory-Limits hart (kein Soft-Verhalten). `--memory-swap` ist memory+swap GESAMT (nicht zusaetzlich).
- **FIX:** Wenn Swap-Limitierung noetig: GRUB `cgroup_enable=memory swapaccount=1` → `update-grub` → reboot. Auf reinen cgroup-v2-Systemen meist ohnehin `memory.swap.max` da (Warnung ggf. irrefuehrend). 8-GB-Server: einfacher nur `mem_limit`, Swap aus dem Spiel lassen.
- **Versionen:** Ubuntu/Debian per Default; cgroup v2 / Kernel 6.8.
- **Quelle:** docs.docker.com/engine/containers/resource_constraints/

### 2.6 (KRITISCH-Falle) `--oom-kill-disable` ohne Memory-Limit friert den Host ein
- **Symptom:** Server haengt komplett (kein SSH), ein Container mit Leak laesst sich nicht killen.
- **Ursache:** `oom_kill_disable: true` ohne `-m/--memory` → Container darf gesamten Host-RAM fressen, Kernel darf den Container-Prozess nicht killen → killt Host-Prozesse / Host friert ein.
- **FIX:** Nur MIT `mem_limit` verwenden — auf 8-GB-Self-Hosting am besten GAR NICHT. Limit setzen, Leak app-seitig fixen. Doku: "Only disable the OOM killer on containers where you have also set the -m/--memory option."
- **Versionen:** alle (per Design gefaehrlich).
- **Quelle:** docs.docker.com/engine/containers/resource_constraints/ · github.com/moby/moby/issues/14440

### 2.7 cgroup v1 vs v2 (Ubuntu 24.04 = v2 unified)
- **Symptom:** Stacks, die nach 22.04→24.04-Upgrade ploetzlich OOM-killen / Limits anders durchsetzen.
- **Ursache:** cgroup v2 unified per Default; Kernel 6.x erzwingt strikter. Pfade geaendert (v2: `/sys/fs/cgroup/<scope>/memory.max|.high|.low|.swap.max`). v1-Pfad-hartkodierende Skripte brechen.
- **FIX:** `docker info` → "Cgroup Version: 2" verifizieren; `mem_reservation`→`memory.low`, `mem_limit`→`memory.max`; keine v1-Pfade hartkodieren.
- **Quelle:** vipinpg.com/blog/debugging-linux-kernel-6x-cgroup-v2-memory-limits-breaking-legacy-docker-compose-stacks-after-ubuntu-2404-upgrade/

> **8-GB-Baseline-Pattern (funktionserhaltend):** pro Dienst `mem_limit` + `mem_reservation` + `cpus` + `stop_grace_period: 30s` + `restart: unless-stopped`; Heap-Env an Limit koppeln; Summe aller `mem_limit` deutlich < 8 GB (Reserve fuer Host/Kernel/Page-Cache).

---

## 3. healthcheck & depends_on (Start-Reihenfolge)

### 3.1 `depends_on: [x]` (Kurzform) wartet NICHT auf "healthy", nur auf "gestartet"
- **Symptom:** Abhaengige App startet vor DB-Bereitschaft (Qdrant/Postgres) → `Connection refused`/Crash-Loop beim ersten `up`/nach Reboot.
- **Ursache (per Design):** Kurz-Listenform = implizit `condition: service_started`. Doku: "Compose does not wait until a container is 'ready', only until it's running."
- **FIX (funktionserhaltend):** Healthcheck am Dependency + Langform `condition: service_healthy`:
  ```yaml
  services:
    db:
      healthcheck:
        test: ["CMD-SHELL", "pg_isready -U $${POSTGRES_USER}"]
        interval: 10s
        timeout: 10s
        retries: 5
        start_period: 30s
    app:
      depends_on:
        db:
          condition: service_healthy
  ```
  (`$$` escaped die Variable, damit Compose sie nicht selbst interpoliert.) Legacy wait-for-it.sh ab Engine ≥23 unnoetig.
- **Versionen:** Kurzform-Verhalten per Design (alle); `service_healthy` seit Compose v2, Standard in v5.1.4.
- **Quelle:** docs.docker.com/compose/how-tos/startup-order/

### 3.2 `condition` wird in der Kurz-Listenform STILL ignoriert
- **Symptom:** Man glaubt auf "healthy" zu warten, aber App startet zu frueh — kein Fehler/Warnung.
- **Ursache:** `condition` ist nur in der Mapping-Langform gueltig; Listenform kennt es nicht (→ `service_started`).
- **FIX:** IMMER Mapping-Form, sobald `service_healthy`/`service_completed_successfully` gewollt ist.
- **Versionen:** per Design.
- **Quelle:** docs.docker.com/reference/compose-file/services/

### 3.3 Healthcheck schlaegt IMMER fehl: curl/wget fehlt in slim/alpine/distroless
- **Symptom:** Container laeuft, ist aber dauerhaft `unhealthy`; `depends_on: service_healthy` blockiert die abhaengige App fuer immer. Log: `curl: not found` / `executable file not found`.
- **Ursache:** `-slim`/alpine/distroless enthalten weder curl noch (distroless) eine Shell.
- **FIX (image-internen Check):** Alpine BusyBox-`wget -q -O- http://localhost:PORT/health`; Postgres `pg_isready`; MySQL `mysqladmin ping`; **Python `["CMD","python","-c","import urllib.request,sys;sys.exit(0 if urllib.request.urlopen('http://127.0.0.1:8000/health',timeout=5).status==200 else 1)"]`** (genau das Muster, das der second-brain-Stack nutzt — richtig so); distroless: statisches Healthcheck-Binary/Sidecar; Qdrant (oft ohne curl): TCP-/Socket-Check oder `service_started` + App-Retry.
- **Debug:** `docker inspect --format='{{json .State.Health}}' <c> | python -m json.tool`.
- **Versionen:** per Design (image-abhaengig).
- **Quelle:** mattknight.io/blog/docker-healthchecks-in-distroless-node-js · fixdevs.com/blog/docker-healthcheck-failing/

### 3.4 `start_period`: erfolgreiche Checks resetten FailingStreak nicht (#11131) — GEFIXT
- **Symptom:** Trotz erfolgreicher Checks waehrend `start_period` kippt der Container direkt nach Ablauf auf `unhealthy`.
- **Ursache:** Waehrend `start_period` erhoehen FEHLversuche den Counter nicht; der Reset des `FailingStreak` bei Erfolg griff aber nicht korrekt.
- **FIX:** `start_period` grosszuegig; `retries` erhoehen; `start_interval` (Engine 25.0+/Compose v2.20.2+) fuer haeufigere Startphasen-Checks.
- **Versionen:** "Fehlversuche zaehlen nicht in start_period" ab Engine **25.0**; **gh-Status: CLOSED/COMPLETED 2023-10-27** (docker/compose#11131) → in v5.1.4 behoben.
- **Quelle:** github.com/docker/compose/issues/11131

### 3.5 `depends_on` greift NICHT bei `--no-deps` / Einzelservice-`up` / `restart`
- **Symptom:** `docker compose up <svc>` / `compose restart <svc>` startet die App ohne auf die DB zu warten.
- **Ursache (per Design):** `--no-deps` ueberspringt Aufloesung; `restart` kennt keine Start-Reihenfolge; eine `restart: always`-App startet ihre Dependency nicht mit (#9077, **CLOSED/COMPLETED 2022**).
- **FIX:** Normales `docker compose up` (ohne `--no-deps`/Einzelservice) respektiert Conditions; `restart: true` in der depends_on-Langform (Compose 2.17.0+) → Re-Start mit der Dependency; ZUSAETZLICH `restart: unless-stopped` an der App als Crash-Recovery. Healthcheck+depends_on (Start-Ordnung) und restart-Policy (Crash) sind komplementaer.
- **Versionen:** per Design; `restart: true` ab 2.17.0; #9686/#9077 **CLOSED/COMPLETED** (in v5.1.4 verbessert).
- **Quelle:** github.com/docker/compose/issues/9686 · /9077

### 3.6 `condition`-Override beim Merge greift auf ALLE Deps (#11993) — GEFIXT
- **Symptom:** Beim Override (override.yml / mehrere `-f`) gilt eine geaenderte `condition` faelschlich fuer alle Dependencies.
- **FIX:** Beim Override die vollstaendige Mapping-Form mit expliziten conditions pro Service angeben.
- **Versionen:** **gh-Status: CLOSED/COMPLETED 2024-07-22** (docker/compose#11993) → in v5.1.4 behoben.
- **Quelle:** github.com/docker/compose/issues/11993

### 3.7 Nebenfunde (kurz)
- **`service_completed_successfully`** — wartet auf Exit 0 der Dependency (Init-/Migrations-Container).
- **`condition` ignoriert mit `volumes_from`** (#9843, CLOSED/COMPLETED 2022) → `volumes_from` durch named volumes ersetzen.
- **`depends_on` gilt nicht bei `docker stack deploy`** (Swarm).
- **`required: false`** (Compose 2.20.0+) — Warnung statt Fehler bei fehlender optionaler Dependency.
- **`healthcheck` (compose) ueberschreibt Dockerfile-`HEALTHCHECK`**; `test: ["NONE"]` / `disable: true` deaktiviert einen Image-Healthcheck.
- **`CMD` vs `CMD-SHELL`:** String-Form → `CMD-SHELL` (braucht Shell im Image!); Listen-Form `["CMD",...]` startet das Binary direkt ohne Shell.

---

## 4. Volume- & Bind-Mount-Permissions (non-root)

> **Faustregel:** Bind-Mount = Host-Permissions gelten direkt (Docker legt NICHTS drauf). Named Volume
> = Ownership EINMALIG aus dem Image-Pfad initialisiert (nur bei LEEREM Volume). Linux prueft nur
> uid/gid (Zahlen), nie Benutzernamen.

### 4.1 (Hauptfall) Bind-Mount uid/gid-Mismatch → "Permission denied"
- **Symptom:** non-root-Container (`USER appuser`, uid 10001/1000) bekommt `EACCES` beim Schreiben in bind-gemountetes Host-Verzeichnis (/app/logs, Daten, SQLite). Lesen geht oft, Schreiben nicht.
- **Ursache:** Bind-Mount behaelt Host-uid/gid; stimmt die Container-uid nicht mit dem Host-Owner ueberein (und nicht world-writable) → kein Schreibzugriff.
- **FIX (funktionserhaltend, nach Praeferenz):**
  1. Host-Ordner auf Container-uid chownen: `sudo chown -R 10001:10001 ./data ./logs` (Container-uid via `docker exec <c> id`).
  2. Container-uid == Host-uid beim Image-Bau (`useradd -u 1000`).
  3. `user: "1000:1000"` in compose (ohne Image-Aenderung).
  4. Entrypoint-chown + Privileg-Drop (`gosu`/`su-exec`), wenn uid zur Laufzeit variabel.
  5. `fixuid` fuer Dev mit wechselnden Host-uids.
- **Direkt relevant fuer den second-brain-Stack:** `agent` (uid 1000) mountet `./agent-data` + `/srv/samba/gedanken/Logbuch`; `brain-api` (uid 10001) mountet `./brain-logs`. Diese Host-Ordner MUESSEN dem jeweiligen uid gehoeren (`chown` beim Deploy) — sonst Permission-denied beim Schreiben.
- **VERBOTEN als "Fix":** `chmod 777`.
- **Versionen:** per Design (alle, inkl. 29.6.0).
- **Quelle:** docs.docker.com/engine/storage/bind-mounts/

### 4.2 Named Volume: Ownership einmalig initialisiert, danach "klebrig"
- **Symptom:** Frisches Volume korrekt; nach uid-Wechsel im Image bekommt der neue User `permission denied`.
- **Ursache:** Beim ERSTEN Mount eines LEEREN Volumes kopiert Docker Inhalt+Ownership aus dem Image-Pfad — danach unveraendert. Bestehendes Volume wird bei spaeterem uid-Wechsel NICHT neu initialisiert.
- **FIX:** Volume neu anlegen (`compose down -v` — loescht Daten!) ODER Ownership korrigieren: `docker run --rm -v myvol:/data alpine chown -R 10001:10001 /data`.
- **Versionen:** per Design.
- **Quelle:** docs.docker.com/engine/storage/volumes/

### 4.3 Anonyme Volumes / `VOLUME`-Instruktion: Shadowing von Code/Inhalt
- **Symptom:** Bind-Mount des Codes → `node_modules`/gebauter Inhalt leer/veraltet; `VOLUME`-Pfad zeigt leeres Verzeichnis.
- **Ursache:** (a) Bind-Mount ueber non-empty Container-Dir **verdeckt** (obscures) den Image-Inhalt unwiderruflich bis Recreate. (b) `VOLUME /pfad` ohne expliziten Mount erzeugt automatisch ein anonymes Volume (verdeckt + sammelt Karteileichen).
- **FIX:** spezifischeres anonymes Volume fuer den Unterpfad (`- /app/node_modules`); `VOLUME` aus dem Dockerfile entfernen, wenn man den Mount selbst steuert; bei "altem Stand" `compose up --force-recreate` + `docker volume prune`.
- **Verwandt:** Subdir eines Host-Volumes als named/anon gemountet wird trotzdem als Host-Volume behandelt — **moby/moby#38564, gh-Status: OPEN**.
- **Versionen:** per Design.
- **Quelle:** docs.docker.com/engine/storage/bind-mounts/ · github.com/moby/moby/issues/38564

### 4.4 `:ro` / read-only Mount: Schreibversuch schlaegt fehl
- **Symptom:** `Read-only file system`/`EROFS` trotz korrekter uid.
- **FIX:** `:ro`/`readonly` nur fuer Nur-Lese-Pfade. Sicheres Muster: `read_only: true` Container + gezielte `tmpfs`/Volumes fuer Schreibpfade. (Der second-brain-Stack mountet `/opt/second-brain:/hostfs:ro` im Dashboard bewusst read-only — korrekt, da nur gelesen wird.)
- **Quelle:** docs.docker.com/engine/storage/bind-mounts/

### 4.5 SELinux `:z`/`:Z` — NICHT relevant fuer Ubuntu (AppArmor), aber dokumentiert
- **Symptom:** Permission denied auf bind-Mount NUR auf RHEL/Fedora; AVC-Denials.
- **FIX:** `:z` (geteilt) / `:Z` (exklusiv) ans Mount. **Falle:** `:Z` NIE auf System-Pfade (macht System unbrauchbar); `:z`/`:Z` ist KEIN uid-Fix.
- **Ubuntu:** nutzt **AppArmor** (`docker-default`), nicht SELinux — `:z`/`:Z` wirkungslos. AppArmor-Denials in `dmesg` (`apparmor="DENIED"`).
- **Quelle:** developers.redhat.com/articles/2025/04/11/my-advice-selinux-container-labeling

### 4.6 `userns-remap` / rootless: uid-Verschiebung um Offset
- **Symptom:** Mit `userns-remap`/rootless `permission denied` auf Host-Ordner, der ohne Remap ging.
- **Ursache:** Container-uid → Host-subuid (z.B. Container 1000 → Host 100000+1000 = 101000); Host-Ordner gehoert realem 1000.
- **FIX:** Host-Ordner auf die remappte uid chownen (`chown 101000:101000`); ODER ACLs; ODER named volume statt Bind-Mount.
- **Versionen:** per Design (Engine 29.x).
- **Quelle:** oneuptime.com/blog/post/2026-02-08-how-to-handle-docker-volume-permissions-with-namespaced-users/view

### 4.7 Windows/WSL2: Bind-Mount 0777, chmod wirkungslos
- **Symptom:** Windows-Pfade im Container als 0777, `chmod` wirkt nicht; spontane Ownership-Wechsel.
- **FIX:** Daten im WSL2-/Linux-FS halten (nicht `/mnt/c`); named volume statt Windows-Bind-Mount. Default 0777 ist nicht aenderbar.
- **Versionen:** Docker Desktop/WSL2 (docker/for-win#4824, #12742).
- **Quelle:** github.com/docker/for-win/issues/4824

> **Diagnose:** `docker exec <c> id` · `ls -ln ./mount-dir` · `docker inspect <c> --format '{{json .Mounts}}'` · `dmesg | grep -i denied`.

---

## 5. restart-Policy, Crash-Loops & Boot-Reihenfolge

### 5.1 restart-Policies — genaues Verhalten
| Policy | Crash | Sauberer Exit | Nach `docker stop` | Nach Reboot/Daemon-Restart |
|--------|-------|---------------|--------------------|----------------------------|
| `no` (Default) | nein | nein | nein | nein |
| `on-failure[:N]` | ja (bis N) | nein | nein | **NEIN** |
| `always` | ja | ja | ignoriert bis Daemon-Restart, dann **wieder hoch** | ja |
| `unless-stopped` | ja | ja | bleibt gestoppt | bleibt gestoppt, wenn vor Reboot manuell gestoppt |

- **5.1a** `always` startet manuell gestoppten Container nach Reboot ungewollt neu → fuer Dienste **`unless-stopped`** (merkt sich manuelles Stop). Per Design.
- **5.1b** `on-failure` macht KEINEN Boot-Autostart (nur Crash-Recovery) → fuer Boot-Persistenz `unless-stopped`/`always`.
- **Aktivierung:** Policy greift erst nach **10 s** erfolgreicher Laufzeit (verhindert Endlos-Restart). Container, der in <10s stirbt, gilt nie als "gestartet".
- **Quelle:** docs.docker.com/engine/containers/start-containers-automatically/

### 5.2 Crash-Loop frisst CPU/Log-Disk
- **Symptom:** Container mit `always` startet hunderte Male, CPU hoch, Log waechst.
- **Backoff:** exponentiell **100 ms → max 1 min**, Reset nur nach ≥10 s Laufzeit. Begrenzt CPU, behebt die Ursache NICHT.
- **FIX:** Root-Cause aus `docker logs` fixen (NICHT die Policy als Pflaster); `restart: on-failure:5` fuer fehleranfaellige Jobs; Log-Driver begrenzen (§6). Erkennen: `docker ps` "Restarting (N)", `docker inspect --format='{{.RestartCount}}'`.
- **Verwandt:** Backoff-Reset-Edge-Case bei knapp-unter-10s-Laufzeit (moby/moby#22283, **CLOSED/COMPLETED 2016**).
- **Quelle:** docs.docker.com/engine/containers/start-containers-automatically/

### 5.3 Container startet nicht nach Reboot trotz `restart: always`
- **Ursache:** `docker.service` nicht `enable`d (häufigste Ursache) ODER Container lief vor Reboot nie >10s.
- **FIX:** `sudo systemctl enable docker.service containerd.service`; Container einmal sauber starten und Stabilitaet verifizieren.
- **Versionen:** alle (docker/for-linux#1037, **CLOSED/NOT_PLANNED** = Config-Problem, kein Code-Bug).
- **Quelle:** docs.docker.com/engine/containers/start-containers-automatically/

### 5.4 (Boot-Klassiker) `bind: cannot assign requested address` — IP noch nicht da
- **Symptom:** Beim Boot `Error starting userland proxy: listen tcp 10.8.0.1:8000: bind: cannot assign requested address`. Manuell spaeter gestartet geht es.
- **Ursache:** Docker-Daemon startet, BEVOR `wg-quick@wg0` die IP `10.8.0.1` gebracht hat.
- **FIX (kombiniert, genau das second-brain-Muster):**
  1. systemd-Drop-in (`sudo systemctl edit docker.service`) → `/etc/systemd/system/docker.service.d/override.conf`:
     ```ini
     [Unit]
     After=wg-quick@wg0.service
     Wants=wg-quick@wg0.service
     ```
     dann `sudo systemctl daemon-reload`. (Bei eigener Compose-systemd-Unit gehoeren `After=`/`Wants=` in DIESE Unit.)
  2. `restart: unless-stopped` als Selbstheilung (Backoff-Retry bis die IP da ist).
  3. Alternative: `network_mode: host` oder `0.0.0.0`-Bind (weniger restriktiv — bewusst abwaegen).
- **Versionen:** Bind-Verhalten per Design (docker/compose#8106, **CLOSED/COMPLETED 2021**).
- **Quelle:** github.com/docker/compose/issues/8106
- **Hinweis Drop-in:** `After=` = Reihenfolge, `Wants=`/`Requires=` = Abhaengigkeit; `Requires=` haerter (Docker startet nicht, wenn Abhaengigkeit fehlschlaegt) — fuer Boot-IP meist `After=`+`Wants=` robuster.

### 5.5 NFS/Netzlaufwerk beim Boot noch nicht gemountet → leeres Verzeichnis
- **Symptom:** Container laeuft, aber Bind-Mount-Verzeichnis ist leer nach Reboot (Daten erscheinen nach manuellem Restart).
- **Ursache:** Docker mountet das (noch leere) Host-Verzeichnis, bevor NFS darueber mountet.
- **FIX:** `RequiresMountsFor=/mnt/foo` im docker.service-Drop-in (`[Unit]`); ODER `After=/Wants=<mountunit>.mount`; in `/etc/fstab` die Option **`bg` entfernen** (`bg` mountet asynchron). (Der second-brain-Stack mountet `/srv/samba/gedanken/Logbuch` — falls das eine spaet verfuegbare Freigabe ist, hier relevant.)
- **Versionen:** Boot-Timing, alle (moby/moby#25584, **CLOSED/NOT_PLANNED**).
- **Quelle:** davejansen.com/systemctl-delay-start-docker-service-until-mounts-available/

### 5.6 Compose als systemd-Unit korrekt einrichten
```ini
[Unit]
Description=My App (docker compose)
Requires=docker.service
After=docker.service
# Boot-Abhaengigkeiten hier: After=wg-quick@wg0.service / RequiresMountsFor=/mnt/nas
[Service]
Type=oneshot
RemainAfterExit=yes
WorkingDirectory=/opt/second-brain
ExecStart=/usr/bin/docker compose up -d
ExecStop=/usr/bin/docker compose down
TimeoutStartSec=0
[Install]
WantedBy=multi-user.target
```
- `WorkingDirectory` MUSS auf den Compose-Ordner zeigen; `Type=oneshot`+`RemainAfterExit=yes`; Boot-Deps in `[Unit]`. **NICHT** zusaetzlich Host-Process-Manager UND compose-`restart:` fuer denselben Container mischen (Doku-Warnung) — Boot-Start via systemd, Container-Restart via compose-Policy. Quadlet ist die modernere systemd-native Alternative.
- **Quelle:** docs.docker.com/engine/containers/start-containers-automatically/ · bootvar.com/systemd-service-for-docker-compose/

---

## 6. Logs fressen Disk + .env/Variablen-Fallen

### 6.1 (KRITISCH) `json-file` rotiert ohne `max-size`/`max-file` NICHT → Disk voll
- **Symptom:** `/var/lib/docker/containers/<id>/<id>-json.log` waechst endlos → Root-Disk voll → Container/Host sterben (auf kleinem VPS sehr real).
- **Ursache:** Default `json-file` ohne Optionen rotiert nicht. "By default, no log-rotation is performed."
- **FIX (funktionserhaltend) — global `/etc/docker/daemon.json`:**
  ```json
  { "log-driver": "json-file", "log-opts": { "max-size": "10m", "max-file": "3" } }
  ```
  dann `sudo systemctl restart docker`. Werte muessen **Strings** sein.
- **FALLE (kritisch):** gilt NUR fuer NEU erstellte Container — bestehende neu erstellen (`docker compose up -d --force-recreate`); alte Riesen-Logs ggf. manuell leeren.
- **Direkt relevant:** der second-brain-Stack hat aktuell KEINE `logging:`-Begrenzung und keine `daemon.json` im Repo → konkreter Handlungspunkt (Startup-Check meldete bereits "Disk ~98% belegt").
- **Versionen:** per Design, alle (29.x). Kein Fix geplant — Konfiguration IST der Fix.
- **Quelle:** docs.docker.com/engine/logging/configure/

### 6.2 `local`-Driver als bessere Alternative (rotiert per Default)
- **FIX:** `/etc/docker/daemon.json` → `{ "log-driver": "local" }` (komprimiert, rotiert per Default, weniger Disk). `docker logs` funktioniert weiter. Gleiche NEU-Container-Falle wie 6.1.
- **Versionen:** seit Docker 18.09, stabil in 29.x.
- **Quelle:** docs.docker.com/engine/logging/drivers/local/

### 6.3 per-service `logging:` in compose (ohne Host-Aenderung)
```yaml
services:
  app:
    logging:
      driver: "json-file"   # oder "local"
      options: { max-size: "10m", max-file: "3" }
```
Vorteil: versioniert im Repo, keine Host-Aenderung. **Empfehlung fuer den second-brain-Stack** (statt globaler daemon.json).
- **Quelle:** docs.docker.com/engine/logging/configure/

### 6.4 Disk voll trotz weniger Images — falsche Diagnose
- **FIX:** ZUERST messen: `docker system df` / `docker system df -v`. Dann gezielt: `docker system prune` (stopped/dangling/build-cache), `docker builder prune`, `docker image prune`. **WARNUNG (funktionserhaltend):** `docker system prune` loescht Volumes NICHT ohne `--volumes`; NIE blind `prune -a --volumes` auf einem Self-Hosting-Host (loescht gestoppte, aber wichtige Container/Volumes/Images).
- **Quelle:** docs.docker.com/engine/manage-resources/pruning/

### 6.5 Anonyme Volumes / overlay2 sammeln sich an
- **FIX:** `docker volume ls -f dangling=true` → `docker volume prune` (nur wenn keine Daten gebraucht). Praevention: Named Volumes statt anonymer; `compose down -v` nur bewusst.
- **Quelle:** docs.docker.com/engine/manage-resources/pruning/

### 6.6 (Top-Falle) `.env` (Interpolation) vs `env_file:` (Container) verwechselt
- **Symptom:** Variable steht in `.env`, ist aber im Container leer.
- **Ursache:** Auto-`.env` (im Projektverzeichnis) speist NUR die `${VAR}`-Ersetzung **innerhalb der compose.yaml** (Tags/Ports), injiziert NICHTS in den Container. `env_file:` laedt Key-Values IN die Container-Umgebung.
- **FIX:** Variable soll in den Container → `env_file:`/`environment:`. (Der second-brain-Stack nutzt korrekt `env_file: .env` pro Service — sauber.)
- **Versionen:** per Design (docker/compose#9443 **CLOSED/COMPLETED**).
- **Quelle:** docs.docker.com/compose/how-tos/environment-variables/set-environment-variables/

### 6.7 `.env` wird nur im Arbeitsverzeichnis gelesen
- **Symptom:** `docker compose up` aus anderem Verzeichnis → Variablen leer, `... variable is not set`-Warnung.
- **FIX:** `docker compose` aus dem Compose-Verzeichnis starten ODER `--env-file /pfad/.env`.
- **Versionen:** per Design (docker/compose#13070 **CLOSED/COMPLETED 2025-07** — `--env-file`+Build-Aspekt gefixt).
- **Quelle:** docs.docker.com/compose/how-tos/environment-variables/variable-interpolation/

### 6.8 Quoting/Syntax-Fallen in `.env`
- **Ursache (Compose interpretiert `.env` anders als die Shell):** `#` = Kommentar; Leerzeichen um `=` weg; single-quote `'...'` = literal (keine Interpolation); unquoted/double-quote = Interpolation; **`${VAR}` in `.env`-Werten expandiert NICHT**; literaler `$` muss `$$` sein (sonst `variable is not set`-Warnung).
- **FIX:** Werte mit `#`/Leerzeichen/`$` in Single-Quotes.
- **Versionen:** Compose v2/v5 (Quoting aenderte sich historisch: #8388, #3702; **CLOSED/NOT_PLANNED** #11755 = per Design).
- **Quelle:** docs.docker.com/reference/compose-file/interpolation/

### 6.9 `environment:` ueberschreibt `env_file:` — Praezedenz
- **Reihenfolge (hoch→niedrig):** `compose run -e` CLI > `${}`-Interpolation (Shell schlaegt `.env`) > `environment:` (statisch) > `env_file:` > Image-`ENV`.
- **FIX:** Bewusst entscheiden; soll `env_file:` gewinnen, denselben Key nicht in `environment:` setzen; Achtung bei gesetzten Shell-Variablen (CI).
- **Quelle:** docs.docker.com/compose/how-tos/environment-variables/envvars-precedence/

### 6.10 Build-Arg (`ARG`) vs Runtime-Env (`ENV`) + Secret-Leak
- **Symptom:** Via `--build-arg` gesetzte Variable im Container leer; Secret in `docker history` sichtbar.
- **Ursache:** `ARG` existiert nur beim Build (nicht zur Laufzeit); ARG/ENV-Werte erscheinen in `docker history`/Image-Metadaten → jedes via build-arg uebergebene Token leakt.
- **FIX:** Laufzeit-Wert → `ENV`/`environment:`/`env_file:` (nicht `ARG`). Secrets NIE via ARG/ENV → **BuildKit Secret-Mounts** `RUN --mount=type=secret,id=tok ...` + `docker build --secret id=tok,src=./token.txt`. Build-check `SecretsUsedInArgOrEnv` warnt. (Der second-brain-Stack uebergibt Keys via `env_file:` zur Laufzeit, kein build-arg → sauber.)
- **Quelle:** docs.docker.com/build/building/secrets/

---

### 6.11 (KRITISCH, SELBST ERLEBT 27.07.2026) Build-Cache waechst unbegrenzt → 54 GB Bau-Abfall
- **Symptom:** Root-Disk 62 % voll (60 GB von 96 GB), obwohl die Anwendungen winzig sind. `du -sh /var/lib/docker` zeigt nur 6,9 GB — die Belegung ist scheinbar "unsichtbar". Erst `du -xh --max-depth=1 /var/lib` findet sie: **`/var/lib/containerd` = 54 GB** (2983 Snapshot-Schichten, aelteste 5 Wochen alt).
- **Ursache:** Docker 29 nutzt den **containerd-Snapshotter** (`docker info` → `driver-type: io.containerd.snapshotter.v1`). Der BuildKit-Cache liegt damit NICHT unter `/var/lib/docker`, sondern unter `/var/lib/containerd/io.containerd.snapshotter.v1.overlayfs/snapshots` — wer nur `/var/lib/docker` misst, sucht am falschen Ort. Ohne `builder.gc` in `daemon.json` gibt es **keine automatische Garbage Collection**: jeder Deploy per `docker compose up -d --build` (siehe `second-brain-server/DEPLOY.md`) legt neue Zwischenschichten an, die NIE wieder verschwinden. Bei 12 selbst gebauten Diensten (second-brain + werft-studio): ~54 GB in gut vier Wochen.
- **Erkennen (immer zuerst messen):** `docker system df` → Zeile `Build Cache` mit `RECLAIMABLE`-Spalte. Hier: `54.81GB / 54.41GB (99 %)` bei nur 2,95 GB Images.
- **FIX (funktionserhaltend, zwei Schichten):**
  1. **Sofort:** `docker builder prune -af` → gab 54,81 GB frei, Belegung 62 % → 10 %, alle 18 Container liefen unterbrechungsfrei weiter. Nur Bau-Zwischenschichten fallen weg; Images/Container/**Volumes** bleiben unangetastet (vgl. Warnung in §6.4).
  2. **Dauerhaft (Praevention):** `/etc/docker/daemon.json` → `"builder": {"gc": {"enabled": true, "policy": [{"keepDuration":"168h","reservedSpace":"10GB"},{"reservedSpace":"10GB"},{"reservedSpace":"10GB","all":true}]}}`. **Vor dem Neustart IMMER `dockerd --validate --config-file=/etc/docker/daemon.json`** — eine kaputte `daemon.json` verhindert den Daemon-Start und killt damit ALLE Dienste. Die Policy wird erst mit einem Daemon-Neustart aktiv (bei `Live Restore Enabled: false` = kurze Downtime), deshalb als Netz darunter:
  3. **Cron (wirkt sofort, ohne Neustart):** `30 3 * * 0 /opt/second-brain/scripts/docker-cache-cleanup.sh` (Repo: `second-brain-server/scripts/docker-cache-cleanup.sh`) — begrenzt den Cache woechentlich auf 10 GB und loggt vorher/nachher.
- **Flag-Falle (Docker 28+/29):** `--keep-storage` existiert NICHT mehr. Gueltig sind `--max-used-space`, `--min-free-space`, `--reserved-space` (per `docker builder prune --help` verifizieren). In `daemon.json` heissen die Policy-Felder `reservedSpace`, `keepDuration`, `filter`, `all`.
- **Kosten des Fixes:** nur der jeweils naechste Bau eines Dienstes ist einmalig langsamer (Cache wird neu aufgebaut). Kein Funktionsverlust.
- **Versionen:** Docker Engine 29.6.0 / buildx 0.35.0 / containerd-Snapshotter aktiv.
- **Quelle:** docs.docker.com/build/cache/garbage-collection/ · docs.docker.com/engine/manage-resources/pruning/ · eigener Vorfall 27.07.2026

---

## 7. Zeitzone/Locale · Image-Pinning · Build-Cache

### 7.1 `TZ`-Env in slim/alpine IGNORIERT (kein tzdata) → Logs in UTC
- **Symptom:** Container loggt UTC trotz `TZ=Europe/Berlin`.
- **Ursache:** `TZ` zeigt auf eine Zonendatei unter `/usr/share/zoneinfo/`; `*-slim`/alpine/distroless haben `tzdata` NICHT.
- **FIX (A, bevorzugt):** im Dockerfile `apt-get install -y --no-install-recommends tzdata` (Debian/Ubuntu-slim) bzw. `apk add --no-cache tzdata` (Alpine) + `ENV TZ=Europe/Berlin`. **(B):** `/etc/localtime`+`/etc/timezone` `:ro` mounten.
- **Direkt relevant:** der second-brain-`agent` installiert `tzdata` + setzt `AGENT_TZ` → richtig. `brain-api`/`dashboard` (`python:3.12-slim` ohne tzdata) loggen in **UTC** → falls lokale Log-Timestamps gewuenscht: tzdata + `TZ` ergaenzen oder /etc/localtime mounten.
- **Versionen:** per Design (Image-Verhalten), alle.
- **Quelle:** oneuptime.com/blog/post/2026-02-08-how-to-handle-timezone-configuration-in-dockerfiles/view

### 7.2 Sprachen lesen TZ unterschiedlich
- **Ursache:** Python liest `TZ` nur nach `time.tzset()`/braucht `tzdata` (OS oder pip fuer `zoneinfo`); Java beim Start oder `-Duser.timezone`; Node nativ (V8). Uneinheitliche Timestamps zwischen Services.
- **FIX:** Zonendaten im Image (7.1) + `TZ` VOR App-Start in der Umgebung; Java zusaetzlich `JAVA_TOOL_OPTIONS=-Duser.timezone=Europe/Berlin`.
- **Quelle:** oneuptime.com/blog/post/2026-01-16-docker-container-timezone/view

### 7.3 Locale fehlt → UTF-8/Umlaute kaputt (`C`/`POSIX` Default)
- **Symptom:** Umlaute als `?`/Mojibake; `locale` zeigt `C`/`POSIX`.
- **FIX (Debian/Ubuntu):** `apt-get install -y locales` + `sed -i '/de_DE.UTF-8/s/^# //g' /etc/locale.gen` + `locale-gen` + `ENV LANG=de_DE.UTF-8 LC_ALL=de_DE.UTF-8`. Alpine (musl): meist `ENV LANG=C.UTF-8`. **Falle:** `LANG` ≠ `LC_ALL` → inkonsistent (`LC_ALL` ueberschreibt). Im Zweifel beide gleich.
- **Versionen:** per Design (Minimal-Images).
- **Quelle:** leimao.github.io/blog/Docker-Locale/

### 7.4 `:latest` driftet zwischen Hosts/Zeitpunkten
- **Symptom:** Verschiedene Hosts/Tage laufen mit verschiedenem Code trotz `:latest`.
- **Ursache:** Tags sind **mutable**.
- **FIX:** per **`@sha256:`-Digest** pinnen (immutable) oder mindestens spezifischer Tag (`nginx:1.27.3`). Trade-off: bewusste Digest-Aktualisierung (Renovate/Dependabot). (Der second-brain-Stack pinnt Qdrant per Tag `v1.18.2` — gut; die `python:3.12-slim`-Basen sind NICHT per Digest gepinnt → driften beim Rebuild.)
- **Versionen:** per Design.
- **Quelle:** docs.docker.com/dhi/core-concepts/digests/

### 7.5 `docker compose up` zieht `:latest` NICHT neu
- **Symptom:** Neue `:latest` gepusht, aber `compose up` startet altes lokales Image.
- **Ursache:** Default `pull_policy: missing` (zieht nur, wenn lokal nicht vorhanden).
- **FIX:** `docker compose pull && docker compose up -d`, ODER `up --pull always`, ODER `pull_policy: always` (schnell-bewegte Tags) / `daily`/`weekly` in compose.
- **Versionen:** per Design; `pull_policy` mit Intervallen in v2.x+/v5.1.4.
- **Quelle:** docs.docker.com/reference/cli/docker/compose/pull/

### 7.6 DockerHub-Digest weicht ab / "manifest unknown"
- **Ursache:** Multi-Arch — DockerHub zeigt oft den Manifest-List-Digest, `docker pull` liefert den plattform-spezifischen.
- **FIX:** Manifest-List-Digest via `docker buildx imagetools inspect foo:tag`; plattform-spezifisch via `docker inspect --format='{{index .RepoDigests 0}}' foo:tag`. Fuer Multi-Arch den Manifest-List-Digest pinnen.
- **Quelle:** codegenes.net/blog/dockerhub-sha-digest-doesn-t-match/

### 7.7 COPY-Reihenfolge: Dependency-Layer baut bei jeder Code-Aenderung neu
- **Symptom:** Jede Code-Aenderung loest vollen `pip install`/`npm install` aus (Builds 3-5 min statt Sekunden).
- **Ursache:** `COPY . .` VOR `RUN pip install` invalidiert bei jeder Code-Aenderung den Dependency-Layer (Kaskade).
- **FIX:** Manifest ZUERST: `COPY requirements.txt ./` → `RUN pip install -r requirements.txt` → `COPY . .`. (Der second-brain-Stack macht das bereits richtig.)
- **Versionen:** per Design (wichtigste Build-Optimierung).
- **Quelle:** docs.docker.com/build/cache/optimize/

### 7.8 `apt-get update` in eigener Layer → "Hash Sum mismatch"/404
- **Symptom:** `apt-get install` schlaegt fehl, besonders nach Aenderung der install-Zeile.
- **Ursache:** `RUN apt-get update` in SEPARATER frueherer Layer wird gecacht; spaeteres `install` nutzt veraltete Paketlisten.
- **FIX:** IMMER in EINEM RUN: `RUN apt-get update && apt-get install -y --no-install-recommends paket-a paket-b && rm -rf /var/lib/apt/lists/*`. Doku woertlich: "Always combine `RUN apt-get update` with `apt-get install` in the same RUN statement."
- **Versionen:** per Design, alle.
- **Quelle:** docs.docker.com/build/building/best-practices/

### 7.9 Fehlende `.dockerignore` → riesiger Build-Kontext + Secrets-Leck
- **Symptom:** Build-Kontext riesig/langsam; `.git`/`node_modules`/`.env` landen im Image; Cache invalidiert grundlos.
- **FIX:** `.dockerignore` im Kontext-Root (`.git`, `node_modules`, `__pycache__`, `*.log`, `.env`, `.venv`, `dist`, `build`, `data/`). Cache-Korrektheits-Anforderung, nicht optional.
- **Versionen:** per Design.
- **Quelle:** docs.docker.com/build/building/best-practices/

### 7.10 Stale Build-Cache: `compose build` baut nicht neu (#7531) — GEFIXT
- **Symptom:** Code geaendert, `docker compose build` nutzt alten Cache / ignoriert Args.
- **FIX:** `docker compose build --no-cache --pull` (voller Neubau) / `--pull` (nur Base neu); Workaround direkt `docker buildx build --no-cache --pull`; gezielt `--no-cache-filter <stage>`.
- **Versionen:** docker/compose#7531 **CLOSED/COMPLETED 2020-12** (BuildKit Default ab Engine 23+) — teils per Design (Cache-Reuse).
- **Quelle:** github.com/docker/compose/issues/7531

### 7.11 BuildKit `COPY --link` / `--no-cache` Cache-Eigenheiten
- **Ursache:** `COPY --link` rebased gecachte Layer nicht in allen Faellen (docker/buildx#1099, **CLOSED/COMPLETED 2022**); `--no-cache` invalidiert `COPY`/`ADD` immer (moby/buildkit#4437, **gh-Status: OPEN**).
- **FIX:** `COPY --link` fuer unabhaengige, selten geaenderte Layer; zusaetzlich Cache-Mounts: `RUN --mount=type=cache,target=/root/.cache/pip pip install -r requirements.txt` (persistiert auch bei Layer-Invalidierung).
- **Versionen:** BuildKit (Default ab Engine 23+, gilt fuer 29.6.0).
- **Quelle:** github.com/moby/buildkit/issues/4437 · docs.docker.com/build/cache/optimize/

---

## Fix-Status (gh-verifiziert am 2026-06-24)

| Frueherer Bug | Issue | gh-Status | Bedeutung fuer v5.1.4 / 29.6.0 |
|---------------|-------|-----------|-------------------------------|
| 127.0.0.1-Ports im LAN erreichbar | moby/moby#41872, #45610 | gefixt ab Engine 28.0.0 | auf 29.6.0 behoben |
| start_period FailingStreak-Reset | docker/compose#11131 | CLOSED/COMPLETED 2023-10 | in v5.1.4 behoben |
| condition-Override-Merge | docker/compose#11993 | CLOSED/COMPLETED 2024-07 | in v5.1.4 behoben |
| depends_on bei up --detach / restart | docker/compose#9686, #9077 | CLOSED/COMPLETED 2022 | verbessert (`restart: true`-Langform) |
| condition mit volumes_from | docker/compose#9843 | CLOSED/COMPLETED 2022 | behoben (volumes_from meiden) |
| env_file laedt erwartete Datei nicht | docker/compose#9443 | CLOSED/COMPLETED 2022 | behoben |
| .env + --env-file Interpolation | docker/compose#13070 | CLOSED/COMPLETED 2025-07 | behoben |
| compose build nutzt alten Cache | docker/compose#7531 | CLOSED/COMPLETED 2020-12 | BuildKit-Default; `--no-cache` Workaround |
| COPY --link rebased Cache nicht | docker/buildx#1099 | CLOSED/COMPLETED 2022 | verbessert |
| oom_kill_disable | moby/moby#14440 | CLOSED/COMPLETED 2016 | dokumentiert (per Design gefaehrlich) |
| restart-Backoff-Reset | moby/moby#22283 | CLOSED/COMPLETED 2016 | 10s-Reset wie dokumentiert |

### Noch NICHT gefixt (Workaround bleibt aktiv)
- **moby/moby#32299** (OPEN) — Swarm `127.0.0.1`→`0.0.0.0`. Workaround `mode: host`. (Kein Swarm im second-brain-Stack.)
- **moby/moby#50694** (OPEN) — IPv4/IPv6-firewalld-Divergenz. IPv6-Regeln spiegeln / IPv6 deaktiviert lassen.
- **moby/moby#38564** (OPEN) — Subdir eines Host-Volumes als named/anon → als Host-Volume behandelt.
- **moby/buildkit#4437** (OPEN) — `--no-cache` invalidiert COPY/ADD immer.
- **Alle "per Design"-Fallen** (UFW-Bypass, json-file-Nicht-Rotation, depends_on-Kurzform, Bind-Mount-uid, `.env` vs `env_file`, TZ/Locale in slim, `:latest`-Drift, OOM-Sichtbarkeit) bleiben dauerhaft aktiv — der "Fix" ist die richtige Konfiguration, kein Versions-Update.

> **Ehrlichkeit:** "per Design" = Verhalten, das nicht "gefixt" wird; "CLOSED/COMPLETED" = im Tooling behoben, die zugrunde liegende Konfigurations-Falle kann trotzdem bestehen bleiben, wenn man sie falsch konfiguriert. Issue-Stati hart per `gh issue view` geprueft.

---

## Pflicht-Checkliste vor "Docker-Stack fertig/deployt"

- [ ] **Ports:** kein Dienst blanko `0.0.0.0`; nur lokal/VPN gebraucht → `127.0.0.1:`/Host-IP-Bind. UFW-Bypass bewusst (Docker filtert nicht ueber INPUT).
- [ ] **Memory:** jeder Dienst gedeckelt (`mem_limit`/`deploy.resources.limits`); `reservations` nur als Top-Level `mem_reservation`; Summe < Host-RAM.
- [ ] **Start-Reihenfolge:** DB-Abhaengige mit `depends_on: condition: service_healthy` + funktionierendem (image-internem) Healthcheck.
- [ ] **Mounts:** Host-Ordner-uid passt zur Container-uid (kein `chmod 777`); `:ro` nur fuer Nur-Lese-Pfade.
- [ ] **Restart/Boot:** `unless-stopped`; `docker.service` enabled; Boot-Abhaengigkeiten (VPN-IP/NFS) ueber systemd-Drop-in.
- [ ] **Disk:** Log-Rotation gesetzt (`daemon.json`/per-service `logging:` oder `local`-Driver); `.dockerignore` vorhanden.
- [ ] **.env:** Container-Variablen via `env_file:`/`environment:` (nicht nur Auto-`.env`); Secrets nie als build-arg.
- [ ] **Reproduzierbarkeit:** Images gepinnt (Tag/Digest); `tzdata`+Locale in slim-Images falls Timestamps/Umlaute wichtig; Dockerfile Manifest-vor-Code.
- [ ] **Nach Fehler:** VOLLTEXT dieses Almanachs lesen (Stufe B); neuen Bug hier ergaenzen.
