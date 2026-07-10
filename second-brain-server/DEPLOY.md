# Deployen auf Cortex / Second-Brain-Server

> Kurzanleitung: Wie man eine Aenderung am Second-Brain-Server (Cortex) auf den VPS bringt.
> Genau EIN Weg. Nicht suchen, nicht raten — dieser Weg gilt.

## Das Wichtigste in einem Satz

Auf dem VPS liegt **KEIN git-Repo**. Deployen heisst: geaenderte Dateien per **scp** ins
Server-Verzeichnis hochladen und den betroffenen Dienst per **`docker compose up -d --build --no-deps <dienst>`**
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

## Einmalig auf jedem neuen Windows-Rechner: SSH-Schluessel-ACL

Windows OpenSSH ignoriert einen privaten Schluessel, sobald eine zusaetzliche Gruppe ihn lesen darf
(`UNPROTECTED PRIVATE KEY FILE` / `bad permissions`). Nach dem Ablegen des Schluessels unter
`~/SK/second-brain/id_ed25519` das idempotente Einrichtungsskript **als der normale Benutzer** ausfuehren:

```powershell
pwsh -NoProfile -File .\windows\set-cortex-ssh-key-acl.ps1
```

Optionaler anderer Schluesselpfad: `-KeyPath "C:\Pfad\id_ed25519"`. Das Skript kann beliebig oft
laufen: Es setzt immer exakt dieselben drei erlaubten ACL-Eintraege mit Vollzugriff — aktueller
Benutzer, `SYSTEM` (`S-1-5-18`) und lokale Administratoren (`S-1-5-32-544`) —, verifiziert den
Zustand und fuehrt danach den echten BatchMode-SSH-Test aus. Die letzte Zeile muss `SSH_ACL_OK`
ausgeben. Nur fuer einen reinen ACL-Test ohne erreichbaren Server `-SkipSshTest` verwenden.
Niemals den Schluesselinhalt anzeigen, ins Repo kopieren oder die OpenSSH-Pruefung global abschalten.

## Dienste und ihre Bau-Pfade

| Dienst (compose-Name) | Bau-Pfad (Repo + VPS) | Port (an 10.8.0.1) | Selbst gebaut? |
|-----------------------|------------------------|--------------------|----------------|
| `brain-api` | `./brain-api` | 8000 (`/health`) | ja (`--build`) |
| `mcp` | `./mcp-server` | 8001 (TCP) | ja (`--build`) |
| `agent` | `./agent` | 8002 (`/health`) | ja (`--build`) |
| `dashboard` | `./dashboard` | 8003 (`/api/health`) | ja (`--build`) |
| `librarian` | `./librarian` | 8004 (`/health`) | ja (`--build`) |
| `qdrant` | (Fertig-Image, kein Bau) | 127.0.0.1:6333 | nein |
| `caddy` | (Fertig-Image, kein Bau) | 443 | nein |

**Wichtig:** Bei den selbst gebauten Diensten ist der Code **ins Docker-Image gebacken**
(`build: ./<dienst>`, KEIN Code-Volume-Mount). Darum ist `--build` PFLICHT — ohne `--build`
laeuft weiter der alte Code, obwohl die Datei auf dem Server schon neu ist.

## Der Deploy-Weg (Schritt fuer Schritt)

> ### ⚠️ PFLICHT bei jedem Deploy MIT NEUEM FEATURE: Feature-Chronik pflegen (Frank-Wunsch 2026-07-04)
> Baut ein Deploy ein neues System/Feature ein (egal welcher Dienst), MUSS in
> `dashboard/features.json` ein Eintrag ERGAENZT werden (neueste zuerst): id, name,
> `eingebaut` (Datum + Uhrzeit), `dienst`, `kurz` (1 Satz) und `erklaerung` (ausfuehrlich,
> leichtes Deutsch — was es ist, wie es funktioniert, wo es sitzt). Diese Datei speist den
> Info-Bereich "System-Info: Was Cortex kann" in den Dashboard-Einstellungen (GET /api/features) —
> Franks Nachschlagewerk, was alles eingebaut ist. Reine Bugfixes ohne neues System: kein Eintrag
> noetig. Da features.json ins dashboard-Image gebacken ist, das dashboard danach mit `--build`
> neu bauen (das passiert durch die Version-Bump-Regel unten ohnehin).
>
> **SCHREIBWEISE der Eintraege (Dauer-Regel, Frank-Wunsch 2026-07-07):** Der Cortex-Hauptagent liest
> ueber das Werkzeug `was_kann_cortex` ALLE Eintraege auf EINMAL ein (um Frank zu erklaeren, was Cortex
> kann) — deshalb: leichtes, verstaendliches Deutsch, kurze Saetze, KOMPAKT halten. ABER: alle
> Fachbegriffe, Namen und Zahlen (BM25, RRF, Qdrant, ENV-Variablen wie `AGENT_RECALL_LIMIT=0`,
> Endpunkt-Namen, Grenzwerte) BLEIBEN erhalten und werden je in einem kurzen Halbsatz erklaert — NIE
> durch Weglassen "vereinfachen" (Frank muss erkennen, WAS konkret gebaut wurde). Einfache Ueberschrift.
> Aehnliche/redundante Eintraege duerfen ZUSAMMENGEFASST werden (Datum ist sekundaer — entscheidend ist,
> WAS Cortex kann). Dieselbe Regel steht im `hinweis`-Feld von `features.json`.
>
> **FALLE beim STRUKTUR-Umbau der Chronik (nicht nur einen Eintrag anhaengen):** `/api/features` liest
> die PERSISTENTE Kopie `dashboard-data/features.json` (Bind-Mount `/opt/second-brain/dashboard-data/`)
> und MERGED fehlende Seed-Eintraege per `id` hinein (0.57.1-Verhalten, schuetzt editierte Eintraege).
> Wer nur EINEN Eintrag anhaengt, ist sicher. Wer aber die STRUKTUR aendert (Eintraege zusammenfasst,
> ids aendert/loescht), bekommt DUPLIKATE: alt (persistent) + neu (Seed) = zu viele Eintraege (real
> 2026-07-07: 55->32 umgebaut, `/api/features` zeigte 68). LOESUNG nach dem `--build`-Deploy die
> persistente Kopie auf dem VPS mit der neuen Version UEBERSCHREIBEN:
> `scp dashboard/features.json root@168.231.83.205:/opt/second-brain/dashboard-data/features.json`,
> dann `chown 10002:10002 .../features.json` + `docker compose restart dashboard`. Danach `/api/features` == neue Zahl.
>
> ### ⚠️ PFLICHT VOR JEDEM DEPLOY: sichtbare Version + Timestamp erhoehen (Frank-Wunsch 2026-07-01)
> Bei JEDER Server-Aenderung — **egal welcher Dienst** (agent, librarian, brain-api, mcp, dashboard) — MUSS die
> **sichtbare Dashboard-Version** `VERSION` in `dashboard/app.py` erhoeht und ihr **Timestamp auf die
> aktuelle Uhrzeit** gesetzt werden. Das ist der EINE Marker, den Frank im Dashboard-Footer sieht:
> `V0.x (TT.MM.JJJJ, HH:MM Uhr)`. Format:
> `VERSION = "0.28.0 (TT.MM.JJJJ, HH:MM Uhr)"  # 0.28.0: <was deployt wurde, kurz>`.
> Aktuelle Zeit holen: `Get-Date -Format 'dd.MM.yyyy, HH:mm'` (Windows).
> **Danach das dashboard IMMER mit-deployen** (`docker compose up -d --build --no-deps dashboard`), auch wenn nur
> der agent ODER librarian (oder brain-api/mcp) geaendert wurde — sonst bleibt der Footer auf dem alten
> Stand und Frank sieht nicht, dass sein Deploy angekommen ist. (Regel `version-bump-visible-always`:
> jede Aenderung sichtbar + Timestamp.) Die Dashboard-Version ist der EINE Gesamt-Versionszaehler des
> ganzen Servers/der Installation — nicht die Version eines Einzeldienstes. Vorfall 2026-07-05: agent
> 0.53.0 + librarian 0.8.0 wurden deployt, der Footer-Bump aber vergessen -> Frank sah 0.45.0/12.13 Uhr
> und dachte, nichts sei angekommen. Genau das darf nie wieder passieren.

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
  "cd /opt/second-brain && docker compose up -d --build --no-deps dashboard"

# 3. Verifizieren (Status healthy + neue Version):
ssh -i ~/SK/second-brain/id_ed25519 root@168.231.83.205 \
  "cd /opt/second-brain && docker compose ps && curl -s http://10.8.0.1:8003/api/health"
```

Fuer einen anderen Dienst einfach `dashboard` durch `brain-api`, `agent`, `librarian` oder `mcp`
ersetzen (und den passenden Bau-Pfad aus der Tabelle nehmen). **Wichtig:** Bei einem Einzelservice-
Deploy immer `--no-deps` verwenden. `depends_on` bleibt in `compose.yaml` fuer Boot-/Laufzeit sinnvoll,
aber ohne `--no-deps` startet/recreated Compose auch unveraenderte Abhaengigkeiten mit (realer Vorfall:
`librarian`-Deploy recreatete `brain-api`). Mehrere bewusst gemeinsam geaenderte Dienste gleichzeitig:
`docker compose up -d --build --no-deps brain-api mcp`.

Nur wenn Abhaengigkeiten absichtlich mitgezogen werden sollen (Erststart, geaenderte `compose.yaml`,
geänderte Dependency oder kompletter Stack-Rollout), `--no-deps` weglassen bzw. den ganzen Stack starten.

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
2. **`--build --no-deps` ist Pflicht fuer Einzelservice-Deploys** — der Code ist ins Image gebacken;
   ohne `--build` bleibt der alte Code aktiv, ohne `--no-deps` werden unveraenderte Abhaengigkeiten
   wie `brain-api` unnoetig mitgestartet oder recreated.
3. **Windscribe / Full-Tunnel AUS** vor dem Deploy — sonst haengt/scheitert SSH.
4. **Ports an `10.8.0.1`**, nicht `0.0.0.0` und nicht `127.0.0.1` — vom VPS aus mit `10.8.0.1` curlen
   (das ist die eigene WireGuard-IP des Servers). Vom PC aus nur mit aktivem WireGuard.
5. **`agent` und `agent-data`**: der Container laeuft als uid 1000 — der Host-Ordner
   `/opt/second-brain/agent-data` (und die Logbuch-/Eval-Ordner) muessen `frank`/uid 1000 gehoeren
   (`chown` beim ersten Anlegen). Sonst kann der Agent seine `prompt.txt`/`config.json` nicht schreiben.
   **Gleiches gilt fuer den `librarian`**: `/opt/second-brain/librarian-data` und
   `/opt/second-brain/librarian-logs` muessen uid 1000 gehoeren (`chown -R 1000:1000 …` beim ersten Anlegen),
   sonst kann der Nachtschicht-Bibliothekar Config/State/Tages-Reports nicht schreiben. Fuer das
   Dashboard muss `/opt/second-brain/dashboard-data` uid 10002 gehoeren (`chown -R 10002:10002 …`),
   damit die bearbeitbare System-Info-Chronik persistent gespeichert werden kann.
6. **`.env` nie ueberschreiben/loeschen** — die Secrets (GEMINI_API_KEY, QDRANT_API_KEY, SB_API_KEY,
   GROQ_API_KEY, …) liegen NUR auf dem VPS in `/opt/second-brain/.env`, nicht im Repo. scp nur die
   geaenderten Code-Dateien, nie die `.env`.
7. **Reihenfolge der Quelle der Wahrheit**: erst lokal im Repo aendern → committen + pushen → DANN
   deployen. Code im Repo allein ist noch nicht live; live wird er erst durch den scp + `--build`.

## Faustregel

> Aendern (lokal/Repo) → committen + pushen → **scp** auf den VPS → **`docker compose up -d --build --no-deps <dienst>`** → Health pruefen.
