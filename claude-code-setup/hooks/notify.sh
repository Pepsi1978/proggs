#!/data/data/com.termux/files/usr/bin/bash
# Termux notification hook for Claude Code
# Uses jq instead of python3 for faster JSON parsing (~5ms vs ~200ms)

INPUT=$(cat)
MESSAGE=$(echo "$INPUT" | jq -r '.message // empty' 2>/dev/null)

if [ -z "$MESSAGE" ]; then
  MESSAGE="Claude Code braucht deine Aufmerksamkeit"
fi

# Truncate very long messages for notification readability
if [ ${#MESSAGE} -gt 200 ]; then
  MESSAGE="${MESSAGE:0:197}..."
fi

termux-notification \
  --title "Claude Code" \
  --content "$MESSAGE" \
  --priority high \
  --vibrate 200 \
  --id claude-notify \
  --button1 "OK" \
  --button1-action "termux-notification-remove claude-notify" 2>/dev/null

termux-vibrate -d 200 2>/dev/null
