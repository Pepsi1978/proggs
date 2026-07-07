#!/usr/bin/env bash
# stopfailure-logger.sh — Logs API failures to whiteboard with rate limiting
# Triggered by StopFailure hook event (Claude Code v2.1.78+)
# Uses whiteboard-insert.sh for section-based writing (echo >> is FORBIDDEN!)
# Rate limit: max 1 whiteboard entry per hour to prevent spam

HOOKS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$HOOKS_DIR/hook-log.sh"
source "$HOOKS_DIR/whiteboard-insert.sh"

TIMESTAMP=$(date '+%Y-%m-%d %H:%M')

# Rate limiting: check if we already logged a StopFailure recently (60 min)
RATE_LIMIT_FILE="$TMPDIR/claude-stopfailure-last.txt"
if [ ! -d "$TMPDIR" ]; then
    RATE_LIMIT_FILE="/tmp/claude-stopfailure-last.txt"
fi

if [ -f "$RATE_LIMIT_FILE" ]; then
    last_write=$(stat -f "%m" "$RATE_LIMIT_FILE" 2>/dev/null || stat -c "%Y" "$RATE_LIMIT_FILE" 2>/dev/null)
    if [ -n "$last_write" ]; then
        now=$(date +%s)
        elapsed=$(( (now - last_write) / 60 ))
        if [ "$elapsed" -lt 60 ]; then
            hook_log "StopFailure rate-limited (last logged ${elapsed}min ago)"
            echo "StopFailure rate-limited (last logged ${elapsed}min ago)"
            exit 0
        fi
    fi
fi

# Read stdin for error details (Claude Code pipes error info)
ERROR_INPUT=""
if [ -t 0 ]; then
    # stdin is a terminal (not redirected) — no error details available
    ERROR_INPUT="No error details available (stdin empty)"
else
    ERROR_INPUT=$(cat 2>/dev/null)
    if [ -z "$(echo "$ERROR_INPUT" | tr -d '[:space:]')" ]; then
        ERROR_INPUT="No error details available (stdin empty)"
    fi
fi

# Truncate long error messages (max 500 chars)
if [ "${#ERROR_INPUT}" -gt 500 ]; then
    ERROR_INPUT="${ERROR_INPUT:0:500}... (truncated)"
fi

# Update rate limit marker
echo "$TIMESTAMP" > "$RATE_LIMIT_FILE" 2>/dev/null || true

# Build whiteboard entry
# 2026-05-30: Status TRANSIENT statt OFFEN — ein externer API-/Rate-Limit-Fehler
# ist kein reparierbarer Harness-Bug. So blaeht er die OFFEN-Liste nicht auf und
# der invariant-check zaehlt ihn nicht als ungeloesten Fehler.
ENTRY=$(cat << ENTRY_EOF

### $TIMESTAMP — StopFailure: API/Rate-Limit Error — Status: TRANSIENT (externer API-Rate-Limit, kein Harness-Bug)
**Quelle:** Hook: StopFailure (command-type, no API dependency)
**Symptom:** Session-Turn endete durch API-Fehler
**Details:** $ERROR_INPUT
**Fix-Vorschlag:** Pruefen ob Rate-Limit temporaer oder dauerhaft. Bei dauerhaftem Fehler: API-Key pruefen.
**Status:** TRANSIENT (externer API-Rate-Limit, kein Harness-Bug)
ENTRY_EOF
)

# Use whiteboard-insert.sh (already sourced above)
WHITEBOARD="$HOME/proggs/.claude/agent-memory/shared/MEMORY.md"
if [ -f "$WHITEBOARD" ]; then
    insert_whiteboard_entry "Offene Fehler & Probleme" "$ENTRY" || \
        echo "[stopfailure-logger] whiteboard-insert failed — error NOT logged to whiteboard. Manual check required."
else
    echo "[stopfailure-logger] whiteboard-insert failed — error NOT logged to whiteboard. Manual check required."
fi

echo "StopFailure logged to whiteboard at $TIMESTAMP"

exit 0
