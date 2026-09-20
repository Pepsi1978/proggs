#!/bin/bash
# Legt eine Verknuepfung der Updater-Zentrale auf den Schreibtisch und gibt ihr das App-Symbol.
#
# Gegenstueck zu UpdateZentrale/create_shortcut.ps1 (Windows, legt eine .lnk an).
#
# Warum ein Alias und keine .command-Datei: ein Finder-Alias zeigt automatisch das Symbol der App,
# laesst sich umbenennen und verschieben, und ein Doppelklick startet die App ohne Terminalfenster.
# Eine Kopie der .app auf dem Schreibtisch waere die schlechtere Wahl -- sie wuerde bei jedem
# Update veralten.
set -e

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
APP_NAME="Updater-Zentrale macOS"
INSTALLED_APP="/Applications/$APP_NAME.app"
BUILD_APP="$PROJECT_DIR/build/$APP_NAME.app"
DESKTOP="$HOME/Desktop"

# Bevorzugt die installierte Fassung: die bleibt liegen, auch wenn der build-Ordner geleert wird.
if [ -d "$INSTALLED_APP" ]; then
    ZIEL="$INSTALLED_APP"
elif [ -d "$BUILD_APP" ]; then
    ZIEL="$BUILD_APP"
else
    echo "Keine gebaute App gefunden - zuerst build.sh ausfuehren." >&2
    exit 1
fi

ALIAS="$DESKTOP/$APP_NAME"

# Einen vorhandenen Alias ersetzen, damit er nie auf eine alte Stelle zeigt.
rm -f "$ALIAS" 2>/dev/null || true

osascript >/dev/null <<AS
tell application "Finder"
    set zielDatei to POSIX file "$ZIEL" as alias
    set schreibtisch to POSIX file "$DESKTOP" as alias
    make new alias file at schreibtisch to zielDatei with properties {name:"$APP_NAME"}
end tell
AS

echo "Schreibtisch-Verknuepfung angelegt: $ALIAS  ->  $ZIEL"
