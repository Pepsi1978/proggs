#!/usr/bin/env bash
# statusline.sh — Effort, path, git, context, rate-limits (5h/7d), model, time
# Claude Code pipes JSON to stdin with session info.
# effortLevel is NOT in the JSON — we read it from settings.json.

input=$(cat)

# Read effort level from settings.json
settings="$HOME/.claude/settings.json"
effort="?"
if [ -f "$settings" ]; then
    effort=$(jq -r '.effortLevel // "?"' "$settings" 2>/dev/null)
fi

# Parse stdin JSON
model=$(echo "$input" | jq -r '.model.display_name // "?"')
remaining=$(echo "$input" | jq -r '.context_window.remaining_percentage // empty')
cwd=$(echo "$input" | jq -r '.workspace.current_dir // empty' | sed "s|$HOME|~|g")

# Rate limits (Claude Max only — optional, only present after first API call)
five_h_used=$(echo "$input" | jq -r '.rate_limits.five_hour.used_percentage // empty')
five_h_resets=$(echo "$input" | jq -r '.rate_limits.five_hour.resets_at // empty')
week_used=$(echo "$input" | jq -r '.rate_limits.seven_day.used_percentage // empty')

# Round percentages to integers
[ -n "$five_h_used" ] && five_h_used=$(printf "%.0f" "$five_h_used" 2>/dev/null)
[ -n "$week_used" ]   && week_used=$(printf "%.0f" "$week_used" 2>/dev/null)

# 5h reset countdown (e.g. "47min" or "1h23min")
five_h_countdown=""
if [ -n "$five_h_resets" ] && [ "$five_h_resets" -gt 0 ] 2>/dev/null; then
    now=$(date +%s)
    diff=$((five_h_resets - now))
    if [ "$diff" -gt 0 ]; then
        mins=$((diff / 60))
        if [ "$mins" -ge 60 ]; then
            h=$((mins / 60))
            m=$((mins % 60))
            five_h_countdown="${h}h${m}m"
        else
            five_h_countdown="${mins}m"
        fi
    fi
fi

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

# Colors (ANSI 24-bit)
E='\033[38;2;255;215;0m'     # Gold   — effort level
B='\033[38;2;30;102;245m'    # Blue   — path
G='\033[38;2;64;160;43m'     # Green  — git branch
Y='\033[38;2;223;142;29m'    # Amber  — dirty marker, time
M='\033[38;2;136;57;239m'    # Purple — context
RL='\033[38;2;0;200;150m'    # Teal   — rate limit normal (<70%)
RW='\033[38;2;255;140;0m'    # Orange — rate limit warning (70-89%)
RC='\033[38;2;220;50;50m'    # Red    — rate limit critical (>=90%)
T='\033[38;2;76;79;105m'     # Gray   — model
R='\033[0m'                  # Reset

# Rate-limit color based on usage
rate_color() {
    local pct=$1
    if [ -z "$pct" ] || ! [[ "$pct" =~ ^[0-9]+$ ]]; then
        echo -n "$RL"
    elif [ "$pct" -ge 90 ]; then
        echo -n "$RC"
    elif [ "$pct" -ge 70 ]; then
        echo -n "$RW"
    else
        echo -n "$RL"
    fi
}

effort_upper=$(echo "$effort" | tr '[:lower:]' '[:upper:]')

# Build output
printf "${E}[${effort_upper}]${R}"
[ -n "$cwd" ]      && printf " ${B}${cwd}${R}"
[ -n "$branch" ]   && printf " ${G}${branch}${Y}${status}${R}"
[ -n "$remaining" ] && printf " ${M}ctx:${remaining}%%${R}"

# 5h rate limit
if [ -n "$five_h_used" ]; then
    col=$(rate_color "$five_h_used")
    if [ -n "$five_h_countdown" ]; then
        printf " ${col}5h:${five_h_used}%%(${five_h_countdown})${R}"
    else
        printf " ${col}5h:${five_h_used}%%${R}"
    fi
fi

# 7d rate limit
if [ -n "$week_used" ]; then
    col=$(rate_color "$week_used")
    printf " ${col}7d:${week_used}%%${R}"
fi

printf " ${T}${model}${R} ${Y}${time}${R}"
echo
