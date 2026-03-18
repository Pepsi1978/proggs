#!/bin/bash
# Notification hook for Claude Code — macOS only
HOOK_NAME="notify" source "$HOME/.claude/hooks/hook-log.sh" 2>/dev/null || true

INPUT=$(cat)

# Parse message
MESSAGE=""
if command -v python3 &>/dev/null; then
    MESSAGE=$(echo "$INPUT" | python3 -c "import sys,json; print(json.load(sys.stdin).get('message',''))" 2>/dev/null || echo "")
fi
[ -z "$MESSAGE" ] && MESSAGE="Claude Code braucht deine Aufmerksamkeit"

# Truncate
[ ${#MESSAGE} -gt 200 ] && MESSAGE="${MESSAGE:0:197}..."

# Escape MESSAGE for safe osascript injection (prevent command injection)
SAFE_MESSAGE=$(printf '%s' "$MESSAGE" | sed 's/\\/\\\\/g; s/"/\\"/g')
osascript -e "display notification \"$SAFE_MESSAGE\" with title \"Claude Code\"" 2>/dev/null || true
exit 0
