#!/bin/bash
# agent-resource-lock.sh — Prevents parallel agents from using the same resource
# C4: Agentmaxxing Resource Lock Pattern
# Usage: ./agent-resource-lock.sh <resource> <action>
# Arguments:
#   <resource> — e.g., "android-emulator", "port-5000", "gradle-daemon"
#   <action>   — acquire | release | check
# Lock files live in ~/.cache/claude-locks/ — auto-expire after 10 minutes

source "$(dirname "$0")/hook-log.sh"

set -euo pipefail

if [ $# -ne 2 ]; then
    echo "Usage: $0 <resource> <acquire|release|check>"
    exit 2
fi

RESOURCE="$1"
ACTION="$2"

# Validate action argument
case "$ACTION" in
    acquire|release|check) ;;
    *)
        echo "Invalid action '$ACTION'. Must be: acquire, release, or check"
        exit 2
        ;;
esac

# Use ~/.cache instead of /tmp to avoid symlink attacks on shared systems
LOCK_DIR="$HOME/.cache/claude-locks"
mkdir -p "$LOCK_DIR"
LOCK_FILE="$LOCK_DIR/${RESOURCE}.lock"

# Returns lock age in whole minutes, or -1 if no lock file exists
get_lock_age_minutes() {
    if [ ! -f "$LOCK_FILE" ]; then
        echo -1
        return
    fi
    local now
    local mtime
    now=$(date +%s)
    # macOS uses -f format, Linux uses -c format — try BSD first (more common on macOS)
    if [ "$(uname)" = "Darwin" ]; then
        mtime=$(stat -f %m "$LOCK_FILE" 2>/dev/null) || mtime=""
    else
        mtime=$(stat -c %Y "$LOCK_FILE" 2>/dev/null) || mtime=""
    fi
    # Fallback: if both stat variants fail, treat mtime as 0 (lock appears very old)
    mtime=${mtime:-0}
    echo $(( (now - mtime) / 60 ))
}

case "$ACTION" in
    acquire)
        age=$(get_lock_age_minutes)
        if [ "$age" -ge 0 ] && [ "$age" -lt 10 ]; then
            owner=$(cat "$LOCK_FILE" 2>/dev/null || echo "unknown")
            hook_log "lock DENIED for '$RESOURCE' — held by $owner (${age}min ago)"
            echo "LOCKED — Resource '$RESOURCE' is held by $owner (${age}min ago). Wait or use a different resource."
            exit 1
        fi
        # Atomic lock acquisition using noclobber — prevents TOCTOU race condition.
        # With noclobber set, the shell refuses to overwrite an existing file via >,
        # making check-and-create a single atomic operation.
        set -o noclobber
        if (echo "$(hostname)-$$-$(date +%H:%M:%S)" > "$LOCK_FILE") 2>/dev/null; then
            set +o noclobber
            hook_log "lock ACQUIRED for '$RESOURCE'"
            echo "ACQUIRED — Resource '$RESOURCE' locked."
            exit 0
        else
            set +o noclobber
            owner=$(cat "$LOCK_FILE" 2>/dev/null || echo "unknown")
            hook_log "lock DENIED for '$RESOURCE' — race condition, now held by $owner"
            echo "LOCKED — Resource '$RESOURCE' was just locked by $owner. Wait or use a different resource."
            exit 1
        fi
        ;;

    release)
        if [ -f "$LOCK_FILE" ]; then
            rm -f "$LOCK_FILE"
            hook_log "lock RELEASED for '$RESOURCE'"
            echo "RELEASED — Resource '$RESOURCE' unlocked."
        else
            hook_log "lock RELEASE called but '$RESOURCE' was not locked"
            echo "NO-LOCK — Resource '$RESOURCE' was not locked."
        fi
        exit 0
        ;;

    check)
        age=$(get_lock_age_minutes)
        if [ "$age" -ge 0 ] && [ "$age" -lt 10 ]; then
            owner=$(cat "$LOCK_FILE" 2>/dev/null || echo "unknown")
            hook_log "lock CHECK: '$RESOURCE' is BUSY — held by $owner (${age}min ago)"
            echo "BUSY — Resource '$RESOURCE' is held by $owner (${age}min ago)."
            exit 1
        else
            hook_log "lock CHECK: '$RESOURCE' is FREE"
            echo "FREE — Resource '$RESOURCE' is available."
            exit 0
        fi
        ;;
esac
