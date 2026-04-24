# Claude Delta Sync for Codex Self-Improve

Use this reference under the whiteboard's `## Oberste Direktive` when the user asks for anything like:

- compare Claude Code with Codex
- port Claude setup improvements to Codex
- check what is newer in Claude rules, hooks, skills, agents, or settings
- check which setup-level bugs or hardening patterns Claude Code already fixed

## Mandatory Guardrails

- treat `claude-code-setup/` and `CLAUDE.md` as read-only comparison sources
- never write, delete, or reorganize files there from a Codex self-improve run
- treat `Gemini-Setup/` and `%USERPROFILE%\GeminiCLI` as read-only comparison sources too
- write only into `codex-setup/`, `AGENTS.md`, or Codex validation files when needed
- run `codex-setup/scripts/audit-claude-delta.*` before proposing any port
- include setup-level bugfixes and hardening logic when they are portable to Codex

## Required Classification

Every Claude-delta candidate must be labeled as one of:

- `ADD`
- `ADAPT`
- `REPLACE`

Meaning:

- `ADD`: directly additive to Codex
- `ADAPT`: useful but requires translation into Codex structure
- `REPLACE`: would overwrite existing Codex text or behavior

Setup-level bugfixes and hardening logic are in scope when they improve rules, hooks,
agents, skills, validation, runtime glue, or other programming-environment behavior.

## Approval Policy

- `ADD`: can be recommended, but only as a proposal
- `ADAPT`: explain the Codex translation path, but only as a proposal
- `REPLACE`: explicit warning first, then wait for approval before replacing anything

Never auto-apply Claude bridge findings.
Always prepare grouped A1/B1/C1/D1 proposals first and wait for explicit approval.

When semantics diverge but both sides are useful:

- keep the Codex rule
- add the Claude idea as a separate Codex rule or additive clause
- prefer compounding intelligence over one-to-one mirroring

## Good Port Targets

- `codex-setup/rules/*.md`
- `codex-setup/skills/self-improve/**`
- `codex-setup/scripts/*.mjs`, `.sh`, `.ps1`
- `AGENTS.md`
- `.github/workflows/codex-setup-validate.yml` if validation scope must grow
