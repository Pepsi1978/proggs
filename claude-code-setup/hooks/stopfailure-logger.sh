#!/usr/bin/env bash
# stopfailure-logger.sh — Logs API failures to whiteboard without requiring API access
# Triggered by StopFailure hook event (Claude Code v2.1.78+)
# Uses whiteboard-insert.sh for section-based writing (echo >> is FORBIDDEN!)

HOOKS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$HOOKS_DIR/hook-log.sh"
source "$HOOKS_DIR/whiteboard-insert.sh"

TIMESTAMP=$(date '+%Y-%m-%d %H:%M')

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

# Build whiteboard entry
ENTRY=$(cat << ENTRY_EOF

### $TIMESTAMP — StopFailure: API/Rate-Limit Error — Status: OFFEN
**Quelle:** Hook: StopFailure (command-type, no API dependency)
**Symptom:** Session-Turn endete durch API-Fehler
**Details:** $ERROR_INPUT
**Fix-Vorschlag:** Pruefen ob Rate-Limit temporaer oder dauerhaft. Bei dauerhaftem Fehler: API-Key pruefen.
**Status:** OFFEN
ENTRY_EOF
)

# Use whiteboard-insert.sh (already sourced above)
WHITEBOARD="$HOME/proggs/.claude/agent-memory/shared/MEMORY.md"
if [ -f "$WHITEBOARD" ]; then
    insert_whiteboard_entry "Offene Fehler & Probleme" "$ENTRY" || \
        echo "[stopfailure-logger] whiteboard-insert failed — error NOT logged to whiteboard. Manual check required." >&2
else
    echo "[stopfailure-logger] whiteboard-insert failed — error NOT logged to whiteboard. Manual check required." >&2
fi

echo "StopFailure logged to whiteboard at $TIMESTAMP"
