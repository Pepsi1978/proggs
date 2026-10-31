#!/usr/bin/env bash
# Verbindet das Android-Handy per WLAN mit adb – mit Kabel und ohne.
# Reihenfolge: schon verbunden und antwortet? -> mDNS (findet das Handy in jedem Netz, auch nach IP-Wechsel)
# -> zuletzt bekannte IP -> WireGuard-Tunnel (fragt dort die aktuelle WLAN-IP ab und verbindet direkt, sonst bleibt der Tunnel) -> Kabel (schaltet WLAN-Debugging + Port 5555 ein).
# Gleicher Stand wie adb-wlan.ps1; nur die Direktverbindung über den PC-Hotspot (Schritt 6) gibt es am Mac nicht.
# Ohne Kabel wird nach einem Handy-Neustart Port 5555 über "Debugging über WLAN" (TLS) wiederhergestellt.
# Gebunden an die Seriennummer des Handys (gemerkt bei Kabelverbindung): fremde Geräte werden nie angesprochen.
# --leise: für den Wachhund (launchd), schreibt nur ins Log und startet den adb-Server nie neu.
# Einrichtung/Hintergrund: best-practices/android/adb-wlan-debugging.md
set -u
PORT=5555
STATE="$HOME/.adb-wlan-last-ip"
SERIAL_DATEI="$HOME/.adb-wlan-serial"
# WireGuard-Adresse des Handys im Second-Brain-Netz (10.8.0.0/24, Hub = VPS 10.8.0.1).
# Überschreibbar per Datei ~/.adb-wlan-tunnel-ip (leere Datei = Tunnelweg aus), dieselbe Datei wie unter Windows.
TUNNEL_DATEI="$HOME/.adb-wlan-tunnel-ip"
if [ -f "$TUNNEL_DATEI" ]; then TUNNEL_IP=$(tr -d '[:space:]' <"$TUNNEL_DATEI"); else TUNNEL_IP="10.8.0.7"; fi
LOGDIR="$HOME/Library/Logs/adb-wlan"; mkdir -p "$LOGDIR" 2>/dev/null || LOGDIR="$HOME"
LOG="$LOGDIR/adb-wlan.log"
LEISE=0; [ "${1:-}" = "--leise" ] && LEISE=1

# Immer dieselbe adb wie Gradle (SDK): verschiedene adb-Versionen beenden sich gegenseitig
# den Server, und dabei gehen alle WLAN-Verbindungen verloren.
for K in "${ADB:-}" "${ANDROID_HOME:-}/platform-tools/adb" "$HOME/Library/Android/sdk/platform-tools/adb" "$(command -v adb 2>/dev/null)"; do
  [ -n "$K" ] && [ -x "$K" ] && { ADB_BIN="$K"; break; }
done
[ -z "${ADB_BIN:-}" ] && { echo "FEHLER: adb nicht gefunden (weder \$ADB noch Android-SDK noch PATH)."; exit 1; }
adb() { "$ADB_BIN" "$@"; }

melde() { echo "$(date '+%d.%m.%Y %H:%M:%S') $*" >> "$LOG"; [ $LEISE -eq 0 ] && echo "$*"; return 0; }

# adb mit Zeitlimit (macOS hat kein "timeout"): adbzeit <sekunden> <adb-argumente…>
adbzeit() {
  local s=$1; shift
  local f; f=$(mktemp)
  adb "$@" >"$f" 2>&1 & local pid=$!
  ( sleep "$s"; kill "$pid" 2>/dev/null ) & local wd=$!
  if wait "$pid" 2>/dev/null; then kill "$wd" 2>/dev/null; wait "$wd" 2>/dev/null; tr -d '\r' <"$f"; rm -f "$f"; return 0; fi
  kill "$wd" 2>/dev/null; wait "$wd" 2>/dev/null; tr -d '\r' <"$f"; rm -f "$f"; return 1
}

ist_netz() { case "$1" in *:[0-9]*|*_adb-tls-connect*) return 0;; esac; return 1; }
bekannte_serial() { tr -d '[:space:]' <"$SERIAL_DATEI" 2>/dev/null; }
usb_geraet() { adb devices 2>/dev/null | awk 'NR>1 && $2=="device" && $1 !~ /:[0-9]+$/ && $1 !~ /_adb-tls-connect/ && $1 !~ /^emulator-/ {print $1; exit}'; }

# 0, wenn das Gerät antwortet UND (falls bekannt) unser Handy ist
ist_unser_handy() {
  local id soll; id=$(adbzeit 8 -s "$1" shell getprop ro.serialno | tr -d '[:space:]')
  [ -z "$id" ] && return 1
  soll=$(bekannte_serial)
  [ -z "$soll" ] || [ "$id" = "$soll" ]
}

netz_verbunden() {
  adb devices 2>/dev/null | awk 'NR>1 && $2=="device" {print $1}' | while read -r s; do
    ist_netz "$s" && ist_unser_handy "$s" && echo "$s"
  done
}

verbinde() {
  local out z i
  out=$(adbzeit 15 connect "$1"); melde "adb connect $1: $out"
  echo "$out" | grep -q "connected to" || return 1
  # "connected" heißt noch nicht freigegeben: auf Autorisierung warten und Identität prüfen
  for i in 1 2 3 4 5; do
    z=$(adb devices 2>/dev/null | awk -v s="$1" '$1==s {print $2}')
    if [ "$z" = "device" ]; then
      # Direkt nach der ersten Freigabe meldet adb kurz „still authorizing“ (gesehen über den Tunnel, 17.10.2026).
      ist_unser_handy "$1" && return 0
      sleep 2; ist_unser_handy "$1" && return 0
      sleep 3; ist_unser_handy "$1" && return 0
      melde "$1 ist nicht das gemerkte Handy – getrennt"; adb disconnect "$1" >/dev/null 2>&1; return 1
    fi
    [ "$z" = "unauthorized" ] && [ $i -eq 1 ] && melde "HINWEIS: Auf dem Handy 'Debugging zulassen' bestätigen (Haken 'Immer zulassen')."
    sleep 2
  done
  return 1
}

# Nie den TLS-Port und nie die Tunnel-Adresse speichern: nur die WLAN-IP des Handys.
merke_ip() { [ -n "$1" ] && [ "$1" != "$TUNNEL_IP" ] && echo "$1" > "$STATE"; return 0; }

# Handy-Seite dauerhaft vorbereiten (über Kabel ODER WLAN, höchstens stündlich; $2=sofort erzwingt):
# Berechtigung für UpdateStation + Ausnahme von der Doze-Drosselung. Idempotent.
PFLEGE_DATEI="$HOME/.adb-wlan-letzte-pflege"
pflege() {
  if [ "${2:-}" != "sofort" ] && [ -n "$(find "$PFLEGE_DATEI" -mmin -60 2>/dev/null)" ]; then return 0; fi
  local pk=de.frank.updatestation
  adbzeit 8 -s "$1" shell pm path "$pk" | grep -q "package:" || return 0
  adbzeit 8 -s "$1" shell pm grant "$pk" android.permission.WRITE_SECURE_SETTINGS >/dev/null
  if ! adbzeit 8 -s "$1" shell cmd deviceidle whitelist | grep -q "$pk"; then
    adbzeit 8 -s "$1" shell cmd deviceidle whitelist "+$pk" >/dev/null
    melde "UpdateStation von Doze-Drosselung ausgenommen"
  fi
  touch "$PFLEGE_DATEI"
}

stabilisiere() { # $1 TLS-Serial, $2 IP
  melde "Nur WLAN-Debugging (TLS) erreichbar – stelle Port $PORT wieder her"
  adbzeit 8 -s "$1" tcpip "$PORT" >/dev/null
  adb disconnect "$1" >/dev/null 2>&1   # TLS-Port wechselt beim adbd-Neustart ohnehin
  for _ in 1 2 3 4 5 6; do
    sleep 2
    verbinde "$2:$PORT" && return 0
  done
  melde "Port $PORT kam nicht zurück – nächster Lauf findet WLAN-Debugging wieder per mDNS"
  return 1
}

# mDNS: findet das Handy mit aktueller IP in jedem Netz (nur Dienste unseres Handys). 0 = verbunden.
mdns_verbinden() {
  local dienste="" z ip
  for _ in 1 2 3; do
    dienste=$(adb mdns services 2>/dev/null | grep -E $'\t_adb(-tls-connect)?\\._tcp\\.?\t')
    [ -n "$SOLL" ] && dienste=$(echo "$dienste" | grep -F "adb-$SOLL-")
    [ -n "$dienste" ] && break; sleep 2
  done
  for z in $(echo "$dienste" | awk -F'\t' '$2 ~ /^_adb\._tcp/ {print $3}'); do
    if verbinde "$z"; then merke_ip "${z%%:*}"; return 0; fi
  done
  for z in $(echo "$dienste" | awk -F'\t' '$2 ~ /^_adb-tls-connect/ {print $3}'); do
    if verbinde "$z"; then ip=${z%%:*}; merke_ip "$ip"; stabilisiere "$z" "$ip"; return 0; fi
  done
  return 1
}

# Über den Tunnel verbunden: das Handy nach seiner aktuellen WLAN-Adresse fragen und direkt verbinden.
# Hilft, wenn das Netz (z. B. ein Hotspot) eine neue Adresse vergeben hat und mDNS sie nicht liefert. 0 = direkt verbunden.
tunnel_umsteigen() {
  local ip
  ip=$(adbzeit 8 -s "$TUNNEL_IP:$PORT" shell ip -f inet addr show wlan0 | awk '/inet /{print $2}' | cut -d/ -f1 | head -1)
  [ -z "$ip" ] && return 1
  melde "Handy meldet über den Tunnel die WLAN-Adresse $ip – versuche direkt"
  verbinde "$ip:$PORT" || return 1
  merke_ip "$ip"
  adb disconnect "$TUNNEL_IP:$PORT" >/dev/null 2>&1
  melde "Direkt per WLAN verbunden ($ip) – Tunnelverbindung getrennt"
  return 0
}

# Parallele Aufrufe nicht gegeneinander laufen lassen
LOCK="${TMPDIR:-/tmp}/adb-wlan.lock"
for _ in $(seq 1 90); do mkdir "$LOCK" 2>/dev/null && break; sleep 1; done
aufraeumen() {
  # Log klein halten
  if [ -f "$LOG" ] && [ "$(wc -c 2>/dev/null <"$LOG" | tr -d ' ')" -gt 524288 2>/dev/null ]; then
    tail -n 2000 "$LOG" > "$LOG.tmp" && mv -f "$LOG.tmp" "$LOG"
  fi
  rmdir "$LOCK" 2>/dev/null
}
trap aufraeumen EXIT

adb start-server >/dev/null 2>&1

# Neuer Mac: Wachhund (LaunchAgent) beim ersten Aufruf selbst einrichten
if [ $LEISE -eq 0 ] && [ ! -f "$HOME/Library/LaunchAgents/de.frank.adb-wlan.wachhund.plist" ]; then
  bash "$(cd "$(dirname "$0")" && pwd)/wachhund-einrichten.sh" >/dev/null 2>&1 && melde "Wachhund de.frank.adb-wlan.wachhund eingerichtet"
fi

# Tote oder hängende Netz-Einträge räumen; antwortende fremde Geräte bleiben unberührt
adb devices 2>/dev/null | awk 'NR>1 && NF>=2 {print $1, $2}' | while read -r s z; do
  ist_netz "$s" || continue
  if [ "$z" != "device" ] || [ -z "$(adbzeit 8 -s "$s" shell echo ok)" ]; then
    adb disconnect "$s" >/dev/null 2>&1; melde "Toten Eintrag $s ($z) getrennt"
  fi
done

# Kabel dran: Handy dauerhaft vorbereiten (idempotent, startet adbd nicht neu)
USB=$(usb_geraet)
if [ -n "$USB" ]; then
  ID=$(adbzeit 8 -s "$USB" shell getprop ro.serialno | tr -d '[:space:]')
  [ -n "$ID" ] && echo "$ID" > "$SERIAL_DATEI"
  pflege "$USB" sofort
  [ "$(adbzeit 8 -s "$USB" shell settings get global adb_wifi_enabled | tr -d '[:space:]')" = "1" ] || \
    adbzeit 8 -s "$USB" shell settings put global adb_wifi_enabled 1 >/dev/null
fi
SOLL=$(bekannte_serial)

for RUNDE in 1 2; do
  # 1. Schon per WLAN verbunden und antwortet?
  NETZ=$(netz_verbunden)
  if [ -n "$NETZ" ]; then
    # Nur über den WireGuard-Tunnel verbunden: im Heim-WLAN direkt ist schneller -> umsteigen
    if [ -n "$TUNNEL_IP" ] && [ -z "$(echo "$NETZ" | grep -vxF "$TUNNEL_IP:$PORT")" ]; then
      if mdns_verbinden; then
        adb disconnect "$TUNNEL_IP:$PORT" >/dev/null 2>&1
        melde "Handy wieder im WLAN erreichbar – Tunnelverbindung getrennt"
      else
        tunnel_umsteigen
      fi
      break
    fi
    echo "$NETZ" | grep -qE "^[0-9.]+:$PORT$" && break
    T=$(echo "$NETZ" | head -1); IP=${T%%:*}
    if echo "$IP" | grep -qE '^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$'; then merke_ip "$IP"; stabilisiere "$T" "$IP"; fi
    break
  fi

  # 2. mDNS
  mdns_verbinden && break

  # 3. Zuletzt bekannte IP
  LETZTE=$(tr -d '[:space:]' <"$STATE" 2>/dev/null)
  [ -n "$LETZTE" ] && verbinde "$LETZTE:$PORT" && break

  # 3b. WireGuard-Tunnel (Second-Brain-Netz über den VPS): Handy unterwegs im Mobilnetz.
  #     Braucht WireGuard an auf Mac und Handy und Port 5555 (seit dem letzten Handy-Neustart).
  #     Danach nach der aktuellen WLAN-Adresse fragen: Ist das Handy im selben Netz, geht es direkt weiter (viel schneller).
  if [ -n "$TUNNEL_IP" ] && verbinde "$TUNNEL_IP:$PORT"; then
    melde "Verbunden über WireGuard-Tunnel $TUNNEL_IP:$PORT"
    tunnel_umsteigen
    break
  fi

  # 4. Kabel
  USB=$(usb_geraet)
  if [ -n "$USB" ]; then
    adbzeit 8 -s "$USB" shell svc wifi enable >/dev/null
    IP=""
    for _ in $(seq 1 15); do
      IP=$(adbzeit 8 -s "$USB" shell ip -f inet addr show wlan0 | awk '/inet /{print $2}' | cut -d/ -f1)
      [ -n "$IP" ] && break; sleep 2
    done
    [ -z "$IP" ] && { melde "FEHLER: Handy hat keine WLAN-IP (WLAN am Handy aus oder nicht verbunden)."; exit 1; }
    merke_ip "$IP"
    adbzeit 8 -s "$USB" tcpip "$PORT" >/dev/null; sleep 3
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

FERTIG=$(netz_verbunden | head -1)
if [ -n "$FERTIG" ]; then
  pflege "$FERTIG"
  [ $LEISE -eq 0 ] && adb devices -l
  exit 0
fi
melde "FEHLER: Handy per WLAN nicht gefunden. Prüfen: Handy im selben WLAN? 'Debugging über WLAN' an? Sonst einmal Kabel anstecken und dieses Skript erneut ausführen."
[ $LEISE -eq 0 ] && adb devices -l
exit 1
