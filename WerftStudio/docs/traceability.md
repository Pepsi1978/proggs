# Anforderungsnachweis

Version: v0.1.16 - 21.07.2026 18:30 Uhr

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
| Projektimport | `apps/web`, `apps/api`, `project_imports`, MinIO | Pfadguard-Tests sowie echter HTML-Ordner- und ZIP-Import |
| OpenAI OAuth | `apps/web`, `apps/api`, `provider_connections` | AES-GCM-Tests und echter Gerätecode-Start gegen OpenAI |
| GPT-5.6 Routing | `apps/web`, `apps/api`, `codex-auth` | Sol/Terra/Luna-, Effort-, Priority- und Live-Verbindungstest |

Offene oder fehlgeschlagene Gates dürfen nicht als bestanden markiert werden.
