#!/bin/bash
# SessionEnd hook: Clean up temp files — macOS only
HOOK_NAME="session-cleanup" source "$HOME/.claude/hooks/hook-log.sh" 2>/dev/null || true

find "$HOME/.tmp" -type f -mmin +120 -delete 2>/dev/null || true
find "$HOME/.tmp" -type d -empty -delete 2>/dev/null || true

TMPDIR_ACTUAL="${TMPDIR:-/tmp}"
find "$TMPDIR_ACTUAL/node-compile-cache" -type f -mtime +1 -delete 2>/dev/null || true

AVAIL=$(df -h "$HOME" 2>/dev/null | awk 'NR==2{print $4}')
echo "Session beendet. Speicher: ${AVAIL:-?} frei." >> "$HOME/.claude/session-cleanup.log" 2>/dev/null || true

exit 0
