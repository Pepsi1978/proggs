# Anforderungsnachweis

Version: v0.1.11 - 20.07.2026 21:17 Uhr

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

Offene oder fehlgeschlagene Gates dürfen nicht als bestanden markiert werden.
