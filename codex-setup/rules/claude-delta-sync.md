# Claude Delta Sync for Codex

Claude comparison sources are read-only for Codex.

Classification:
- `ADD`: new additive idea for Codex
- `ADAPT`: useful idea that needs Codex-specific translation
- `REPLACE`: would replace existing Codex behavior and therefore requires explicit approval

Rules:
- never write into `claude-code-setup/`
- prefer additive Codex integration over replacement
- report grouped proposals before any implementation that would replace Codex behavior
