#!/usr/bin/env bash
# zombie-cleanup.sh — SessionStart hook: Kill truly orphaned node processes
# Category: Standalone Non-Blocking — MUST end with exit 0
#
# Strategy: Only kill node processes whose PARENT process no longer exists.
# This safely preserves MCP servers for ALL running sessions while cleaning up
# orphans from crashed/closed sessions.
#
# IMPORTANT: NEVER kill claude processes — they manage their own lifecycle.
# Killing older claude processes DESTROYS active sessions in other tabs.
# (Root cause of "session destruction" bug, fixed 2026-04-12)

set +e  # Never fail

ZOMBIE_COUNT=0

if [[ "$(uname)" == "Darwin" ]]; then
    # macOS: check each node process's parent
    while IFS= read -r line; do
        NODE_PID=$(echo "$line" | awk '{print $1}')
        # Get parent PID
        PPID_VAL=$(ps -o ppid= -p "$NODE_PID" 2>/dev/null | tr -d ' ')
        if [[ -n "$PPID_VAL" ]] && [[ "$PPID_VAL" -gt 1 ]]; then
            # Check if parent still exists
            if ! kill -0 "$PPID_VAL" 2>/dev/null; then
                # Parent is GONE — true orphan
                kill -9 "$NODE_PID" 2>/dev/null && ((ZOMBIE_COUNT++))
            fi
        fi
    done < <(ps -eo pid,comm 2>/dev/null | grep '[n]ode' | grep -v grep)
else
    # Linux: use /proc to check parent existence
    for NODE_PID in $(pgrep node 2>/dev/null); do
        PPID_VAL=$(awk '{print $4}' "/proc/$NODE_PID/stat" 2>/dev/null)
        if [[ -n "$PPID_VAL" ]] && [[ "$PPID_VAL" -gt 1 ]]; then
            if [[ ! -d "/proc/$PPID_VAL" ]]; then
                # Parent is GONE — true orphan
                kill -9 "$NODE_PID" 2>/dev/null && ((ZOMBIE_COUNT++))
            fi
        fi
    done
fi

if [[ "$ZOMBIE_COUNT" -gt 0 ]]; then
    echo "Zombie-Cleanup: $ZOMBIE_COUNT zombie processes killed"
fi

# --- Plugin-Cache Cleanup (Poka-Yoke Stufe 3) ---
# Remove temp_git_* directories from plugin installs older than 7 days.
# Root cause: plugin installer creates temp_git_<ts>_<hash> clones but does not clean up on failure.
# Without cleanup these accumulate silently. Non-destructive: only touches temp_git_* names.
PLUGIN_CACHE="$HOME/.claude/plugins/cache"
if [[ -d "$PLUGIN_CACHE" ]]; then
    REMOVED=0
    # -mindepth 1 -maxdepth 1 restricts to direct children only
    # -mtime +7 = last modified more than 7 days ago
    while IFS= read -r dir; do
        [[ -z "$dir" ]] && continue
        if rm -rf "$dir" 2>/dev/null; then
            REMOVED=$((REMOVED + 1))
        fi
    done < <(find "$PLUGIN_CACHE" -mindepth 1 -maxdepth 1 -type d -name 'temp_git_*' -mtime +7 2>/dev/null)
    if [[ "$REMOVED" -gt 0 ]]; then
        echo "Plugin-Cache-Cleanup: $REMOVED stale temp_git_* directories removed"
    fi
fi

exit 0
