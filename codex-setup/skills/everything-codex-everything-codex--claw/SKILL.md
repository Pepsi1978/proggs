---
name: claw
description: Start NanoClaw v2 — ECC's persistent, zero-dependency REPL with model routing, skill hot-load, branching, compaction, export, and metrics.
---

# Claw Command

Start an interactive AI agent session with persistent markdown history and operational controls.

## Usage

```bash
node scripts/claw.js
```

Or via npm:

```bash
npm run claw
```

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `CLAW_SESSION` | `default` | Session name (alphanumeric + hyphens) |
| `CLAW_SKILLS` | *(empty)* | Comma-separated skills loaded at startup |
| `CLAW_MODEL` | `sonnet` | Default model for the session |

## REPL Commands

```text
/prompts:help                          Show help
/prompts:clear                         Clear current session history
/prompts:history                       Print full conversation history
/prompts:everything-codex-everything-codex--sessions                      List saved sessions
/prompts:model [name]                  Show/set model
/prompts:load <skill-name>             Hot-load a skill into context
/prompts:branch <session-name>         Branch current session
/prompts:search <query>                Search query across sessions
/prompts:compact                       Compact old turns, keep recent context
/prompts:export <md|json|txt> [path]   Export session
/prompts:metrics                       Show session metrics
exit                           Quit
```

## Notes

- NanoClaw remains zero-dependency.
- Sessions are stored at `~/.codex/claw/<session>.md`.
- Compaction keeps the most recent turns and writes a compaction header.
- Export supports markdown, JSON turns, and plain text.
