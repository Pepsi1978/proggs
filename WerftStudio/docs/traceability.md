# Anforderungsnachweis

Version: v0.1.1 - 20.07.2026 20:08 Uhr

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

Offene oder fehlgeschlagene Gates dürfen nicht als bestanden markiert werden.
