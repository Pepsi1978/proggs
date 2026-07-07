#!/usr/bin/env bash
# Intent-Anker — Saves the original session goal for drift prevention
# UserPromptSubmit hook: Turn 1 saves goal, counter tracks session length
# Platform: macOS / Linux (bash equivalent of intent-anker.ps1)
#
# The goal file is used by:
# - intent-tracking.md rule (every ~5 turns: verify alignment)
# - PreCompact prompt hook (preserve goal during compaction)

HOOKS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$HOOKS_DIR/hook-log.sh"

GOAL_FILE="${TMPDIR:-/tmp}/claude-session-goal.txt"
COUNTER_FILE="${TMPDIR:-/tmp}/claude-turn-counter.txt"

# Read JSON input from stdin (Claude Code sends hook context as JSON)
hook_input=$(cat 2>/dev/null)

# Increment turn counter
turn=0
if [ -f "$COUNTER_FILE" ]; then
    turn=$(cat "$COUNTER_FILE" 2>/dev/null | tr -d '[:space:]')
    # Validate that it is a number
    [[ "$turn" =~ ^[0-9]+$ ]] || turn=0
fi
turn=$((turn + 1))
printf '%s' "$turn" > "$COUNTER_FILE"

# On turn 1: extract and save the user's prompt as session goal
if [ "$turn" -eq 1 ] && [ -n "$hook_input" ]; then
    # Try to extract .prompt (string) or .message (string) from JSON via python3
    # Falls back to empty string if python3 unavailable or field missing
    goal=$(python3 - "$hook_input" <<'PYEOF' 2>/dev/null
import sys, json
raw = sys.argv[1]
try:
    data = json.loads(raw)
except Exception:
    sys.exit(0)

goal = ""
if "prompt" in data:
    p = data["prompt"]
    goal = p if isinstance(p, str) else (p.get("content", "") if isinstance(p, dict) else "")
elif "message" in data:
    m = data["message"]
    goal = m if isinstance(m, str) else (m.get("content", "") if isinstance(m, dict) else "")

# Trim to 300 characters
goal = goal[:300]
print(goal, end="")
PYEOF
)

    if [ -n "$goal" ]; then
        printf '%s' "$goal" > "$GOAL_FILE"
    fi
fi

# Every 5 turns: write a reminder marker file
# Drift onset peaks at turns 4-7 (arxiv 2510.07777), so 5-turn interval catches it early
if [ $((turn % 5)) -eq 0 ] && [ -f "$GOAL_FILE" ]; then
    reminder_file="${TMPDIR:-/tmp}/claude-intent-reminder.txt"
    printf '%s' "$turn" > "$reminder_file"
fi

exit 0
