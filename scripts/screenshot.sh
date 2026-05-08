#!/usr/bin/env bash
# Robuster ADB-Screenshot fuer Cross-Platform-Nutzung (Windows-Git-Bash + macOS).
#
# Warum dieses Skript existiert:
#   adb.exe auf Windows hat seit ueber 10 Jahren einen bekannten Bug —
#   `adb exec-out screencap -p` konvertiert im eigenen Stdout-Handling LF zu CRLF
#   und zerstoert damit das PNG. Ausserdem konvertiert MSYS (die Bash-Emulation
#   hinter Git Bash) Pfade wie /sdcard/ automatisch zu C:\Program Files\Git\sdcard\.
#   Beide Probleme zusammen machen alle naiven Screenshot-Versuche unbrauchbar.
#
#   Funktionierendes Rezept (verifiziert 2026-05-09 mit Fold 6):
#     1. screencap auf dem GERAET ausfuehren (kein Stdout-Stream)
#     2. Doppelter Slash //sdcard/ verhindert MSYS-Pfad-Konvertierung
#     3. adb pull transferiert Binaerdaten ueber das adb-Protokoll (sauber)
#     4. Aufraeumen: temp-Datei auf dem Geraet loeschen
#
# Usage:
#   ./screenshot.sh                              # erstes verbundenes Geraet, Default-Pfad
#   ./screenshot.sh -s RFCX70KTDFX               # spezifisches Geraet
#   ./screenshot.sh -o ~/Desktop/foo.png         # spezifischer Pfad
#   ./screenshot.sh -d 4630947194243491972       # spezifisches Display (Foldable)
#   ./screenshot.sh -n my_test                   # nur Name, in Default-Verzeichnis
#
# Default-Output: ~/proggs/screenshots/<modell>_<timestamp>.png

set -euo pipefail

# Defaults
DEVICE=""
OUTPUT=""
NAME_HINT=""
DISPLAY_ID=""
# Frank-Standardordner 2026-05-09: alle Screenshots landen ab sofort hier.
# Funktioniert identisch auf Windows (C:\Users\barwa\Pictures\Claude Screenshots)
# und macOS (/Users/barwa/Pictures/Claude Screenshots). Mit -o ueberschreibbar.
OUTPUT_DIR="$HOME/Pictures/Claude Screenshots"

# Argumente parsen
while [[ $# -gt 0 ]]; do
    case "$1" in
        -s|--device)    DEVICE="$2"; shift 2 ;;
        -o|--output)    OUTPUT="$2"; shift 2 ;;
        -n|--name)      NAME_HINT="$2"; shift 2 ;;
        -d|--display)   DISPLAY_ID="$2"; shift 2 ;;
        -h|--help)
            sed -n '2,/^$/p' "$0" | sed 's/^# \?//'
            exit 0 ;;
        *)
            echo "Unbekanntes Argument: $1" >&2
            exit 2 ;;
    esac
done

# 1) Geraete-Auswahl
if [ -z "$DEVICE" ]; then
    devices=$(adb devices | awk 'NR>1 && $2=="device" {print $1}')
    count=$(printf '%s\n' "$devices" | grep -c . || true)
    if [ "$count" -eq 0 ]; then
        echo "FEHLER: Kein angeschlossenes Geraet gefunden. Pruefen mit: adb devices" >&2
        exit 3
    elif [ "$count" -gt 1 ]; then
        echo "WARNUNG: Mehrere Geraete verbunden, nehme erstes:" >&2
        echo "$devices" | sed 's/^/  /' >&2
        DEVICE=$(printf '%s\n' "$devices" | head -n1)
    else
        DEVICE="$devices"
    fi
fi

# 2) Geraete-Modell fuer Dateinamen ermitteln (sanitized)
MODEL=$(adb -s "$DEVICE" shell getprop ro.product.model 2>/dev/null | tr -d '\r' | tr ' /' '__' | tr -cd '[:alnum:]_-' || echo "device")
[ -z "$MODEL" ] && MODEL="device"

# 3) Output-Pfad bestimmen
if [ -z "$OUTPUT" ]; then
    mkdir -p "$OUTPUT_DIR"
    timestamp=$(date '+%Y%m%d_%H%M%S')
    if [ -n "$NAME_HINT" ]; then
        OUTPUT="$OUTPUT_DIR/${NAME_HINT}_${timestamp}.png"
    else
        OUTPUT="$OUTPUT_DIR/${MODEL}_${timestamp}.png"
    fi
fi

# 4) Screenshot-Logik: auf Geraet erzeugen, dann pullen, dann aufraeumen
DEVICE_TMP="//sdcard/screenshot_$$_$(date +%s).png"
DISPLAY_FLAG=""
[ -n "$DISPLAY_ID" ] && DISPLAY_FLAG="-d $DISPLAY_ID"

# Cleanup-Trap: temp-Datei am Geraet aufraeumen, auch bei Fehler
cleanup() { adb -s "$DEVICE" shell "rm -f $DEVICE_TMP" >/dev/null 2>&1 || true; }
trap cleanup EXIT

if ! adb -s "$DEVICE" shell "screencap -p $DISPLAY_FLAG $DEVICE_TMP" 2>/dev/null; then
    echo "FEHLER: screencap auf dem Geraet fehlgeschlagen." >&2
    exit 4
fi

if ! adb -s "$DEVICE" pull "$DEVICE_TMP" "$OUTPUT" >/dev/null 2>&1; then
    echo "FEHLER: adb pull fehlgeschlagen." >&2
    exit 5
fi

# 5) Sanity-Check: Datei sollte ein gueltiges PNG sein (Magic-Bytes 89 50 4E 47)
if ! head -c 4 "$OUTPUT" | od -An -tx1 | tr -d ' \n' | grep -q '^89504e47$'; then
    echo "FEHLER: Heruntergeladene Datei ist kein gueltiges PNG." >&2
    rm -f "$OUTPUT"
    exit 6
fi

echo "OK: $OUTPUT"
