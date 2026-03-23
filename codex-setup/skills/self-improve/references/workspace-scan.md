# Workspace Scan for Codex

This scan is Codex-specific. It must not use `proggs` or Claude's whiteboard as an operational source.

## Required Scan Targets

Always inspect:
- `AGENTS.md`
- `codex-setup/README.md`
- `codex-setup/rules/global.md`
- `codex-setup/rules/self-observation.md`
- `codex-setup/rules/german-trigger-routing.md`
- `codex-setup/rules/claude-delta-sync.md`
- `codex-setup/agent-memory/shared/MEMORY.md`
- `codex-setup/scripts/`
- `codex-setup/scripts/audit-claude-delta.mjs`
- `codex-setup/state/claude-delta-state.json`
- `codex-setup/state/environment-fixes.json`
- `codex-setup/bridges/`
- `codex-setup/skills/self-improve/`
- `.github/workflows/`
- `~/.codex/config.toml`
- `~/.codex/skills/self-improve/` if present

Conditional:
- `mcp-code-search/` when semantic search exists
- `~/.codex/log/` when runtime failures or startup warnings are relevant
- repo launchers or wrapper scripts when they affect Codex startup

## Required Findings

The scan must answer:
1. Does Codex have its own whiteboard in `codex-setup/`?
2. Is Codex already 100 percent operationally independent from `proggs` and Claude memory?
3. Is the self-improve skill repo-first and deployable to `~/.codex/skills/`?
4. Do the whiteboard bridge scripts exist and validate?
5. Are GitHub workflows validating `codex-setup/**`?
6. Are local MCP registrations aligned with this workspace?
7. Is the whiteboard's `## Oberste Direktive` propagated across the attached Codex control files, prompts, validation paths, and `AGENTS.md`?
8. Is `self-observation` visible as the second directive under the `## Oberste Direktive`?
9. Can Claude/Cloud Code setup changes be audited without writing into Claude files?
10. Does the Claude-delta audit state cleanly track only programming-environment deltas?
11. Does `codex-setup/bridges/` expose a reusable Cloud-Code bridge spec that other CLIs can read and mirror?
12. Does Codex log environment-level fixes with `what` and `why` in a machine-readable form that other CLIs can read?

## Forbidden Operational Sources

Do not use these as the active Codex memory or control plane:
- `~/proggs/...`
- `claude-code-setup/`
- `.claude/agent-memory/shared/MEMORY.md`
- `~/.claude/...`

They may exist in the repository, but for this skill they are not authoritative.
