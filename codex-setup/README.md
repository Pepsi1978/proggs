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
- `codex-setup/hooks/`
- `codex-setup/scripts/`
- `codex-setup/state/`
- `codex-setup/bridges/`
- `codex-setup/skills/`

Restore on a new machine:

```bash
# macOS/Linux, from the repo root
bash codex-setup/scripts/restore-codex-setup.sh "$PWD"
```

```powershell
# Windows, from the repo root
pwsh -File codex-setup/scripts/restore-codex-setup.ps1 -Workspace (Get-Location).Path
```

The restore scripts install repo-managed Codex rules, skills, agents, and hooks into
`~/.codex/` and set the required global Git defaults:
`pull.rebase=true`, `rebase.autoStash=true`, and `rerere.enabled=true`.
Codex runtime `.system` skills are intentionally not vendored here because they are
provided by the installed Codex runtime.

Operational rules:
- The authoritative Codex whiteboard is `codex-setup/agent-memory/shared/MEMORY.md`.
- Cross-platform supplemental Codex rules live in `codex-setup/rules/` and are synced to `~/.codex/rules/` by `codex-setup/scripts/session-start-sync.ps1` and `.sh`.
- `claude-code-setup/` is a read-only comparison source for Codex.
- `.claude/` and `~/.claude/` are not valid Codex control planes.
- Repository automation should target `codex-setup/**` when the change is Codex-specific.

Validation:
- Run `node codex-setup/scripts/validate-codex-setup.mjs` after Codex-environment changes.
- The validator checks repo and live `SKILL.md` frontmatter safety, blocks Windows Bash-to-Python heredoc drift in Codex rules/scripts, and verifies that the active PowerShell launcher still invokes `session-scorer.ts` without whiteboard side effects. Hooks are validated separately with shell and PowerShell syntax checks because macOS `.sh` hooks may intentionally use heredocs.
