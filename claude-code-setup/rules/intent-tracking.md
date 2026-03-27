# Intent Drift Prevention

## Rule: Track and Verify Original Intent

The original session goal is automatically saved by the intent-anker hook.
A reminder marker is written every 5 turns.

> **Finding the goal file (CRITICAL — platform-dependent):**
> - **macOS**: The file is at `$TMPDIR/claude-session-goal.txt` — on macOS `$TMPDIR` is NOT `/tmp/` but `/var/folders/.../T/`. Always use `${TMPDIR:-/tmp}/claude-session-goal.txt` or read the env var first.
> - **Windows**: `$env:TEMP/claude-session-goal.txt`
> - **Linux**: `/tmp/claude-session-goal.txt`
> - **Quick check**: Run `echo ${TMPDIR:-/tmp}` to find the correct directory.
> - The reminder file is at the same location: `${TMPDIR:-/tmp}/claude-intent-reminder.txt`

When working on a task that spans more than 10 tool calls:

1. **At the start**: Read the goal file (use `cat "${TMPDIR:-/tmp}/claude-session-goal.txt"`) to recall the user's original request
2. **Every ~5 turns** (when reminder file updates): Re-read the goal file and verify: "Am I still working toward this exact goal?"
3. **Before any major direction change**: Explicitly state what you're about to do differently and why
4. **If you notice drift**: Stop immediately and ask the user

## Why This Matters

In long sessions (100+ turns), the agent tends to drift from the original intent.
Measured: 12.1% correction rate (13 corrections in 107 turns).
Research shows periodic goal reminders reduce KL-divergence by 30% (arxiv 2510.07777).

## Signs of Intent Drift

- You're working on something the user didn't explicitly ask for
- You're "improving" code that wasn't part of the original request
- You've lost track of which step you're on in a multi-step task
- The user says "nein", "nicht das", "stop", "so nicht" — these are drift corrections

## Recovery

If you detect drift: Don't try to justify it. Read the goal file (`cat "${TMPDIR:-/tmp}/claude-session-goal.txt"`) and say:
"Ich bin vom ursprünglichen Ziel abgewichen. Das Ziel war: [goal from file]. Soll ich zurückkehren?"
