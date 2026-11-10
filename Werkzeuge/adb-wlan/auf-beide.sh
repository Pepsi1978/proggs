#!/usr/bin/env bash
# Installiert eine APK auf Handy UND Tablet (jeweils über die feste Seriennummer aus geraete.json, Netz vor Kabel).
# Für die Apps, deren Daten Jarvis zwischen den Geräten abgleicht (Aufgaben, Ziele, Geniale Ideen, Epigenetik, Stack-Labor):
# Der Abgleich braucht auf beiden Geräten dieselbe Fassung. Nicht für Jarvis selbst (dafür jarvis-install mit --ziel).
# Aufruf: auf-beide.sh <apk> [<apk> …]     Ein nicht erreichbares Gerät wird gemeldet, das andere trotzdem bedient.
# Letzte Zeile AUF_BEIDE_STATUS: fertig (alle Installationen gelungen), teilweise oder fehler.
set -u
export MSYS_NO_PATHCONV=1
HIER="$(cd "$(dirname "$0")" && pwd)"
for K in "${ADB:-}" "${ANDROID_HOME:-}/platform-tools/adb" "$HOME/Library/Android/sdk/platform-tools/adb" "${LOCALAPPDATA:-}/Android/Sdk/platform-tools/adb.exe" "$(command -v adb 2>/dev/null)"; do
  [ -n "$K" ] && [ -x "$K" ] && { ADB="$K"; break; }
done
[ -n "${ADB:-}" ] || { echo "AUF_BEIDE_STATUS=fehler (adb nicht gefunden)"; exit 1; }
[ $# -gt 0 ] || { echo "AUF_BEIDE_STATUS=fehler (keine APK genannt)"; exit 2; }
serie() { awk -v g="\"$1\":" '$1==g {drin=1; next} drin && /}/ {exit} drin && $1=="\"serial\":" {gsub(/[",\r]/,"",$2); print $2; exit}' "$HIER/geraete.json"; }
adresse() {
  local s kabel=""
  for s in $("$ADB" devices | awk 'NR>1 && $2=="device" {print $1}'); do
    [ "$("$ADB" -s "$s" shell getprop ro.serialno </dev/null 2>/dev/null | tr -d '[:space:]')" = "$1" ] || continue
    case "$s" in *:[0-9]*) echo "$s"; return ;; esac
    [ -z "$kabel" ] && kabel="$s"
  done
  [ -n "$kabel" ] && echo "$kabel"
}
# Beide Geräte gleichzeitig: Die Installationen laufen über getrennte Verbindungen und halbieren so die Wartezeit
# (gemessen 10.11.2026: fünf APKs nacheinander auf beide Geräte 3 bis 6 Minuten).
ein_geraet() {
  local G=$1; shift
  local GUT=0 SCHLECHT=0 A APK AUS
  A=$(adresse "$(serie $G)")
  if [ -z "$A" ]; then
    # Einmal neu verbinden lassen (WLAN, Tunnel), dann erneut suchen.
    if [ -f "$HIER/adb-wlan.sh" ] && [ "$(uname -s | cut -c1-5)" != "MINGW" ] && [ "$(uname -s | cut -c1-4)" != "MSYS" ]; then bash "$HIER/adb-wlan.sh" --geraet $G >/dev/null 2>&1
    else powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$(cygpath -w "$HIER/adb-wlan.ps1" 2>/dev/null || echo "$HIER/adb-wlan.ps1")" -Geraet $G >/dev/null 2>&1; fi
    A=$(adresse "$(serie $G)")
  fi
  if [ -z "$A" ]; then echo "$G: nicht erreichbar"; echo "ZAEHLER 0 $#"; return; fi
  for APK in "$@"; do
    AUS=$("$ADB" -s "$A" install -r "$APK" </dev/null 2>&1 | tr -d '\r' | tail -1)
    echo "$G ($A): $(basename "$(dirname "$(dirname "$(dirname "$(dirname "$(dirname "$(dirname "$APK")")")")")")") → $AUS"
    case "$AUS" in Success*) GUT=$((GUT + 1)) ;; *) SCHLECHT=$((SCHLECHT + 1)) ;; esac
  done
  echo "ZAEHLER $GUT $SCHLECHT"
}
ZWISCHEN=$(mktemp -d)
for G in handy tablet; do ein_geraet $G "$@" > "$ZWISCHEN/$G" 2>&1 & done
wait
GUT=0; SCHLECHT=0
for G in handy tablet; do
  grep -v '^ZAEHLER ' "$ZWISCHEN/$G"
  Z=$(grep '^ZAEHLER ' "$ZWISCHEN/$G" | tail -1)
  # Ohne Zählerzeile ist der Lauf für dieses Gerät abgebrochen: alles als nicht installiert werten.
  if [ -n "$Z" ]; then GUT=$((GUT + $(echo "$Z" | cut -d' ' -f2))); SCHLECHT=$((SCHLECHT + $(echo "$Z" | cut -d' ' -f3))); else SCHLECHT=$((SCHLECHT + $#)); fi
done
rm -rf "$ZWISCHEN"
if [ $SCHLECHT -eq 0 ]; then echo "AUF_BEIDE_STATUS=fertig"; exit 0; fi
if [ $GUT -gt 0 ]; then echo "AUF_BEIDE_STATUS=teilweise ($SCHLECHT nicht installiert)"; exit 1; fi
echo "AUF_BEIDE_STATUS=fehler"; exit 1
