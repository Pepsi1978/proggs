#!/usr/bin/env bash
# Verbindet das Android-Handy per WLAN mit adb – mit Kabel und ohne.
# Reihenfolge: schon verbunden? -> mDNS (findet das Handy in jedem Netz, auch nach IP-Wechsel)
# -> zuletzt bekannte IP -> Kabel (schaltet WLAN-Debugging + Port 5555 ein).
# Ohne Kabel wird nach einem Handy-Neustart Port 5555 über "Debugging über WLAN" (TLS) wiederhergestellt.
# --leise: für den Wachhund (launchd), schreibt nur ins Log und startet den adb-Server nie neu.
# Einrichtung/Hintergrund: best-practices/android/adb-wlan-debugging.md
set -u
PORT=5555
STATE="$HOME/.adb-wlan-last-ip"
LOG="$HOME/Library/Logs/adb-wlan.log"; [ -d "$HOME/Library/Logs" ] || LOG="$HOME/.adb-wlan.log"
LEISE=0; [ "${1:-}" = "--leise" ] && LEISE=1

melde() { echo "$(date '+%d.%m.%Y %H:%M:%S') $*" >> "$LOG"; [ $LEISE -eq 0 ] && echo "$*"; }
netz_verbunden() { adb devices 2>/dev/null | awk 'NR>1 && $2=="device" && ($1 ~ /:[0-9]+$/ || $1 ~ /_adb-tls-connect/) {print $1}'; }
usb_geraet() { adb devices 2>/dev/null | awk 'NR>1 && $2=="device" && $1 !~ /:[0-9]+$/ && $1 !~ /^emulator-|_adb-tls/ {print $1; exit}'; }
verbinde() { local out; out=$(adb connect "$1" 2>&1); melde "adb connect $1: $out"; echo "$out" | grep -qE "connected to|already connected"; }
merke_ip() { [ -n "$1" ] && echo "$1" > "$STATE"; }
stabilisiere() { # $1 TLS-Serial, $2 IP
  melde "Nur WLAN-Debugging (TLS) erreichbar – stelle Port $PORT wieder her"
  adb -s "$1" tcpip "$PORT" >/dev/null 2>&1; sleep 4
  adb disconnect "$1" >/dev/null 2>&1
  verbinde "$2:$PORT"
}

# Parallele Aufrufe nicht gegeneinander laufen lassen
LOCK="${TMPDIR:-/tmp}/adb-wlan.lock"
for _ in $(seq 1 90); do mkdir "$LOCK" 2>/dev/null && break; sleep 1; done
trap 'rmdir "$LOCK" 2>/dev/null' EXIT

adb start-server >/dev/null 2>&1

# Hängende Netz-Einträge räumen
adb devices 2>/dev/null | awk 'NR>1 && $2!="device" && ($1 ~ /:[0-9]+$/ || $1 ~ /_adb-tls-connect/) {print $1}' | while read -r s; do
  adb disconnect "$s" >/dev/null 2>&1; melde "Hängenden Eintrag $s getrennt"
done

# Kabel dran: Handy dauerhaft vorbereiten (idempotent)
USB=$(usb_geraet)
if [ -n "$USB" ]; then
  adb -s "$USB" shell settings put global adb_wifi_enabled 1 >/dev/null 2>&1
  adb -s "$USB" shell pm grant de.frank.updatestation android.permission.WRITE_SECURE_SETTINGS >/dev/null 2>&1
fi

for RUNDE in 1 2; do
  # 1. Schon per WLAN verbunden?
  NETZ=$(netz_verbunden)
  if [ -n "$NETZ" ]; then
    echo "$NETZ" | grep -qE "^[0-9.]+:$PORT$" && break
    T=$(echo "$NETZ" | head -1); IP=${T%%:*}
    if echo "$IP" | grep -qE '^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$'; then merke_ip "$IP"; stabilisiere "$T" "$IP"; fi
    break
  fi

  # 2. mDNS
  DIENSTE=""
  for _ in 1 2 3; do
    DIENSTE=$(adb mdns services 2>/dev/null | grep -E $'\t_adb(-tls-connect)?\\._tcp\\.?\t')
    [ -n "$DIENSTE" ] && break; sleep 2
  done
  OK=0
  for Z in $(echo "$DIENSTE" | awk -F'\t' '$2 ~ /^_adb\._tcp/ {print $3}'); do
    if verbinde "$Z"; then merke_ip "${Z%%:*}"; OK=1; break; fi
  done
  if [ $OK -eq 0 ]; then
    for Z in $(echo "$DIENSTE" | awk -F'\t' '$2 ~ /^_adb-tls-connect/ {print $3}'); do
      if verbinde "$Z"; then IP=${Z%%:*}; merke_ip "$IP"; sleep 1; stabilisiere "$Z" "$IP"; OK=1; break; fi
    done
  fi
  [ $OK -eq 1 ] && break

  # 3. Zuletzt bekannte IP
  LETZTE=$(cat "$STATE" 2>/dev/null | tr -d '[:space:]')
  [ -n "$LETZTE" ] && verbinde "$LETZTE:$PORT" && break

  # 4. Kabel
  USB=$(usb_geraet)
  if [ -n "$USB" ]; then
    adb -s "$USB" shell svc wifi enable >/dev/null 2>&1
    IP=""
    for _ in $(seq 1 15); do
      IP=$(adb -s "$USB" shell ip -f inet addr show wlan0 2>/dev/null | awk '/inet /{print $2}' | cut -d/ -f1)
      [ -n "$IP" ] && break; sleep 2
    done
    [ -z "$IP" ] && { melde "FEHLER: Handy hat keine WLAN-IP (WLAN am Handy aus oder nicht verbunden)."; exit 1; }
    merke_ip "$IP"
    adb -s "$USB" tcpip "$PORT" >/dev/null; sleep 3
    verbinde "$IP:$PORT" && break
  fi

  # 5. Einmal adb-Server neu starten (macOS: "No route to host"). Nie im Wachhund.
  if [ "$RUNDE" = 1 ] && [ $LEISE -eq 0 ]; then
    melde "Keine Verbindung – starte adb-Server neu und versuche es erneut"
    adb kill-server; adb start-server >/dev/null 2>&1; sleep 3
  else
    break
  fi
done

if [ -n "$(netz_verbunden)" ]; then
  [ $LEISE -eq 0 ] && adb devices -l
  exit 0
fi
melde "FEHLER: Handy per WLAN nicht gefunden. Prüfen: Handy im selben WLAN? 'Debugging über WLAN' an? Sonst einmal Kabel anstecken und dieses Skript erneut ausführen."
[ $LEISE -eq 0 ] && adb devices -l
exit 1
