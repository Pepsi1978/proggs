#!/usr/bin/env bash
# statusline.sh — Schoene Statusline mit Icons + Fortschrittsbalken
# Zweizeilig (Frank 2026-08-12):
#   Zeile 1: Context | Modell | Effort | 5h-Balken | 5h-Pacing | 7d-Balken | 7d-Pacing | Zeit
#   Zeile 2: Arbeitsordner (allein, damit er NIE vom rechten Rand abgeschnitten wird)
#   (Context ganz vorne — Frank 2026-06-07)

input=$(cat)

# jq-Selbstheilung: Die Statusline braucht jq zum Parsen des Session-JSON. Wenn
# jq NICHT im PATH ist (typisch: Claude-Code-Session wurde VOR der jq-Installation
# gestartet — Windows winget legt jq in WinGet\Links, das erst ein neuer Prozess
# im PATH sieht), suchen wir es direkt in den bekannten Installationsorten und
# haengen den Fundort an PATH. Ohne das rendert die Leiste leer ("--" statt
# Modell/Effort/Prozenten), bis Claude Code neu startet. (Frank-Bug 2026-07-08,
# vgl. bugs/claude-tooling/claude-hooks.md §13.2 "jq nicht installiert".)
if ! command -v jq >/dev/null 2>&1; then
    for _d in \
        "$LOCALAPPDATA/Microsoft/WinGet/Links" \
        "$HOME/AppData/Local/Microsoft/WinGet/Links" \
        "$HOME/bin" "$HOME/.local/bin" \
        /opt/homebrew/bin /usr/local/bin /usr/bin; do
        if [ -x "$_d/jq" ] || [ -x "$_d/jq.exe" ]; then
            PATH="$_d:$PATH"
            break
        fi
    done
fi

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
    .transcript_path // "",
    .context_window.used_percentage // ""
] | join("")' 2>/dev/null)
IFS=$'\x1f' read -r effort model cwd_raw ctx_remaining five_h_used_raw five_h_resets_raw week_used_raw week_resets_raw session_id transcript_path ctx_used_pct <<< "$parsed"

if [ -z "$effort" ]; then
    settings="$HOME/.claude/settings.json"
    if [ -f "$settings" ]; then
        effort=$(jq -r '.effortLevel // "?"' "$settings" 2>/dev/null)
    else
        effort="?"
    fi
fi
# POSIX-Uppercase via tr — NICHT ${effort^^} (bash 4): macOS hat bash 3.2,
# dort wirft ${v^^} "bad substitution" → effort_upper blieb leer → Statusline
# zeigte nur das ⚡-Symbol ohne Wert (Frank-Bug-Report 2026-06-11).
effort_upper=$(printf '%s' "$effort" | tr '[:lower:]' '[:upper:]')

# Modellname kuerzen: "(1M context)" -> "(1M)" — spart Platz in der Leiste (Frank 2026-05-24)
model="${model/ context)/)}"
model="${model/ Context)/)}"
model="${model/ Kontext)/)}"

# Windows-Pfad-Normalisierung (Frank-Bug 2026-07-08): cwd kommt auf Windows mit
# BACKSLASHES (C:\Users\barwa\proggs). Der Pfad wird weiter unten in einen printf-
# FORMAT-String interpoliert (`printf "...${cwd}..."`), wo `\U`, `\b`, `\p` als
# Escapes gelten — `\b` ist ein Backspace (0x08), der beim Rendern sogar VORHER
# gedruckte Segmente (Context, Modell) von der Zeile loescht. Symptom: mit
# Backslash-cwd zeigte die Leiste nur Effort/5h/7d/Zeit, Modell war leer, Context
# und Ordner fehlten ganz. Backslashes -> Forward-Slashes macht den Pfad escape-frei
# und zeigt ihn zugleich einheitlich (wie die uebrigen Pfade in der Leiste).
cwd_raw="${cwd_raw//\\//}"

# Home-Pfad zu ~ kuerzen ohne Subprocess
case "$cwd_raw" in
    "$HOME"*) cwd_raw="~${cwd_raw#$HOME}" ;;
esac

# Smart-truncate fuer den Ordner-Pfad (Variante B):
# - <= 35 Zeichen: unveraendert
# - > 35 Zeichen: erstes Segment + …/ + letzte 2 Segmente
# - Erstes Segment ist bei Frank meist "~/proggs/<Projekt>" → zwei Segmente am Anfang behalten
# max_len ist seit 2026-08-12 ein Parameter (Default 35): der Ordner steht jetzt ALLEIN
# in Zeile 2 und hat dort viel Platz — dort wird mit 100 aufgerufen, sodass praktisch
# jeder reale Pfad ungekuerzt erscheint und nur absurd lange Pfade gekuerzt werden.
shorten_path() {
    local path="$1"
    local max_len="${2:-35}"
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
cwd=$(shorten_path "$cwd_raw" 100)
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
# date statt printf '%(%s)T' — letzteres ist bash 4.2+, faellt auf macOS bash 3.2
# still aus (now_ts leer → alle Zeit-/Countdown-Berechnungen kaputt). Frank 2026-06-11.
# Performance (Frank 2026-07-08): now_ts UND Uhrzeit in EINEM date-Aufruf holen statt
# zwei (spart einen Subprozess ~0.3s auf Windows). "|" als Trenner (kommt in %s/%H:%M
# nie vor). Die Uhrzeit wird erst weiter unten fuer die Anzeige gebraucht.
_dt=$(date '+%s|%H:%M')
now_ts=${_dt%%|*}
time=${_dt#*|}

# Defekte Datei mit leerer session_id IMMER entfernen (Self-Healing, Schicht 2)
[ -f "$state_dir/rate-limits-.json" ] && rm -f "$state_dir/rate-limits-.json" 2>/dev/null

# Account-Fingerprint (Frank-Bug-Report 2026-05-10): Nach Account-Wechsel
# (Logout/Login mit anderem Account) zeigte die Statusline noch die rate_limits
# vom alten Account, weil alte State-Files mit hohen Werten ueberlebten und im
# MAX-Pass gewonnen haben. Fix: Fingerprint des AKTUELLEN Accounts in jedes
# State-File schreiben. Beim Lesen werden nur Files mit dem AKTUELLEN Fingerprint
# beruecksichtigt — alte Account-Files werden ignoriert (und beim naechsten Cleanup
# geloescht). Wenn keine Account-Quelle verfuegbar: Fingerprint = "default".
#
# ROOT-CAUSE-FIX (Frank-Bug-Report 2026-06-13): account_fp war auf macOS IMMER
# "default", weil die Credentials dort im Keychain liegen und ~/.claude/.credentials.json
# gar nicht existiert. Die ganze Account-Trennung war damit auf macOS wirkungslos.
# Das fiel beim 7d-Wert auf: nach einem Account-Wechsel teilen altes und neues Konto
# denselben kalendarischen seven_d_resets (Wochenreset zur selben Uhrzeit), sodass die
# Fenster-Trennung den hohen Altwert nicht aussortierte → MAX zeigte 58% statt frischer 2%.
# Fix: Fingerprint BEVORZUGT aus ~/.claude.json (.oauthAccount.accountUuid) — existiert
# auf BEIDEN Plattformen, ist global ueber alle parallelen Sessions konstant und wechselt
# beim Account-Wechsel. Fallback: .credentials.json-Datei-Hash (Windows), dann "default".
account_fp="default"
claude_json="$HOME/.claude.json"
acct_uuid=""
# PERFORMANCE-CACHE (Frank-Bug-Report 2026-07-08): Der account_fp aendert sich fast
# nie (nur bei Account-Wechsel). Ihn bei JEDEM Statusline-Aufruf per jq (~370ms) +
# sha256sum aus ~/.claude.json zu berechnen kostete auf Windows/Git-Bash ~1.5s pro
# Aufruf — zusammen mit den anderen Subprozessen brauchte die Statusline ~5s und wurde
# von Claude Code abgebrochen (in-flight execution cancelled), sobald der naechste
# refreshInterval-Trigger (1s) kam -> Leiste erschien in neuen Sessions NIE.
# Fix: fp cachen mit ZEIT-basiertem TTL (10 Min), NICHT ueber ~/.claude.json-mtime:
# ~/.claude.json wird von Claude Code bei fast jeder Aktion neu geschrieben (mtime
# aendert sich im Sekundentakt — verifiziert 2026-07-08), ein mtime-Check wuerde den
# Cache also NIE greifen lassen. Der accountUuid darin ist aber quasi-konstant. TTL 600s:
# ein Account-Wechsel wird spaetestens nach 10 Min uebernommen (selten; der MAX-jq unten
# filtert fremde Fingerprints ohnehin heraus). $(< file) liest ohne cat-Subprozess.
# Cache-Format: "fp|ts_seconds".
fp_cache="$state_dir/account-fp.cache"
if [ -s "$fp_cache" ]; then
    _c=$(< "$fp_cache")
    _cfp=${_c%%|*}
    _cts=${_c#*|}
    if [ -n "$_cts" ] && [ "$_cts" != "$_c" ] && [ "$((now_ts - _cts))" -lt 600 ] 2>/dev/null; then
        account_fp="$_cfp"
        [ -z "$account_fp" ] && account_fp="default"
        _fp_from_cache=1
    fi
fi
[ -f "$claude_json" ] && [ -z "$_fp_from_cache" ] && acct_uuid=$(jq -r '.oauthAccount.accountUuid // .userID // empty' "$claude_json" 2>/dev/null)
if [ -n "$_fp_from_cache" ]; then
    : # account_fp bereits aus Cache — keine Berechnung noetig
elif [ -n "$acct_uuid" ]; then
    # accountUuid hashen (nie roh ins State-File schreiben). sha256sum (Linux/Git-Bash)
    # ODER shasum -a 256 (macOS) — ohne Fallback waere der fp auf macOS leer.
    if command -v sha256sum >/dev/null 2>&1; then
        account_fp=$(printf '%s' "$acct_uuid" | sha256sum 2>/dev/null | cut -c1-16)
    elif command -v shasum >/dev/null 2>&1; then
        account_fp=$(printf '%s' "$acct_uuid" | shasum -a 256 2>/dev/null | cut -c1-16)
    fi
    [ -z "$account_fp" ] && account_fp="default"
else
    # Fallback: credentials.json-Datei direkt hashen (wie bisher; unveraendert fuer
    # Windows, wo die Datei existiert) — Datei-Hash statt String-Hash beibehalten,
    # damit bestehende Windows-State-Files nicht invalidiert werden.
    cred_file="$HOME/.claude/.credentials.json"
    if [ -f "$cred_file" ]; then
        if command -v sha256sum >/dev/null 2>&1; then
            account_fp=$(sha256sum "$cred_file" 2>/dev/null | cut -c1-16)
        elif command -v shasum >/dev/null 2>&1; then
            account_fp=$(shasum -a 256 "$cred_file" 2>/dev/null | cut -c1-16)
        fi
        [ -z "$account_fp" ] && account_fp="default"
    fi
fi
# Frisch berechneten fp in den Cache schreiben (atomic tmp+mv). Beim naechsten Aufruf
# wird er ohne jq/sha256sum gelesen, solange ~/.claude.json sich nicht aendert.
if [ -z "$_fp_from_cache" ]; then
    printf '%s|%s' "$account_fp" "$now_ts" > "$fp_cache.tmp" 2>/dev/null \
        && mv -f "$fp_cache.tmp" "$fp_cache" 2>/dev/null
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
        # Performance (Frank 2026-07-08): frueher 1 jq PRO State-Datei (bei 16 parallelen
        # Sessions = 16 jq ~6s -> Ausreisser bis 12s). Jetzt EIN jq ohne slurp ueber alle
        # Dateien; input_filename liefert pro Eintrag den Dateinamen. Nur fremde
        # (nicht-leere, abweichende) Fingerprints werden ausgegeben und geloescht.
        jq -r --arg fp "$account_fp" \
            'select((.account_fp // "") != "" and (.account_fp // "") != $fp) | input_filename' \
            "$state_dir"/rate-limits-*.json 2>/dev/null | while IFS= read -r f; do
                [ -n "$f" ] && rm -f "$f" 2>/dev/null
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
        # Reset-Fenster aus der FRISCHESTEN Session (hoechstes ts_seen) bestimmen,
        # NICHT aus max(resets_at). Grund (Frank-Bug-Report 2026-05-25): Der
        # Reset-Zeitpunkt ist eine accountweite Konstante des aktuellen Fensters —
        # kein Wert der hochzaehlt. max(resets_at) liess eine liegengebliebene
        # Test-/Anomalie-Datei mit kuenstlich grossem Timestamp den Countdown kapern
        # (zeigte 3D10H statt echter 1D23H). Die frischeste API-Antwort hat per
        # Definition den korrekten aktuellen Reset. Verbrauch bleibt max (zaehlt hoch).
        ($valid | max_by(.ts_seen // 0)) as $freshest |
        ($freshest.ts_seen // 0) as $freshTs |
        ($freshest.five_h_resets // 0) as $freshR |
        ($valid | map(select((.five_h_resets // 0) == $freshR)) | max_by(.five_h // 0)) as $bestF |
        ($freshest.seven_d_resets // 0) as $maxSR |
        # 7d-Verbrauch NUR aus dem AKTUELLEN 7d-Fenster (gleiches seven_d_resets wie die
        # frischeste Session) — analog zur 5h-Fenster-Logik. Verhindert dass eine idle Session
        # aus dem VORHERIGEN Fenster mit hohem seven_d den frischen niedrigen Wert nach einem
        # 7d-Reset kapert (Frank-Bug-Report 2026-06-03: 48% statt 5%). Fallback auf alle wenn
        # kein gueltiges Fenster (maxSR=0) bekannt ist (lossless).
        #
        # ZUSATZ-FILTER (Defense, Frank-Bug-Report 2026-06-13): Nach einem Account-Wechsel
        # teilen altes und neues Konto denselben kalendarischen seven_d_resets, sodass die
        # Fenster-Trennung den hohen Altwert NICHT aussortiert. Primaer faengt das jetzt der
        # account_fp-Filter oben ab; falls der aber mal "default" bleibt (keine Account-Quelle
        # lesbar), begrenzt dieser Frische-Filter das 7d-MAX zusaetzlich auf Dateien innerhalb
        # von 5h (18000s) der frischesten Session. Parallele Live-Sessions refreshen sekuendlich
        # (immer frisch, bleiben drin); tote Vortags-Sessions eines alten Kontos fallen raus.
        # Lossless: kein echter aktueller Wert wird verworfen, nur veraltete Fremdkonto-Reste.
        ($valid | (if $maxSR > 0 then map(select((.seven_d_resets // 0) == $maxSR)) else . end)
                | map(select((.ts_seen // 0) >= ($freshTs - 18000))) | map(.seven_d // 0) | max) as $bestS |
        "\($bestF.five_h // 0)|\($freshR)|\($bestS)|\($bestF.session_id // "")|\($maxSR)"
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

# Prozent runden — robust OHNE printf "%.0f".
# Frank-Bug-Report 2026-06-12: Die 7d-Anzeige sprang auf 0%, obwohl der echte Wert
# hoeher war (z.B. 55%). Root Cause: Die API liefert seven_day.used_percentage
# gelegentlich als Floating-Point-Artefakt "55.00000000000001". Die bash-3.2-builtin
# printf (macOS) kann so einen langen Float NICHT parsen ("invalid number") und gibt
# 0 zurueck. Glatte Ganzzahlen (5h=36) funktionierten, der Float-7d-Wert nicht — und
# genau dieser wird von der MAX-Logik als hoechster Wert ausgewaehlt. Deshalb ging es
# "am Anfang" (solange 7d eine glatte Zahl war) und dann nicht mehr.
# Fix: reine Parameter-Expansion — locale-unabhaengig (kein de_DE-Komma-Problem) und
# bash-3.2-fest (kein Float-Parser). Ganzzahlteil nehmen, an erster Nachkommastelle
# (>=5) kaufmaennisch aufrunden. Vorzeichen bleibt erhalten (fuer 100-remaining-Faelle).
round_pct() {
    local v="$1"
    [ -z "$v" ] && { echo ""; return; }
    case "$v" in
        *.*)
            local int="${v%%.*}"
            local frac="${v#*.}"
            local neg=""
            case "$int" in -*) neg="-"; int="${int#-}" ;; esac
            int="${int:-0}"
            case "$int" in ''|*[!0-9]*) echo ""; return ;; esac
            case "$frac" in [5-9]*) int=$((int + 1)) ;; esac
            echo "${neg}${int}"
            ;;
        *)
            echo "$v"
            ;;
    esac
}
[ -n "$five_h_used" ] && five_h_used=$(round_pct "$five_h_used")
[ -n "$week_used" ]   && week_used=$(round_pct "$week_used")
[ -n "$ctx_remaining" ] && ctx_remaining=$(round_pct "$ctx_remaining")

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
# Kontext-Verbrauch: das OFFIZIELLE Feld used_percentage DIREKT nehmen. Das ist genau
# das Mass das auch die Auto-Compaction triggert ((input+cache_read+cache_creation)/
# context_window_size, nur Input-Tokens) und das die TUI oben anzeigt. Frueher: 100 -
# remaining_percentage gerechnet (in aktueller CC-Version identisch, aber used_percentage
# ist die robuste Quelle, falls Anthropic je einen Reserve-Puffer einfuehrt der die Summe
# != 100 macht). Fallback auf 100-remaining fuer aeltere CC-Versionen ohne das Feld.
# (Empirisch verifiziert 2026-05-25: context_window liefert used_percentage + remaining_percentage,
#  context_window_size=1000000 bei opus-1M, total_input_tokens; Quelle: code.claude.com/docs/en/statusline.)
ctx_used=""
if [ -n "$ctx_used_pct" ]; then
    ctx_used=$(round_pct "$ctx_used_pct")
elif [ -n "$ctx_remaining" ]; then
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

# Uhrzeit — bereits oben zusammen mit now_ts in EINEM date-Aufruf geholt ($time).
# Frueher hier ein separates date +%H:%M (Frank 2026-06-11); 2026-07-08 zusammengelegt.

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
    # 5 Segmente (Frank 2026-05-31, ~30% schmaler als die alten 7 — mehr Platz in der
    # Statuszeile). Round half up: filled = round(pct*5/100). Prozent-Logik unveraendert,
    # nur Gesamtbreite kleiner. Konsistent zur ps1-Variante.
    local filled=$(( (pct * 5 + 50) / 100 ))
    [ "$filled" -gt 5 ] && filled=5
    local empty=$((5 - filled))
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
EMPTY_BAR="${TRACK}░░░░░${R}"

# ===== ZEILE 1: Modell | Effort | 5h + Pacing | 7d + Pacing | Kontext (Frank 2026-05-25) =====

# 1. Context-Verbrauch (jetzt GANZ vorne — Frank 2026-06-07).
#    Erstes Element von Zeile 1: KEIN fuehrender Trenner. Wenn kein ctx-Wert bekannt
#    ist (frueher Session-Start), faellt das Modell auf die erste Position zurueck.
ctx_shown=""
if [ -n "$ctx_used" ]; then
    # Ab 80% BLINKT der Kontextbereich auffaellig (Frank 2026-06-07): zwei Techniken
    # kombiniert. (1) Skript-Puls ueber refreshInterval=1 — in der ungeraden Sekunde
    # wird der ganze Bereich invers dargestellt (weisser fetter Text auf rotem
    # Hintergrund), in der geraden normal rot → pulst ~1 Hz, terminal-UNABHAENGIG.
    # (2) zusaetzlich das native ANSI-Blink-Attribut (SGR 5) fuer Terminals die es
    # koennen (Windows Terminal ja). Eigener Balken ohne interne Resets, damit der
    # rote Hintergrund im Invers-Zustand durchgehend bleibt.
    if [ "$ctx_used" -ge 80 ] 2>/dev/null && [ $((now_ts % 2)) -eq 1 ]; then
        cf=$(( (ctx_used * 5 + 50) / 100 )); [ "$cf" -gt 5 ] && cf=5; ce=$((5 - cf))
        cbar=""; ci=0
        while [ "$ci" -lt "$cf" ]; do cbar="${cbar}█"; ci=$((ci+1)); done
        ci=0; while [ "$ci" -lt "$ce" ]; do cbar="${cbar}░"; ci=$((ci+1)); done
        BLINK='\033[5m'
        INV='\033[1m\033[38;2;255;255;255m\033[48;2;235;70;70m'  # fett, weiss auf rot
        printf "${BLINK}${INV} ${ICON_CTX} ctx ${cbar} ${ctx_used}%% ${R}"
    else
        col=$(pct_color "$ctx_used")
        bar=$(make_bar "$ctx_used" "$col")
        printf "${col}${ICON_CTX} ctx${R} ${bar} ${col}${ctx_used}%%${R}"
    fi
    ctx_shown="1"
fi

# 2. Modell — fuehrender Trenner NUR wenn der Kontext davor steht (sonst erstes Element)
if [ -n "$ctx_shown" ]; then
    printf "${SEP}${B}${ICON_MODEL} ${BOLD}${model}${R}"
else
    printf "${B}${ICON_MODEL} ${BOLD}${model}${R}"
fi

# 3. Effort (direkt hinter dem Modell)
printf "${SEP}${EFFORT_COL}${ICON_EFFORT} ${effort_upper}${R}"

# 3. 5h-Limit mit Balken
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

# 4. 5h-Pacing-Pendel direkt hinter dem 5h-Balken (Frank 2026-05-24):
#     Nur zeigen wenn Verbrauch UND Reset-Zeitpunkt bekannt sind.
if [ -n "$five_h_used" ] && [ -n "$five_h_resets" ] && [ "$five_h_resets" -gt 0 ] 2>/dev/null; then
    pacebar=$(make_pace_bar "$five_h_used" "$five_h_resets" 18000)
    printf "${GSEP}${PACE}slow${R} ${pacebar} ${PACE}fast${R}"
fi

# 5. 7d-Limit mit Balken (mit Reset-Countdown in Tagen+Stunden — Frank 2026-05-25)
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

# 6. 7d-Pacing-Pendel (Frank 2026-05-24): gleiche Logik wie 5h, Fenster 7 Tage
#     (604800s), eigene Feature-Farbe (Pink). Nur wenn Verbrauch UND 7d-Reset bekannt.
if [ -n "$week_used" ] && [ -n "$week_resets" ] && [ "$week_resets" -gt 0 ] 2>/dev/null; then
    pacebar7=$(make_pace_bar "$week_used" "$week_resets" 604800)
    printf "${GSEP}${PACE7}slow${R} ${pacebar7} ${PACE7}fast${R}"
fi

# (Context steht jetzt GANZ vorne — Frank 2026-06-07, frueher hier an Position 7)

# 8. Uhrzeit — letztes Element von Zeile 1
printf "${SEP}${TIMECOL}${ICON_TIME} ${time}${R}"
echo

# ===== ZEILE 2: NUR der Arbeitsordner (Frank 2026-08-12) =====
# Grund: In Zeile 1 wurde der Ordner am rechten Rand abgeschnitten und war damit
# unsichtbar. Er bekommt jetzt eine eigene Zeile und wird dort praktisch ungekuerzt
# gezeigt (shorten_path mit max_len 100 nur als Notbremse fuer absurd lange Pfade).
# Der Pfad wird als printf-ARGUMENT (%s) uebergeben, nicht in den Format-String
# interpoliert — so koennen Backslashes/Prozentzeichen im Pfad nie als Escape wirken
# (vgl. Windows-Pfad-Bug 2026-07-08, wo "\b" gedruckte Segmente wieder loeschte).
[ -n "$cwd" ] && printf "${P}${ICON_DIR} %s${R}\n" "$cwd"

exit 0
