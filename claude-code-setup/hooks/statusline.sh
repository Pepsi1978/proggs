#!/usr/bin/env bash
# statusline.sh — Schoene Statusline mit Icons + Fortschrittsbalken
# Zweizeilig (Frank 2026-05-25):
#   Zeile 1: Modell | Effort | Ordner | Zeit
#   Zeile 2: 5h-Balken | 5h-Pacing | 7d-Balken | 7d-Pacing | Context

input=$(cat)

# Effort: ZUERST aus Stdin (.effort.level) — das ist der LIVE-Session-Wert,
# der von /effort low/medium/high/xhigh aktualisiert wird. settings.json haelt
# nur den Default fuer den Session-Start. Frueher hat die Statusline NUR aus
# settings.json gelesen und deshalb "HIGH" angezeigt obwohl die Session auf
# "xhigh" stand (Frank-Bug-Report 2026-05-09 Abend).
# Performance-Optimierung 2026-05-09 22:06: Alle Input-Felder in EINEM jq-Aufruf
# parsen statt 8 separaten Subprocesses (auf Windows extrem teuer). Frueher 4.4s,
# jetzt <1s. Felder werden tab-separiert ausgegeben und mit IFS=tab read gesplittet.
# Trenner ist "\x1f" (ASCII Unit Separator) — nicht-whitespace, kommt in keinem
# realen Wert vor. Frueher Tab via IFS=$'\t': bash read faltet aufeinanderfolgende
# Whitespace-IFS-Zeichen zu EINEM Trenner, was bei leeren rate_limits-Feldern alle
# folgenden Felder verschoben hat. session_id landete dann in five_h_used,
# transcript_path war leer, fresh-session-Erkennung schlug fehl.
parsed=$(echo "$input" | jq -r '[
    .effort.level // "",
    .model.display_name // .model.id // "?",
    .workspace.current_dir // "",
    .context_window.remaining_percentage // "",
    .rate_limits.five_hour.used_percentage // "",
    .rate_limits.five_hour.resets_at // "",
    .rate_limits.seven_day.used_percentage // "",
    .rate_limits.seven_day.resets_at // "",
    .session_id // "unknown",
    .transcript_path // ""
] | join("")' 2>/dev/null)
IFS=$'\x1f' read -r effort model cwd_raw ctx_remaining five_h_used_raw five_h_resets_raw week_used_raw week_resets_raw session_id transcript_path <<< "$parsed"

if [ -z "$effort" ]; then
    settings="$HOME/.claude/settings.json"
    if [ -f "$settings" ]; then
        effort=$(jq -r '.effortLevel // "?"' "$settings" 2>/dev/null)
    else
        effort="?"
    fi
fi
effort_upper="${effort^^}"

# Modellname kuerzen: "(1M context)" -> "(1M)" — spart Platz in der Leiste (Frank 2026-05-24)
model="${model/ context)/)}"
model="${model/ Context)/)}"
model="${model/ Kontext)/)}"

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
#
# ROBUSTHEITS-FIX (Frank-Bug-Report 2026-05-09 22:24): Statusline zeigte 1778368200%
# bei 5h und 309% bei 7d. Root Cause: Eine State-Datei rate-limits-.json (mit LEERER
# session_id) hatte verschobene Felder — five_h enthielt einen Unix-Timestamp,
# seven_d eine UUID. Die MAX-Logik nahm diese Muellwerte ohne Plausibilitaetspruefung.
# Fix nach Direktive 3 (Defense in Depth, Poka-Yoke Stufe 3):
#   1. SCHREIB-GUARDS: keine leere/ungueltige session_id, Werte nur 0..100,
#      plausibler Reset-Timestamp
#   2. LESE-VALIDIERUNG: kaputte Files automatisch entfernt, jq filtert
#      ungueltige Eintraege beim MAX-Pass
#   3. ATOMIC WRITE: tmp + mv verhindert Half-Read
state_dir="$HOME/.claude/state"
[ -d "$state_dir" ] || mkdir -p "$state_dir" 2>/dev/null
printf -v now_ts '%(%s)T' -1

# Defekte Datei mit leerer session_id IMMER entfernen (Self-Healing, Schicht 2)
[ -f "$state_dir/rate-limits-.json" ] && rm -f "$state_dir/rate-limits-.json" 2>/dev/null

# Account-Fingerprint (Frank-Bug-Report 2026-05-10): Nach Account-Wechsel
# (Logout/Login mit anderem Account) zeigte die Statusline noch die rate_limits
# vom alten Account, weil alte State-Files mit hohen Werten ueberlebten und im
# MAX-Pass gewonnen haben. Fix: Hash der credentials.json als Fingerprint in
# jedes State-File schreiben. Beim Lesen werden nur Files mit dem AKTUELLEN
# Fingerprint beruecksichtigt — alte Account-Files werden ignoriert (und beim
# naechsten Cleanup geloescht). Wenn kein sha256sum verfuegbar oder Credentials
# fehlen: Fingerprint = "default" (Verhalten wie vorher, kein Regression).
account_fp="default"
cred_file="$HOME/.claude/.credentials.json"
if [ -f "$cred_file" ]; then
    # Plattformuebergreifend: sha256sum (Linux/Git-Bash) ODER shasum -a 256 (macOS).
    # Ohne Fallback wuerde der Fingerprint auf macOS immer "default" sein (Frank 2026-05-24).
    if command -v sha256sum >/dev/null 2>&1; then
        account_fp=$(sha256sum "$cred_file" 2>/dev/null | cut -c1-16)
    elif command -v shasum >/dev/null 2>&1; then
        account_fp=$(shasum -a 256 "$cred_file" 2>/dev/null | cut -c1-16)
    fi
    [ -z "$account_fp" ] && account_fp="default"
fi

# Plausibilitaet: Prozent muss 0..100 sein (nur Ziffern + optional . — keine UUIDs)
is_valid_pct() {
    local v="$1"
    { [ -z "$v" ] || [ "$v" = "null" ]; } && return 1
    case "$v" in
        ''|*[!0-9.]*) return 1 ;;
    esac
    local i="${v%.*}"
    [ -z "$i" ] && return 1
    [ "$i" -ge 0 ] 2>/dev/null && [ "$i" -le 100 ] 2>/dev/null
}

# Reset-Timestamp plausibel? (zwischen now-1h und now+6h, oder 0/leer = nicht gesetzt)
is_valid_reset() {
    local ts="$1"
    { [ -z "$ts" ] || [ "$ts" = "null" ] || [ "$ts" = "0" ]; } && return 0
    case "$ts" in
        ''|*[!0-9]*) return 1 ;;
    esac
    [ "$ts" -ge "$((now_ts - 3600))" ] 2>/dev/null && [ "$ts" -le "$((now_ts + 21600))" ] 2>/dev/null
}

# Reset-Timestamp fuer 7d plausibel? (zwischen now-1h und now+8 Tagen, oder 0/leer = nicht gesetzt)
# Eigene Funktion weil das 7d-Fenster bis zu 7 Tage in der Zukunft liegt (is_valid_reset deckt nur 6h ab).
is_valid_reset_7d() {
    local ts="$1"
    { [ -z "$ts" ] || [ "$ts" = "null" ] || [ "$ts" = "0" ]; } && return 1
    case "$ts" in
        ''|*[!0-9]*) return 1 ;;
    esac
    [ "$ts" -ge "$((now_ts - 3600))" ] 2>/dev/null && [ "$ts" -le "$((now_ts + 691200))" ] 2>/dev/null
}

# Session-ID-Format: nur a-z A-Z 0-9 _ - erlaubt — verhindert leere/Mull-IDs
is_valid_sid() {
    local sid="$1"
    { [ -z "$sid" ] || [ "$sid" = "unknown" ]; } && return 1
    case "$sid" in
        *[!a-zA-Z0-9_-]*) return 1 ;;
    esac
    return 0
}

# 1. SCHREIBEN — nur wenn ALLE Werte plausibel sind (Schicht 1: Praeventiv).
#    NIEMALS bei leerer/ungueltiger session_id schreiben (kein rate-limits-.json mehr).
my_state="$state_dir/rate-limits-$session_id.json"
if is_valid_sid "$session_id" \
   && is_valid_pct "$five_h_used_raw" \
   && { [ -z "$week_used_raw" ] || [ "$week_used_raw" = "null" ] || is_valid_pct "$week_used_raw"; } \
   && is_valid_reset "${five_h_resets_raw:-0}"; then
    # 7d-Wert: Wenn API in dieser Antwort einen gueltigen Wert liefert -> nehmen.
    # Wenn API keinen Wert liefert (leer/null), den existierenden 7d-Wert aus dem
    # eigenen State-File behalten — sonst wuerde ein API-Call ohne seven_day-Feld
    # den 7d-Wert auf 0 zuruecksetzen, was bei MAX-Aggregation den echten hoeheren
    # Wert anderer Sessions plattmachen koennte (Frank-Bug-Report 2026-05-10 abend).
    if [ -n "$week_used_raw" ] && [ "$week_used_raw" != "null" ]; then
        seven_d_safe="$week_used_raw"
    elif [ -f "$my_state" ]; then
        seven_d_safe=$(jq -r '.seven_d // 0' "$my_state" 2>/dev/null)
        [ -z "$seven_d_safe" ] || [ "$seven_d_safe" = "null" ] && seven_d_safe="0"
    else
        seven_d_safe="0"
    fi
    resets_safe="${five_h_resets_raw:-0}"
    [ "$resets_safe" = "null" ] && resets_safe="0"
    # 7d-Reset: nur uebernehmen wenn plausibel. Wenn nicht (z.B. leer/null), den
    # existierenden Wert aus dem eigenen State behalten — der Wochen-Reset ist ueber
    # die ganze Woche konstant, geht also nicht verloren wenn ein API-Call ihn mal weglaesst.
    week_resets_safe="${week_resets_raw:-0}"
    [ "$week_resets_safe" = "null" ] && week_resets_safe="0"
    if ! is_valid_reset_7d "$week_resets_safe"; then
        if [ -f "$my_state" ]; then
            week_resets_safe=$(jq -r '.seven_d_resets // 0' "$my_state" 2>/dev/null)
            { [ -z "$week_resets_safe" ] || [ "$week_resets_safe" = "null" ]; } && week_resets_safe="0"
        else
            week_resets_safe="0"
        fi
    fi
    # Atomic write: tmp + mv (Schicht 3: Eliminierung von Half-Read)
    tmp_state="$my_state.tmp"
    printf '{"ts_seen":%s,"session_id":"%s","account_fp":"%s","five_h":%s,"five_h_resets":%s,"seven_d":%s,"seven_d_resets":%s}\n' \
        "$now_ts" "$session_id" "$account_fp" "$five_h_used_raw" "$resets_safe" "$seven_d_safe" "$week_resets_safe" \
        > "$tmp_state" 2>/dev/null \
        && mv -f "$tmp_state" "$my_state" 2>/dev/null
fi

# 2. Cleanup: State-Files aelter als 24h. Nur ~jeder 600. Aufruf (also ca. alle
#    10 Minuten bei refreshInterval=1) — find ist teuer auf Windows Git Bash und
#    der Cleanup ist nicht zeitkritisch.
if [ $((now_ts % 600)) -lt 2 ]; then
    find "$state_dir" -name "rate-limits-*.json" -mmin +1440 -delete 2>/dev/null
    find "$state_dir" -name "ctx-*" -mmin +1440 -delete 2>/dev/null
    # Zusaetzlich: Files von fremden Accounts (anderer Fingerprint) sofort weg —
    # nicht erst nach 24h. Sonst zeigt nach Account-Wechsel die alte Anzeige.
    if [ "$account_fp" != "default" ]; then
        for f in "$state_dir"/rate-limits-*.json; do
            [ -f "$f" ] || continue
            file_fp=$(jq -r '.account_fp // ""' "$f" 2>/dev/null)
            if [ -n "$file_fp" ] && [ "$file_fp" != "$account_fp" ]; then
                rm -f "$f" 2>/dev/null
            fi
        done
    fi
fi

# 3. MAX-Logik in EINEM jq-Aufruf mit VALIDIERUNG — slurp alle State-Files,
#    filter kaputte Eintraege (session_id leer/ungueltig, Werte nicht 0..100,
#    Account-Fingerprint passt nicht zum aktuellen Account),
#    dann finde aktuellsten resets_at und hoechsten five_h/seven_d darin.
#    Schicht 2 (Reaktiv): falls trotz Schreib-Guards Mullwerte reinrutschen
#    werden sie hier verworfen statt blind angezeigt zu werden.
#    Account-Filter: wenn account_fp im File leer ist (alte Files vor dem Fix),
#    wird er akzeptiert — sonst muss er exakt zum aktuellen passen.
fresh=$(jq -sr --arg fp "$account_fp" '
    map(select(
        (.session_id // "" | tostring) != ""
        and (.session_id | tostring | test("^[A-Za-z0-9_-]+$"))
        and (.five_h | type == "number") and .five_h >= 0 and .five_h <= 100
        and ((.seven_d == null) or ((.seven_d | type == "number") and .seven_d >= 0 and .seven_d <= 100))
        and ((.account_fp // "") == "" or (.account_fp // "") == $fp)
    )) as $valid |
    if ($valid | length) == 0 then ""
    else
        ($valid | map(.five_h_resets // 0) | max) as $maxR |
        ($valid | map(select((.five_h_resets // 0) == $maxR)) | max_by(.five_h // 0)) as $bestF |
        ($valid | map(.seven_d // 0) | max) as $bestS |
        ($valid | map(.seven_d_resets // 0) | max) as $maxSR |
        "\($bestF.five_h // 0)|\($bestF.five_h_resets // 0)|\($bestS)|\($bestF.session_id // "")|\($maxSR)"
    end
' "$state_dir"/rate-limits-*.json 2>/dev/null)

if [ -n "$fresh" ]; then
    fresh_five="${fresh%%|*}"
    rest="${fresh#*|}"
    fresh_resets="${rest%%|*}"
    rest="${rest#*|}"
    fresh_seven="${rest%%|*}"
    rest="${rest#*|}"
    fresh_session="${rest%%|*}"
    fresh_week_resets="${rest#*|}"
fi

# 4. Wenn Cross-Session-Werte vorhanden sind: diese verwenden statt Input.
from_other_session=""
if [ -n "$fresh_five" ] && [ "$fresh_five" != "null" ] && [ "$fresh_five" != "0" ]; then
    five_h_used="$fresh_five"
    week_used="$fresh_seven"
    five_h_resets="$fresh_resets"
    week_resets="$fresh_week_resets"
    # Fallback: wenn kein gueltiger 7d-Reset aggregiert wurde, den aus dem aktuellen Input nehmen
    if [ -z "$week_resets" ] || [ "$week_resets" = "0" ]; then week_resets="${week_resets_raw:-0}"; fi
    if [ "$fresh_session" != "$session_id" ] && [ -n "$fresh_session" ]; then
        from_other_session="1"
    fi
else
    five_h_used="$five_h_used_raw"
    week_used="$week_used_raw"
    five_h_resets="$five_h_resets_raw"
    week_resets="$week_resets_raw"
fi

# Prozent runden
[ -n "$five_h_used" ] && five_h_used=$(printf "%.0f" "$five_h_used" 2>/dev/null)
[ -n "$week_used" ]   && week_used=$(printf "%.0f"   "$week_used"   2>/dev/null)
[ -n "$ctx_remaining" ] && ctx_remaining=$(printf "%.0f" "$ctx_remaining" 2>/dev/null)

# Letzter Schutzwall vor der Anzeige (Schicht 2: Reaktiv) — falls trotz aller
# Validierung ein Mullwert durchschluepft, lieber 100% als 1.778.368.200% zeigen.
clamp_pct() {
    local v="$1"
    [ -z "$v" ] && { echo ""; return; }
    case "$v" in
        ''|*[!0-9-]*) echo ""; return ;;
    esac
    [ "$v" -gt 100 ] 2>/dev/null && v=100
    [ "$v" -lt 0   ] 2>/dev/null && v=0
    echo "$v"
}
five_h_used=$(clamp_pct "$five_h_used")
week_used=$(clamp_pct "$week_used")

# Context-VERBRAUCH (= 100 - remaining)
# Frank-Bug-Report 2026-05-10 abend: Nach /clear oder Session-Start zeigt Claude
# Code in stdin noch den ALTEN context_window.remaining_percentage aus der letzten
# API-Antwort (z.B. 99% obwohl gerade gecleart). Erst nach dem ersten neuen API-
# Call wird der Wert aktualisiert. Fix: Wenn das Transcript-File sehr klein ist
# (frische Session), ueberschreiben wir ctx_used mit 0 — sobald Claude Code echte
# Werte liefert, uebernehmen wir die. Schwelle 8 Zeilen = grob 1-2 Turns.
ctx_used=""
if [ -n "$ctx_remaining" ]; then
    ctx_used=$((100 - ctx_remaining))
fi
# Fallback: wenn transcript_path leer ist, aus session_id + cwd_raw rekonstruieren.
# Convention: ~/.claude/projects/<encoded-dir>/<session_id>.jsonl
# encoded-dir: jedes Nicht-Alphanum durch "-" ersetzen.
if [ -z "$transcript_path" ] && [ -n "$session_id" ] && [ "$session_id" != "unknown" ]; then
    cwd_for_path="${cwd_raw/#\~/$HOME}"
    encoded_dir=$(echo "$cwd_for_path" | sed 's/[^A-Za-z0-9]/-/g')
    candidate="$HOME/.claude/projects/$encoded_dir/$session_id.jsonl"
    [ -f "$candidate" ] && transcript_path="$candidate"
fi
if [ -n "$transcript_path" ] && [ -f "$transcript_path" ]; then
    transcript_lines=$(wc -l < "$transcript_path" 2>/dev/null || echo 0)
    if [ "$transcript_lines" -lt 8 ] 2>/dev/null; then
        ctx_used=0
    fi
fi

# CTX-Verbrauch fuer den session-backup Stop-Hook exportieren (Frank 2026-05-24).
# Der Stop-Hook bekommt context_window NICHT im stdin — er liest hier den EXAKTEN
# Wert (genau das was die Statusline anzeigt) aus einer PRO-SESSION-Datei. Pro Session
# weil Frank 4-5 parallele Sessions faehrt; eine gemeinsame Datei wuerde sich gegenseitig
# ueberschreiben und der Hook laese den Kontext einer fremden Session. Atomic write.
if [ -n "$ctx_used" ] && is_valid_sid "$session_id"; then
    # Export-Wert gegen Mullwerte absichern (gleiche Fehlerklasse wie die rate_limit-Mullwerte
    # weiter oben): auf 0..100 clampen, damit der Stop-Hook EXAKT den angezeigten Wert liest
    # (make_bar zeigt out-of-range ebenfalls geclampt). Normalwerte 0..100 bleiben unveraendert.
    ctx_out="$ctx_used"
    if [ "$ctx_out" -lt 0 ] 2>/dev/null; then ctx_out=0; fi
    if [ "$ctx_out" -gt 100 ] 2>/dev/null; then ctx_out=100; fi
    ctx_file="$state_dir/ctx-$session_id"
    printf '%s\n' "$ctx_out" > "$ctx_file.tmp" 2>/dev/null && mv -f "$ctx_file.tmp" "$ctx_file" 2>/dev/null
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

# 7d Reset-Countdown — Tage + Stunden (Frank 2026-05-25). Format: "3D10H" bzw. nur
# "10H" wenn weniger als ein Tag verbleibt. Wird in derselben grauen Farbe (DIM) in
# Klammern hinter den 7d-Balken gesetzt, analog zum 5h-Countdown. Plausibel: 0..8
# Tage (7d-Fenster + 1 Tag Toleranz), damit kaputte Timestamps keinen Absurd-Countdown zeigen.
week_countdown=""
if [ -n "$week_resets" ] && [ "$week_resets" -gt 0 ] 2>/dev/null; then
    wdiff=$((week_resets - now_ts))
    if [ "$wdiff" -gt 0 ] && [ "$wdiff" -le 691200 ]; then
        wdays=$((wdiff / 86400))
        whours=$(( (wdiff % 86400) / 3600 ))
        if [ "$wdays" -ge 1 ]; then
            week_countdown="${wdays}D${whours}H"
        else
            week_countdown="${whours}H"
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
PACE='\033[38;2;45;212;191m' # Teal         — Pacing-Feature 5h (Symbol + slow/fast)
PACE7='\033[38;2;255;120;180m' # Pink/Rosa  — Pacing-Feature 7d (Symbol + slow/fast)
MID='\033[1m\033[38;2;240;240;250m' # Hellweiss fett — Pacing-Ziellinie (Mittelstrich)
T='\033[38;2;130;135;160m'   # Grau         — Commit, Modell-Name
TIMECOL='\033[38;2;220;180;100m' # Amber    — Uhrzeit
DIM='\033[38;2;90;95;115m'   # Dunkelgrau   — (alt) Trennzeichen
SEPCOL='\033[38;2;235;70;70m' # Rot         — Bereichs-Trenner (dick)
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
    # 7 Segmente (Frank 2026-05-24, schmaler). Round half up: filled = round(pct*7/100).
    # Prozent-Logik unveraendert, nur Gesamtbreite kleiner. Konsistent zur ps1-Variante.
    local filled=$(( (pct * 7 + 50) / 100 ))
    [ "$filled" -gt 7 ] && filled=7
    local empty=$((7 - filled))
    local fpart=""
    local epart=""
    local i=0
    while [ "$i" -lt "$filled" ]; do fpart="${fpart}█"; i=$((i+1)); done
    i=0
    while [ "$i" -lt "$empty" ];  do epart="${epart}░"; i=$((i+1)); done
    printf "${col}${fpart}${TRACK}${epart}${R}"
}

# Pacing-Pendel (Frank 2026-05-24): Zeigt ob der 5h-Verbrauch dem Zeitverlauf
# voraus (zu schnell) oder hinterher (zu langsam) ist. ┃ = Idealtempo (Verbrauch
# == verstrichene Zeit im 5h-Fenster), ● = Ist-Position. Links vom Strich = langsam
# (Reserve da), rechts = schnell (laeuft vor Fensterende ins Limit). Track 7 Zeichen
# (schmal). Mitte (Index 3) ist IMMER der weisse Strich (Ziellinie). Der Marker sitzt
# IMMER daneben — NIE auf der Mitte, damit Strich UND Marker stets sichtbar sind
# (Frank 2026-05-24). Marker GRUEN links (slow, Reserve) / ROT rechts (fast, ins Limit);
# Abstand vom Strich = Staerke der Abweichung (1 nah, 3 weit).
make_pace_bar() {
    local used=$1
    local resets=$2
    local window=$3        # Fensterlaenge in Sekunden: 18000 (5h) oder 604800 (7d)
    local rem=$((resets - now_ts))
    local elapsed=$((window - rem))
    [ "$elapsed" -lt 0 ] && elapsed=0
    [ "$elapsed" -gt "$window" ] && elapsed=$window
    local ideal=$(( elapsed * 100 / window ))
    local delta=$(( used - ideal ))
    local mid=3
    # Seite + Farbe: delta>0 = fast (rot, rechts), delta<=0 = slow/ideal (gruen, links).
    local mcol adev
    if [ "$delta" -gt 0 ]; then
        mcol="$RED"; adev=$delta
    else
        mcol="$GREEN"; adev=$(( -delta ))
    fi
    # Abstand vom Strich nach Abweichungsstaerke (1=nah, 2=mittel, 3=weit).
    local abstand
    if   [ "$adev" -le 10 ]; then abstand=1
    elif [ "$adev" -le 25 ]; then abstand=2
    else abstand=3; fi
    local mpos
    if [ "$delta" -gt 0 ]; then
        mpos=$(( mid + abstand ))
    else
        mpos=$(( mid - abstand ))
    fi
    local out="" i=0
    while [ "$i" -lt 7 ]; do
        if [ "$i" -eq "$mid" ]; then
            out="${out}${MID}┃${R}"      # Ziellinie: immer weiss, immer sichtbar
        elif [ "$i" -eq "$mpos" ]; then
            out="${out}${mcol}●${R}"     # Marker: gruen (slow) / rot (fast), nie auf der Mitte
        else
            out="${out}${TRACK}─${R}"
        fi
        i=$((i+1))
    done
    printf "%b" "$out"
}

# Trennzeichen — ein dicker roter Strich (Frank 2026-05-24)
SEP="${SEPCOL} ┃ ${R}"
# Gruppen-Abstand (kein Trenner, 1 Leerzeichen): haelt zusammengehoerige Bereiche wie 5h+Pendel eng zusammen
GSEP=" "

# Icons (Nerd-Font; Fallback auf Emoji wenn nicht gerendert)
# Wenn keine Nerd Font: emoji greift
ICON_MODEL="🤖"
ICON_EFFORT="⚡"
ICON_DIR="📁"
ICON_5H="⏱"
ICON_7D="📅"
ICON_CTX="🧠"
ICON_TIME="🕐"
ICON_PACE="🎯"

# Effort-Farbe nach Level
case "$effort" in
    xhigh)  EFFORT_COL='\033[38;2;255;90;220m' ;;   # Magenta — extra-hoch
    high)   EFFORT_COL='\033[38;2;255;180;30m' ;;   # Gold
    medium) EFFORT_COL='\033[38;2;100;180;255m' ;;  # Cyan
    low)    EFFORT_COL='\033[38;2;130;135;160m' ;;  # Grau
    *)      EFFORT_COL='\033[38;2;130;135;160m' ;;
esac

# --- Ausgabe ---
# Reihenfolge (Frank 2026-05-25): zuerst die Limit-Bereiche (5h, 5h-Pacing, 7d,
# 7d-Pacing), DANN Modell, Effort, Ordner, Kontext, Uhrzeit. Die Limit-Bereiche
# wandern nach vorne; Modell/Effort/Ordner/Kontext (in dieser Reihenfolge)
# folgen dahinter. Kontext bleibt direkt hinter dem Ordner wie zuvor.
EMPTY_BAR="${TRACK}░░░░░░░${R}"

# ===== ZEILE 1: Modell | Effort | Ordner | Uhrzeit (Frank 2026-05-25) =====

# 1. Modell (erstes Element von Zeile 1 — KEIN fuehrender Trenner)
printf "${B}${ICON_MODEL} ${BOLD}${model}${R}"

# 2. Effort (direkt hinter dem Modell)
printf "${SEP}${EFFORT_COL}${ICON_EFFORT} ${effort_upper}${R}"

# 3. Ordner
[ -n "$cwd" ] && printf "${SEP}${P}${ICON_DIR} ${cwd}${R}"

# 4. Uhrzeit
printf "${SEP}${TIMECOL}${ICON_TIME} ${time}${R}"

# Zeilenumbruch -> Zeile 2 (jedes echo erzeugt eine eigene Statusline-Zeile, siehe Claude-Code-Doku)
echo

# ===== ZEILE 2: 5h + Pacing | 7d + Pacing | Kontext (Frank 2026-05-25) =====

# 5. 5h-Limit mit Balken (erstes Element von Zeile 2 — KEIN fuehrender Trenner)
if [ -n "$five_h_used" ]; then
    col=$(pct_color "$five_h_used")
    bar=$(make_bar "$five_h_used" "$col")
    if [ -n "$five_h_countdown" ]; then
        printf "${col}${ICON_5H} 5h${R} ${bar} ${col}${five_h_used}%%${R} ${DIM}(${five_h_countdown})${R}"
    else
        printf "${col}${ICON_5H} 5h${R} ${bar} ${col}${five_h_used}%%${R}"
    fi
else
    printf "${DIM}${ICON_5H} 5h${R} ${EMPTY_BAR} ${DIM}--${R}"
fi

# 6. 5h-Pacing-Pendel direkt hinter dem 5h-Balken (Frank 2026-05-24):
#     Nur zeigen wenn Verbrauch UND Reset-Zeitpunkt bekannt sind.
if [ -n "$five_h_used" ] && [ -n "$five_h_resets" ] && [ "$five_h_resets" -gt 0 ] 2>/dev/null; then
    pacebar=$(make_pace_bar "$five_h_used" "$five_h_resets" 18000)
    printf "${GSEP}${PACE}slow${R} ${pacebar} ${PACE}fast${R}"
fi

# 7. 7d-Limit mit Balken (mit Reset-Countdown in Tagen+Stunden — Frank 2026-05-25)
if [ -n "$week_used" ]; then
    col=$(pct_color "$week_used")
    bar=$(make_bar "$week_used" "$col")
    if [ -n "$week_countdown" ]; then
        printf "${SEP}${col}${ICON_7D} 7d${R} ${bar} ${col}${week_used}%%${R} ${DIM}(${week_countdown})${R}"
    else
        printf "${SEP}${col}${ICON_7D} 7d${R} ${bar} ${col}${week_used}%%${R}"
    fi
else
    printf "${SEP}${DIM}${ICON_7D} 7d${R} ${EMPTY_BAR} ${DIM}--${R}"
fi

# 8. 7d-Pacing-Pendel (Frank 2026-05-24): gleiche Logik wie 5h, Fenster 7 Tage
#     (604800s), eigene Feature-Farbe (Pink). Nur wenn Verbrauch UND 7d-Reset bekannt.
if [ -n "$week_used" ] && [ -n "$week_resets" ] && [ "$week_resets" -gt 0 ] 2>/dev/null; then
    pacebar7=$(make_pace_bar "$week_used" "$week_resets" 604800)
    printf "${GSEP}${PACE7}slow${R} ${pacebar7} ${PACE7}fast${R}"
fi

# 9. Context-Verbrauch (am Ende von Zeile 2)
if [ -n "$ctx_used" ]; then
    col=$(pct_color "$ctx_used")
    bar=$(make_bar "$ctx_used" "$col")
    printf "${SEP}${col}${ICON_CTX} ctx${R} ${bar} ${col}${ctx_used}%%${R}"
fi
echo

exit 0
