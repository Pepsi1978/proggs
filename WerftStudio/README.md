# Werft Studio

Browserbasiertes, anbieterneutrales KI-Designstudio nach der verbindlichen Referenz
`../Designs/Design-App für Browser/Studio.dc.html`.

## Lokal starten

```powershell
Copy-Item .env.example .env
docker compose up -d
corepack pnpm install
corepack pnpm db:migrate
corepack pnpm db:seed
corepack pnpm dev
```

Web: `http://localhost:5173`, API: `http://localhost:4100`, Realtime: `ws://localhost:4101`.

## Hostinger-Server

Das produktive WireGuard-Deployment verwendet `compose.server.yaml` unter `/opt/werft-studio`.
Nur `https://10.8.0.1:8443` wird an die WireGuard-IP gebunden. Alle Datenbanken, Queues,
Objektspeicher und Worker bleiben im internen Docker-Netz `werft-internal`. Cortex auf Port 443
und dessen `/api`-Routen werden dadurch nicht berührt.

### SSH-Zugang

Werft Studio hat keinen eigenen SSH-Key: der Server ist derselbe Hostinger-VPS wie Second Brain
(Cortex auf 443, Werft Studio auf 8443). Der Schlüssel liegt zentral unter
`~/SK/second-brain/id_ed25519`, nicht in `~/.ssh/`. Ein Eintrag in `~/.ssh/config` verweist darauf:

```
Host 10.8.0.1 168.231.83.205
    User root
    IdentityFile ~/SK/second-brain/id_ed25519
    IdentitiesOnly yes
```

Damit genügt ein nacktes `ssh root@10.8.0.1` — ohne diesen Eintrag melden Werkzeuge, die nur
`~/.ssh/` durchsuchen (OpenCode, Cowork), „kein SSH-Key gefunden". Der Tunnel muss stehen.

### Stand übertragen

Der Server ist kein Git-Klon; die Quellen werden als Archiv übertragen. `.env` bleibt dabei außen vor,
sonst würden die Serverschlüssel überschrieben.

```sh
tar -czf /tmp/werft.tgz --exclude=node_modules --exclude=.git --exclude=dist \
  --exclude=.turbo --exclude='*.tsbuildinfo' --exclude=.env \
  apps packages docker package.json pnpm-lock.yaml pnpm-workspace.yaml \
  tsconfig.base.json turbo.json compose.server.yaml compose.yaml README.md SPEC.md docs
scp /tmp/werft.tgz root@10.8.0.1:/tmp/werft.tgz
ssh root@10.8.0.1 'cd /opt/werft-studio && tar -tzf /tmp/werft.tgz >/dev/null \
  && tar -xzf /tmp/werft.tgz && docker compose -f compose.server.yaml up -d --build \
  && install -m 0644 docker/werft-docker-cache-prune.service docker/werft-docker-cache-prune.timer docker/werft-docker-cache-prune.path /etc/systemd/system/ \
  && systemctl daemon-reload && systemctl enable --now werft-docker-cache-prune.timer werft-docker-cache-prune.path \
  && systemctl start werft-docker-cache-prune.service'
```

Das Archiv wird per `scp` als Datei übertragen und vor dem Entpacken mit `tar -tzf` geprüft; die
Prüfsumme beider Seiten muss übereinstimmen. Ein `tar -czf - … | ssh …` mit `&` im selben Befehl
liefert das Archiv abgeschnitten aus und würde einen Rebuild auf halbem Quellstand starten.

### Abnahme nach dem Deployment (Pflicht — der Exit-Code genügt nicht)

`docker compose up` **bricht beim ersten Container-Fehler ab** und lässt die in der Startreihenfolge
folgenden Dienste auf `created` stehen — erzeugt, aber nie gestartet. Der Exit-Code des umgebenden
Befehls verrät das nicht. Am 10.08.2026 blockierte ein verwaister Umbenennungs-Rest eines
abgebrochenen Laufs den Namen `werft-api`; `realtime` und `web` starteten deshalb nicht, die API
antwortete aber korrekt mit der neuen Version. Die Oberfläche war offline, und jede oberflächliche
Prüfung hätte das Deployment als erfolgreich gemeldet.

Darum nach **jedem** Deployment beide Proben laufen lassen:

```sh
# 1. Alle Dienste müssen "running" sein — das -a ist entscheidend, ohne es sieht man die
#    nicht gestarteten gar nicht.
ssh root@10.8.0.1 'cd /opt/werft-studio && docker compose -f compose.server.yaml ps -a \
  --format "{{.Service}}\t{{.State}}"'

# 2. Oberfläche und Version gegenprüfen.
ssh root@10.8.0.1 'curl -sk https://10.8.0.1:8443/api/v1/health/live; \
  curl -sk -o /dev/null -w "\n/app HTTP %{http_code}\n" https://10.8.0.1:8443/app/designs'
```

Die gemeldete `version` muss der `version` in der Wurzel-`package.json` entsprechen, `/app` muss
`200` liefern.

Steht ein Dienst auf `created`, gibt es zwei Ursachen, die man **nicht verwechseln darf**:

- **Nur noch nicht fertig.** `web` hängt an `api: service_healthy` und `realtime: service_started`,
  `realtime` ebenfalls an `api: service_healthy`. Solange der API-Healthcheck auf `starting` steht,
  hält Compose beide absichtlich zurück. Dann gilt: **abwarten** und `ps -a` erneut ausführen —
  nichts entfernen.
- **Abgebrochenes `up`.** Steht im Deploy-Log eine Fehlermeldung (typisch
  `Conflict. The container name … is already in use`) und der Dienst taucht im Log gar nicht als
  Container auf, wurde er nie gestartet. Dann `docker ps -a --filter name=<dienst>` prüfen, einen
  verwaisten Umbenennungs-Rest mit `docker rm -f` entfernen (Volumes bleiben unberührt) und
  `docker compose -f compose.server.yaml up -d` **ohne** `--build` nachziehen. Ist der blockierende
  Container selbst gesund und aktuell, genügt `up -d` allein.

Hintergrund und die Unterscheidungstabelle: `bugs/server/docker.md` §8.

Der Timer entfernt den ungenutzten Docker-Build-Cache stündlich und nach jedem Deployment. Der
Papierkorb in den Einstellungen löst denselben Dienst sofort über eine feste Dateibrücke aus. Laufende Container,
Images, Datenbanken und die im MinIO-Manifest geführten Design-Dateien werden dabei nicht gelöscht.

## Architektur

- `apps/web`: React-Oberfläche mit Hub, Studio und Administration
- `apps/api`: Fastify REST-API und OpenAPI
- `apps/realtime`: projektbezogene WebSocket-Ereignisse
- `apps/worker-*`: getrennte Queue-Worker für KI, Build, Render, Export und Import
- `packages/contracts`: versionierte Zod-Verträge
- `packages/design-model`: kanonisches DesignDocument und Operationen
- `packages/database`: PostgreSQL-Schema, Migration und Seed
- `packages/authz`: zentrale Rollen- und Policy-Auswertung
- `packages/ai-gateway`: austauschbare Provideradapter
- `packages/platform-profiles`: Web-, Android-, Apple- und Windows-Regeln
- `packages/ui`: Werft-Tokens und UI-Primitives

Die Preview-Origin darf in Produktion keine Cookies der Hauptanwendung erhalten. Provider-Credentials
werden ausschließlich serverseitig verarbeitet und niemals an Web oder Preview ausgegeben.
