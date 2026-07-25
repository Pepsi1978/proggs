# Anforderungsnachweis

Version: v0.4.0 - 25.07.2026 14:58 Uhr

| Bereich | Implementierung | Evidenz |
|---|---|---|
| Visuelle Shell | `apps/web/src`, `packages/ui` | Komponenten- und visuelle Tests |
| Verträge | `packages/contracts` | Schema- und Contracttests |
| Designmodell | `packages/design-model` | Operations- und Migrationstests |
| Persistenz | `packages/database` | Integrationstests gegen PostgreSQL |
| Authz/Audit | `packages/authz`, `apps/api` | Cross-Tenant- und Policytests |
| Realtime | `apps/realtime` | Sequenz- und Reconnecttests |
| KI | `packages/ai-gateway`, `apps/worker-ai` | Provider- und Schema-Evals |
| Preview/Export | Worker-Apps | Sandbox- und Exporttests |
| Projektimport | `apps/web`, `apps/api`, `project_imports`, `jobs`, MinIO | Streaming-Ordnerimport, vollständige UI-Quellchunkung, Framework-/Pfadguard-Tests und echter Fortschrittsstatus |
| OpenAI OAuth | `apps/web`, `apps/api`, `provider_connections` | AES-GCM-Tests und echter Gerätecode-Start gegen OpenAI |
| GPT-5.6 Routing | `apps/web`, `apps/api`, `codex-auth` | Sol/Terra/Luna-, Effort-, Priority- und Live-Verbindungstest |
| Leinwand-Navigation | `apps/web`, `apps/api/preview-canvas-bridge.ts` | Zoom-Anker-, Zoomgrenzen- und Bridge-Injektionstests |
| Leinwand-Vollbild | `apps/web/src/App.tsx` | Browser-Vollbild mit fokussiertem Canvas und getrennten Panel-Schaltern |
| Import-Fidelity | `apps/api/src/import-reconstruction.ts`, `preview-canvas-bridge.ts`, `apps/web/src/App.tsx` | Plattform-/Quellviewport, unverändertes iframe-DOM, Assetbasis-, Dateivollständigkeits- und Fidelity-Prüfrunde |

Offene oder fehlgeschlagene Gates dürfen nicht als bestanden markiert werden.
