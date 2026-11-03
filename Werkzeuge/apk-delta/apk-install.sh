#!/bin/bash
# Installiert eine App-APK per adb (Tunnel/WLAN vor Kabel) wie `adb install -r`, überträgt aber nur die geänderten
# Einträge gegenüber der installierten Fassung (SHA-256 geprüft, sonst die ganze APK mit lz4).
# Nicht für Jarvis: dafür immer Werkzeuge/jarvis-install/jarvis-install.sh.
# Aufruf: apk-install.sh <apk> [-s <gerät>]
# Letzte Zeile APK_INSTALL_STATUS: installiert oder fehler. Bei INSTALL_FAILED_UPDATE_INCOMPATIBLE nie deinstallieren.
set -u
HIER="$(cd "$(dirname "$0")" && pwd)"
source "$HIER/delta-push.sh"
ende() { echo "APK_INSTALL_STATUS=$1${2:+ ($2)}"; [ "$1" = fehler ] && exit 1; exit 0; }

APK=""; GERAET=""
while [ $# -gt 0 ]; do
  case "$1" in
    -s) GERAET="${2:-}"; shift 2 ;;
    *) APK="$1"; shift ;;
  esac
done
[ -f "$APK" ] || ende fehler "APK nicht gefunden: $APK"

for K in "${ADB:-}" "${ANDROID_HOME:-}/platform-tools/adb" "$HOME/android-sdk/platform-tools/adb" "$HOME/Library/Android/sdk/platform-tools/adb" "$(command -v adb 2>/dev/null)"; do
  [ -n "$K" ] && [ -x "$K" ] && { ADB_BIN="$K"; break; }
done
[ -n "${ADB_BIN:-}" ] || ende fehler "adb nicht gefunden"

waehle_geraet() {
  local netz
  netz=$("$ADB_BIN" devices | awk '$2=="device" && $1 ~ /:[0-9]+$/ {print $1; exit}')
  [ -n "$netz" ] && { echo "$netz"; return; }
  "$ADB_BIN" devices | awk 'NR>1 && $2=="device" {print $1; exit}'
}
if [ -z "$GERAET" ] || [ "$("$ADB_BIN" -s "$GERAET" get-state 2>/dev/null)" != device ]; then
  GERAET=$(waehle_geraet)
  [ -n "$GERAET" ] || { "$HIER/../adb-wlan/adb-wlan.sh" >/dev/null 2>&1; GERAET=$(waehle_geraet); }
fi
[ -n "$GERAET" ] || ende fehler "Handy nicht erreichbar"

# Paketname aus der APK; ohne aapt2 bleibt nur der normale Weg.
AAPT2=$(ls -d "${ANDROID_HOME:-$HOME/android-sdk}"/build-tools/*/aapt2 "$HOME/Library/Android/sdk"/build-tools/*/aapt2 2>/dev/null | sort -V | tail -1)
PAKET=""
[ -n "$AAPT2" ] && PAKET=$("$AAPT2" dump packagename "$APK" 2>/dev/null | head -1)
if [ -z "$PAKET" ]; then
  AUSGABE=$("$ADB_BIN" -s "$GERAET" install -r "$APK" 2>&1)
  echo "$AUSGABE" | tail -1
  echo "$AUSGABE" | grep -q '^Success' && ende installiert "$GERAET"
  ende fehler "$(echo "$AUSGABE" | tail -1)"
fi

ZIEL=/data/local/tmp/apk-install-$PAKET.apk
delta_push "$ADB_BIN" "$GERAET" "$APK" "$ZIEL" "$PAKET" || ende fehler "APK ließ sich nicht aufs Handy laden"
AUSGABE=$("$ADB_BIN" -s "$GERAET" shell pm install -r "$ZIEL" 2>&1 | tr -d '\r')
"$ADB_BIN" -s "$GERAET" shell rm -f "$ZIEL" >/dev/null 2>&1
echo "$AUSGABE" | tail -1
echo "$AUSGABE" | grep -q '^Success' && ende installiert "$GERAET"
ende fehler "$(echo "$AUSGABE" | tail -1)"
