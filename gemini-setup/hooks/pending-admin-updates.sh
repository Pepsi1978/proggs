#!/usr/bin/env bash
# Pending Admin Updates — SessionEnd Hook (macOS/Linux version)
# Checks for available system updates and logs them.
# Equivalent of pending-admin-updates.ps1 for non-Windows platforms.

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/hook-log.sh"
source "$SCRIPT_DIR/whiteboard-insert.sh"

if [ "$(uname)" = "Darwin" ]; then
    # macOS: check brew
    if command -v brew &>/dev/null; then
        # brew outdated returns exit code 1 when updates exist (not an error!)
        outdated=$(brew outdated 2>/dev/null | head -20) || true
        if [ -n "$outdated" ]; then
            count=$(echo "$outdated" | wc -l | tr -d ' ')
            update_list=$(echo "$outdated" | tr '\n' ', ' | sed 's/, $//')
            hook_log "found $count brew updates: $update_list"
            # whiteboard insert is best-effort — don't fail the hook if section is missing
            replace_whiteboard_entry "Systemzustand" "Pending Admin Updates" "- **Pending Admin Updates ($count):** $update_list" 2>/dev/null || true
            echo "Pending-Admin-Updates: $count brew Updates verfuegbar."
        else
            hook_log "no pending brew updates"
            echo "Pending-Admin-Updates: Keine ausstehenden Updates."
        fi
    else
        hook_log "brew not installed — skipped"
    fi
else
    # Linux: check apt
    if command -v apt &>/dev/null; then
        updates=$(apt list --upgradable 2>/dev/null | grep -c upgradable || echo 0)
        if [ "$updates" -gt 0 ]; then
            update_list=$(apt list --upgradable 2>/dev/null | grep upgradable | awk -F'/' '{print $1}' | tr '\n' ', ' | sed 's/, $//')
            hook_log "found $updates apt updates: $update_list"
            replace_whiteboard_entry "Systemzustand" "Pending Admin Updates" "- **Pending Admin Updates ($updates):** $update_list" 2>/dev/null || true
            echo "Pending-Admin-Updates: $updates apt Updates verfuegbar."
        else
            hook_log "no pending apt updates"
            echo "Pending-Admin-Updates: Keine ausstehenden Updates."
        fi
    else
        hook_log "apt not installed — skipped"
    fi
fi

exit 0
