---
name: checkpoint
description: Converted from command checkpoint
---

# Checkpoint Command

Create or verify a checkpoint in your workflow.

## Usage

`/prompts:everything-codex-everything-codex--checkpoint [create|verify|list] [name]`

## Create Checkpoint

When creating a checkpoint:

1. Run `/prompts:everything-codex-everything-codex--verify quick` to ensure current state is clean
2. Create a git stash or commit with checkpoint name
3. Log checkpoint to `.codex/checkpoints.log`:

```bash
echo "$(date +%Y-%m-%d-%H:%M) | $CHECKPOINT_NAME | $(git rev-parse --short HEAD)" >> .codex/checkpoints.log
```

4. Report checkpoint created

## Verify Checkpoint

When verifying against a checkpoint:

1. Read checkpoint from log
2. Compare current state to checkpoint:
   - Files added since checkpoint
   - Files modified since checkpoint
   - Test pass rate now vs then
   - Coverage now vs then

3. Report:
```
CHECKPOINT COMPARISON: $NAME
============================
Files changed: X
Tests: +Y passed / -Z failed
Coverage: +X% / -Y%
Build: [PASS/FAIL]
```

## List Checkpoints

Show all checkpoints with:
- Name
- Timestamp
- Git SHA
- Status (current, behind, ahead)

## Workflow

Typical checkpoint flow:

```
[Start] --> /prompts:everything-codex-everything-codex--checkpoint create "feature-start"
   |
[Implement] --> /prompts:everything-codex-everything-codex--checkpoint create "core-done"
   |
[Test] --> /prompts:everything-codex-everything-codex--checkpoint verify "core-done"
   |
[Refactor] --> /prompts:everything-codex-everything-codex--checkpoint create "refactor-done"
   |
[PR] --> /prompts:everything-codex-everything-codex--checkpoint verify "feature-start"
```

## Arguments

$ARGUMENTS:
- `create <name>` - Create named checkpoint
- `verify <name>` - Verify against named checkpoint
- `list` - Show all checkpoints
- `clear` - Remove old checkpoints (keeps last 5)
