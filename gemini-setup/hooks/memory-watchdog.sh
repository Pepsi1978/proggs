#!/usr/bin/env bash
# Memory Watchdog — SubagentStop Hook (v1.0, macOS/Linux version)
# Checks if a senior agent wrote back to MEMORY.md after finishing.
# Equivalent of memory-watchdog.ps1 for non-Windows platforms.

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/hook-log.sh"
source "$SCRIPT_DIR/whiteboard-insert.sh"

# Input guard — only run on REAL SubagentStop events that have an agent_id.
# Prevents the 2026-04-15/18 endless-loop bug (50+ spurious fires per session).
# NEVER dot-sourced — exit 0 is safe.
stdin_input=$(cat)
if [ -z "$stdin_input" ]; then exit 0; fi
agent_id=$(echo "$stdin_input" | python3 -c "import sys,json
try:
    d=json.load(sys.stdin)
    print(d.get('agent_id','') or '')
except Exception:
    print('')
" 2>/dev/null)
if [ -z "$agent_id" ]; then exit 0; fi

MEMORY_FILE="$HOME/proggs/.Gemini/agent-memory/shared/MEMORY.md"
COUNTER_FILE="/tmp/Gemini-writeback-counter.txt"

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

# Alert after 5+ consecutive misses (was 3 — raised to 5 to reduce log-spam when
# parallel coder/researcher agents run in batches without needing to write MEMORY.md)
# Deduplication: skip logging if the last entry of this kind is less than 60 minutes old.
LAST_LOG_FILE="/tmp/Gemini-writeback-last-log.txt"
if [ "$miss_count" -ge 5 ]; then
    should_log=true
    if [ -f "$LAST_LOG_FILE" ]; then
        last_log_ts=$(cat "$LAST_LOG_FILE" 2>/dev/null || echo 0)
        now_ts=$(date +%s)
        diff_sec=$((now_ts - last_log_ts))
        if [ "$diff_sec" -lt 3600 ]; then
            should_log=false
        fi
    fi
    if [ "$should_log" = true ]; then
        ts=$(date +"%Y-%m-%d %H:%M")
        entry="### [$ts] Agent: Write-Back nicht erfolgt ($miss_count aufeinanderfolgende Agents) — Status: AUTO-LOGGED"
        insert_whiteboard_entry "Offene Fehler & Probleme" "$entry" 2>/dev/null || true
        date +%s > "$LAST_LOG_FILE" 2>/dev/null || true
        hook_log "$miss_count consecutive misses — logged to MEMORY.md"
        echo "MEMORY_WATCHDOG: $miss_count consecutive misses — logged to MEMORY.md"
    else
        hook_log "$miss_count consecutive misses — dedup (last log <60min)"
        echo "MEMORY_WATCHDOG: $miss_count consecutive misses — dedup (last log <60min)"
    fi
    # Reset counter whether logged or deduped
    echo "0" > "$COUNTER_FILE" 2>/dev/null || true
else
    hook_log "no write-back ($miss_count/5 misses)"
    echo "MEMORY_WATCHDOG: No write-back ($miss_count/5 misses)"
fi

exit 0

