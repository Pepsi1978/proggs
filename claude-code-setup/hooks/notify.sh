#!/bin/bash
# Dynamic notification: extracts the actual message from Claude Code
input=$(cat)
msg=$(echo "$input" | yq -r '.notification.message // empty' 2>/dev/null)
[[ -z "$msg" ]] && msg="Braucht deine Aufmerksamkeit"
# Truncate to 200 chars for readability
msg="${msg:0:200}"
terminal-notifier -title 'Claude Code' -message "$msg" -sound Glass -group claude-code 2>/dev/null
exit 0
