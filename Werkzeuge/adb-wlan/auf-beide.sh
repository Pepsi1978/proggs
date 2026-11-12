#!/usr/bin/env bash
# Installiert eine APK auf Handy UND Tablet (jeweils über die feste Seriennummer aus geraete.json, Netz vor Kabel).
# Standardweg für jede App-Installation (Profilregel „Android-Installation über WLAN“): Was fürs Handy gilt, gilt
# genauso fürs Tablet, UpdateStation ist nur der Rückweg. Die abgeglichenen Apps brauchen ohnehin auf beiden Geräten
# dieselbe Fassung. Nicht für Jarvis selbst (dafür jarvis-install mit --ziel); Jarvis-APKs werden abgelehnt.
# Windows: mit Git Bash starten ("C:\Program Files\Git\bin\bash.exe"), nie mit WSL-bash.
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
for APK in "$@"; do
  case "$APK" in
    */Jarvis/app/*|*\\Jarvis\\app\\*|*[Jj]arvis*.apk) echo "AUF_BEIDE_STATUS=fehler (Jarvis nie hierüber, sondern mit jarvis-install und Ziel handy/tablet: $APK)"; exit 2 ;;
  esac
  [ -f "$APK" ] || { echo "AUF_BEIDE_STATUS=fehler (APK fehlt: $APK)"; exit 2; }
done
# Git Bash: /c/…-Pfade erreichen adb.exe sonst teils unverwandelt („failed to stat /c/…“), daher in Windows-Form.
if command -v cygpath >/dev/null 2>&1; then N=(); for APK in "$@"; do N+=("$(cygpath -m "$APK")"); done; set -- "${N[@]}"; fi
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
# Projektname aus dem Pfad (…/<Projekt>/app/build/…), sonst der Dateiname.
bezeichnung() {
  local p="${1//\\//}"
  case "$p" in */app/build/*) p="${p%%/app/build/*}"; echo "${p##*/}" ;; *) basename "$p" ;; esac
}
# Erst ALLE Adressen klären und fehlende Geräte nacheinander neu verbinden: adb-wlan kann den adb-Server neu starten
# und würde sonst eine parallel laufende Installation auf dem anderen Gerät abbrechen.
for G in handy tablet; do
  if [ -z "$(adresse "$(serie $G)")" ]; then
    if [ -f "$HIER/adb-wlan.sh" ] && [ "$(uname -s | cut -c1-5)" != "MINGW" ] && [ "$(uname -s | cut -c1-4)" != "MSYS" ]; then bash "$HIER/adb-wlan.sh" --geraet $G >/dev/null 2>&1
    else powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$(cygpath -w "$HIER/adb-wlan.ps1" 2>/dev/null || echo "$HIER/adb-wlan.ps1")" -Geraet $G >/dev/null 2>&1; fi
  fi
done
# Nach den Neuverbindungen alle Adressen frisch lesen (ein Server-Neustart kann auch das erste Gerät neu verbunden haben).
ADR_handy=$(adresse "$(serie handy)")
ADR_tablet=$(adresse "$(serie tablet)")
# Beide Geräte gleichzeitig: Die Installationen laufen über getrennte Verbindungen und halbieren so die Wartezeit
# (gemessen 10.11.2026: fünf APKs nacheinander auf beide Geräte 3 bis 6 Minuten).
ein_geraet() {
  local G=$1 A=$2; shift 2
  local GUT=0 SCHLECHT=0 APK AUS
  if [ -z "$A" ]; then echo "$G: nicht erreichbar"; echo "ZAEHLER 0 $#"; return; fi
  for APK in "$@"; do
    AUS=$("$ADB" -s "$A" install -r "$APK" </dev/null 2>&1 | tr -d '\r' | tail -1)
    echo "$G ($A): $(bezeichnung "$APK") → $AUS"
    case "$AUS" in
      Success*) GUT=$((GUT + 1)) ;;
      *INSTALL_FAILED_UPDATE_INCOMPATIBLE*) SCHLECHT=$((SCHLECHT + 1)); echo "$G: andere Signatur installiert – NIE deinstallieren (Datenverlust), siehe best-practices/android/debug-signing.md" ;;
      *) SCHLECHT=$((SCHLECHT + 1)) ;;
    esac
  done
  echo "ZAEHLER $GUT $SCHLECHT"
}
ZWISCHEN=$(mktemp -d)
ein_geraet handy "$ADR_handy" "$@" > "$ZWISCHEN/handy" 2>&1 &
ein_geraet tablet "$ADR_tablet" "$@" > "$ZWISCHEN/tablet" 2>&1 &
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
