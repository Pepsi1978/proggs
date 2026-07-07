#!/bin/bash
# Dynamic notification for macOS: extracts the actual message from Claude Code
# Hook event: Notification
# Platform: macOS (uses osascript for native notifications)
#
# On Linux without osascript, falls back to notify-send if available.
# Silently succeeds if no notification tool is present — notifications are non-critical.

HOOKS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$HOOKS_DIR/hook-log.sh"

# ---------------------------------------------------------------------------
# Read JSON from stdin and extract notification message
# ---------------------------------------------------------------------------
hook_input="$(cat)"

# Guard: if python3 is unavailable, use fallback message
if ! command -v python3 &>/dev/null; then
    msg=""
else
    msg="$(printf '%s' "$hook_input" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    print(d.get('notification', {}).get('message', '') or '')
except Exception:
    print('')
" 2>/dev/null)"
fi

if [ -z "$msg" ]; then
    msg="Braucht deine Aufmerksamkeit"
fi

# Truncate to 200 characters (same limit as the PS1 version)
msg="${msg:0:200}"

# ---------------------------------------------------------------------------
# Send notification
# ---------------------------------------------------------------------------
if command -v osascript &>/dev/null; then
    # macOS native notification via AppleScript
    # Escape backslashes and double-quotes to prevent quote injection into osascript
    safe_msg="${msg//\\/\\\\}"
    safe_msg="${safe_msg//\"/\\\"}"
    osascript -e "display notification \"${safe_msg}\" with title \"Claude Code\"" 2>/dev/null
    if [ $? -ne 0 ]; then
        hook_log_warn "osascript notification failed"
    fi
elif command -v notify-send &>/dev/null; then
    # Linux fallback (requires libnotify-bin)
    notify-send "Claude Code" "$msg" 2>/dev/null
    if [ $? -ne 0 ]; then
        hook_log_warn "notify-send failed"
    fi
fi
# Silently fail on any other platform — notifications are non-critical

exit 0
