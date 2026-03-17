#!/bin/bash
# Disk space guardian — macOS only
HOOK_NAME="disk-guard" source "$HOME/.claude/hooks/hook-log.sh" 2>/dev/null || true

USAGE=$(df "$HOME" 2>/dev/null | awk 'NR==2{gsub(/%/,"",$5); print $5}')
AVAIL=$(df -h "$HOME" 2>/dev/null | awk 'NR==2{print $4}')

if [ -n "$USAGE" ] && [ "$USAGE" -ge 95 ] 2>/dev/null; then
    echo "WARNUNG: Speicherplatz bei ${USAGE}% — nur $AVAIL frei!"
    osascript -e "display notification \"Nur noch $AVAIL frei (${USAGE}%)\" with title \"Speicherplatz kritisch!\"" 2>/dev/null || true
fi

exit 0
