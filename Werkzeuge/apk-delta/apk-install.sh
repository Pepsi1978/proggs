#!/bin/bash
# Installiert eine App-APK per adb (Tunnel/WLAN vor Kabel) wie `adb install -r`, überträgt aber nur die geänderten
# Einträge gegenüber der installierten Fassung (SHA-256 geprüft, sonst die ganze APK mit lz4).
# Nicht für Jarvis: dafür immer Werkzeuge/jarvis-install/jarvis-install.sh.
# Aufruf: apk-install.sh <apk> [--ziel handy|tablet] [-s <gerät>]   (Vorgabe handy, Seriennummer aus adb-wlan/geraete.json)
# Letzte Zeile APK_INSTALL_STATUS: installiert oder fehler. Bei INSTALL_FAILED_UPDATE_INCOMPATIBLE nie deinstallieren.
set -u
HIER="$(cd "$(dirname "$0")" && pwd)"
source "$HIER/delta-push.sh"
ende() { echo "APK_INSTALL_STATUS=$1${2:+ ($2)}"; [ "$1" = fehler ] && exit 1; exit 0; }

APK=""; GERAET=""; ZIELNAME=handy
while [ $# -gt 0 ]; do
  case "$1" in
    -s) GERAET="${2:-}"; shift; [ $# -gt 0 ] && shift ;;
    --ziel) ZIELNAME="${2:-handy}"; shift; [ $# -gt 0 ] && shift ;;
    *) APK="$1"; shift ;;
  esac
done
[ -f "$APK" ] || ende fehler "APK nicht gefunden: $APK"
# Zielgerät über die feste Seriennummer (Regel 18): Sind Handy und Tablet beide verbunden (Server: 10.8.0.7 und 10.8.0.8),
# zählt nie „das erste Gerät“ in adb devices. Ohne geraete.json gilt wie früher das erste Netzgerät.
ZIEL_SERIE=$(awk -v g="\"$ZIELNAME\":" '$1==g {drin=1; next} drin && /}/ {exit} drin && $1=="\"serial\":" {gsub(/[",\r]/,"",$2); print $2; exit}' "$HIER/../adb-wlan/geraete.json" 2>/dev/null)
[ -z "$ZIEL_SERIE" ] && [ "$ZIELNAME" != handy ] && ende fehler "Ziel '$ZIELNAME' steht nicht in Werkzeuge/adb-wlan/geraete.json"

for K in "${ADB:-}" "${ANDROID_HOME:-}/platform-tools/adb" "$HOME/android-sdk/platform-tools/adb" "$HOME/Library/Android/sdk/platform-tools/adb" "$(command -v adb 2>/dev/null)"; do
  [ -n "$K" ] && [ -x "$K" ] && { ADB_BIN="$K"; break; }
done
[ -n "${ADB_BIN:-}" ] || ende fehler "adb nicht gefunden"

ist_ziel() {
  [ -z "$ZIEL_SERIE" ] && return 0
  [ "$("$ADB_BIN" -s "$1" shell getprop ro.serialno </dev/null 2>/dev/null | tr -d '[:space:]')" = "$ZIEL_SERIE" ]
}
# $1 = netz: nur IP:Port, sonst jedes Gerät; jeweils nur das Zielgerät.
geraet_suchen() {
  local s
  for s in $("$ADB_BIN" devices | awk 'NR>1 && $2=="device" {print $1}'); do
    case "$s" in *:[0-9]*) ;; *) [ "$1" = netz ] && continue ;; esac
    ist_ziel "$s" && { echo "$s"; return; }
  done
}
waehle_geraet() { geraet_suchen alle; }
netzgeraet() { geraet_suchen netz; }
if [ -z "$GERAET" ] || [ "$("$ADB_BIN" -s "$GERAET" get-state 2>/dev/null)" != device ]; then
  # Regel 18: erst WLAN/Tunnel, ohne Netzverbindung einmal adb-wlan.sh, Kabel nur als Notnagel.
  GERAET=$(netzgeraet)
  [ -n "$GERAET" ] || { "$HIER/../adb-wlan/adb-wlan.sh" --geraet "$ZIELNAME" >/dev/null 2>&1; GERAET=$(netzgeraet); }
  if [ -z "$GERAET" ]; then
    GERAET=$(waehle_geraet)
    [ -n "$GERAET" ] && echo "WLAN/Tunnel nicht erreichbar, auch adb-wlan.sh half nicht: installiere per Kabel ($GERAET)."
  fi
fi
[ -n "$GERAET" ] || ende fehler "Handy nicht erreichbar"

# Paketname aus der APK; ohne aapt2 bleibt nur der normale Weg.
AAPT2=$(ls -d "${ANDROID_HOME:-$HOME/android-sdk}"/build-tools/*/aapt2 "$HOME/Library/Android/sdk"/build-tools/*/aapt2 2>/dev/null | sort -V | tail -1)
PAKET=""
[ -n "$AAPT2" ] && PAKET=$("$AAPT2" dump packagename "$APK" 2>/dev/null | head -1)
[ "$PAKET" = de.frank.jarvis ] && exec "$HIER/../jarvis-install/jarvis-install.sh" "$APK" --ziel "$ZIELNAME" -s "$GERAET"
if [ -z "$PAKET" ]; then
  echo "Paketname nicht lesbar (aapt2 fehlt?), installiere ohne Delta per adb install."
  AUSGABE=$("$ADB_BIN" -s "$GERAET" install -r "$APK" 2>&1)
  echo "$AUSGABE" | tail -1
  echo "$AUSGABE" | grep -q '^Success' && ende installiert "$GERAET"
  ende fehler "$(echo "$AUSGABE" | tail -1)"
fi

ZIEL=/data/local/tmp/apk-install-$PAKET-$$.apk
trap '"$ADB_BIN" -s "$GERAET" shell rm -f "$ZIEL" >/dev/null 2>&1' EXIT
trap 'exit 130' INT TERM
delta_push "$ADB_BIN" "$GERAET" "$APK" "$ZIEL" "$PAKET" || ende fehler "APK ließ sich nicht aufs Handy laden"
AUSGABE=$("$ADB_BIN" -s "$GERAET" shell pm install -r "$ZIEL" 2>&1 | tr -d '\r')
"$ADB_BIN" -s "$GERAET" shell rm -f "$ZIEL" >/dev/null 2>&1
echo "$AUSGABE" | tail -1
echo "$AUSGABE" | grep -q '^Success' && { delta_merken "$APK" "$PAKET"; ende installiert "$GERAET"; }
ende fehler "$(echo "$AUSGABE" | tail -1)"
