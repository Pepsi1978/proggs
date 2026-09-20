#!/bin/bash
# Legt eine Verknuepfung der Updater-Zentrale auf den Schreibtisch und gibt ihr das App-Symbol —
# OHNE den kleinen Verknuepfungspfeil in der Ecke.
#
# Gegenstueck zu UpdateZentrale/create_shortcut.ps1 (Windows, legt eine .lnk an).
#
# Warum ein Alias und keine Kopie der App: eine Kopie auf dem Schreibtisch wuerde bei jedem Update
# veralten. Der Alias zeigt immer auf die installierte Fassung.
#
# Warum der Pfeil verschwindet: der Finder malt das Pfeil-Abzeichen nur dann auf, wenn er das
# Symbol selbst aus dem Ziel ableitet. Traegt die Datei ein EIGENES Symbol (Finder-Merkmal
# kHasCustomIcon), zeigt er dieses unveraendert — ohne Abzeichen.
#
# Gesetzt wird das mit Rez und SetFile, NICHT mit NSWorkspace.setIcon: letzteres meldet bei einer
# Alias-Datei zwar Erfolg, setzt kHasCustomIcon aber nicht (nachgeprueft am 20.09.2026 — das Flag
# blieb 0x8000 statt 0x8400, der Pfeil blieb sichtbar).
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

# ---------------------------------------------------------------------------
# Eigenes Symbol setzen -> der Verknuepfungspfeil faellt weg.
# ---------------------------------------------------------------------------
ICNS="$PROJECT_DIR/UpdaterZentrale/Resources/AppIcon.icns"
[ -f "$ICNS" ] || ICNS="$ZIEL/Contents/Resources/AppIcon.icns"

if [ -f "$ICNS" ] && command -v Rez >/dev/null && command -v SetFile >/dev/null; then
    WORK_DIR="$(mktemp -d)"
    trap 'rm -rf "$WORK_DIR"' EXIT

    cp "$ICNS" "$WORK_DIR/symbol.icns"

    # sips -i legt IM icns eine Symbol-Ressource an; DeRez holt sie als Rez-Quelltext heraus,
    # Rez haengt sie an die Ressourcen-Gabel der Alias-Datei. Die Alias-Daten selbst bleiben
    # dabei erhalten (nachgeprueft: der Alias zeigt danach weiterhin auf sein Ziel).
    if sips -i "$WORK_DIR/symbol.icns" >/dev/null 2>&1 \
       && DeRez -only icns "$WORK_DIR/symbol.icns" > "$WORK_DIR/symbol.rsrc" 2>/dev/null \
       && [ -s "$WORK_DIR/symbol.rsrc" ] \
       && Rez -append "$WORK_DIR/symbol.rsrc" -o "$ALIAS" 2>/dev/null \
       && SetFile -a C "$ALIAS" 2>/dev/null; then
        # Den Finder das Symbol neu einlesen lassen - sonst zeigt er bis zum naechsten
        # Neuzeichnen noch das alte Bild samt Pfeil.
        touch "$ALIAS"
        echo "Eigenes Symbol gesetzt - ohne Verknuepfungspfeil."
    else
        echo "Hinweis: Das eigene Symbol liess sich nicht setzen; die Verknuepfung traegt den Pfeil." >&2
    fi
else
    echo "Hinweis: Rez/SetFile oder das Symbol fehlen - die Verknuepfung traegt den Pfeil." >&2
fi

echo "Schreibtisch-Verknuepfung angelegt: $ALIAS  ->  $ZIEL"
