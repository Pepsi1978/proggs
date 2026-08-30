#!/bin/bash
# Version 1.1.0 - 30.08.2026, 11:54 Uhr
#
# AUFNAHME-SCHUTZ VOR DEM BUILD (macOS-Gegenstueck zu voice-overlay-deploy-guard.ps1)
#
# WARUM ES DEN PORT-TEST GIBT (Vorfall 30.08.2026, auf Windows beobachtet):
# Antwortete ein Overlay nicht, pollte der Guard stumm bis zum Timeout — 600
# Sekunden — und brach dann ab. Jeder Build kostete so zehn Minuten und
# lieferte am Ende nichts. Ursache war eine fehlende Unterscheidung:
# "antwortet nicht" wurde behandelt wie "koennte gerade aufnehmen". Der offene
# Port trennt die beiden Faelle sauber:
#
#   KEIN LISTENER   Das Overlay bietet den Schutz-Endpunkt gar nicht an — eine
#                   aeltere Fassung, oder der Status-Server ist bewusst still
#                   ausgefallen (Port belegt, Bind verweigert). Es gibt nichts
#                   zu reservieren; der Build faehrt ohne Reservierung fort.
#   PORT OFFEN,     Das Overlay laeuft und sollte antworten, tut es aber nicht
#   ABER STUMM      — es haengt. Nur hier ist fail-closed richtig, und auch
#                   hier nach Sekunden statt nach Minuten.

# Lauscht jemand auf dem Port? Ueber /dev/tcp, damit keine zusaetzlichen
# Werkzeuge (nc, lsof) vorausgesetzt werden.
_voice_overlay_port_open() {
    local port="$1"
    (exec 3<>"/dev/tcp/127.0.0.1/${port}") >/dev/null 2>&1 || return 1
    exec 3<&- 2>/dev/null
    exec 3>&- 2>/dev/null
    return 0
}

reserve_voice_overlay_deployment() {
    local process_name="$1"
    local port="$2"
    # Ein Diktat dauert selten laenger als zwei Minuten. Frueher standen hier
    # 600 s — bei einem stummen Overlay hiess das zehn Minuten Stillstand.
    local timeout_seconds="${3:-180}"
    # So lange darf ein Overlay MIT offenem Port stumm bleiben, bevor es als
    # haengend gilt. Deckt den Moment kurz nach dem Start ab.
    local unreadable_grace_seconds="${4:-10}"
    local deadline=$((SECONDS + timeout_seconds))
    local started=$SECONDS
    local announced_busy=0
    local unreadable_since=-1
    local last_progress=$SECONDS
    local waiting_for="eine Antwort des Overlays"

    if ! pgrep -x "$process_name" >/dev/null 2>&1; then
        return 1
    fi

    while (( SECONDS < deadline )); do
        local response
        if response="$(curl --fail --silent --show-error --max-time 2 -X POST "http://127.0.0.1:${port}/deployment/prepare" 2>/dev/null)"; then
            if [[ "$response" == *'"ready":true'* ]]; then
                echo "Aufnahme-Schutz reserviert: $process_name ist idle; neue Aufnahmen bleiben bis zum Build gesperrt."
                return 0
            fi
            unreadable_since=-1
            waiting_for="das Ende der laufenden Aufnahme"
            if (( announced_busy == 0 )); then
                echo "$process_name nimmt auf, transkribiert oder fuegt Text ein. Build wartet..."
                announced_busy=1
            fi
        else
            if ! pgrep -x "$process_name" >/dev/null 2>&1; then
                return 1
            fi

            if ! _voice_overlay_port_open "$port"; then
                echo "$process_name bietet keinen Aufnahme-Schutz an (niemand lauscht auf Port ${port}) — vermutlich eine aeltere Fassung oder der Status-Server ist aus."
                echo "Es gibt nichts zu reservieren. Build faehrt ohne Reservierung fort."
                return 1
            fi

            if (( unreadable_since < 0 )); then
                unreadable_since=$SECONDS
                echo "$process_name lauscht auf Port ${port}, antwortet aber nicht. Warte bis zu ${unreadable_grace_seconds}s auf eine Antwort..."
            fi
            if (( SECONDS - unreadable_since >= unreadable_grace_seconds )); then
                echo "Aufnahme-Schutz fuer ${process_name}: Port ${port} ist offen, antwortet aber seit ${unreadable_grace_seconds}s nicht — das Overlay haengt. Fail-closed, kein Build. Build sicher abgebrochen." >&2
                return 2
            fi
            waiting_for="eine Antwort auf Port ${port}"
        fi

        if (( SECONDS - last_progress >= 5 )); then
            echo "  ... ${process_name}: warte seit $((SECONDS - started))s auf ${waiting_for} (Abbruch nach ${timeout_seconds}s)"
            last_progress=$SECONDS
        fi
        sleep 0.5
    done

    echo "Aufnahme-Schutz fuer $process_name nach ${timeout_seconds}s nicht freigegeben. Build sicher abgebrochen." >&2
    return 2
}

release_voice_overlay_deployment() {
    local process_name="$1"
    local port="$2"
    local reserved="$3"
    [[ "$reserved" == "1" ]] || return 0
    pgrep -x "$process_name" >/dev/null 2>&1 || return 0
    curl --fail --silent --max-time 2 -X POST "http://127.0.0.1:${port}/deployment/release" >/dev/null 2>&1 ||
        echo "WARNUNG: Deployment-Sperre von $process_name konnte nicht freigegeben werden." >&2
}
