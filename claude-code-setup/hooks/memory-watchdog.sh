#!/usr/bin/env bash
# Memory Watchdog — SubagentStop Hook (v1.0, macOS/Linux version)
# Checks if a senior agent wrote back to MEMORY.md after finishing.
# Equivalent of memory-watchdog.ps1 for non-Windows platforms.

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/hook-log.sh"
source "$SCRIPT_DIR/whiteboard-insert.sh"

MEMORY_FILE="$HOME/proggs/.claude/agent-memory/shared/MEMORY.md"
COUNTER_FILE="/tmp/claude-writeback-counter.txt"

# Check if MEMORY.md was modified in the last 3 minutes
recent_write=false
if [ -f "$MEMORY_FILE" ]; then
    if [ "$(uname)" = "Darwin" ]; then
        last_mod=$(stat -f %m "$MEMORY_FILE")
    else
        last_mod=$(stat -c %Y "$MEMORY_FILE")
    fi
    now=$(date +%s)
    diff=$((now - last_mod))
    if [ "$diff" -lt 180 ]; then
        recent_write=true
    fi
fi

if [ "$recent_write" = true ]; then
    echo "0" > "$COUNTER_FILE"
    hook_log "write-back detected — counter reset"
    echo "MEMORY_WATCHDOG: Write-back detected — counter reset"
    exit 0
fi

# Acquire file lock for atomic read-increment-write to prevent race conditions
# when multiple SubagentStop hooks run concurrently (parallel agents finishing)
if command -v flock &>/dev/null; then
    (
        flock -w 2 200 || exit 1
        miss_count=$(cat "$COUNTER_FILE" 2>/dev/null || echo "0")
        miss_count=$((miss_count + 1))
        echo "$miss_count" > "$COUNTER_FILE"
    ) 200>"$COUNTER_FILE.lock"
else
    # flock not available — proceed without locking (race condition possible but non-fatal)
    miss_count=$(cat "$COUNTER_FILE" 2>/dev/null || echo "0")
    miss_count=$((miss_count + 1))
    echo "$miss_count" > "$COUNTER_FILE"
fi

miss_count=$(cat "$COUNTER_FILE" 2>/dev/null || echo 0)

# Alert after 2+ consecutive misses (was 5 — lowered to catch write-back failures faster)
if [ "$miss_count" -ge 2 ]; then
    ts=$(date +"%Y-%m-%d %H:%M")
    entry="### [$ts] Agent: Write-Back nicht erfolgt ($miss_count aufeinanderfolgende Agents) — Status: AUTO-LOGGED"
    insert_whiteboard_entry "Offene Fehler & Probleme" "$entry" || true
    # Reset counter after logging
    echo "0" > "$COUNTER_FILE"
    hook_log "$miss_count consecutive misses — logged to MEMORY.md"
    echo "MEMORY_WATCHDOG: $miss_count consecutive misses — logged to MEMORY.md"
else
    hook_log "no write-back ($miss_count/2 misses)"
    echo "MEMORY_WATCHDOG: No write-back ($miss_count/2 misses)"
fi

exit 0
