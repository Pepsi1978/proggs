# Codex Setup

This directory is the repo-local control plane for Codex in this workspace.

Purpose:
- provide an authoritative Codex whiteboard inside the repository
- keep Codex operationally independent from `claude-code-setup/` and `.claude/`
- give the `self-improve` skill a repo-native source of truth
- store Codex rules, bridges, state, and lightweight runtime helpers

Core paths:
- `codex-setup/agent-memory/shared/MEMORY.md`
- `codex-setup/rules/`
- `codex-setup/scripts/`
- `codex-setup/state/`
- `codex-setup/bridges/`
- `codex-setup/skills/self-improve/`

Operational rules:
- The authoritative Codex whiteboard is `codex-setup/agent-memory/shared/MEMORY.md`.
- Cross-platform supplemental Codex rules live in `codex-setup/rules/` and are synced to `~/.codex/rules/` by `codex-setup/scripts/session-start-sync.ps1` and `.sh` when explicitly allowlisted in those scripts.
- `claude-code-setup/` is a read-only comparison source for Codex.
- `.claude/` and `~/.claude/` are not valid Codex control planes.
- Repository automation should target `codex-setup/**` when the change is Codex-specific.

Validation:
- Run `node codex-setup/scripts/validate-codex-setup.mjs` after Codex-environment changes.
- The validator checks repo and live `SKILL.md` frontmatter safety, blocks Windows Bash-to-Python heredoc drift in Codex control files, and verifies that the active PowerShell launcher still invokes `session-scorer.ts` without whiteboard side effects.
