#!/bin/bash
# Intent-Anker — Saves the original session goal for drift prevention
# UserPromptSubmit hook: Turn 1 saves goal, counter tracks session length
#
# The goal file is used by:
# - intent-tracking.md rule (every ~20 turns: verify alignment)
# - PreCompact prompt hook (preserve goal during compaction)

GOAL_FILE="/tmp/claude-session-goal.txt"
COUNTER_FILE="/tmp/claude-turn-counter.txt"

# Read JSON input from stdin (Claude Code sends hook context)
INPUT=$(cat 2>/dev/null || echo '{}')

# Increment turn counter
TURN=$(cat "$COUNTER_FILE" 2>/dev/null || echo 0)
TURN=$((TURN + 1))
echo "$TURN" > "$COUNTER_FILE"

# On turn 1: extract and save the user's prompt as session goal
if [ "$TURN" -eq 1 ]; then
  GOAL=$(echo "$INPUT" | python3 -c "
import json, sys
try:
    d = json.load(sys.stdin)
    prompt = d.get('prompt', d.get('message', ''))
    if isinstance(prompt, dict):
        prompt = prompt.get('content', str(prompt))
    # Save first 300 chars as goal
    print(str(prompt)[:300])
except:
    print('')
" 2>/dev/null)
  if [ -n "$GOAL" ]; then
    echo "$GOAL" > "$GOAL_FILE"
  fi
fi

# Every 20 turns: write a reminder marker file
# The intent-tracking.md rule and PreCompact hook use this
if [ $((TURN % 20)) -eq 0 ] && [ -f "$GOAL_FILE" ]; then
  echo "$TURN" > "/tmp/claude-intent-reminder.txt"
fi

exit 0
