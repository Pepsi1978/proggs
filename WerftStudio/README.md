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
  && tar -xzf /tmp/werft.tgz && docker compose -f compose.server.yaml up -d --build'
```

Das Archiv wird per `scp` als Datei übertragen und vor dem Entpacken mit `tar -tzf` geprüft; die
Prüfsumme beider Seiten muss übereinstimmen. Ein `tar -czf - … | ssh …` mit `&` im selben Befehl
liefert das Archiv abgeschnitten aus und würde einen Rebuild auf halbem Quellstand starten.

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
