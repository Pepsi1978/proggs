#!/usr/bin/env bash
# statusline.sh — Schoene Statusline mit Icons + Fortschrittsbalken
# Reihenfolge: Modell | Ordner | 5h-Balken | 7d-Balken | Context-Balken | Commit | Zeit

input=$(cat)

# Effort: ZUERST aus Stdin (.effort.level) — das ist der LIVE-Session-Wert,
# der von /effort low/medium/high/xhigh aktualisiert wird. settings.json haelt
# nur den Default fuer den Session-Start. Frueher hat die Statusline NUR aus
# settings.json gelesen und deshalb "HIGH" angezeigt obwohl die Session auf
# "xhigh" stand (Frank-Bug-Report 2026-05-09 Abend).
# Performance-Optimierung 2026-05-09 22:06: Alle Input-Felder in EINEM jq-Aufruf
# parsen statt 8 separaten Subprocesses (auf Windows extrem teuer). Frueher 4.4s,
# jetzt <1s. Felder werden tab-separiert ausgegeben und mit IFS=tab read gesplittet.
parsed=$(echo "$input" | jq -r '[
    .effort.level // "",
    .model.display_name // .model.id // "?",
    .workspace.current_dir // "",
    .context_window.remaining_percentage // "",
    .rate_limits.five_hour.used_percentage // "",
    .rate_limits.five_hour.resets_at // "",
    .rate_limits.seven_day.used_percentage // "",
    .session_id // "unknown"
] | @tsv' 2>/dev/null)
IFS=$'\t' read -r effort model cwd_raw ctx_remaining five_h_used_raw five_h_resets_raw week_used_raw session_id <<< "$parsed"

if [ -z "$effort" ]; then
    settings="$HOME/.claude/settings.json"
    if [ -f "$settings" ]; then
        effort=$(jq -r '.effortLevel // "?"' "$settings" 2>/dev/null)
    else
        effort="?"
    fi
fi
effort_upper="${effort^^}"

# Home-Pfad zu ~ kuerzen ohne Subprocess
case "$cwd_raw" in
    "$HOME"*) cwd_raw="~${cwd_raw#$HOME}" ;;
esac

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
# Hinweis: ctx_remaining, five_h_used_raw, five_h_resets_raw, week_used_raw,
# session_id wurden bereits oben in einem einzigen jq-Aufruf geparst.

# Cross-Session-State-Sharing fuer rate_limits (Frank-Bug-Report 2026-05-09 Abend):
# Jede Claude-Code-Session sieht nur ihre EIGENE letzte API-Antwort der rate_limits.
# Eine idle Session zeigt deshalb veraltete Werte. Frank arbeitet mit 4-5 parallelen
# Sessions — Loesung: alle Sessions schreiben in ~/.claude/state/. Beim Anzeigen wird
# der HOECHSTE Wert ueber alle Sessions genommen (rate_limits zaehlen im Fenster nur
# hoch). Beim Reset (resets_at aendert sich) wird MAX nur innerhalb des aktuellsten
# resets_at-Fensters gemacht.
#
# PERFORMANCE-KRITISCH (Frank-Bug-Report 2026-05-09 22:04): Vorherige Version mit
# Bash-Schleifen + 20+ jq-Aufrufen brauchte 8.4s — Claude Code Statusline-Timeout
# liegt bei ~3-5s, deshalb wurde die Statusline gar nicht mehr angezeigt. Diese
# Version: 1 cat (Schreiben), 1 find (Cleanup), 1 jq-Aufruf (Lesen+MAX-Berechnung).
state_dir="$HOME/.claude/state"
[ -d "$state_dir" ] || mkdir -p "$state_dir" 2>/dev/null
my_state="$state_dir/rate-limits-$session_id.json"
printf -v now_ts '%(%s)T' -1

# 1. Aktuellen Input fuer DIESE Session schreiben — printf statt cat heredoc
#    (printf ist Bash-builtin, kein Subprocess).
if [ -n "$five_h_used_raw" ] && [ "$five_h_used_raw" != "null" ]; then
    printf '{"ts_seen":%s,"session_id":"%s","five_h":%s,"five_h_resets":%s,"seven_d":%s}\n' \
        "$now_ts" "$session_id" "$five_h_used_raw" "${five_h_resets_raw:-0}" "${week_used_raw:-0}" \
        > "$my_state" 2>/dev/null
fi

# 2. Cleanup: State-Files aelter als 24h. Nur ~jeder 600. Aufruf (also ca. alle
#    10 Minuten bei refreshInterval=1) — find ist teuer auf Windows Git Bash und
#    der Cleanup ist nicht zeitkritisch.
if [ $((now_ts % 600)) -lt 2 ]; then
    find "$state_dir" -name "rate-limits-*.json" -mmin +1440 -delete 2>/dev/null
fi

# 3. MAX-Logik in EINEM jq-Aufruf — slurp alle State-Files, finde aktuellsten
#    resets_at, dann hoechsten five_h darin, dann hoechsten seven_d global.
fresh=$(jq -sr '
    if length == 0 then ""
    else
        (map(.five_h_resets // 0) | max) as $maxR |
        (map(select(.five_h_resets == $maxR)) | max_by(.five_h // 0)) as $bestF |
        (map(.seven_d // 0) | max) as $bestS |
        "\($bestF.five_h // 0)|\($bestF.five_h_resets // 0)|\($bestS)|\($bestF.session_id // "")"
    end
' "$state_dir"/rate-limits-*.json 2>/dev/null)

if [ -n "$fresh" ]; then
    fresh_five="${fresh%%|*}"
    rest="${fresh#*|}"
    fresh_resets="${rest%%|*}"
    rest="${rest#*|}"
    fresh_seven="${rest%%|*}"
    fresh_session="${rest#*|}"
fi

# 4. Wenn Cross-Session-Werte vorhanden sind: diese verwenden statt Input.
from_other_session=""
if [ -n "$fresh_five" ] && [ "$fresh_five" != "null" ] && [ "$fresh_five" != "0" ]; then
    five_h_used="$fresh_five"
    week_used="$fresh_seven"
    five_h_resets="$fresh_resets"
    if [ "$fresh_session" != "$session_id" ] && [ -n "$fresh_session" ]; then
        from_other_session="1"
    fi
else
    five_h_used="$five_h_used_raw"
    week_used="$week_used_raw"
    five_h_resets="$five_h_resets_raw"
fi

# Prozent runden
[ -n "$five_h_used" ] && five_h_used=$(printf "%.0f" "$five_h_used" 2>/dev/null)
[ -n "$week_used" ]   && week_used=$(printf "%.0f"   "$week_used"   2>/dev/null)
[ -n "$ctx_remaining" ] && ctx_remaining=$(printf "%.0f" "$ctx_remaining" 2>/dev/null)

# Context-VERBRAUCH (= 100 - remaining)
ctx_used=""
if [ -n "$ctx_remaining" ]; then
    ctx_used=$((100 - ctx_remaining))
fi

# 5h Reset-Countdown — mit Plausibilitaetspruefung:
# Realistische Werte sind 0..18000 Sekunden (5h). Wenn der Server mal einen
# kaputten Timestamp liefert (z.B. weit in der Zukunft), zeigen wir lieber
# nichts statt absurde "2.000.000h"-Countdowns.
five_h_countdown=""
if [ -n "$five_h_resets" ] && [ "$five_h_resets" -gt 0 ] 2>/dev/null; then
    diff=$((five_h_resets - now_ts))
    # 21600 = 6h Toleranz oberhalb der nominellen 5h-Fenstergrenze
    if [ "$diff" -gt 0 ] && [ "$diff" -le 21600 ]; then
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
printf -v time '%(%H:%M)T' -1

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
    # Round half up: 35% -> 4 Bloecke, 34% -> 3 Bloecke. Konsistent zur ps1-Variante.
    # Vorher: pct/10 (truncate) — sh und ps1 zeigten unterschiedlich viele Bloecke.
    local filled=$(( (pct + 5) / 10 ))
    [ "$filled" -gt 10 ] && filled=10
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
