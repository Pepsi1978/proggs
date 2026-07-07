#!/usr/bin/env bash
# session-cleanup.sh — SessionEnd Hook
# Cleans up temp files when Claude Code exits.
# Platform: macOS/Linux (bash)

HOOKS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$HOOKS_DIR/hook-log.sh"

CLEANED=0

# Clean Claude temp directory (files older than 2 hours)
CLAUDE_TEMP="${TMPDIR:-/tmp}/claude"
if [ -d "$CLAUDE_TEMP" ]; then
    # Remove files older than 2 hours (120 minutes)
    while IFS= read -r -d '' file; do
        rm -f "$file" 2>/dev/null && CLEANED=$((CLEANED + 1)) || true
    done < <(find "$CLAUDE_TEMP" -type f -mmin +120 -print0 2>/dev/null)

    # Remove empty directories (bottom-up)
    find "$CLAUDE_TEMP" -type d -empty -delete 2>/dev/null || true
fi

# Clean node compile cache (files older than 1 day)
NODE_CACHE="${TMPDIR:-/tmp}/node-compile-cache"
if [ -d "$NODE_CACHE" ]; then
    while IFS= read -r -d '' file; do
        rm -f "$file" 2>/dev/null && CLEANED=$((CLEANED + 1)) || true
    done < <(find "$NODE_CACHE" -type f -mmin +1440 -print0 2>/dev/null)
fi

# Clean /tmp/claude-* files — but preserve intent-anker state files
while IFS= read -r -d '' file; do
    rm -f "$file" 2>/dev/null && CLEANED=$((CLEANED + 1)) || true
done < <(find "${TMPDIR:-/tmp}" -maxdepth 1 -name "claude-*" -type f \
    ! -name "claude-session-goal.txt" \
    ! -name "claude-turn-counter.txt" \
    ! -name "claude-intent-reminder.txt" \
    -print0 2>/dev/null)

# Clean /tmp/agent-writeback-* sentinel files older than 2 hours
while IFS= read -r -d '' file; do
    rm -f "$file" 2>/dev/null && CLEANED=$((CLEANED + 1)) || true
done < <(find /tmp -maxdepth 1 -name "agent-writeback-*.json" -mmin +120 -type f -print0 2>/dev/null)

# Clean /tmp/debate-round.json
rm -f /tmp/debate-round.json 2>/dev/null && CLEANED=$((CLEANED + 1)) || true

# Clean old hook log files (belt-and-suspenders alongside hook-log.sh rotation)
HOOK_LOGS="$HOME/.claude/logs/hooks"
if [ -d "$HOOK_LOGS" ]; then
    while IFS= read -r -d '' file; do
        rm -f "$file" 2>/dev/null && CLEANED=$((CLEANED + 1)) || true
    done < <(find "$HOOK_LOGS" -name "*.log" -mtime +14 -print0 2>/dev/null)
fi

# Log remaining disk space
if command -v df > /dev/null 2>&1; then
    FREE_KB=$(df -k "$HOME" 2>/dev/null | awk 'NR==2 {print $4}')
    if [ -n "$FREE_KB" ]; then
        FREE_GB=$(echo "scale=1; $FREE_KB / 1048576" | bc 2>/dev/null || echo "?")
        hook_log "cleanup done: $CLEANED files removed, ${FREE_GB}GB free"
    else
        hook_log "cleanup done: $CLEANED files removed"
    fi
else
    hook_log "cleanup done: $CLEANED files removed"
fi

exit 0
