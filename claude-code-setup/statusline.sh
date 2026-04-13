#!/usr/bin/env bash
# statusline.sh — Shows effort level, model, context remaining, git branch, time
# Claude Code pipes JSON to stdin with session info.
# effortLevel is NOT in the JSON — we read it from settings.json.

input=$(cat)

# Read effort level from settings.json (not provided in stdin JSON)
settings="$HOME/.claude/settings.json"
effort="?"
if [ -f "$settings" ]; then
    effort=$(jq -r '.effortLevel // "?"' "$settings" 2>/dev/null)
fi

# Parse stdin JSON
model=$(echo "$input" | jq -r '.model.display_name // "?"')
remaining=$(echo "$input" | jq -r '.context_window.remaining_percentage // empty')
cwd=$(echo "$input" | jq -r '.workspace.current_dir // empty' | sed "s|$HOME|~|g")

# Git branch
branch=""
status=""
work_dir=$(echo "$input" | jq -r '.workspace.current_dir // empty')
if [ -n "$work_dir" ]; then
    cd "$work_dir" 2>/dev/null
    branch=$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo "")
    if [ -n "$branch" ]; then
        [ -n "$(git status --porcelain 2>/dev/null | head -1)" ] && status="*"
    fi
fi

# Time
time=$(date +%H:%M)

# Colors (ANSI 256)
E='\033[38;2;255;215;0m'   # Gold — effort level (eye-catching)
B='\033[38;2;30;102;245m'  # Blue — path
G='\033[38;2;64;160;43m'   # Green — git branch
Y='\033[38;2;223;142;29m'  # Yellow — dirty marker, time
M='\033[38;2;136;57;239m'  # Magenta — context
T='\033[38;2;76;79;105m'   # Gray — model
R='\033[0m'                 # Reset

# Effort display with uppercase
effort_upper=$(echo "$effort" | tr '[:lower:]' '[:upper:]')

# Build output
printf "${E}[${effort_upper}]${R}"
[ -n "$cwd" ] && printf " ${B}${cwd}${R}"
[ -n "$branch" ] && printf " ${G}${branch}${Y}${status}${R}"
[ -n "$remaining" ] && printf " ${M}ctx:${remaining}%%${R}"
printf " ${T}${model}${R} ${Y}${time}${R}"
echo
