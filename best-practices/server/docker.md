# Docker & Docker-Compose Self-Hosting — Best Practices (Stand 2026-06-24)

> **Zweite Seite der Medaille zu `bugs/server/docker.md`.** Der Almanach sagt *was schiefgeht und
> wie man es loest*; hier steht, *wie man einen Compose-Stack fuer einen selbst gehosteten Dienst
> von vornherein richtig baut und betreibt, damit der Bug nie entsteht.* Beide Kurzchecks VOR der
> Arbeit lesen (erst Almanach, dann Best Practices). Funktionserhaltend (Direktive #3).
> Bezug: der second-brain-Stack (`~/proggs/second-brain-server`, compose.yaml + */Dockerfile).
>
> **Abgrenzung:** Uebergeordnete Infra (VPS-Wahl, Dimensionierung, DSGVO, 3-2-1-Backup, Reverse-Proxy/TLS)
> steht in `best-practices/second-brain/server-infrastruktur.md`. HIER konkret Docker/Compose-Mechanik.
> WireGuard selbst → `server/wireguard.md`; Qdrant-spezifischer Betrieb → `server/qdrant.md`;
> FastAPI-App-Schicht → `server/fastapi.md`.
>
> **Anker:** docker=29.6.0 (API 1.55, BuildKit-Default seit Engine 23, nftables-Backend opt-in seit 29.0,
> `start_interval` seit 25.0), compose=5.1.4 (Compose-v2-Linie), ubuntu=24.04.4 (cgroup v2), python=3.12-slim.
> Quellen: offizielle docs.docker.com (7-Opus-Schwarm, 2026-06, alle Kernpunkte mit Doc-Beleg) +
> Firecrawl/MiniMax-Recherche (extern, klar gelabelt). Jeder Punkt: offiziell zuerst, `extern` markiert.

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Regel | Detail |
|---|-------|--------|
| 1 | **Schlanke Basis + non-root** | `python:3.12-slim` als Default (glibc, breite Wheel-Kompatibilitaet). Alpine bei Python eher meiden (musl bricht C-Extensions). `USER` non-root, `useradd --uid`, `COPY --chown`. |
| 2 | **Multi-Stage nur bei echten Build-Deps** | Compiler/Header (`gcc`, `build-essential`, `lib*-dev`) NUR in die Build-Stage; venv nach `/opt/venv` und `COPY --from=builder`. Bei reinem Wheel-pip bringt Multi-Stage kaum Vorteil. |
| 3 | **Layer-Cache: Manifest vor Code** | `COPY requirements.txt` → `pip install` → `COPY . .`. `RUN --mount=type=cache,target=/root/.cache/pip`. `apt-get update && install` in EINEM RUN + `--no-install-recommends` + `rm -rf /var/lib/apt/lists/*`. |
| 4 | **`.dockerignore` ist PFLICHT** | Schlanker Context (Speed + Secret-Leak-Schutz). Mindestens `.git`, `__pycache__`, `*.pyc`, `.venv`, `.env`, `data`, `*.log`, Tests/Docs, `Dockerfile*`/`compose*`. Last-Match-Wins. |
| 5 | **Secrets nie ins Image, nie als Klartext-ENV** | Build-Zeit → BuildKit `--mount=type=secret` (nie `ARG`/`ENV` → `docker history`-Leak). Laufzeit → Compose `secrets:` (file-based, `/run/secrets/<name>`, ohne Swarm) oder `env_file:` fuer nicht-sensibles. `.env`-Interpolation geht NICHT in den Container. |
| 6 | **Healthcheck image-intern (kein curl)** | slim/alpine/distroless haben kein curl. Python-`urllib`/TCP-Socket-Check, `pg_isready`, BusyBox-`wget --spider`. `start_period` grosszuegig, `timeout < interval`. Listen-Form `["CMD",...]` (kein Shell-Zwang). |
| 7 | **Start-Ordnung: `depends_on` Langform** | DB-Abhaengige: `condition: service_healthy` (+ Healthcheck am Dependency). Init/Migration: `service_completed_successfully`. Kurz-Listenform wartet NUR auf "gestartet". |
| 8 | **Restart/Boot: `unless-stopped` + systemd** | Dienste `restart: unless-stopped` (Crash/Reboot). `systemctl enable docker.service`. Host-Boot-Deps (VPN-IP/NFS) ueber systemd-Drop-in `After=`/`Wants=`/`RequiresMountsFor=`. Nicht systemd UND compose-restart fuer denselben Container mischen. |
| 9 | **Netz: intern per DNS, aussen minimal** | Inter-Container ueber Service-DNS (`http://brain-api:8000`), KEIN `ports:` noetig. Nach aussen nur was muss; an `127.0.0.1:`/VPN-IP binden (nie blanko `0.0.0.0`, Docker umgeht UFW). DB-Schicht in `internal: true`-Netz. |
| 10 | **Memory/Disk/Pinning** | Jeden Dienst deckeln (`mem_limit` + `mem_reservation`); Summe < Host-RAM. Log-Rotation (per-service `logging:` oder `local`-Driver). Images pinnen (Tag/Digest). Bind-Mount-uid == Container-uid (kein `chmod 777`). |

---

## 🔗 Bezugs-Tabelle: Best-Practice ↔ Bug
| Best-Practice (hier) | Bug-Abschnitt (`bugs/server/docker.md`) |
|----------------------|------------------------------------------|
| §1 Image-Bau (slim/Multi-Stage/non-root/Layer-Cache/apt) | §7.7 COPY-Reihenfolge · §7.8 apt-get-Layer · §4 non-root-uid (Kontext) |
| §2 BuildKit (cache/secret/`# syntax`/`COPY --link`) | §6.10 ARG/ENV-Secret-Leak · §7.11 Cache-Mounts |
| §3 `.dockerignore` | §7.9 fehlende `.dockerignore` → Context + Secret-Leak |
| §4 Secrets & ENV (Compose) | §6.6 `.env` vs `env_file` · §6.8 `.env`-Quoting · §6.9 Praezedenz · §6.10 ARG/ENV-Leak |
| §5 Healthchecks | §3.3 curl/wget fehlt · §3.4 `start_period` · §3.7 `CMD` vs `CMD-SHELL` |
| §6 `depends_on` / restart / Boot | §3.1 Kurzform wartet nicht · §3.2 `condition` ignoriert · §3.5 `--no-deps`/restart · §5 restart-Policies + Boot |
| §7 Netze & Port-Binding | §1.1 UFW-Bypass · §1.10 127.0.0.1 schuetzt nicht vor Containern · §1.3 Swarm-Bind |
| §8 Memory & Limits | §2.1 `deploy.limits` wirkt · §2.2 `reservations` ohne Swarm wirkungslos · §2.6 oom-kill-disable |
| §9 Volumes / Logging / Pinning / TZ | §4 Bind-Mount-Permissions · §6.1-§6.5 Log-Rotation/Disk · §7.1-§7.6 TZ/Locale/`:latest`-Drift |

---

## §1 — Image-Bau: schlanke, gecachte, non-root Images

**Basis-Wahl (offiziell + extern):** `python:3.12-slim` (Debian/glibc) ist der Default — "choose a
minimal base image ... shrinks the size of your image and minimizes the number of vulnerabilities"
(docs.docker.com/build/building/best-practices/, 2026-06). **Alpine bei Python eher vermeiden**: musl
statt glibc zwingt C-Extensions (numpy/Pillow/psycopg2) oft zum Kompilieren → laengere Builds, groesser
(`extern`). **distroless** (`gcr.io/distroless/python3`, Debian/glibc, non-root uid 65532, keine Shell/kein
pip) ist die haerteste Wahl — erzwingt Multi-Stage (venv kopieren) und `CMD` in Exec-Form mit vollem
Interpreter-Pfad; nicht in den Container debuggbar (`extern`). Der offizielle Python-Guide nutzt inzwischen
Docker Hardened Images (nonroot by default, docs.docker.com/guides/python/).

**Multi-Stage — wann (offiziell):** "leaving behind everything you don't want in the final image"
(docs.docker.com/build/building/multi-stage/). Lohnt, sobald `pip install` Compiler/Header braucht
(C-Extensions). Bei reinen Pure-Python-Wheels bringt es **kaum Groessenvorteil**, weil `slim` ohnehin
keinen Compiler enthaelt (`extern`, pythonspeed 2024-09). **Faustregel: Build-Deps fuer `pip install`? → Multi-Stage. Sonst optional.**

**venv-Copy-Skelett (FastAPI, slim, mit C-Extension-Beispiel libpq):**
```dockerfile
# syntax=docker/dockerfile:1
# ---- Build-Stage ----
FROM python:3.12-slim AS builder
ENV PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1
RUN apt-get update && apt-get install -y --no-install-recommends \
        build-essential gcc libpq-dev \
    && rm -rf /var/lib/apt/lists/*
RUN python -m venv /opt/venv
ENV PATH="/opt/venv/bin:$PATH"
COPY requirements.txt .
RUN --mount=type=cache,target=/root/.cache/pip pip install -r requirements.txt
# ---- Runtime-Stage ----
FROM python:3.12-slim
ENV PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1
RUN apt-get update && apt-get install -y --no-install-recommends libpq5 \
    && rm -rf /var/lib/apt/lists/*
COPY --from=builder /opt/venv /opt/venv
ENV PATH="/opt/venv/bin:$PATH"
RUN useradd --create-home --uid 10001 appuser
USER appuser
WORKDIR /app
COPY --chown=appuser:appuser . .
EXPOSE 8000
CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8000", "--no-access-log"]
```

**Layer-Cache (offiziell, build/cache/optimize/):** "place instructions for layers that change
frequently after the ones that incur less changes." → Manifest ZUERST kopieren+installieren, App-Code
ZULETZT ("no need to rebuild those layers when a project file has changed"). **apt in EINEM RUN**
(best-practices/): "Always combine `RUN apt-get update` with `apt-get install` in the same RUN statement"
+ `--no-install-recommends` + `rm -rf /var/lib/apt/lists/*` (sonst Stale-Cache + groesseres Image).

**non-root (offiziell):** "If a service can run without privileges, use `USER`." `useradd --uid <N>` +
`USER` + `COPY --chown` — begrenzt Schaden bei Container-Ausbruch und macht Bind-Mount-Permissions sauber (§9).

> **Im Stack:** Alle 4 Dockerfiles vorbildlich — `python:3.12-slim`, non-root (uid 10001/1000/10002),
> Manifest vor Code, `PIP_NO_CACHE_DIR`. **Ausbaupotenzial:** kein Multi-Stage (ok, da keine C-Extensions
> kompiliert werden — reine Wheels), kein `# syntax`/Cache-Mount (siehe §2).

## §2 — BuildKit: cache-, secret-, syntax-, link-Patterns (alle offiziell)

- **`# syntax=docker/dockerfile:1` als ALLERERSTE Zeile** — "All parser directives must be at the top";
  steht sie nicht zuoberst, wird sie als Kommentar ignoriert. Zieht die neueste stabile Dockerfile-Syntax
  ohne Engine-Upgrade → macht die `--mount`-Features zukunftssicher. (docs.docker.com/reference/dockerfile/)
- **pip-Cache-Mount:** `RUN --mount=type=cache,target=/root/.cache/pip pip install -r requirements.txt`.
  Cache ist "cumulative across builds", liegt **ausserhalb des Layers** → ueberlebt Layer-Invalidierung
  ("you only download new or changed packages"). `sharing=locked` braucht NUR apt, **nicht** pip. Bei
  non-root-USER Pfad anpassen (`/home/<user>/.cache/pip`). (build/cache/optimize/)
- **Build-Secret-Mount statt ARG/ENV:** ARG/ENV "persist in the final image" → `docker history`-Leak.
  `RUN --mount=type=secret,id=tok ...` + `docker build --secret id=tok,src=./tok.txt` — nur "for the
  duration of the build instruction", landet in keinem Layer. Der Build-Check **`SecretsUsedInArgOrEnv`**
  warnt aktiv. (build/building/secrets/ · reference/build-checks/secrets-used-in-arg-or-env/)
- **Compose `build.secrets`** (v2/v5): Top-Level-Secret + explizite Freigabe pro Service-Build (Pflicht),
  short syntax → `/run/secrets/<name>`, long syntax mit `target/uid/gid/mode`; mappt auf `docker build
  --secret`. Bonus `build.cache_from`/`cache_to` (registry/local/gha) fuer geteilten Cache. (reference/compose-file/build/)
- **`COPY --link`** — Dateien "remain independent on their own layer and don't get invalidated when
  commands on previous layers are changed"; ideal in Multi-Stage (venv/Wheels ins Runtime holen) +
  Rebasing nach Base-Image-Update. (reference/dockerfile/)

## §3 — `.dockerignore` (offiziell + etabliertes Template)

**Was/Warum (offiziell, build/concepts/context/):** schliesst Dateien aus dem Build-Context aus, BEVOR er
an den Daemon gesendet wird — "improving build speed, especially when using a remote builder". Syntax:
Unix-Glob, `**` rekursiv, `!`-Ausnahmen, **Last-Match-Wins** (Reihenfolge zaehlt!), `#`-Kommentare.
Schlanker Context = schnellerer Upload + Cache-Korrektheit (`COPY . .` invalidiert sonst bei jeder
Context-Aenderung) + **Secret-Leak-Schutz**: ohne `.dockerignore` landet `.env`/`.git` via `COPY . .` in
einem **immutable Image-Layer** — spaeteres Loeschen heilt das NICHT (`docker history` liest es aus).

**Minimal-`.dockerignore` fuer einen FastAPI/Python-Dienst** (Prinzip offiziell, Vollliste `extern`/etabliert):
```dockerignore
.git
.gitignore
__pycache__/
*.py[cod]
.venv/
venv/
env/
.pytest_cache/
.mypy_cache/
.ruff_cache/
.coverage
htmlcov/
dist/
build/
*.egg-info/
.env
.env.*
*.pem
*.key
data/
*.sqlite3
*.log
logs/
*.md
docs/
.idea/
.vscode/
.DS_Store
node_modules/
Dockerfile*
compose*
.dockerignore
```

> **Im Stack:** Aktuell **keine `.dockerignore`** in `brain-api/`, `agent/`, `mcp-server/`, `dashboard/`
> → konkreter Handlungspunkt (Build-Context enthaelt `.env`-Risiko + lokale `*-logs`/`*-data`-Ordner).
> Je Dienst die obige Datei anlegen.

## §4 — Secrets & Umgebungsvariablen in Compose (offiziell)

**Rangordnung der Mechanismen (offiziell, compose/how-tos/use-secrets/ + .../environment-variables/):**

| Mechanismus | Zweck | Sensibel? |
|-------------|-------|-----------|
| `secrets:` (file-based) | Passwoerter/API-Keys/Tokens/Zertifikate → Mount `/run/secrets/<name>` (read-only) | **JA** — der empfohlene Weg |
| `env_file:` | nicht-sensible Config aus der compose.yaml raushalten | nein |
| `environment:` | Klartext im YAML | nein — fuer Secrets vermeiden |
| `.env`-Interpolation `${VAR}` | NUR Substitution IN der compose.yaml (Image-Tags etc.) | **geht NICHT in den Container** |

- **Warum nicht ENV fuer Secrets (offiziell):** "Environment variables are often available to all
  processes ... can also be printed in logs when debugging errors without your knowledge." Bei korrekten
  Secrets erscheinen Klartextwerte **nicht** in `docker inspect`/`compose exec`.
- **File-based ohne Swarm:** Funktioniert in der gesamten Compose-v2/v5-Linie; nur `environment`-Secrets
  sind Swarm-inkompatibel, `file`/`external` gelten fuer beide (der `version:`-Top-Key ist obsolet).
- **`*_FILE`-Konvention (offiziell, docker-library):** postgres/mysql lesen den Wert aus einer Datei, wenn
  `POSTGRES_PASSWORD_FILE=/run/secrets/db_password` zeigt — kein Klartext durch `environment:`.
- **Datei-Rechte (WICHTIG, moby#40046):** bei file-backed Compose-secrets werden `uid/gid/mode` **ignoriert**
  (Container-Default `0444` read-only). Der Hebel ist die **Host-Quelldatei**: `chmod 600`/`0400`,
  Owner = Betreiber, und in `.gitignore`. (`environment`-Mode-Secrets beachten `uid/gid/mode`, sind aber
  Compose-only + erben ENV-Sichtbarkeitsrisiken → fuer Self-Hosting ist `file:` robuster.)

**Haertungs-Skelett:**
```yaml
secrets:
  db_password:
    file: ./secrets/db_password.txt   # Host: chmod 600, Owner = Betreiber, in .gitignore
services:
  db:
    image: postgres:18
    environment:
      POSTGRES_PASSWORD_FILE: /run/secrets/db_password
    secrets:
      - db_password
    env_file:
      - .env                          # nur nicht-sensible Werte
```

> **Im Stack:** Heute `env_file: .env` pro Dienst (sauber — Keys gehen in den Container, kein build-arg,
> kein Klartext im compose). Wer haerter will: API-Keys auf file-based `secrets:` + `*_FILE`/Pfad-Lesen
> umstellen. `.env`-Quoting beachten (Werte mit `#`/Leerzeichen/`$` in Single-Quotes; `$$` fuer literalen `$`).

## §5 — Healthchecks image-intern (kein curl) — offiziell

slim/alpine/distroless haben oft **kein curl/wget** → Check im selben Runtime schreiben (keine
Zusatz-Abhaengigkeit, kein +2,5 MB Angriffsflaeche, `extern`).

```yaml
# HTTP-Dienst (FastAPI, python-slim) — stdlib urllib, Listen-Form (kein Shell):
healthcheck:
  test: ["CMD","python","-c","import urllib.request,sys; sys.exit(0 if urllib.request.urlopen('http://127.0.0.1:8000/health',timeout=5).status==200 else 1)"]
  interval: 30s
  timeout: 5s
  retries: 3
  start_period: 30s

# Dienst OHNE HTTP-Health (MCP/TCP) — reiner Socket-Connect:
healthcheck:
  test: ["CMD","python","-c","import socket,sys; s=socket.create_connection(('127.0.0.1',8001),5); s.close(); sys.exit(0)"]

# Postgres (offiziell empfohlen) — pg_isready, CMD-SHELL wegen $$-Expansion:
healthcheck:
  test: ["CMD-SHELL","pg_isready -U $${POSTGRES_USER} -d $${POSTGRES_DB}"]
  interval: 10s
  timeout: 10s
  retries: 5
  start_period: 30s
```

- **Timing (offiziell):** `interval`(30s)=Abstand nach Start, `timeout`(30s, < interval halten)=max
  Probe-Dauer, `retries`(3)=Fehler bis `unhealthy`, `start_period`(0s)=Grace-Phase (Fehler zaehlen
  nicht), **`start_interval`(5s, Engine 25.0+)**=haeufigere Probes WAEHREND der Startphase → schneller
  `healthy`. Faustwert langsamer Start: `start_period` grosszuegig (30–120s) + `start_interval: 5s`.
- **`CMD` vs `CMD-SHELL`:** Listen-Form `["CMD",...]` startet das Binary direkt (kein `/bin/sh` noetig —
  Pflicht fuer slim/distroless). `CMD-SHELL`/String-Form nur fuer `||`, Pipes, `$VAR`-Expansion.
- **Dockerfile vs compose:** Image-eigener Default → `HEALTHCHECK` im Dockerfile (reist mit, auch `docker
  run`). Umgebungs-Tuning/Abschalten → compose (ueberschreibt Dockerfile; `test: ["NONE"]`/`disable: true`).
- **State:** `starting` → erste OK → `healthy`; `retries` Fehler → `unhealthy`. Exit **0=healthy, 1=unhealthy,
  2=reserviert (nie nutzen)**. `depends_on: condition: service_healthy` wartet genau darauf (§6).

> **Im Stack:** brain-api/agent/dashboard nutzen image-internes Python-`urllib`, mcp einen TCP-Socket-Check
> — vorbildlich (genau das Muster oben). `start_period` 20–40s gesetzt.

## §6 — `depends_on`, restart-Policy & Boot-Reihenfolge (offiziell)

**`depends_on` Langform (compose/how-tos/startup-order/):** Kurz-Listenform wartet NUR bis der Container
*laeuft*, nicht bis *healthy* — und ignoriert `condition` komplett. Mapping-Langform nutzen:

| `condition` | wann |
|-------------|------|
| `service_started` | loser Dienst (Cache mit Reconnect) |
| `service_healthy` | **DB-/Backing-Service-Abhaengige** (wartet auf Healthcheck) |
| `service_completed_successfully` | **Init/Migrations-Container** (App startet nach Exit 0) |

`restart: true` (seit Compose 2.17) startet den Dependent neu, wenn die Dependency aktualisiert wird;
`required: false` (seit 2.20) warnt nur statt zu blockieren.

**restart-Policy (engine/containers/start-containers-automatically/):**

| Policy | Crash | Exit 0 | nach `docker stop` | nach Reboot |
|--------|-------|--------|--------------------|-------------|
| `no` (Default) | nein | nein | — | nein |
| `on-failure[:N]` | ja (bis N) | nein | — | nur wenn vorher laufend |
| `always` | ja | ja | bleibt gestoppt bis Daemon-Restart, dann wieder hoch | ja |
| `unless-stopped` | ja | ja | bleibt gestoppt | bleibt gestoppt (wenn vor Reboot manuell gestoppt) |

**`unless-stopped` ist der Dienst-Standard** — merkt sich manuelles `docker stop` ueber den Reboot
(`always` ignoriert das). Zwei Schutzmechanismen: 10-s-Stabilitaetsfenster + Manual-Stop-Sperre (verhindern
Crash-Loops), exponentielles Backoff. **Komplementaer:** `depends_on`+`healthcheck` ordnet NUR den initialen
Start; laufende Crash-/Reboot-Recovery macht die restart-Policy. **Beides setzen** + App reconnect-resilient bauen.

**Boot (offiziell + extern):** `systemctl enable docker.service` ist Pflicht, sonst startet nach Reboot
KEIN Container. Host-Boot-Abhaengigkeiten (VPN-IP, NFS) kann `depends_on` NICHT — dafuer systemd:
```ini
# /etc/systemd/system/mystack.service
[Unit]
Description=My Compose Stack
Requires=docker.service
After=docker.service network-online.target
Wants=network-online.target
[Service]
Type=oneshot
RemainAfterExit=yes
WorkingDirectory=/srv/mystack
ExecStart=/usr/bin/docker compose up -d --remove-orphans
ExecStop=/usr/bin/docker compose down
TimeoutStartSec=0
[Install]
WantedBy=multi-user.target
```
Boot-Deps als Drop-in (`*.service.d/deps.conf`): `After=`/`Wants=wg-quick@wg0.service`,
`RequiresMountsFor=/srv/mystack/data`. **Nicht** systemd UND compose-`restart:` fuer denselben Container
mischen (offizielle Warnung — Konflikt). Quadlet (Podman) ist die moderne systemd-native Alternative (`extern`).

> **Im Stack:** `restart: unless-stopped` ueberall (richtig). `depends_on` nutzt die **Kurzform**
> (`depends_on: [qdrant]` / `[brain-api]`) → wartet nur auf Start, nicht auf `healthy`. Da brain-api/agent
> Healthchecks haben, ist die Aufwertung auf `condition: service_healthy` ein sinnvoller Haertungsschritt
> (verhindert "connection refused" beim ersten `up`). Boot-Ordnung VPN-IP `10.8.0.1` ueber systemd-Drop-in
> `After=wg-quick@wg0` + `restart: unless-stopped` ist bereits der dokumentierte Plan.

## §7 — Netze & Port-Binding (offiziell)

- **Intern per Service-DNS:** Container im Compose-Default-Netz erreichen sich per Service-Name
  ("always reference services by name, not IP address") — `http://brain-api:8000`. Fuer reine
  Inter-Container-Kommunikation ist **kein `ports:` noetig**; "the Docker daemon blocks access to ports
  that have not been published". Das ist die sichere Default-Architektur. (compose/how-tos/networking/)
- **`internal: true`:** "externally isolated network" ohne Gateway — DB-Schicht daran haengen → nur fuer
  die App erreichbar, ohne eigenen Internetzugang. Dienste an internal- UND normalem Netz behalten ueber
  das normale Netz externe Konnektivitaet. (reference/compose-file/networks/)
- **Aussen-Bind minimal:** `ports:` nur fuer wirklich extern noetige Dienste (Reverse-Proxy 80/443).
  Sonst an `127.0.0.1:PORT:PORT` (nur Host/hinter Proxy) oder VPN-IP `10.8.0.1:8000:8000` binden — **nie
  blanko `0.0.0.0`** (Default ohne Host-IP). **Wichtig:** Docker umgeht UFW ("packets are diverted before
  ... the INPUT/OUTPUT chains that ufw uses") → der Host-IP-Bind IST die wirksame Grenze, nicht UFW.
  (engine/network/port-publishing/ · .../packet-filtering-firewalls/)
- **`expose:` vs `ports:`:** `expose` dokumentiert nur ("should not be published to the host"), `ports`
  veroeffentlicht auf dem Host. `expose` ist KEINE Sicherheitsgrenze (Container im selben Netz erreichen
  jeden lauschenden Port ohnehin).
- **Isolation:** `127.0.0.1`-Bind regelt nur die HOST-Veroeffentlichung, NICHT Verkehr zwischen Containern
  im selben Netz → echte Isolation kommt aus getrennten/`internal`-Netzen.

**Referenz-Muster (Reverse-Proxy + abgekapselte DB):**
```yaml
services:
  proxy:        { image: caddy:2, ports: ["80:80","443:443"], networks: [edge] }   # einziges Tor
  brain-api:    { build: ./brain-api, expose: ["8000"], networks: [edge, backend] } # kein ports: -> per DNS
  db:           { image: postgres:18, networks: [backend] }                          # nie veroeffentlicht
networks:
  edge:    { driver: bridge }
  backend: { driver: bridge, internal: true }   # DB-Schicht ohne Gateway
```

> **Im Stack:** Alle Dienste an `127.0.0.1`/WireGuard-IP gebunden, interne Calls per Service-DNS
> (`http://brain-api:8000`, `QDRANT_HOST: qdrant`) — vorbildlich. Optionaler Haertungsschritt: Qdrant in
> ein `internal: true`-Backend-Netz (statt `127.0.0.1`-Host-Bind), da nur brain-api es braucht.

## §8 — Memory & Resource-Limits (offiziell)

- **`deploy.resources.limits.memory`/`.cpus` WIRKT** in Compose v2/v5 ohne Swarm (die alte
  "wird ignoriert"-Weisheit galt nur Compose v1). Gleichwertig: Top-Level `mem_limit`/`cpus` (eindeutigster
  Weg). **Aber `deploy.resources.reservations.memory` wirkt OHNE Swarm NICHT** → fuer echte Soft-Reservierung
  Top-Level `mem_reservation:` (setzt cgroup `memory.low`). (compose/issues/7307, /10046)
- **8-GB-Baseline-Pattern:** pro Dienst `mem_limit` + `mem_reservation` + `cpus` + `stop_grace_period: 30s`
  + `restart: unless-stopped`; Summe aller `mem_limit` deutlich < Host-RAM (Reserve fuer Host/Kernel/Page-Cache).
  Container-aware-Runtimes (JVM `MaxRAMPercentage`, Node `--max-old-space-size`) ans Limit koppeln.
- **`--oom-kill-disable` NIE ohne Limit** (friert den Host ein). Exit 137 ist NICHT immer OOM
  (`docker inspect --format '{{.State.OOMKilled}}'` pruefen).

> **Im Stack:** `deploy.resources.limits` (memory+cpus) pro Dienst gesetzt — wirkt. **Praezisierung:** die
> `deploy.resources.reservations.memory`-Eintraege (Qdrant 512M, brain-api 256M …) sind ohne Swarm
> **wirkungslos** → fuer echte Soft-Reservierung auf Top-Level `mem_reservation:` umstellen. Summe der
> `mem_limit` (2G+1G+512M+256M+256M ≈ 4 GB) < 8 GB — gesunde Reserve.

## §9 — Volumes, Logging, Pinning, Zeitzone (offiziell)

- **Bind-Mount-Permissions:** Bind-Mount = Host-uid/gid gilt direkt → Host-Ordner muss der Container-uid
  gehoeren (`chown -R <uid>:<uid> ./data ./logs`), NIE `chmod 777`. `:ro` fuer reine Lese-Pfade. Named
  Volume = Ownership einmalig aus dem Image-Pfad initialisiert.
- **Log-Rotation (sonst Disk voll):** `json-file` rotiert ohne `max-size`/`max-file` NICHT. Pro Service im
  Repo versioniert (empfohlen statt globaler `daemon.json`):
  ```yaml
  logging: { driver: "json-file", options: { max-size: "10m", max-file: "3" } }   # oder driver: "local"
  ```
  Gilt nur fuer NEU erstellte Container (`up -d --force-recreate`). Diagnose: `docker system df`.
- **Image-Pinning:** mutable Tags driften → per Tag (`qdrant/qdrant:v1.18.2`) oder `@sha256:`-Digest
  pinnen; `docker compose pull` / `pull_policy: always` fuer schnell-bewegte Tags.
- **Zeitzone/Locale:** `TZ` wird in slim/alpine IGNORIERT (kein `tzdata`) → `apt-get install tzdata` +
  `ENV TZ=...` ODER `/etc/localtime`+`/etc/timezone` `:ro` mounten. Umlaute → `locales` + `LANG`/`LC_ALL`.

> **Im Stack:** `agent` mountet Logbuch auf die Samba-Platte (uid 1000 == 'frank' — sauber), `dashboard`
> mountet `/opt/second-brain:/hostfs:ro` bewusst read-only. Qdrant per Tag gepinnt. **Handlungspunkte:**
> (1) per-service `logging:`-Limit ergaenzen (Startup-Check meldete Disk ~98%); (2) `python:3.12-slim`-Basen
> nicht per Digest gepinnt → driften beim Rebuild; (3) brain-api/dashboard ohne `tzdata` → Logs in UTC
> (falls lokale Timestamps gewuenscht: tzdata+`TZ` ergaenzen).

---

## Pflicht-Checkliste vor "Compose-Stack fertig/deployt"

- [ ] **Image:** schlanke Basis (slim, kein Alpine fuer Python), non-root `USER`, Manifest vor Code, apt in EINEM RUN, `.dockerignore` pro Dienst.
- [ ] **BuildKit:** `# syntax=docker/dockerfile:1` Zeile 1; pip-Cache-Mount; Secrets via `--mount=type=secret`, NIE ARG/ENV.
- [ ] **Secrets:** sensibles via `secrets:` (file-based) oder `env_file:`; `.env`-Interpolation geht NICHT in den Container; Host-Secret-Datei `chmod 600`, in `.gitignore`.
- [ ] **Healthcheck:** image-intern (kein curl), Listen-Form, `start_period` grosszuegig, `timeout < interval`.
- [ ] **Start-Ordnung:** DB-Abhaengige `depends_on: condition: service_healthy`; Init `service_completed_successfully`.
- [ ] **Restart/Boot:** `unless-stopped`; `systemctl enable docker.service`; Host-Boot-Deps via systemd-Drop-in; nicht systemd+compose-restart mischen.
- [ ] **Netz:** intern per Service-DNS (kein `ports:`); aussen nur was muss, an `127.0.0.1`/VPN-IP (nie `0.0.0.0`); DB in `internal:true`-Netz.
- [ ] **Memory:** jeder Dienst `mem_limit` + `mem_reservation` (Top-Level, nicht `deploy.reservations`); Summe < Host-RAM.
- [ ] **Disk/Pinning/TZ:** per-service `logging:`-Limit; Images gepinnt; Bind-Mount-uid passt; `tzdata`/Locale falls noetig.
- [ ] **Bei Fehler:** VOLLTEXT von `bugs/server/docker.md` lesen (Stufe B).
