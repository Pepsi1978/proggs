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
    # Validate: must be numeric, otherwise reset to 0
    case "$turns" in
        ''|*[!0-9]*) turns=0 ;;
    esac
fi

# Skip trivial sessions
if [ "$turns" -lt 3 ] 2>/dev/null; then
    exit 0
fi

# 2. Session goal (truncated, JSON-safe)
GOAL_FILE="${TMPDIR:-/tmp}/claude-session-goal.txt"
goal="unknown"
if [ -f "$GOAL_FILE" ]; then
    goal=$(head -c 100 "$GOAL_FILE" 2>/dev/null | tr '"' "'" | tr '\n\r' '  ' | sed 's/\\/\\\\/g')
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

# 6. Calculate IQ score and build JSON line
timestamp=$(date -u +"%Y-%m-%dT%H:%M:%SZ")

# Try Python first (guarantees correct JSON escaping and float math)
python_cmd=""
for candidate in python3 python; do
    if command -v "$candidate" >/dev/null 2>&1; then
        python_cmd="$candidate"
        break
    fi
done

if [ -n "$python_cmd" ]; then
    "$python_cmd" -c "
import json, os, pathlib
goal_path = os.path.join(os.environ.get('TMPDIR', '/tmp'), 'claude-session-goal.txt')
if not os.path.exists(goal_path):
    goal_path = '/tmp/claude-session-goal.txt'
goal = 'unknown'
if os.path.exists(goal_path):
    raw = pathlib.Path(goal_path).read_text(encoding='utf-8', errors='replace').strip()
    goal = raw[:100].replace('\n', ' ').replace('\r', '')
turns = $turns
hook_errors = $hook_errors
commit_count = $commit_count
duration_min = $duration_min
efficiency = min(commit_count / max(turns, 1) * 100, 40)
quality = max(0, 30 - hook_errors * 10)
duration_bonus = min(duration_min / 3, 30)
iq_score = max(0, min(100, round(efficiency + quality + duration_bonus)))
data = {
    'date': '$timestamp',
    'turns': turns,
    'hook_errors': hook_errors,
    'commits': commit_count,
    'duration_min': duration_min,
    'goal': goal,
    'iq_score': iq_score
}
scores = '$SCORES_FILE'
line = json.dumps(data, ensure_ascii=False)
with open(scores, 'a', encoding='utf-8') as f:
    f.write(line + '\n')
" 2>/dev/null
else
    # Fallback: bash integer arithmetic (no floats — use scaled integers)
    # efficiency = min(commits * 100 / max(turns, 1), 40)
    denom=$(( turns > 0 ? turns : 1 ))
    efficiency=$(( commit_count * 100 / denom ))
    efficiency=$(( efficiency > 40 ? 40 : efficiency ))
    # quality = max(0, 30 - hook_errors * 10)
    quality=$(( 30 - hook_errors * 10 ))
    quality=$(( quality < 0 ? 0 : quality ))
    # duration_bonus = min(duration_min / 3, 30)
    duration_bonus=$(( duration_min / 3 ))
    duration_bonus=$(( duration_bonus > 30 ? 30 : duration_bonus ))
    # iq_score clamped to 0-100
    iq_score=$(( efficiency + quality + duration_bonus ))
    iq_score=$(( iq_score < 0 ? 0 : iq_score ))
    iq_score=$(( iq_score > 100 ? 100 : iq_score ))
    goal_clean=$(echo "$goal" | tr '\\' '_' | tr '"' '_')
    json_line="{\"date\":\"$timestamp\",\"turns\":$turns,\"hook_errors\":$hook_errors,\"commits\":$commit_count,\"duration_min\":$duration_min,\"goal\":\"$goal_clean\",\"iq_score\":$iq_score}"
    echo "$json_line" >> "$SCORES_FILE" 2>/dev/null
fi

exit 0
