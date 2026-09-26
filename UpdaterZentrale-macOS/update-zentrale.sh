#!/bin/bash
# update-zentrale.sh — Selbst-Update der Updater-Zentrale macOS.
#
# Die Rueckfrage stellt die App selbst (leeres statusPraefix im Katalog), bevor sie dieses Skript
# startet. Ablauf:
#   1. bauen (build.sh) — die laufende App bleibt dabei unberuehrt,
#   2. das neue Buendel nach /Applications legen — das geht auf dem Mac auch waehrend die alte
#      Fassung laeuft; der laufende Prozess behaelt seine geladene Programmdatei,
#   3. einen abgekoppelten Helfer starten, der die laufende App sauber beendet und die neue
#      Fassung oeffnet. Abgekoppelt, weil die App nicht auf ein Skript warten kann, das sie selbst
#      beendet; und erst nach dem Ende oeffnen, weil die App nur eine Instanz zulaesst.
#
# Aufruf:  bash ~/proggs/UpdaterZentrale-macOS/update-zentrale.sh
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
APP_NAME="Updater-Zentrale macOS"
BUILD_APP="$PROJECT_DIR/build/$APP_NAME.app"
INSTALLED_APP="/Applications/$APP_NAME.app"
BUNDLE_ID="de.frank.UpdaterZentrale"
PROZESS="$APP_NAME.app/Contents/MacOS/UpdaterZentrale"

bash "$PROJECT_DIR/build.sh"
[ -x "$BUILD_APP/Contents/MacOS/UpdaterZentrale" ] || { echo "Build ohne Programmdatei." >&2; exit 1; }

rm -rf "$INSTALLED_APP"
ditto "$BUILD_APP" "$INSTALLED_APP"
VERSION=$(/usr/libexec/PlistBuddy -c "Print :CFBundleShortVersionString" "$INSTALLED_APP/Contents/Info.plist")
echo "Installiert: $INSTALLED_APP ($VERSION)"

if pgrep -f "$PROZESS" >/dev/null 2>&1; then
    # Ausgabe in eine Datei statt in die Pipe der App: sonst wartet die App auf das Pipe-Ende
    # und damit auf ihr eigenes Beenden.
    LOG="$HOME/Library/Logs/UpdaterZentrale-selbstupdate.log"
    nohup /bin/bash -c '
        sleep 3
        osascript -e "tell application id \"'"$BUNDLE_ID"'\" to quit" >/dev/null 2>&1 || true
        for i in $(seq 1 60); do pgrep -f "'"$PROZESS"'" >/dev/null || break; sleep 0.5; done
        open "'"$INSTALLED_APP"'"
    ' >"$LOG" 2>&1 </dev/null &
    disown
    echo "Die Updater-Zentrale startet sich in wenigen Sekunden neu."
else
    open "$INSTALLED_APP"
fi

echo "Fertig — alle Ziele erfolgreich ($VERSION)"
