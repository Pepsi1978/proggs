#!/bin/bash
# Session Scorer — Quantitative Session-Metrics Tracker
# Event: SessionEnd (fires when Claude Code session ends)
# Type: command (runs as background script)
# Platform: macOS/Linux
#
# PURPOSE: Collects quantitative metrics from the session and appends them
# to ~/.claude/session-scores.jsonl for trend analysis.
#
# ROBUSTNESS: Non-critical. Any failure → exit 0 silently.

set +e  # Don't exit on error

SCORES_FILE="$HOME/.claude/session-scores.jsonl"
mkdir -p "$(dirname "$SCORES_FILE")"

# 1. Turn count
COUNTER_FILE="${TMPDIR:-/tmp}/claude-turn-counter.txt"
turns=0
if [ -f "$COUNTER_FILE" ]; then
    turns=$(cat "$COUNTER_FILE" 2>/dev/null | tr -d '[:space:]')
    turns=${turns:-0}
fi

# Skip trivial sessions
if [ "$turns" -lt 3 ] 2>/dev/null; then
    exit 0
fi

# 2. Session goal (truncated, JSON-safe)
GOAL_FILE="${TMPDIR:-/tmp}/claude-session-goal.txt"
goal="unknown"
if [ -f "$GOAL_FILE" ]; then
    goal=$(head -c 100 "$GOAL_FILE" 2>/dev/null | tr '"' "'" | tr '\n' ' ' | tr '\r' '')
    goal=${goal:-unknown}
fi

# 3. Hook errors today
LOG_DATE=$(date +%Y-%m-%d)
LOG_FILE="$HOME/.claude/logs/hooks/$LOG_DATE.log"
hook_errors=0
if [ -f "$LOG_FILE" ]; then
    # Only count errors from last 2 hours (current session), not entire day
    cutoff=$(date -v-2H +%H:%M:%S 2>/dev/null || date -d "2 hours ago" +%H:%M:%S 2>/dev/null || echo "00:00:00")
    hook_errors=$(awk -v cutoff="$cutoff" '/^\[([0-9]{2}:[0-9]{2}:[0-9]{2})\].*ERROR/ { match($0, /\[([0-9]{2}:[0-9]{2}:[0-9]{2})\]/, t); if (t[1] >= cutoff) c++ } END { print c+0 }' "$LOG_FILE" 2>/dev/null || echo 0)
fi

# 4. Commits in last 2 hours
commit_count=0
if cd "$HOME/proggs" 2>/dev/null; then
    commit_count=$(git log --oneline --since="2 hours ago" 2>/dev/null | wc -l | tr -d ' ')
    commit_count=${commit_count:-0}
fi

# 5. Session duration
duration_min=0
if [ -f "$GOAL_FILE" ]; then
    if [ "$(uname)" = "Darwin" ]; then
        goal_mtime=$(stat -f %m "$GOAL_FILE" 2>/dev/null || echo 0)
    else
        goal_mtime=$(stat -c %Y "$GOAL_FILE" 2>/dev/null || echo 0)
    fi
    now=$(date +%s)
    elapsed=$(( (now - goal_mtime) / 60 ))
    if [ "$elapsed" -lt 240 ] 2>/dev/null; then
        duration_min=$elapsed
    fi
fi

# 6. Build JSON line
timestamp=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
json_line="{\"date\":\"$timestamp\",\"turns\":$turns,\"hook_errors\":$hook_errors,\"commits\":$commit_count,\"duration_min\":$duration_min,\"goal\":\"$goal\"}"

# 7. Append to scores file
echo "$json_line" >> "$SCORES_FILE" 2>/dev/null

exit 0
