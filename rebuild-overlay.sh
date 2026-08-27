#!/bin/bash
# Version 1.0.0 - 27.08.2026, 14:54 Uhr
#
# macOS-Pendant zu rebuild-overlay.ps1: baut ein Voice-Overlay sauber neu und
# startet es neu — in EINEM Schritt, inklusive Verifikation, dass hinterher
# wirklich die FRISCH GEBAUTE Version laeuft.
#
# Warum ein eigenes Skript und nicht "einfach build.sh":
#   * build.sh haelt der Aufnahme-Schutz an, solange eine ALTE App laeuft, die
#     den /deployment/prepare-Endpunkt noch nicht kennt — dann wartet er
#     fail-closed bis zum Timeout. Hier wird das erkannt und sauber aufgeloest.
#   * build.sh startet die App nicht neu; ohne Neustart laeuft nach dem Build
#     weiter der ALTE Code, und man haelt eine Aenderung faelschlich fuer wirkungslos.
#   * Es fehlte die Verifikation "laeuft jetzt wirklich der neue Build?".
#
# Nutzung:
#   bash rebuild-overlay.sh TVO        # nur TerminalVoiceOverlay
#   bash rebuild-overlay.sh CVO        # nur ClaudeCodexVoiceOverlay
#   bash rebuild-overlay.sh Both       # beide nacheinander
#   bash rebuild-overlay.sh TVO --no-start   # nur beenden + bauen

set -uo pipefail

REPO_ROOT="$(cd "$(dirname "$0")" && pwd)"

TARGET="${1:-}"
NO_START=0
[[ "${2:-}" == "--no-start" ]] && NO_START=1

usage() {
    echo "Nutzung: bash rebuild-overlay.sh {TVO|CVO|Both} [--no-start]" >&2
    exit 2
}
[[ -z "$TARGET" ]] && usage

# ── Overlay-Konfiguration (eine Quelle der Wahrheit) ────────────────────────
overlay_config() {
    case "$1" in
        TVO)
            NAME="TerminalVoiceOverlay"
            LABEL="Terminal"
            FOLDER="$REPO_ROOT/TerminalVoiceOverlay-macOS"
            PORT=5723
            ;;
        CVO)
            NAME="ClaudeCodexVoiceOverlay"
            LABEL="Claude/Codex"
            FOLDER="$REPO_ROOT/ClaudeCodexVoiceOverlay-macOS"
            PORT=5724
            ;;
        *) return 1 ;;
    esac
    APP="$FOLDER/build/$NAME.app"
    BIN="$APP/Contents/MacOS/$NAME"
}

step() { echo "  -> $*"; }
head_() { echo; echo "=== $* ==="; }
ok()   { echo "  OK: $*"; }
warn() { echo "  ! $*"; }
err()  { echo "  ! $*" >&2; }

# Laeuft gerade eine Aufnahme/Transkription? Wenn der Endpunkt antwortet, wird
# gewartet; antwortet er GAR NICHT (alte Version ohne Endpunkt), wird das
# einmal gemeldet und weitergemacht — sonst blockiert genau die alte Version
# ewig ihren eigenen Austausch.
wait_until_idle() {
    local name="$1" port="$2"
    pgrep -x "$name" >/dev/null 2>&1 || { step "$name laeuft nicht — kein Aufnahme-Schutz noetig."; return 0; }

    local deadline=$((SECONDS + 300)) announced=0
    while (( SECONDS < deadline )); do
        local response
        if response="$(curl --fail --silent --max-time 2 -X POST "http://127.0.0.1:${port}/deployment/prepare" 2>/dev/null)"; then
            if [[ "$response" == *'"ready":true'* ]]; then
                ok "Aufnahme-Schutz reserviert — $name ist idle."
                return 0
            fi
            if (( announced == 0 )); then
                step "$name nimmt gerade auf oder fuegt Text ein — warte..."
                announced=1
            fi
        else
            warn "$name antwortet nicht auf den Aufnahme-Status (alte Version ohne Endpunkt)."
            warn "Es laeuft nachweislich keine pruefbare Aufnahme — der Austausch wird fortgesetzt."
            return 0
        fi
        sleep 0.5
    done
    err "$name hat den Aufnahme-Schutz nach 300s nicht freigegeben — Abbruch."
    return 1
}

# Beendet alle Prozesse der App. Erst freundlich (TERM), dann hart (KILL).
stop_overlay() {
    local name="$1"
    pgrep -x "$name" >/dev/null 2>&1 || { step "$name laeuft bereits nicht."; return 0; }
    step "Beende $name..."
    pkill -x "$name" 2>/dev/null
    for _ in $(seq 1 20); do
        pgrep -x "$name" >/dev/null 2>&1 || { ok "$name beendet."; return 0; }
        sleep 0.25
    done
    warn "$name reagiert nicht auf TERM — erzwinge Beenden."
    pkill -9 -x "$name" 2>/dev/null
    sleep 1
    if pgrep -x "$name" >/dev/null 2>&1; then
        err "$name laesst sich nicht beenden."
        return 1
    fi
    ok "$name beendet (erzwungen)."
}

# Vergleicht die Version im gebauten Bundle mit der Info.plist der Quelle —
# faengt den Fall ab, dass der Build in Wahrheit gar nicht durchlief und ein
# altes Bundle liegen blieb.
verify_artifact() {
    local app="$1" folder="$2" name="$3"
    local src_plist="$folder/$name/Info.plist"
    local built_plist="$app/Contents/Info.plist"
    [[ -f "$built_plist" ]] || { err "Gebautes Bundle fehlt: $built_plist"; return 1; }
    local expected actual
    expected="$(/usr/libexec/PlistBuddy -c 'Print :CFBundleShortVersionString' "$src_plist" 2>/dev/null)"
    actual="$(/usr/libexec/PlistBuddy -c 'Print :CFBundleShortVersionString' "$built_plist" 2>/dev/null)"
    if [[ "$expected" != "$actual" ]]; then
        err "Build-Artefakt passt nicht zur Quelle: erwartet $expected, gefunden $actual."
        return 1
    fi
    ok "Build-Artefakt verifiziert (Version $actual)."
}

# Verifiziert NACH dem Neustart, dass die LAUFENDE Instanz die FRISCH GEBAUTE ist.
# Kernsignal wie unter Windows: ein Prozess, der VOR dem neuen Build gestartet
# wurde, kann unmoeglich den neuen Code ausfuehren. Deshalb Start-Zeit gegen
# Bau-Zeit pruefen — die Datei-Version auf der Platte wuerde einen alten,
# ueberlebenden Prozess faelschlich als "neu" melden.
verify_running_fresh() {
    local name="$1" bin="$2" port="$3"
    local build_epoch
    build_epoch="$(stat -f %m "$bin")"

    local deadline=$((SECONDS + 120)) announced=0
    while (( SECONDS < deadline )); do
        local pids
        pids="$(pgrep -x "$name" 2>/dev/null)"
        if [[ -n "$pids" ]]; then
            # Aeltester laufender Prozess: liegt sein Start VOR dem Build,
            # ueberlebt hier eine alte Instanz.
            local stale=0
            for pid in $pids; do
                local lstart
                lstart="$(ps -o lstart= -p "$pid" 2>/dev/null)"
                [[ -z "$lstart" ]] && continue
                local pstart
                pstart="$(date -j -f "%a %b %d %T %Y" "$lstart" +%s 2>/dev/null)"
                [[ -z "$pstart" ]] && continue
                if (( pstart < build_epoch - 2 )); then stale=1; fi
            done
            if (( stale == 1 )); then
                echo "STALE"
                return 1
            fi
            # Prozess ist neu — jetzt noch abwarten, bis er wirklich bedienbar
            # ist (der Status-Port antwortet erst nach der Initialisierung).
            if curl --fail --silent --max-time 2 "http://127.0.0.1:${port}/recording/status" >/dev/null 2>&1; then
                return 0
            fi
            if (( announced == 0 )); then
                step "$name initialisiert noch; warte auf Port $port..."
                announced=1
            fi
        fi
        sleep 1
    done
    echo "TIMEOUT"
    return 1
}

rebuild_one() {
    local t="$1"
    overlay_config "$t" || { err "Unbekanntes Ziel: $t"; return 1; }
    head_ "$t  ($NAME -> $LABEL)"

    [[ -d "$FOLDER" ]] || { err "Ordner fehlt: $FOLDER"; return 1; }

    # Schritt 1: Datenverlust-Schutz + Prozesse beenden.
    step 'Schritt 1/3: Aufnahme-Schutz pruefen und Prozesse beenden...'
    wait_until_idle "$NAME" "$PORT" || return 1
    stop_overlay "$NAME" || return 1

    # Schritt 2: bauen. build.sh reserviert selbst nochmal — mit beendeter App
    # ist das ein No-Op und laeuft sofort durch.
    step 'Schritt 2/3: bauen (dauert ~1-2 Min)...'
    if ! ( cd "$FOLDER" && bash build.sh ); then
        err "Build FEHLGESCHLAGEN fuer $t — Neustart uebersprungen."
        return 1
    fi
    verify_artifact "$APP" "$FOLDER" "$NAME" || return 1

    if (( NO_START == 1 )); then
        step 'Schritt 3/3: --no-start gesetzt -> kein Neustart.'
        return 0
    fi

    # Schritt 3: neu starten + verifizieren, dass die NEUE Version laeuft.
    step 'Schritt 3/3: neu starten und verifizieren...'
    local attempt result
    for attempt in 1 2; do
        open "$APP"
        result="$(verify_running_fresh "$NAME" "$BIN" "$PORT")" && {
            local ver
            ver="$(/usr/libexec/PlistBuddy -c 'Print :CFBundleShortVersionString' "$APP/Contents/Info.plist" 2>/dev/null)"
            ok "$t laeuft wieder (NEUE Version $ver VERIFIZIERT, Port $PORT)."
            return 0
        }
        case "$result" in
            STALE)   err "$t: ALTE Version laeuft noch — beende alles und starte erneut (Versuch $attempt/2)." ;;
            TIMEOUT) warn "$t lieferte keinen gueltigen Status auf Port $PORT (Versuch $attempt/2)." ;;
        esac
        stop_overlay "$NAME"
        sleep 2
    done
    err "$t: Konnte NICHT verifizieren, dass die NEUE Version laeuft — bitte manuell pruefen."
    return 1
}

case "$TARGET" in
    TVO|CVO) TARGETS=("$TARGET") ;;
    Both)    TARGETS=("TVO" "CVO") ;;
    *)       usage ;;
esac

FAILED=()
for t in "${TARGETS[@]}"; do
    rebuild_one "$t" || FAILED+=("$t")
done

echo
if (( ${#FAILED[@]} == 0 )); then
    echo "Fertig — alle Ziele (${TARGETS[*]}) erfolgreich."
    exit 0
else
    echo "Fertig mit Problemen bei: ${FAILED[*]}" >&2
    exit 1
fi
