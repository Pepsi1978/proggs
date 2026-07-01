# Deployen auf Cortex / Second-Brain-Server

> Kurzanleitung: Wie man eine Aenderung am Second-Brain-Server (Cortex) auf den VPS bringt.
> Genau EIN Weg. Nicht suchen, nicht raten — dieser Weg gilt.

## Das Wichtigste in einem Satz

Auf dem VPS liegt **KEIN git-Repo**. Deployen heisst: geaenderte Dateien per **scp** ins
Server-Verzeichnis hochladen und den betroffenen Dienst per **`docker compose up -d --build`**
neu bauen. Fertig. (Es gibt KEIN `git pull` auf dem Server — wer danach sucht, sucht umsonst.)

## Server-Fakten (die "wichtigen Informationen")

| Was | Wert |
|-----|------|
| VPS-IP | `168.231.83.205` (Hostinger) |
| SSH-User | `root` |
| SSH-Key | `~/SK/second-brain/id_ed25519` (passwortlos, BatchMode) |
| App-Verzeichnis auf dem VPS | `/opt/second-brain` |
| Lokale Quelle (Repo) | `~/proggs/second-brain-server/` |
| compose-Datei | `/opt/second-brain/compose.yaml` (im Repo: `second-brain-server/compose.yaml`) |
| Secrets (NICHT im Repo) | `/opt/second-brain/.env` auf dem VPS; Tool-Konfig lokal in `~/SK/second-brain/` |

Alle Dienste sind an die WireGuard-IP `10.8.0.1` gebunden (oeffentlich unsichtbar, nur ueber den
VPN-Tunnel erreichbar). Oeffentlich offen ist nur SSH (22) + WireGuard (51820/udp).

## Dienste und ihre Bau-Pfade

| Dienst (compose-Name) | Bau-Pfad (Repo + VPS) | Port (an 10.8.0.1) | Selbst gebaut? |
|-----------------------|------------------------|--------------------|----------------|
| `brain-api` | `./brain-api` | 8000 (`/health`) | ja (`--build`) |
| `mcp` | `./mcp-server` | 8001 (TCP) | ja (`--build`) |
| `agent` | `./agent` | 8002 (`/health`) | ja (`--build`) |
| `dashboard` | `./dashboard` | 8003 (`/api/health`) | ja (`--build`) |
| `qdrant` | (Fertig-Image, kein Bau) | 127.0.0.1:6333 | nein |
| `caddy` | (Fertig-Image, kein Bau) | 443 | nein |

**Wichtig:** Bei den selbst gebauten Diensten ist der Code **ins Docker-Image gebacken**
(`build: ./<dienst>`, KEIN Code-Volume-Mount). Darum ist `--build` PFLICHT — ohne `--build`
laeuft weiter der alte Code, obwohl die Datei auf dem Server schon neu ist.

## Der Deploy-Weg (Schritt fuer Schritt)

> ### ⚠️ PFLICHT VOR JEDEM DEPLOY: sichtbare Version + Timestamp erhoehen (Frank-Wunsch 2026-07-01)
> Bei JEDER Server-Aenderung — **egal welcher Dienst** (agent, brain-api, mcp, dashboard) — MUSS die
> **sichtbare Dashboard-Version** `VERSION` in `dashboard/app.py` erhoeht und ihr **Timestamp auf die
> aktuelle Uhrzeit** gesetzt werden. Das ist der EINE Marker, den Frank im Dashboard-Footer sieht:
> `V0.x (TT.MM.JJJJ, HH:MM Uhr)`. Format:
> `VERSION = "0.28.0 (TT.MM.JJJJ, HH:MM Uhr)"  # 0.28.0: <was deployt wurde, kurz>`.
> Aktuelle Zeit holen: `Get-Date -Format 'dd.MM.yyyy, HH:mm'` (Windows).
> **Danach das dashboard IMMER mit-deployen** (`docker compose up -d --build dashboard`), auch wenn nur
> der agent geaendert wurde — sonst bleibt der Footer auf dem alten Stand und Frank sieht nicht, dass
> sein Deploy angekommen ist. (Regel `version-bump-visible-always`: jede Aenderung sichtbar + Timestamp.)

Beispiel: Aenderung am **dashboard** (`app.py`, `static/index.html` o.ae.).

```bash
# 0. VORBEREITUNG: Windscribe / jeden Full-Tunnel-VPN AUS.
#    Ein Full-Tunnel blockiert SSH zum VPS. (WireGuard zum Gehirn darf an bleiben.)

# 1. Geaenderte Dateien hochladen (scp, mit dem SSH-Key):
scp -i ~/SK/second-brain/id_ed25519 -r \
  ~/proggs/second-brain-server/dashboard/* \
  root@168.231.83.205:/opt/second-brain/dashboard/

# 2. Auf dem VPS NUR den geaenderten Dienst neu bauen + starten:
ssh -i ~/SK/second-brain/id_ed25519 root@168.231.83.205 \
  "cd /opt/second-brain && docker compose up -d --build dashboard"

# 3. Verifizieren (Status healthy + neue Version):
ssh -i ~/SK/second-brain/id_ed25519 root@168.231.83.205 \
  "cd /opt/second-brain && docker compose ps && curl -s http://10.8.0.1:8003/api/health"
```

Fuer einen anderen Dienst einfach `dashboard` durch `brain-api`, `agent` oder `mcp` ersetzen
(und den passenden Bau-Pfad aus der Tabelle nehmen). Mehrere Dienste gleichzeitig:
`docker compose up -d --build brain-api mcp`.

Hat sich die `compose.yaml` selbst geaendert: die compose.yaml ebenfalls hochladen
(`scp ... compose.yaml root@...:/opt/second-brain/`), dann `docker compose up -d`.

## Verifikation (woran man sieht, dass es lief)

- `docker compose ps` → der Dienst ist `Up` und `(healthy)`.
- Health-Endpunkt (vom VPS aus, oder vom PC ueber WireGuard):
  - dashboard: `curl http://10.8.0.1:8003/api/health` (zeigt `dash_version`)
  - brain-api: `curl http://10.8.0.1:8000/health`
  - agent: `curl http://10.8.0.1:8002/health`

## Stolperfallen (die Zeit kosten, wenn man sie nicht kennt)

1. **KEIN `git pull` auf dem VPS** — `/opt/second-brain` ist kein git-Repo. Immer scp + `--build`.
2. **`--build` ist Pflicht** — der Code ist ins Image gebacken; ohne `--build` bleibt der alte Code aktiv.
3. **Windscribe / Full-Tunnel AUS** vor dem Deploy — sonst haengt/scheitert SSH.
4. **Ports an `10.8.0.1`**, nicht `0.0.0.0` und nicht `127.0.0.1` — vom VPS aus mit `10.8.0.1` curlen
   (das ist die eigene WireGuard-IP des Servers). Vom PC aus nur mit aktivem WireGuard.
5. **`agent` und `agent-data`**: der Container laeuft als uid 1000 — der Host-Ordner
   `/opt/second-brain/agent-data` (und die Logbuch-/Eval-Ordner) muessen `frank`/uid 1000 gehoeren
   (`chown` beim ersten Anlegen). Sonst kann der Agent seine `prompt.txt`/`config.json` nicht schreiben.
6. **`.env` nie ueberschreiben/loeschen** — die Secrets (GEMINI_API_KEY, QDRANT_API_KEY, SB_API_KEY,
   GROQ_API_KEY, …) liegen NUR auf dem VPS in `/opt/second-brain/.env`, nicht im Repo. scp nur die
   geaenderten Code-Dateien, nie die `.env`.
7. **Reihenfolge der Quelle der Wahrheit**: erst lokal im Repo aendern → committen + pushen → DANN
   deployen. Code im Repo allein ist noch nicht live; live wird er erst durch den scp + `--build`.

## Faustregel

> Aendern (lokal/Repo) → committen + pushen → **scp** auf den VPS → **`docker compose up -d --build <dienst>`** → Health pruefen.
