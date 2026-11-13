#!/usr/bin/env bash
# Verbindet das Android-Handy per WLAN mit adb – mit Kabel und ohne.
# Reihenfolge: schon verbunden und antwortet? -> mDNS (findet das Handy in jedem Netz, auch nach IP-Wechsel)
# -> zuletzt bekannte IP -> WireGuard-Tunnel (fragt dort die aktuelle WLAN-IP ab und verbindet direkt, sonst bleibt der Tunnel) -> Kabel (schaltet WLAN-Debugging + Port 5555 ein).
# Gleicher Stand wie adb-wlan.ps1; nur die Direktverbindung über den PC-Hotspot (Schritt 6) gibt es am Mac nicht,
# und nur hier gibt es Schritt 3c (TLS-Portsuche durch den Tunnel; der PC findet das Gerät im Heimnetz per mDNS,
# unterwegs holt der Server-Wachhund Port 5555 zurück).
# Ohne Kabel wird nach einem Handy-Neustart Port 5555 über "Debugging über WLAN" (TLS) wiederhergestellt.
# Gebunden an die Seriennummer des Handys (gemerkt bei Kabelverbindung): fremde Geräte werden nie angesprochen.
# --leise: für den Wachhund (launchd), schreibt nur ins Log und startet den adb-Server nie neu.
# --geraet handy|tablet|alle (Vorgabe handy): Die Geräte stehen mit fester Seriennummer in geraete.json
#   (Fold 8 = handy, Tab S9 = tablet); so verdrängt ein angestecktes Tablet nie das Handy und umgekehrt.
# Einrichtung/Hintergrund: best-practices/android/adb-wlan-debugging.md
set -u
# Mac: zusätzlich den adb-Schlüssel des Servers anbieten. Das Tablet zeigt über den Tunnel keine Freigabefrage für neue Rechner,
# kennt aber den Server-Schlüssel (13.11.2026). Wirkt beim Start des adb-Servers; fehlt die Datei, ändert sich nichts.
[ -f "$HOME/SK/Android/adbkey-server" ] && export ADB_VENDOR_KEYS="${ADB_VENDOR_KEYS:-$HOME/SK/Android/adbkey-server}"
LEISE=0; GERAET=handy
while [ $# -gt 0 ]; do
  case "$1" in
    --leise) LEISE=1 ;;
    --geraet) GERAET="${2:-handy}"; [ $# -gt 1 ] && shift ;;
  esac
  shift
done
# Hostinger-Server (erkannt am tmux-Dienst der Sitzungen): kein Heim-WLAN in Reichweite, nur der Tunnel
# (überschreibbar per ADB_WLAN_NUR_TUNNEL=0). Andere Linux-Rechner (WSL, Laptop) bleiben im normalen Ablauf.
[ -z "${ADB_WLAN_NUR_TUNNEL:-}" ] && [ "$(uname -s)" = Linux ] && [ -f /etc/systemd/system/jarvis-tmux.service ] && ADB_WLAN_NUR_TUNNEL=1
NUR_TUNNEL=${ADB_WLAN_NUR_TUNNEL:-0}
HIER="$(cd "$(dirname "$0")" && pwd)"
GERAETE_DATEI="$HIER/geraete.json"
# reg <gerät> <feld>: liest ein Feld aus geraete.json (ein Feld je Zeile, wie im Repo abgelegt)
reg() {
  awk -v g="\"$1\":" -v f="\"$2\":" '$1==g {drin=1; next} drin && /}/ {exit} drin && $1==f {gsub(/[",\r]/,"",$2); print $2; exit}' "$GERAETE_DATEI" 2>/dev/null
}
reg_namen() { awk '/^  "[a-z0-9]+": *\{/ {gsub(/[":]/,"",$1); print $1}' "$GERAETE_DATEI" 2>/dev/null; }
if [ "$GERAET" = "alle" ]; then
  # Wachhund: jedes eingetragene Gerät nacheinander; der Rückgabewert richtet sich nach dem Handy.
  CODE=0
  for N in $(reg_namen); do
    if [ $LEISE -eq 1 ]; then bash "$0" --leise --geraet "$N"; else bash "$0" --geraet "$N"; fi
    R=$?; [ "$N" = handy ] && CODE=$R
  done
  exit $CODE
fi
FEST_SERIAL=$(reg "$GERAET" serial)
if [ -z "$FEST_SERIAL" ] && [ "$GERAET" != handy ]; then echo "FEHLER: Gerät '$GERAET' steht nicht in $GERAETE_DATEI."; exit 3; fi
# Seriennummern der anderen eingetragenen Geräte: gelten nie als dieses Gerät.
FREMDE_SERIALS=$(for N in $(reg_namen); do [ "$N" != "$GERAET" ] && reg "$N" serial; done)
ZUSATZ=""; [ "$GERAET" != handy ] && ZUSATZ="-$GERAET"
PORT=5555
STATE="$HOME/.adb-wlan${ZUSATZ}-last-ip"
SERIAL_DATEI="$HOME/.adb-wlan-serial"
# WireGuard-Adresse des Geräts im Second-Brain-Netz (10.8.0.0/24, Hub = VPS 10.8.0.1): Handy 10.8.0.7, Tablet 10.8.0.8.
# Fürs Handy überschreibbar per Datei ~/.adb-wlan-tunnel-ip (leere Datei = Tunnelweg aus), dieselbe Datei wie unter Windows.
TUNNEL_DATEI="$HOME/.adb-wlan-tunnel-ip"
if [ "$GERAET" = handy ] && [ -f "$TUNNEL_DATEI" ]; then TUNNEL_IP=$(tr -d '[:space:]' <"$TUNNEL_DATEI")
else TUNNEL_IP=$(reg "$GERAET" tunnelIp); [ -z "$TUNNEL_IP" ] && [ "$GERAET" = handy ] && TUNNEL_IP="10.8.0.7"; fi
LOGDIR="$HOME/Library/Logs/adb-wlan"; mkdir -p "$LOGDIR" 2>/dev/null || LOGDIR="$HOME"
LOG="$LOGDIR/adb-wlan.log"

# Immer dieselbe adb wie Gradle (SDK): verschiedene adb-Versionen beenden sich gegenseitig
# den Server, und dabei gehen alle WLAN-Verbindungen verloren.
for K in "${ADB:-}" "${ANDROID_HOME:-}/platform-tools/adb" "$HOME/Library/Android/sdk/platform-tools/adb" "$(command -v adb 2>/dev/null)"; do
  [ -n "$K" ] && [ -x "$K" ] && { ADB_BIN="$K"; break; }
done
[ -z "${ADB_BIN:-}" ] && { echo "FEHLER: adb nicht gefunden (weder \$ADB noch Android-SDK noch PATH)."; exit 3; }
adb() { "$ADB_BIN" "$@"; }

melde() {
  local t="$*"; [ -n "$ZUSATZ" ] && t="[$GERAET] $t"
  echo "$(date '+%d.%m.%Y %H:%M:%S') $t" >> "$LOG"; [ $LEISE -eq 0 ] && echo "$t"; return 0
}

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
bekannte_serial() { if [ -n "$FEST_SERIAL" ]; then echo "$FEST_SERIAL"; else tr -d '[:space:]' <"$SERIAL_DATEI" 2>/dev/null; fi; }
ist_fremd() { [ -n "$1" ] && echo "$FREMDE_SERIALS" | grep -qxF "$1"; }
# Per Kabel zählt nur das Zielgerät: mit fester Nummer genau dieses, sonst (neues Handy) jedes nicht anders eingetragene.
usb_geraet() {
  adb devices 2>/dev/null | awk 'NR>1 && $2=="device" && $1 !~ /:[0-9]+$/ && $1 !~ /_adb-tls-connect/ && $1 !~ /^emulator-/ {print $1}' | while read -r s; do
    if [ -n "$FEST_SERIAL" ]; then [ "$s" = "$FEST_SERIAL" ] || continue; else ist_fremd "$s" && continue; fi
    echo "$s"; break
  done
}

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
      # Das andere eingetragene Gerät (Handy bzw. Tablet) sitzt dort zu Recht: verbunden lassen.
      if ist_fremd "$(adbzeit 8 -s "$1" shell getprop ro.serialno | tr -d '[:space:]')"; then melde "$1 ist ein anderes eingetragenes Gerät – bleibt verbunden"; return 1; fi
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
PFLEGE_DATEI="$HOME/.adb-wlan${ZUSATZ}-letzte-pflege"
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

STABILISIERT=0
stabilisiere() { # $1 TLS-Serial, $2 IP
  STABILISIERT=1
  melde "Nur WLAN-Debugging (TLS) erreichbar – stelle Port $PORT wieder her"
  adbzeit 8 -s "$1" tcpip "$PORT" >/dev/null
  adb disconnect "$1" >/dev/null 2>&1   # TLS-Port wechselt beim adbd-Neustart ohnehin
  for _ in 1 2 3 4 5 6; do
    sleep 2
    verbinde "$2:$PORT" && return 0
  done
  if [ "$2" = "$TUNNEL_IP" ]; then melde "Port $PORT kam nicht zurück – nächster Lauf sucht WLAN-Debugging erneut über den Tunnel"
  else melde "Port $PORT kam nicht zurück – nächster Lauf findet WLAN-Debugging wieder per mDNS"; fi
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

# Über den Tunnel den TLS-Port von „Debugging über WLAN“ finden und Port 5555 zurückholen. 0 = verbunden.
# Die volle Suche (tls-port-suche.py, meldet nur Ports, die wie adbd antworten) lässt adbd kurz hängen und kostet im Mobilnetz
# Daten. Direkt nach einem Neustart ist WLAN-Debugging oft erst Minuten später an (Entsperren, WLAN, UpdateStation; gemessen
# 13.11.2026 am Tablet: erste Suche zu früh). Deshalb die ersten 4 erfolglosen Suchen alle 2 Minuten, danach wachsender
# Abstand (5, 10, 20, 40, höchstens 60 Minuten). Ein Erfolg setzt ihn zurück.
# Gemerkt werden Ports nur, wenn die Verbindung danach scheiterte (nach „adb tcpip“ wählt adbd ohnehin einen neuen Port).
TLS_PORTS_DATEI="$HOME/.adb-wlan${ZUSATZ}-tls-ports"
TLS_SUCHE_MARKE="$HOME/.adb-wlan${ZUSATZ}-tls-suche"
TLS_ABSTAND_DATEI="$HOME/.adb-wlan${ZUSATZ}-tls-abstand"
tls_ports_versuchen() { # $1 Ports; 0 = verbunden, 1 = keiner antwortet, 2 = tcpip lief, 5555 kam nicht (alte Ports sind dann ungültig)
  local p
  for p in $1; do
    if verbinde "$TUNNEL_IP:$p"; then stabilisiere "$TUNNEL_IP:$p" "$TUNNEL_IP" && return 0; return 2; fi
    adb disconnect "$TUNNEL_IP:$p" >/dev/null 2>&1
  done
  return 1
}
tls_erfolg() { rm -f "$TLS_PORTS_DATEI" "$TLS_ABSTAND_DATEI" "$TLS_SUCHE_MARKE"; return 0; }
tunnel_tls_verbinden() {
  local py ports code runde r alt abstand
  alt=$(tr -d '\r' 2>/dev/null <"$TLS_PORTS_DATEI")
  if [ -n "$alt" ]; then
    rm -f "$TLS_PORTS_DATEI"
    tls_ports_versuchen "$alt"; r=$?
    [ $r -eq 0 ] && { tls_erfolg; return 0; }
    [ $r -eq 2 ] && return 1
  fi
  # Datei hält die Zahl der erfolglosen Suchen in Folge; daraus der Abstand in Minuten.
  local fehl; fehl=$(tr -dc '0-9' 2>/dev/null <"$TLS_ABSTAND_DATEI"); [ -z "$fehl" ] && fehl=0
  if [ "$fehl" -lt 4 ]; then abstand=2; else abstand=$((5 << (fehl - 4))); [ $abstand -gt 60 ] && abstand=60; fi
  [ "$fehl" -gt 0 ] && [ -n "$(find "$TLS_SUCHE_MARKE" -mmin -"$abstand" 2>/dev/null)" ] && return 1
  for py in python3 python; do command -v "$py" >/dev/null 2>&1 && "$py" -c "import asyncio" >/dev/null 2>&1 && break; py=""; done
  [ -z "$py" ] && { melde "TLS-Suche übersprungen: kein Python gefunden"; return 1; }
  ports=$("$py" "$HIER/tls-port-suche.py" "$TUNNEL_IP" 2>&1); code=$?
  ports=$(echo "$ports" | tr -d '\r')
  [ $code -eq 2 ] && return 1   # Gerät antwortet im Tunnel gar nicht (aus oder WireGuard weg): keine Suche, keine Marke
  touch "$TLS_SUCHE_MARKE"
  if [ $code -ne 0 ]; then melde "TLS-Suche fehlgeschlagen (Exit $code): $(echo "$ports" | tail -1)"; return 1; fi
  if [ -z "$ports" ]; then
    fehl=$((fehl + 1)); [ $fehl -gt 20 ] && fehl=20; echo "$fehl" > "$TLS_ABSTAND_DATEI"
    if [ "$fehl" -lt 4 ]; then abstand=2; else abstand=$((5 << (fehl - 4))); [ $abstand -gt 60 ] && abstand=60; fi
    melde "Tunnel $TUNNEL_IP: Port $PORT zu, kein adb-TLS-Port (WLAN-Debugging aus oder Gerät nicht im WLAN) – nächste Suche in $abstand Minuten"
    return 1
  fi
  melde "Tunnel $TUNNEL_IP: Port $PORT zu, adb-TLS-Port $(echo $ports) – verbinde"
  # Direkt nach der Suche scheitert „adb connect“ („failed to connect“): adbd hängt danach bis über eine Minute
  # (gemessen 13.11.2026 am Tablet: Versuche nach 20, 40 und 60 s gescheitert, nach 90 s sofort verbunden).
  # Deshalb wachsende Pausen, zusammen gut 2 Minuten.
  for runde in 5 25 45 60; do
    sleep "$runde"
    tls_ports_versuchen "$ports"; r=$?
    [ $r -eq 0 ] && { tls_erfolg; return 0; }
    [ $r -eq 2 ] && return 1
  done
  echo "$ports" > "$TLS_PORTS_DATEI"
  return 1
}

# Parallele Aufrufe nicht gegeneinander laufen lassen
LOCK="${TMPDIR:-/tmp}/adb-wlan.lock"
HABE_SPERRE=0
# Wachhund 90 s, direkte Aufrufe (Installation) bis 5 Minuten: Ein Lauf mit TLS-Suche dauert länger als 90 s.
WARTE_SPERRE=90; [ $LEISE -eq 0 ] && WARTE_SPERRE=300
for _ in $(seq 1 $WARTE_SPERRE); do
  if mkdir "$LOCK" 2>/dev/null; then echo $$ > "$LOCK/pid"; HABE_SPERRE=1; break; fi
  # Verwaiste Sperre (Lauf hart beendet): atomar beiseiteschieben (mv gelingt nur einem Wartenden), dann neu versuchen.
  # ps statt kill -0: kill -0 scheitert auch bei lebenden Prozessen anderer Benutzer.
  ALT_PID=$(cat "$LOCK/pid" 2>/dev/null)
  if { [ -n "$ALT_PID" ] && ! ps -p "$ALT_PID" >/dev/null 2>&1; } || \
     { [ -z "$ALT_PID" ] && [ -n "$(find "$LOCK" -maxdepth 0 -mmin +10 2>/dev/null)" ]; }; then
    mv "$LOCK" "$LOCK.alt.$$" 2>/dev/null && rm -rf "$LOCK.alt.$$"
    continue
  fi
  sleep 1
done
if [ $HABE_SPERRE -ne 1 ]; then
  # Ein anderer Lauf arbeitet noch: nicht parallel weitermachen, aber ehrlich melden, ob das Gerät schon da ist.
  if [ -n "$(netz_verbunden | head -1)" ]; then echo "adb-wlan läuft schon – Gerät ist verbunden"; exit 0; fi
  echo "adb-wlan läuft schon (Sperre $LOCK) – Gerät noch nicht verbunden"; exit 1
fi
aufraeumen() {
  # Log klein halten
  if [ -f "$LOG" ] && [ "$(wc -c 2>/dev/null <"$LOG" | tr -d ' ')" -gt 524288 2>/dev/null ]; then
    tail -n 2000 "$LOG" > "$LOG.tmp" && mv -f "$LOG.tmp" "$LOG"
  fi
  [ $HABE_SPERRE -eq 1 ] && rm -rf "$LOCK"
}
trap aufraeumen EXIT

adb start-server >/dev/null 2>&1

# Tunnel-Adresse verbunden, aber nicht freigegeben: Warten und alle weiteren Wege ändern daran nichts (kostete 1:41, 13.11.2026).
if [ -n "$TUNNEL_IP" ] && [ "$(adb devices 2>/dev/null | awk -v s="$TUNNEL_IP:$PORT" '$1==s {print $2}')" = "unauthorized" ]; then
  melde "FEHLER: $TUNNEL_IP:$PORT ist verbunden, aber nicht freigegeben (unauthorized). Am Gerät 'Debugging zulassen' mit 'Immer zulassen' bestätigen."
  # Neu verbinden lässt die Freigabe-Abfrage am Gerät wieder erscheinen; im Hintergrund, damit niemand darauf wartet.
  # Höchstens alle 30 Minuten, sonst erscheint die Abfrage beim Minuten-Wachhund jede Minute neu.
  FRAGE_MARKE="$HOME/.adb-wlan${ZUSATZ}-freigabe-frage"
  if [ -z "$(find "$FRAGE_MARKE" -mmin -30 2>/dev/null)" ]; then
    touch "$FRAGE_MARKE"
    ( adb disconnect "$TUNNEL_IP:$PORT"; sleep 1; adb connect "$TUNNEL_IP:$PORT" ) >/dev/null 2>&1 </dev/null &
  fi
  exit 1
fi

# Neuer Mac: Wachhund (LaunchAgent) beim ersten Aufruf selbst einrichten. Auch ein schon eingerichteter, der noch nur das Handy
# kennt (ohne „--geraet alle“), wird einmal neu eingerichtet. Nur am Mac (launchd).
WACHHUND_DATEI="$HOME/Library/LaunchAgents/de.frank.adb-wlan.wachhund.plist"
if [ $LEISE -eq 0 ] && [ "$(uname -s)" = Darwin ] && { [ ! -f "$WACHHUND_DATEI" ] || ! grep -q '<string>alle</string>' "$WACHHUND_DATEI"; }; then
  bash "$HIER/wachhund-einrichten.sh" >/dev/null 2>&1 && melde "Wachhund de.frank.adb-wlan.wachhund eingerichtet"
fi

# Tote oder hängende Netz-Einträge räumen; antwortende fremde Geräte bleiben unberührt
adb devices 2>/dev/null | awk 'NR>1 && NF>=2 {print $1, $2}' | while read -r s z; do
  ist_netz "$s" || continue
  # Server: nur nicht verbundene Einträge räumen. Eine Shell-Probe wartet über den langsamen Tunnel hinter laufenden
  # Installationen, und das Trennen bräche sie ab; ein wirklich toter Eintrag wird von adb selbst „offline“.
  if [ "$z" != "device" ] || { [ "$NUR_TUNNEL" != 1 ] && [ -z "$(adbzeit 8 -s "$s" shell echo ok)" ]; }; then
    adb disconnect "$s" >/dev/null 2>&1; melde "Toten Eintrag $s ($z) getrennt"
  fi
done

# Kabel dran: Handy dauerhaft vorbereiten (idempotent, startet adbd nicht neu)
USB=$(usb_geraet)
if [ -n "$USB" ]; then
  ID=$(adbzeit 8 -s "$USB" shell getprop ro.serialno | tr -d '[:space:]')
  [ -n "$ID" ] && [ -z "$FEST_SERIAL" ] && echo "$ID" > "$SERIAL_DATEI"
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
      # Server (ADB_WLAN_NUR_TUNNEL=1): kein Heim-WLAN in Reichweite, der Tunnel ist dort der einzige Weg.
      [ "$NUR_TUNNEL" = 1 ] && break
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

  # 2. mDNS (nicht auf dem Server: dort gibt es kein Heim-WLAN)
  [ "$NUR_TUNNEL" = 1 ] || { mdns_verbinden && break; }

  # 3. Zuletzt bekannte IP
  LETZTE=$(tr -d '[:space:]' 2>/dev/null <"$STATE")
  [ -n "$LETZTE" ] && [ "$NUR_TUNNEL" != 1 ] && verbinde "$LETZTE:$PORT" && break

  # 3b. WireGuard-Tunnel (Second-Brain-Netz über den VPS): Handy unterwegs im Mobilnetz.
  #     Braucht WireGuard an auf Mac und Handy und Port 5555 (seit dem letzten Handy-Neustart).
  #     Danach nach der aktuellen WLAN-Adresse fragen: Ist das Handy im selben Netz, geht es direkt weiter (viel schneller).
  if [ -n "$TUNNEL_IP" ] && verbinde "$TUNNEL_IP:$PORT"; then
    melde "Verbunden über WireGuard-Tunnel $TUNNEL_IP:$PORT"
    [ "$NUR_TUNNEL" = 1 ] || tunnel_umsteigen
    break
  fi

  # 3c. Gerät neu gestartet: Im Tunnel ist nur noch „Debugging über WLAN“ (TLS) auf einem Zufallsport offen
  #     (UpdateStation schaltet es nach dem Start im WLAN ein). mDNS reicht nicht durch den Tunnel, deshalb
  #     Ports direkt suchen, per TLS verbinden und Port 5555 wiederherstellen. So klappt es auch ohne PC (Server-Wachhund).
  if [ -n "$TUNNEL_IP" ] && tunnel_tls_verbinden; then break; fi

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
  #    Auch nie für ein Zweitgerät: Das Tablet ist oft einfach aus, und der Neustart trennte dann grundlos das Handy.
  #    Nie auf dem Server: Dort teilen sich alle Sitzungen einen adb-Server.
  if [ "$RUNDE" = 1 ] && [ $LEISE -eq 0 ] && [ -z "$ZUSATZ" ] && [ "$NUR_TUNNEL" != 1 ]; then
    melde "Keine Verbindung – starte adb-Server neu und versuche es erneut"
    adb kill-server; adb start-server >/dev/null 2>&1; sleep 3
  else
    break
  fi
done

FERTIG=$(netz_verbunden | head -1)
# Direkt nach „adb tcpip“ antwortet die erste Shell über den Tunnel manchmal erst nach Sekunden: einmal nachfassen.
[ -z "$FERTIG" ] && [ $STABILISIERT -eq 1 ] && { sleep 4; FERTIG=$(netz_verbunden | head -1); }
# Ein Zweitgerät ist oft einfach aus: Der Wachhund meldet das nur beim Wechsel, nicht alle zwei Minuten.
WEG_MARKE="$HOME/.adb-wlan${ZUSATZ}-nicht-erreichbar"
if [ -n "$FERTIG" ]; then
  rm -f "$WEG_MARKE"
  pflege "$FERTIG"
  [ $LEISE -eq 0 ] && adb devices -l
  exit 0
fi
if [ $LEISE -eq 1 ] && [ -n "$ZUSATZ" ]; then
  [ -f "$WEG_MARKE" ] || { melde "nicht erreichbar (aus, anderes Netz oder WLAN-Debugging aus)"; touch "$WEG_MARKE"; }
  exit 1
fi
melde "FEHLER: Handy per WLAN nicht gefunden. Prüfen: Handy im selben WLAN? 'Debugging über WLAN' an? Sonst einmal Kabel anstecken und dieses Skript erneut ausführen."
[ $LEISE -eq 0 ] && adb devices -l
exit 1
