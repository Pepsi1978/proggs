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

exit 0
