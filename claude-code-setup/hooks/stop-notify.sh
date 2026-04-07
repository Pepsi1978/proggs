#!/bin/bash
# stop-notify.sh — Fires immediately when Claude finishes work
# Hook event: Stop
# Platform: macOS (uses osascript for native notification)

HOOKS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$HOOKS_DIR/hook-log.sh"

# Drain stdin (required by hook protocol)
cat > /dev/null

# --- 1. Terminal BEL (instant) ---
printf '\a' 2>/dev/null

# --- 2. macOS notification ---
if command -v osascript &>/dev/null; then
    osascript -e 'display notification "Claude Code ist fertig" with title "Claude Code"' 2>/dev/null &
fi

exit 0
