#!/usr/bin/env bash
# Session Score: Automatic quality scoring at session end
# Runs as Stop hook — calculates a score based on session metrics
# Writes to ~/.claude/session-scores.jsonl for trend analysis
# Platform: macOS/Linux (Bash)

set +e

HOOKS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$HOOKS_DIR/hook-log.sh"

SCORE_FILE="$HOME/.claude/session-scores.jsonl"
REPO_DIR="$HOME/proggs"

# Collect metrics
score=5  # Base score
commits=0
fix_commits=0
disk_free="?"

# 1. Check if commits were made in the last 60 minutes
since=$(date -v-60M '+%Y-%m-%dT%H:%M:%S' 2>/dev/null || date -d '60 minutes ago' '+%Y-%m-%dT%H:%M:%S' 2>/dev/null)
if [ -n "$since" ] && [ -d "$REPO_DIR/.git" ]; then
    commits=$(cd "$REPO_DIR" && git log --oneline --since="$since" 2>/dev/null | wc -l | tr -d ' ')
fi

# 2. Score adjustments based on commits
if [ "$commits" -ge 3 ] 2>/dev/null; then
    score=$((score + 2))  # Productive session
elif [ "$commits" -ge 1 ] 2>/dev/null; then
    score=$((score + 1))  # Some output
else
    score=$((score - 1))  # No commits
fi

# 3. Check if any fix commits (fixing bugs is good work)
if [ -n "$since" ] && [ -d "$REPO_DIR/.git" ]; then
    fix_commits=$(cd "$REPO_DIR" && git log --oneline --since="$since" --grep="fix\|bug\|hotfix" -i 2>/dev/null | wc -l | tr -d ' ')
fi
if [ "$fix_commits" -gt 0 ] 2>/dev/null; then
    score=$((score + 1))
fi

# 4. Check disk space (penalize if critically low)
disk_pct=$(df -h / 2>/dev/null | awk 'NR==2 {gsub(/%/,""); print $5}')
disk_free=$(df -h / 2>/dev/null | awk 'NR==2 {print $4}')
if [ "$disk_pct" -ge 95 ] 2>/dev/null; then
    score=$((score - 1))
fi

# Clamp score to 1-10
if [ "$score" -lt 1 ]; then score=1; fi
if [ "$score" -gt 10 ]; then score=10; fi

# Build JSON line and append to JSONL file
current_date=$(date '+%Y-%m-%d')
current_time=$(date '+%H:%M:%S')
entry="{\"date\":\"$current_date\",\"time\":\"$current_time\",\"score\":$score,\"commits\":$commits,\"fix_commits\":$fix_commits,\"disk_free\":\"$disk_free\"}"

echo "$entry" >> "$SCORE_FILE" 2>/dev/null

hook_log "score=$score commits=$commits fixes=$fix_commits disk=$disk_free"

# Output for Claude Code hook display (stderr, not stdout)
echo "Session-Score: $score/10 (Commits: $commits, Fixes: $fix_commits)" >&2

exit 0
