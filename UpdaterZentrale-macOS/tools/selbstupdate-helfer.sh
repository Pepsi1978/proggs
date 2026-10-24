#!/bin/bash
# selbstupdate-helfer.sh — zweiter Teil des Selbst-Updates (Gegenstueck zu selbstupdate-helfer.ps1).
#
# Aufruf (nur durch update-zentrale.sh):  selbstupdate-helfer.sh <neue .app> <Ziel in /Applications> [<PID>]
#   PID gesetzt: die App beendet sich selbst; gewartet wird auf genau diesen Prozess (hoechstens 60 s,
#                danach wird sie gebeten, sich zu beenden).
#   PID leer:    laeuft die App, wird sie nach einer Sekunde gebeten, sich zu beenden.
# Danach: neue Fassung nach /Applications legen und starten. Schlaegt das fehl, startet die
# bisherige Fassung wieder. Jeder Schritt steht mit Uhrzeit in der Ausgabe (-> Protokolldatei).
set -u

NEU="$1"
ZIEL="$2"
PID="${3:-}"
BUNDLE_ID="de.frank.UpdaterZentrale"
PROZESS="$(basename "$ZIEL")/Contents/MacOS/UpdaterZentrale"

log() { echo "$(date '+%d.%m.%Y %H:%M:%S')  $*"; }
laeuft() {
    if [ -n "$PID" ]; then kill -0 "$PID" 2>/dev/null; else pgrep -f "$PROZESS" >/dev/null 2>&1; fi
}

log "Selbst-Update: neue Fassung $NEU, Ziel $ZIEL, Prozessnummer ${PID:-(keine)}"

if [ -z "$PID" ] && laeuft; then
    sleep 1
    log "Kein Aufruf aus der App – bitte die App, sich zu beenden."
    osascript -e "tell application id \"$BUNDLE_ID\" to quit" >/dev/null 2>&1 || true
fi

for _ in $(seq 1 120); do laeuft || break; sleep 0.5; done
if laeuft; then
    log "Die App läuft nach 60 s noch – bitte sie, sich zu beenden."
    osascript -e "tell application id \"$BUNDLE_ID\" to quit" >/dev/null 2>&1 || true
    for _ in $(seq 1 60); do laeuft || break; sleep 0.5; done
fi
laeuft && log "Warnung: die App läuft noch – die neue Fassung wird trotzdem eingespielt."

# Erst daneben ablegen, dann tauschen: ein abgebrochenes Kopieren laesst die alte Fassung stehen.
TMP_ZIEL="$ZIEL.neu"
rm -rf "$TMP_ZIEL"
if ditto "$NEU" "$TMP_ZIEL" && rm -rf "$ZIEL" && mv "$TMP_ZIEL" "$ZIEL"; then
    log "Eingespielt: $ZIEL ($(/usr/libexec/PlistBuddy -c 'Print :CFBundleShortVersionString' "$ZIEL/Contents/Info.plist" 2>/dev/null))"
    rm -rf "$(dirname "$NEU")"
else
    log "FEHLER: Die neue Fassung ließ sich nicht nach /Applications legen – die bisherige Fassung startet wieder."
    rm -rf "$TMP_ZIEL"
fi

open "$ZIEL" && log "Gestartet: $ZIEL" || log "FEHLER: $ZIEL ließ sich nicht starten."
