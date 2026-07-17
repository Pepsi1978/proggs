#!/bin/bash
# Version 1.0.0 - 17.07.2026, 17:50 Uhr

reserve_voice_overlay_deployment() {
    local process_name="$1"
    local port="$2"
    local timeout_seconds="${3:-600}"
    local deadline=$((SECONDS + timeout_seconds))
    local announced_busy=0
    local announced_unavailable=0

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
            if (( announced_busy == 0 )); then
                echo "$process_name nimmt auf, transkribiert oder fuegt Text ein. Build wartet..."
                announced_busy=1
            fi
        else
            if ! pgrep -x "$process_name" >/dev/null 2>&1; then
                return 1
            fi
            if (( announced_unavailable == 0 )); then
                echo "Status von $process_name ist nicht sicher lesbar. Fail-closed: kein Build."
                announced_unavailable=1
            fi
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
