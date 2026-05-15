# Codex Setup

This directory is the repo-local control plane for Codex in this workspace.

Purpose:
- provide an authoritative Codex whiteboard inside the repository
- keep Codex operationally independent from `claude-code-setup/`, `gemini-setup/`, and `.claude/`
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

Git multi-session lock:
- Runtime hook: `~/.codex/hooks/codex-git-multi-session-lock.cmd` on Windows.
  The `.cmd` launcher only starts the PowerShell hook when the shell command
  contains Git text; this avoids PowerShell startup cost for unrelated shell
  commands.
- Runtime wrapper: `~/bin/git.cmd` on Windows PowerShell and `~/.local/bin/git`
  or `~/bin/git` on Bash-compatible shells.
- Repo source: `codex-setup/hooks/codex-git-multi-session-lock.cmd`, `.ps1`,
  `.sh`, `codex-setup/hooks/codex-git-wrapper.ps1`, and `codex-setup/bin/git*`.
- Codex config: `~/.codex/config.toml` enables `[[hooks.PreToolUse]]` with
  `matcher = "^Bash$"` and `timeout = 130`; it also prepends `~/bin` to
  subprocess `PATH` through `[shell_environment_policy].set.PATH` so `git`
  resolves to the wrapper before the real Git executable.
- Shared lock file: `<git-common-dir>/claude-multi-session.lock`.
- Shared JSON fields: `sessionId`, `acquired`, `pid`, `command`, and `repo`.
- The file name and JSON shape intentionally stay Claude-compatible so Codex CLI
  and Claude Code can see the same lock.
- The PreToolUse hook is a fast preflight guard. The wrapper is the runtime guard:
  it writes the same lock with the wrapper PID, keeps that PID alive while the
  real `git` process runs, then removes only its own lock after Git exits.
- Test harness: `pwsh -NoProfile -ExecutionPolicy Bypass -File codex-setup/scripts/test-git-multi-session-lock.ps1`.

Delta and bridge commands:

```bash
# Compare Claude Code setup changes as read-only Codex port candidates
node codex-setup/scripts/audit-claude-delta.mjs scan --limit 20

# Compare Gemini setup changes as read-only Codex port candidates
node codex-setup/scripts/audit-gemini-delta.mjs scan --limit 20

# Print Codex bootstrap, ledger, and bridge addresses
node codex-setup/scripts/bootstrap-report.mjs
```

```powershell
# Windows bootstrap entrypoint
powershell -ExecutionPolicy Bypass -File codex-setup/scripts/bootstrap-codex-setup.ps1
```

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
- `gemini-setup/` is a read-only comparison source for Codex.
- `.claude/` and `~/.claude/` are not valid Codex control planes.
- Repository automation should target `codex-setup/**` when the change is Codex-specific.

Validation:
- Run `node codex-setup/scripts/validate-codex-setup.mjs` after Codex-environment changes.
- The validator checks repo and live `SKILL.md` frontmatter safety, blocks Windows Bash-to-Python heredoc drift in Codex rules/scripts, and verifies that the active PowerShell launcher still invokes `session-scorer.ts` without whiteboard side effects. Hooks are validated separately with shell and PowerShell syntax checks because macOS `.sh` hooks may intentionally use heredocs.
