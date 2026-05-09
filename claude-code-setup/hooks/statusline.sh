#!/usr/bin/env bash
# statusline.sh — Schoene Statusline mit Icons + Fortschrittsbalken
# Reihenfolge: Modell | Ordner | 5h-Balken | 7d-Balken | Context-Balken | Commit | Zeit

input=$(cat)

# Effort: ZUERST aus Stdin (.effort.level) — das ist der LIVE-Session-Wert,
# der von /effort low/medium/high/xhigh aktualisiert wird. settings.json haelt
# nur den Default fuer den Session-Start. Frueher hat die Statusline NUR aus
# settings.json gelesen und deshalb "HIGH" angezeigt obwohl die Session auf
# "xhigh" stand (Frank-Bug-Report 2026-05-09 Abend).
effort=$(echo "$input" | jq -r '.effort.level // empty' 2>/dev/null)
if [ -z "$effort" ]; then
    settings="$HOME/.claude/settings.json"
    if [ -f "$settings" ]; then
        effort=$(jq -r '.effortLevel // "?"' "$settings" 2>/dev/null)
    else
        effort="?"
    fi
fi
effort_upper=$(echo "$effort" | tr '[:lower:]' '[:upper:]')

# JSON-Felder parsen
model=$(echo "$input" | jq -r '.model.display_name // "?"')
cwd_raw=$(echo "$input" | jq -r '.workspace.current_dir // empty' | sed "s|$HOME|~|g")

# Smart-truncate fuer den Ordner-Pfad (Variante B):
# - <= 35 Zeichen: unveraendert
# - > 35 Zeichen: erstes Segment + …/ + letzte 2 Segmente
# - Erstes Segment ist bei Frank meist "~/proggs/<Projekt>" → zwei Segmente am Anfang behalten
shorten_path() {
    local path="$1"
    local max_len=35
    if [ -z "$path" ] || [ ${#path} -le $max_len ]; then
        echo "$path"
        return
    fi
    IFS='/' read -ra segs <<< "$path"
    local n=${#segs[@]}
    if [ "$n" -le 4 ]; then
        echo "$path"
        return
    fi
    # Wenn Pfad mit ~/proggs/ startet: ~/proggs/<Projekt>/…/<vorletztes>/<letztes>
    if [ "${segs[0]}" = "~" ] && [ "${segs[1]}" = "proggs" ] && [ "$n" -ge 5 ]; then
        echo "~/proggs/${segs[2]}/…/${segs[n-2]}/${segs[n-1]}"
    else
        # Generisch: erstes Segment + … + letzte 2
        echo "${segs[0]}/${segs[1]}/…/${segs[n-2]}/${segs[n-1]}"
    fi
}
cwd=$(shorten_path "$cwd_raw")
ctx_remaining=$(echo "$input" | jq -r '.context_window.remaining_percentage // empty')
five_h_used=$(echo "$input"   | jq -r '.rate_limits.five_hour.used_percentage // empty')
five_h_resets=$(echo "$input" | jq -r '.rate_limits.five_hour.resets_at // empty')
week_used=$(echo "$input"     | jq -r '.rate_limits.seven_day.used_percentage // empty')

# Prozent runden
[ -n "$five_h_used" ] && five_h_used=$(printf "%.0f" "$five_h_used" 2>/dev/null)
[ -n "$week_used" ]   && week_used=$(printf "%.0f"   "$week_used"   2>/dev/null)
[ -n "$ctx_remaining" ] && ctx_remaining=$(printf "%.0f" "$ctx_remaining" 2>/dev/null)

# Context-VERBRAUCH (= 100 - remaining)
ctx_used=""
if [ -n "$ctx_remaining" ]; then
    ctx_used=$((100 - ctx_remaining))
fi

# 5h Reset-Countdown
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

# Uhrzeit
time=$(date +%H:%M)

# --- Farben (ANSI 24-bit) ---
B='\033[38;2;100;180;255m'   # Cyan-Blau    — Modell
P='\033[38;2;30;144;255m'    # Blau         — Ordner
GREEN='\033[38;2;64;200;90m' # Gruen        — < 50%
YELLOW='\033[38;2;255;190;40m' # Gelb       — 50-79%
RED='\033[38;2;240;70;70m'   # Rot          — >= 80%
M='\033[38;2;180;130;255m'   # Lila         — Context
T='\033[38;2;130;135;160m'   # Grau         — Commit, Modell-Name
TIMECOL='\033[38;2;220;180;100m' # Amber    — Uhrzeit
DIM='\033[38;2;90;95;115m'   # Dunkelgrau   — Trennzeichen
R='\033[0m'                  # Reset
BOLD='\033[1m'

# Farbe nach Prozent (gruen <50, gelb 50-79, rot >=80)
pct_color() {
    local pct=$1
    if [ -z "$pct" ] || ! [[ "$pct" =~ ^[0-9]+$ ]]; then
        echo -n "$T"
    elif [ "$pct" -ge 80 ]; then
        echo -n "$RED"
    elif [ "$pct" -ge 50 ]; then
        echo -n "$YELLOW"
    else
        echo -n "$GREEN"
    fi
}

# Track-Farbe fuer leere Balken-Segmente (sehr dunkles Grau, hoher Kontrast)
TRACK='\033[38;2;55;58;75m'

# Fortschrittsbalken mit Unicode-Vollbloecken (10 Segmente, hoher Kontrast)
# Gefuellter Teil: █ in Limit-Farbe — leerer Teil: ░ in TRACK-Grau
make_bar() {
    local pct=$1
    local col=$2
    if [ -z "$pct" ] || ! [[ "$pct" =~ ^[0-9]+$ ]]; then
        pct=0
    fi
    [ "$pct" -gt 100 ] && pct=100
    local filled=$((pct / 10))
    local empty=$((10 - filled))
    local fpart=""
    local epart=""
    local i=0
    while [ "$i" -lt "$filled" ]; do fpart="${fpart}█"; i=$((i+1)); done
    i=0
    while [ "$i" -lt "$empty" ];  do epart="${epart}░"; i=$((i+1)); done
    printf "${col}${fpart}${TRACK}${epart}${R}"
}

# Trennzeichen
SEP="${DIM} │ ${R}"

# Icons (Nerd-Font; Fallback auf Emoji wenn nicht gerendert)
# Wenn keine Nerd Font: emoji greift
ICON_MODEL="🤖"
ICON_EFFORT="⚡"
ICON_DIR="📁"
ICON_5H="⏱"
ICON_7D="📅"
ICON_CTX="🧠"
ICON_TIME="🕐"

# Effort-Farbe nach Level
case "$effort" in
    xhigh)  EFFORT_COL='\033[38;2;255;90;220m' ;;   # Magenta — extra-hoch
    high)   EFFORT_COL='\033[38;2;255;180;30m' ;;   # Gold
    medium) EFFORT_COL='\033[38;2;100;180;255m' ;;  # Cyan
    low)    EFFORT_COL='\033[38;2;130;135;160m' ;;  # Grau
    *)      EFFORT_COL='\033[38;2;130;135;160m' ;;
esac

# --- Ausgabe ---

# 1. Modell
printf "${B}${ICON_MODEL} ${BOLD}${model}${R}"

# 2. Effort
printf "${SEP}${EFFORT_COL}${ICON_EFFORT} ${effort_upper}${R}"

# 3. Ordner
[ -n "$cwd" ] && printf "${SEP}${P}${ICON_DIR} ${cwd}${R}"

# 3. 5h-Limit mit Balken
EMPTY_BAR="${TRACK}░░░░░░░░░░${R}"
if [ -n "$five_h_used" ]; then
    col=$(pct_color "$five_h_used")
    bar=$(make_bar "$five_h_used" "$col")
    if [ -n "$five_h_countdown" ]; then
        printf "${SEP}${col}${ICON_5H} 5h${R} ${bar} ${col}${five_h_used}%%${R} ${DIM}(${five_h_countdown})${R}"
    else
        printf "${SEP}${col}${ICON_5H} 5h${R} ${bar} ${col}${five_h_used}%%${R}"
    fi
else
    printf "${SEP}${DIM}${ICON_5H} 5h${R} ${EMPTY_BAR} ${DIM}--${R}"
fi

# 4. 7d-Limit mit Balken
if [ -n "$week_used" ]; then
    col=$(pct_color "$week_used")
    bar=$(make_bar "$week_used" "$col")
    printf "${SEP}${col}${ICON_7D} 7d${R} ${bar} ${col}${week_used}%%${R}"
else
    printf "${SEP}${DIM}${ICON_7D} 7d${R} ${EMPTY_BAR} ${DIM}--${R}"
fi

# 5. Context-Verbrauch mit Balken
if [ -n "$ctx_used" ]; then
    col=$(pct_color "$ctx_used")
    bar=$(make_bar "$ctx_used" "$col")
    printf "${SEP}${col}${ICON_CTX} ctx${R} ${bar} ${col}${ctx_used}%%${R}"
fi

# 6. Uhrzeit
printf "${SEP}${TIMECOL}${ICON_TIME} ${time}${R}"
echo
