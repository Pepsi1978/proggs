# claudewatch — During-Session Protocol

## Hook Response

If the PostToolUse hook fires with a `⚠` warning, stop immediately and call
`get_session_dashboard` (claudewatch MCP). Do NOT continue without assessing
the situation. This warning indicates error loops, drift, context pressure,
or cost spikes that require immediate attention.

## Context Pressure & Memory

- If context pressure reaches "pressure" or "critical", consider compacting or
  scoping down the current task. At "pressure" level, call
  `extract_current_session_memory` before compaction to preserve work-in-progress.
- If cost velocity is "burning", identify the source before continuing.
- Every 30-45 minutes in long sessions, ask: "Have I learned something worth
  preserving?" If yes, call `extract_current_session_memory` now.
- Before destructive operations (git reset --hard, git push --force, rm -rf,
  large refactors), ALWAYS call `extract_current_session_memory` first.

## Error & Blocker Handling

- When hitting errors or blockers: Before retrying or investigating extensively,
  call `get_blockers()` to check if this is a known issue with a documented solution.
  If found, apply the solution instead of rediscovering it.
- On repetitive tool errors: If the same tool/operation fails 2-3 times in a row,
  STOP and call `get_blockers()`. This pattern indicates a known issue, not a
  one-off mistake.

## Task History Awareness

- Before implementing large features: Query `get_task_history(query: "<feature>")`
  to check if this was attempted before. If status is "abandoned", read why it
  failed and avoid the same approach.
- When the user expresses frustration ("this isn't working", "why is this broken",
  "we tried this before"), immediately call both `get_task_history` and
  `get_blockers` to surface prior context.
- When the user asks "how did we...?", call `search_transcripts(query: "X")`
  to find relevant sessions before `get_task_history`.

## Progress Tracking

- After completing major work (feature, complex bug fix, multi-step task),
  call `extract_current_session_memory` to save context while it's fresh.
- Before major commits, check `get_session_dashboard` for the
  commit-to-attempt ratio. Low ratio (<0.3) suggests guessing — investigate
  before committing potentially broken code.
- If stuck exploring (reading files repeatedly without making progress),
  call `get_drift_signal`. If status is "drifting", scope down or ask
  the user for clarification.
