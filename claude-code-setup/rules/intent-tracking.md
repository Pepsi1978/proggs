# Intent Drift Prevention

## Rule: Track and Verify Original Intent

The original session goal is automatically saved to `/tmp/claude-session-goal.txt` by the intent-anker hook.
A reminder marker is written every 20 turns to `/tmp/claude-intent-reminder.txt`.

When working on a task that spans more than 10 tool calls:

1. **At the start**: Read `/tmp/claude-session-goal.txt` to recall the user's original request
2. **Every ~20 tool calls**: Re-read the goal file and verify: "Am I still working toward this exact goal?"
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

If you detect drift: Don't try to justify it. Read `/tmp/claude-session-goal.txt` and say:
"Ich bin vom ursprünglichen Ziel abgewichen. Das Ziel war: [goal from file]. Soll ich zurückkehren?"
