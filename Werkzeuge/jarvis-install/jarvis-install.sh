#!/bin/bash
# Installiert eine Jarvis-APK per adb (Tunnel, WLAN, notfalls Kabel), aber erst nach Rückfrage in Jarvis:
# Das Fenster bietet „Sofort“ oder „In 1 bis 5 Minuten“, damit eine laufende Aufnahme nicht abbricht.
# Aufruf: jarvis-install.sh <apk> [-s <gerät>]
# Letzte Zeile JARVIS_INSTALL_STATUS: installiert, abgebrochen, keine-antwort oder fehler.
set -u
PAKET=de.frank.jarvis
FENSTER=$PAKET/.ui.InstallFrageActivity
ANTWORT=/sdcard/Android/data/$PAKET/files/install-antwort.txt
WARTEZEIT_S=240
WLAN_SKRIPT="$(cd "$(dirname "$0")/.." && pwd)/adb-wlan/adb-wlan.sh"

ende() { echo "JARVIS_INSTALL_STATUS=$1${2:+ ($2)}"; [ "$1" = fehler ] && exit 1; exit 0; }

APK=""; GERAET=""
while [ $# -gt 0 ]; do
  case "$1" in
    -s) GERAET="${2:-}"; shift 2 ;;
    *) APK="$1"; shift ;;
  esac
done
[ -f "$APK" ] || ende fehler "APK nicht gefunden: $APK"

for K in "${ADB:-}" "${ANDROID_HOME:-}/platform-tools/adb" "$HOME/Library/Android/sdk/platform-tools/adb" "$(command -v adb 2>/dev/null)"; do
  [ -n "$K" ] && [ -x "$K" ] && { ADB_BIN="$K"; break; }
done
[ -n "${ADB_BIN:-}" ] || ende fehler "adb nicht gefunden"

# WLAN oder Tunnel (IP:Port) vor Kabel; ohne Verbindung einmal das WLAN-Werkzeug fragen.
waehle_geraet() {
  local netz
  netz=$("$ADB_BIN" devices | awk '$2=="device" && $1 ~ /:[0-9]+$/ {print $1; exit}')
  [ -n "$netz" ] && { echo "$netz"; return; }
  "$ADB_BIN" devices | awk 'NR>1 && $2=="device" {print $1; exit}'
}
verbinde() {
  if [ -n "$GERAET" ] && [ "$("$ADB_BIN" -s "$GERAET" get-state 2>/dev/null)" = device ]; then return 0; fi
  GERAET=$(waehle_geraet)
  [ -n "$GERAET" ] && return 0
  "$WLAN_SKRIPT" >/dev/null 2>&1
  GERAET=$(waehle_geraet)
  [ -n "$GERAET" ]
}
installiere() {
  verbinde || ende fehler "Handy nicht erreichbar"
  echo "Installiere über $GERAET ..."
  local ausgabe
  ausgabe=$("$ADB_BIN" -s "$GERAET" install -r "$APK" 2>&1)
  echo "$ausgabe" | tail -2
  echo "$ausgabe" | grep -q '^Success' && ende installiert "$GERAET"
  ende fehler "adb install gescheitert: $(echo "$ausgabe" | tail -1)"
}

verbinde || ende fehler "Handy nicht erreichbar"

KENNUNG="$(date +%s)-$$"
"$ADB_BIN" -s "$GERAET" shell rm -f "$ANTWORT" >/dev/null 2>&1
START=$("$ADB_BIN" -s "$GERAET" shell am start -n "$FENSTER" --es kennung "$KENNUNG" 2>&1)
if echo "$START" | grep -q "does not exist"; then
  # Die installierte Version kennt das Fenster noch nicht: nur dann ohne Rückfrage.
  echo "Installierte Jarvis-Version hat das Fenster noch nicht, installiere ohne Rückfrage."
  installiere
fi
echo "$START" | grep -q "^Error" && ende fehler "Fenster ließ sich nicht öffnen: $(echo "$START" | grep -m1 '^Error')"
echo "Fenster in Jarvis geöffnet, warte auf Antwort (höchstens $WARTEZEIT_S s) ..."

BIS=$(( $(date +%s) + WARTEZEIT_S ))
WAHL=""
while [ "$(date +%s)" -lt "$BIS" ]; do
  ZEILE=$("$ADB_BIN" -s "$GERAET" shell cat "$ANTWORT" 2>/dev/null | tr -d '\r')
  case "$ZEILE" in
    "$KENNUNG "*) WAHL="${ZEILE#* }"; break ;;
  esac
  sleep 2
done
"$ADB_BIN" -s "$GERAET" shell rm -f "$ANTWORT" >/dev/null 2>&1

case "$WAHL" in
  "") ende keine-antwort "nichts geändert" ;;
  abbruch) ende abgebrochen "nichts geändert" ;;
  0) echo "Gewählt: Sofort" ;;
  [1-5]) echo "Gewählt: in $WAHL Minute(n), Installation um $(date -v+"${WAHL}"M +%H:%M 2>/dev/null || date -d "+$WAHL minutes" +%H:%M) Uhr"; sleep $(( WAHL * 60 )) ;;
  *) ende fehler "unbekannte Antwort: $WAHL" ;;
esac
installiere
