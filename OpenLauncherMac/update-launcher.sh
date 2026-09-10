#!/bin/bash
# update-launcher.sh — der vorgeschriebene Weg, den OpenLauncher zu aktualisieren.
# macOS-Gegenstueck zu OpenLauncher/update-launcher.ps1 (Windows).
#
# Bauen, pruefen und ersetzen in EINEM Schritt. Der Grund ist derselbe wie unter Windows: von Hand
# wurde mehrfach die ALTE Fassung gestartet und als neue gemeldet. Dieses Skript
#   1. fragt nach, bevor ein laufender Launcher geschlossen wird,
#   2. schliesst ihn sauber (statt ihn abzuschiessen),
#   3. prueft den Build-Exit-Code,
#   4. prueft, dass wirklich eine NEUE Datei entstanden ist (Zeitstempel-Vergleich),
#   5. installiert nach /Applications, damit Spotlight den Launcher findet,
#   6. startet ihn und erkennt einen sofortigen Absturz der neuen Fassung.
#
# Aufruf:  bash ~/proggs/OpenLauncherMac/update-launcher.sh
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
BUILD_APP="$PROJECT_DIR/build/OpenLauncher.app"
BUILD_BINARY="$BUILD_APP/Contents/MacOS/OpenLauncher"
INSTALLED_APP="/Applications/OpenLauncher.app"
INSTALLED_BINARY="$INSTALLED_APP/Contents/MacOS/OpenLauncher"
BUNDLE_ID="de.frank.OpenLauncher"

fehler() { printf '\033[31m%s\033[0m\n' "$1" >&2; exit 1; }
hinweis() { printf '\033[90m%s\033[0m\n' "$1"; }
erfolg() { printf '\033[32m%s\033[0m\n' "$1"; }

[ -f "$PROJECT_DIR/build.sh" ] || fehler "Build-Skript nicht gefunden: $PROJECT_DIR/build.sh"

# ---------------------------------------------------------------------------
# 1. Laeuft der Launcher noch? Dann nachfragen und sauber schliessen.
# ---------------------------------------------------------------------------
laeuft() { pgrep -f "OpenLauncher.app/Contents/MacOS/OpenLauncher" >/dev/null 2>&1; }

# Die Rueckfrage kommt IMMER -- auch wenn ein Agent (Claude Code, Codex, OpenCode) das Skript
# aufruft. Frueher entfiel sie ohne Terminal und es galt still "Aktualisieren": das Update lief dann
# ohne Freigabe. Gegen das unsichtbare Haengen: der Dialog wird aktiviert (Vordergrund) und gibt
# nach DIALOG_TIMEOUT Sekunden auf -- dann gilt "Nein", nie "Ja".
# OPENLAUNCHER_UPDATE_FORCE=1 ueberspringt die Frage, NUR auf ausdrueckliche Ansage des Benutzers.
DIALOG_TIMEOUT="${OPENLAUNCHER_UPDATE_DIALOG_TIMEOUT:-240}"

frage_freigabe() {
    local frage="$1"
    osascript - "$frage" "$DIALOG_TIMEOUT" <<'AS' 2>/dev/null || echo "Nein"
on run argv
    tell application "System Events"
        activate
        set r to display dialog (item 1 of argv) with title "OpenLauncher aktualisieren" buttons {"Nein", "Ja"} default button "Ja" with icon caution giving up after ((item 2 of argv) as integer)
    end tell
    if gave up of r then return "TIMEOUT"
    return button returned of r
end run
AS
}

# Ist der vorhandene Build schon der Quellstand? Dann nicht ein zweites Mal bauen. Verglichen wird
# der Zeitstempel der gebauten Binaerdatei gegen die neueste Quelldatei (nur echter Quellcode --
# Laufzeitdateien schreibt der Launcher im Betrieb selbst neu).
build_ist_aktuell() {
    [ -f "$BUILD_BINARY" ] || return 1
    local neueste
    neueste=$(find "$PROJECT_DIR/OpenLauncher" -type f \( -name '*.swift' -o -name '*.plist' \)         -newer "$BUILD_BINARY" -print -quit 2>/dev/null)
    [ -z "$neueste" ]
}

if build_ist_aktuell && laeuft; then
    VERSION=$(/usr/libexec/PlistBuddy -c "Print :CFBundleShortVersionString" "$INSTALLED_APP/Contents/Info.plist" 2>/dev/null || echo "?")
    erfolg "LAUNCHER_UPDATE_STATUS=already-current VERSION=$VERSION"
    exit 0
fi

# Ohne Freigabe per Klick passiert nichts: kein Schliessen, kein Build, kein Start.
if [ "${OPENLAUNCHER_UPDATE_FORCE:-0}" = "1" ]; then
    hinweis "OPENLAUNCHER_UPDATE_FORCE=1 gesetzt: Update ohne Rueckfrage."
else
    if laeuft; then
        FRAGE="Der OpenLauncher läuft noch. Für das Update wird er geschlossen, die neue Version wird gebaut und danach automatisch gestartet.

Nicht gespeicherte Eingaben gehen dabei verloren. Jetzt aktualisieren?"
    else
        FRAGE="Die neue Version des OpenLauncher wird gebaut und danach gestartet.

Jetzt aktualisieren?"
    fi
    ANTWORT=$(frage_freigabe "$FRAGE")
    case "$ANTWORT" in
        Ja) hinweis "Update per Klick freigegeben." ;;
        TIMEOUT) echo "LAUNCHER_UPDATE_STATUS=no-answer (kein Klick innerhalb von $DIALOG_TIMEOUT Sekunden -- nichts geaendert)"; exit 0 ;;
        *) echo "LAUNCHER_UPDATE_STATUS=cancelled"; exit 0 ;;
    esac
fi

if laeuft; then
    # Sauber beenden statt abschiessen: die App speichert dabei ihr Fensterlayout.
    osascript -e "tell application id \"$BUNDLE_ID\" to quit" >/dev/null 2>&1 || true

    ENDE=$((SECONDS + 10))
    while laeuft && [ "$SECONDS" -lt "$ENDE" ]; do sleep 0.2; done
    if laeuft; then
        fehler "Der Launcher wurde nicht innerhalb von 10 Sekunden geschlossen. Das Update wurde nicht durchgeführt."
    fi
    hinweis "Laufender Launcher geschlossen."
fi

# ---------------------------------------------------------------------------
# 2. Bauen - mit Pruefung, dass wirklich etwas Neues entsteht.
# ---------------------------------------------------------------------------
# Zeitstempel VOR dem Build merken. Ein Build, der nichts erzeugt, faellt sonst nicht auf:
# genau daran scheiterte der Weg von Hand (alte Fassung gestartet, neue Version gemeldet).
if build_ist_aktuell; then
    hinweis "Build ist bereits aktuell - es wird nur installiert und gestartet."
else
    STEMPEL_VORHER=0
    [ -f "$BUILD_BINARY" ] && STEMPEL_VORHER=$(stat -f %m "$BUILD_BINARY")

    if ! bash "$PROJECT_DIR/build.sh"; then
        fehler "Der Build ist fehlgeschlagen. Das Update wurde nicht durchgeführt."
    fi

    [ -f "$BUILD_BINARY" ] || fehler "Die aktualisierte Launcher-Datei wurde nicht erzeugt: $BUILD_BINARY"

    STEMPEL_NACHHER=$(stat -f %m "$BUILD_BINARY")
    if [ "$STEMPEL_NACHHER" -le "$STEMPEL_VORHER" ]; then
        fehler "Der Build lief durch, hat die Launcher-Datei aber nicht erneuert. Das Update wurde abgebrochen."
    fi
fi

# ---------------------------------------------------------------------------
# 3. Nach /Applications installieren (dort findet Spotlight den Launcher).
# ---------------------------------------------------------------------------
# ditto statt cp: es uebernimmt Ressource-Forks, erweiterte Attribute und die Signatur
# vollstaendig - bei App-Buendeln ist cp -R dafuer nicht zuverlaessig (Best Practice §G17).
if [ -d "$INSTALLED_APP" ]; then
    rm -rf "$INSTALLED_APP"
fi
if ditto "$BUILD_APP" "$INSTALLED_APP" 2>/dev/null; then
    ZIEL_APP="$INSTALLED_APP"
    hinweis "Installiert nach $INSTALLED_APP"
else
    # Kein Schreibrecht auf /Applications -> der Launcher laeuft trotzdem, nur ohne Spotlight.
    ZIEL_APP="$BUILD_APP"
    printf '\033[33m%s\033[0m\n' "Hinweis: /Applications ist nicht beschreibbar - der Launcher startet aus $BUILD_APP."
fi

# Schreibtisch-Symbol auf die installierte Fassung zeigen lassen.
if [ -x "$PROJECT_DIR/tools/install-desktop-icon.sh" ]; then
    bash "$PROJECT_DIR/tools/install-desktop-icon.sh" >/dev/null 2>&1 || true
fi

# ---------------------------------------------------------------------------
# 4. Starten und pruefen, dass die neue Fassung nicht sofort wieder aussteigt.
# ---------------------------------------------------------------------------
open "$ZIEL_APP"
sleep 2

PID=$(pgrep -f "OpenLauncher.app/Contents/MacOS/OpenLauncher" | head -1 || true)
if [ -z "$PID" ]; then
    fehler "Der aktualisierte Launcher wurde gestartet, aber sofort wieder beendet."
fi

VERSION=$(/usr/libexec/PlistBuddy -c "Print :CFBundleShortVersionString" "$ZIEL_APP/Contents/Info.plist" 2>/dev/null || echo "?")
STAND=$(/usr/libexec/PlistBuddy -c "Print :BuildTimestamp" "$ZIEL_APP/Contents/Info.plist" 2>/dev/null || echo "?")

erfolg "LAUNCHER_UPDATE_STATUS=started PID=$PID"
erfolg "Version $VERSION ($STAND Uhr) läuft aus $ZIEL_APP"
