#!/usr/bin/env bash
# statusline.sh — Effort, path, git, context, rate-limits, version, tokens, agent, commit, todos, cpu/ram, model, time

input=$(cat)

# Effort level aus settings.json (nicht im JSON)
settings="$HOME/.claude/settings.json"
effort="?"
if [ -f "$settings" ]; then
    effort=$(jq -r '.effortLevel // "?"' "$settings" 2>/dev/null)
fi

# JSON-Felder parsen
model=$(echo "$input" | jq -r '.model.display_name // "?"')
remaining=$(echo "$input" | jq -r '.context_window.remaining_percentage // empty')
cwd=$(echo "$input" | jq -r '.workspace.current_dir // empty' | sed "s|$HOME|~|g")
version=$(echo "$input" | jq -r '.version // empty')
tokens_in=$(echo "$input" | jq -r '.context_window.total_input_tokens // empty')
tokens_out=$(echo "$input" | jq -r '.context_window.total_output_tokens // empty')
agent_name=$(echo "$input" | jq -r '.agent.name // empty')

# Rate limits (nur Claude Max, erst nach erstem API-Call vorhanden)
five_h_used=$(echo "$input" | jq -r '.rate_limits.five_hour.used_percentage // empty')
five_h_resets=$(echo "$input" | jq -r '.rate_limits.five_hour.resets_at // empty')
week_used=$(echo "$input" | jq -r '.rate_limits.seven_day.used_percentage // empty')

# Prozent auf Integer runden
[ -n "$five_h_used" ] && five_h_used=$(printf "%.0f" "$five_h_used" 2>/dev/null)
[ -n "$week_used" ]   && week_used=$(printf "%.0f" "$week_used" 2>/dev/null)

# 5h Reset-Countdown (z.B. "47m" oder "1h23m")
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

# Git: Branch, Dirty-Flag, letzter Commit
branch=""
status=""
last_commit=""
work_dir=$(echo "$input" | jq -r '.workspace.current_dir // empty')
if [ -n "$work_dir" ]; then
    cd "$work_dir" 2>/dev/null
    branch=$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo "")
    if [ -n "$branch" ]; then
        [ -n "$(git status --porcelain 2>/dev/null | head -1)" ] && status="*"
        last_commit=$(git log --oneline -1 2>/dev/null | cut -c1-40 | sed 's/ /:/' | cut -c1-38)
    fi
fi

# TODOs: schnelles Grep in Kotlin-Quellen
todos=""
if [ -n "$work_dir" ] && [ -d "$work_dir/app/src" ]; then
    todos=$(grep -rl "TODO\|FIXME" "$work_dir/app/src" --include="*.kt" 2>/dev/null | wc -l | tr -d ' ')
fi

# CPU und RAM via PowerShell (einmaliger Aufruf fuer beide)
cpu_ram=""
if command -v pwsh.exe &>/dev/null; then
    cpu_ram=$(pwsh.exe -NoProfile -Command \
        '$c=[math]::Round((Get-CimInstance Win32_Processor|Measure-Object LoadPercentage -Average).Average);$o=Get-CimInstance Win32_OperatingSystem;$f=[math]::Round($o.FreePhysicalMemory/1MB,1);$t=[math]::Round($o.TotalVisibleMemorySize/1MB,1);Write-Output "CPU:${c}% RAM:${f}/${t}G"' \
        2>/dev/null | tr -d '\r')
elif command -v powershell.exe &>/dev/null; then
    cpu_ram=$(powershell.exe -NoProfile -Command \
        '$c=[math]::Round((Get-CimInstance Win32_Processor|Measure-Object LoadPercentage -Average).Average);$o=Get-CimInstance Win32_OperatingSystem;$f=[math]::Round($o.FreePhysicalMemory/1MB,1);$t=[math]::Round($o.TotalVisibleMemorySize/1MB,1);Write-Output "CPU:${c}% RAM:${f}/${t}G"' \
        2>/dev/null | tr -d '\r')
fi

# Uhrzeit
time=$(date +%H:%M)

# Farben (ANSI 24-bit)
E='\033[38;2;255;215;0m'     # Gold   — Effort
B='\033[38;2;30;102;245m'    # Blau   — Pfad
G='\033[38;2;64;160;43m'     # Gruen  — Git-Branch
Y='\033[38;2;223;142;29m'    # Amber  — Dirty, Zeit, TODOs
M='\033[38;2;136;57;239m'    # Lila   — Context
RL='\033[38;2;0;200;150m'    # Teal   — Rate normal (<70%)
RW='\033[38;2;255;140;0m'    # Orange — Rate Warnung (70-89%)
RC='\033[38;2;220;50;50m'    # Rot    — Rate kritisch (>=90%)
T='\033[38;2;76;79;105m'     # Grau   — Modell, Version, System
C='\033[38;2;100;180;255m'   # Cyan   — Token-Zaehler
A='\033[38;2;255;100;200m'   # Pink   — Aktiver Agent
R='\033[0m'                  # Reset

# Rate-Limit-Farbe
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

# Tokens als "123K" formatieren
fmt_tokens() {
    local n=$1
    if [ -z "$n" ] || ! [[ "$n" =~ ^[0-9]+$ ]] || [ "$n" -eq 0 ] 2>/dev/null; then
        echo -n "0"
        return
    fi
    if [ "$n" -ge 1000 ]; then
        echo -n "$((n / 1000))K"
    else
        echo -n "${n}"
    fi
}

effort_upper=$(echo "$effort" | tr '[:lower:]' '[:upper:]')

# --- Ausgabe aufbauen ---

printf "${E}[${effort_upper}]${R}"
[ -n "$cwd" ]       && printf " ${B}${cwd}${R}"
[ -n "$branch" ]    && printf " ${G}${branch}${Y}${status}${R}"
[ -n "$remaining" ] && printf " ${M}ctx:${remaining}%%${R}"

# Aktiver Agent (nur wenn einer laeuft)
[ -n "$agent_name" ] && printf " ${A}[${agent_name}]${R}"

# 5h Rate-Limit
if [ -n "$five_h_used" ]; then
    col=$(rate_color "$five_h_used")
    if [ -n "$five_h_countdown" ]; then
        printf " ${col}5h:${five_h_used}%%(${five_h_countdown})${R}"
    else
        printf " ${col}5h:${five_h_used}%%${R}"
    fi
fi

# 7d Rate-Limit
if [ -n "$week_used" ]; then
    col=$(rate_color "$week_used")
    printf " ${col}7d:${week_used}%%${R}"
fi

# Token-Zaehler (Input/Output)
if [ -n "$tokens_in" ] || [ -n "$tokens_out" ]; then
    t_in=$(fmt_tokens "$tokens_in")
    t_out=$(fmt_tokens "$tokens_out")
    printf " ${C}tok:${t_in}/${t_out}${R}"
fi

# Offene TODOs (nur wenn >0)
if [ -n "$todos" ] && [ "$todos" -gt 0 ] 2>/dev/null; then
    printf " ${Y}TODO:${todos}${R}"
fi

# CPU und RAM
[ -n "$cpu_ram" ] && printf " ${T}${cpu_ram}${R}"

# Letzter Commit
[ -n "$last_commit" ] && printf " ${T}${last_commit}${R}"

# Modell + Claude Code Version
printf " ${T}${model}${R}"
[ -n "$version" ] && printf " ${T}v${version}${R}"

# Uhrzeit
printf " ${Y}${time}${R}"
echo
