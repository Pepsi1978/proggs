#!/bin/bash
# update-zentrale.sh — Selbst-Update der Updater-Zentrale macOS.
#
# Die Rueckfrage stellt die App selbst (leeres statusPraefix im Katalog), bevor sie dieses Skript
# startet. Ablauf (wie update-zentrale.ps1 unter Windows seit 1.3.40):
#   1. bauen (build.sh) — die laufende App bleibt dabei unberuehrt,
#   2. einen abgekoppelten Helfer starten (tools/selbstupdate-helfer.sh). Kam das Update aus der App,
#      uebergibt sie ihre Prozessnummer (--app-pid): sie schreibt Protokoll und Verlauf fertig und
#      beendet sich danach selbst; der Helfer wartet nur auf das Ende genau dieses Prozesses
#      (hoechstens 60 s), legt erst DANN die neue Fassung nach /Applications und startet sie. Kam es
#      von aussen, schliesst er die App nach einer Sekunde selbst.
#      Erst nach dem Ende kopieren, damit die neue Fassung das Update bei ihrem Start nachweislich
#      bestaetigen kann (der Stand vor dem Ende ist noch der alte).
#   3. Schlaegt das Kopieren fehl, startet der Helfer die bisherige Fassung wieder und schreibt das
#      ins Protokoll ~/Library/Logs/UpdaterZentrale-selbstupdate.log.
#
# Aufruf:  bash ~/proggs/UpdaterZentrale-macOS/update-zentrale.sh [--app-pid <PID>]
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
APP_NAME="Updater-Zentrale macOS"
BUILD_APP="$PROJECT_DIR/build/$APP_NAME.app"
INSTALLED_APP="/Applications/$APP_NAME.app"
PROZESS="$APP_NAME.app/Contents/MacOS/UpdaterZentrale"
LOG="$HOME/Library/Logs/UpdaterZentrale-selbstupdate.log"

APP_PID=""
while [ $# -gt 0 ]; do
    case "$1" in
        --app-pid) APP_PID="${2:-}"; shift 2 ;;
        *) echo "Unbekannte Option: $1" >&2; exit 2 ;;
    esac
done

bash "$PROJECT_DIR/build.sh"
[ -x "$BUILD_APP/Contents/MacOS/UpdaterZentrale" ] || { echo "Build ohne Programmdatei." >&2; exit 1; }
VERSION=$(/usr/libexec/PlistBuddy -c "Print :CFBundleShortVersionString" "$BUILD_APP/Contents/Info.plist")

# Zwischenkopie: build/ wird beim naechsten Bau geloescht, der Helfer braucht sie aber noch.
STAGE="$(mktemp -d "${TMPDIR:-/tmp}/updaterzentrale-neu.XXXXXX")"
ditto "$BUILD_APP" "$STAGE/$APP_NAME.app"
echo "Gebaut: $VERSION ($STAGE)"

if [ -z "$APP_PID" ] && ! pgrep -f "$PROZESS" >/dev/null 2>&1; then
    # Die App laeuft nicht: direkt einspielen und starten.
    bash "$PROJECT_DIR/tools/selbstupdate-helfer.sh" "$STAGE/$APP_NAME.app" "$INSTALLED_APP" "" >>"$LOG" 2>&1
    echo "Fertig — alle Ziele erfolgreich ($VERSION)"
    exit 0
fi

# Ausgabe in eine Datei statt in die Pipe der App: sonst wartet die App auf das Pipe-Ende und
# damit auf ihr eigenes Beenden.
nohup /bin/bash "$PROJECT_DIR/tools/selbstupdate-helfer.sh" "$STAGE/$APP_NAME.app" "$INSTALLED_APP" "$APP_PID" \
    >>"$LOG" 2>&1 </dev/null &
disown
if [ -n "$APP_PID" ]; then
    echo "Die Updater-Zentrale beendet sich gleich selbst und startet danach in der neuen Fassung ($VERSION)."
else
    echo "Die Updater-Zentrale wird in wenigen Sekunden geschlossen und in der neuen Fassung gestartet ($VERSION)."
fi
echo "Fertig — alle Ziele erfolgreich ($VERSION)"
