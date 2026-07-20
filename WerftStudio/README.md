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
