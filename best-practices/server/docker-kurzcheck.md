# Docker & Docker-Compose Self-Hosting Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

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
