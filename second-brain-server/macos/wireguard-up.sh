#!/bin/bash
# wireguard-up.sh — haelt den WireGuard-Tunnel zum Gehirn-Server DAUERHAFT oben.
# Wird vom LaunchDaemon de.frank.secondbrain.wireguard.plist als ROOT gestartet.
#
# Version 2.0.0 (27.08.2026, 20:48 Uhr) — wird beim Start ins Log geschrieben.
#
# ============================================================================
# WARUM DAS SKRIPT NICHT MEHR ZURUECKKEHRT (Root Cause, Direktive #3, 2026-08-27)
# ============================================================================
# Vorher war das ein EINMAL-Skript (plist: RunAtLoad, kein KeepAlive): Tunnel hoch,
# exit 0. Genau das hat den Tunnel nach jedem Neustart getoetet:
# launchd beendet beim Exit eines Jobs ALLE verbliebenen Prozesse seiner
# Prozessgruppe — und `wg-quick up` startet `wireguard-go` genau da hinein.
#
# Beweis aus dem Systemlog (Boot 27.08.2026 19:47:18):
#   19:47:45.134  launchd  [system/de.frank.secondbrain.wireguard [578]] exited due to exit(0)
#   19:47:45.136  kernel   utun4 detaching          <- 2 ms spaeter war der Tunnel weg
#   19:47:45.137  kernel   utun4 detached
# Danach meldete wg-drive-mount.sh im 5-Minuten-Takt nur noch
#   "SMB 445 (10.8.0.1) nicht erreichbar — Tunnel unten?"
# -> die Laufwerke gedanken/daten konnten nach einem Neustart NIE erscheinen.
#
# FIX (Fehlerklasse statt Symptom): Das Skript laeuft als Watchdog-DAUERSCHLEIFE.
#   1. Der Job endet nie -> launchd killt `wireguard-go` nie (Poka-Yoke Stufe 3:
#      der Prozessgruppen-Kill kann gar nicht mehr eintreten).
#   2. Die Schleife prueft alle 30 s die Gesundheit und baut den Tunnel selbst
#      wieder auf — das heilt zusaetzlich: Sleep/Wake, Client-IP-Wechsel
#      (Almanach samba-wireguard §13), wireguard-go-Crash, Netz beim Boot noch
#      nicht bereit.
#   3. Kommt der Tunnel (neu) hoch, wird der Mount-LaunchAgent des Benutzers sofort
#      angestossen -> die Laufwerke sind in Sekunden da, nicht erst nach bis zu 5 Min.
# Ergaenzend steht in der plist KeepAlive=true: stirbt das Skript selbst, startet
# launchd es neu und die Schleife baut den Tunnel wieder auf (zweite Schicht).
# ============================================================================
set +e

SCRIPT_VERSION="2.0.0 (27.08.2026, 20:48 Uhr)"
export PATH="/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin"

CONF="/Users/frank/SK/second-brain/wireguard/second-brain.conf"
LOG="/Users/frank/Library/Logs/wg-tunnel.log"
INTERVAL=30          # Sekunden zwischen zwei Gesundheitspruefungen
FAIL_LIMIT=4         # so viele Fehlschlaege in Folge (= ~2 Min) -> Tunnel neu aufbauen
MOUNT_AGENT="de.frank.secondbrain.drivemount"

log() { echo "$(date '+%Y-%m-%d %H:%M:%S')  $*" >>"$LOG" 2>/dev/null; }

# Log-Rotation: die Schleife laeuft dauerhaft, also darf das Log nicht unbegrenzt wachsen.
rotate_log() {
  if [ -f "$LOG" ] && [ "$(wc -c <"$LOG" 2>/dev/null || echo 0)" -gt 524288 ]; then
    mv -f "$LOG" "$LOG.1" 2>/dev/null
  fi
}

# Tunnel-IP des Mac aus der Konfig (nicht hart kodiert).
tunnel_ip() { grep -E '^ *Address' "$CONF" | head -1 | sed -E 's/.*= *([0-9.]+).*/\1/'; }

# Interface mit dieser inet-Adresse vorhanden?
iface_up() { ifconfig 2>/dev/null | grep -q "inet ${1} "; }

# Stale-Aufraeumung: wg-quick legt /var/run/wireguard/<name>.name an. Wurde wireguard-go
# hart getoetet (genau der alte Bug), bleibt die Datei mit einem laengst weggeraeumten
# utun-Namen zurueck und ein spaeteres `wg-quick down/up` verheddert sich daran.
clean_stale_state() {
  local name f utun
  name="$(basename "$CONF" .conf)"
  f="/var/run/wireguard/${name}.name"
  [ -f "$f" ] || return 0
  utun="$(cat "$f" 2>/dev/null)"
  if [ -z "$utun" ] || ! ifconfig "$utun" >/dev/null 2>&1; then
    log "Verwaiste Zustandsdatei ${f} (zeigt auf '${utun}', existiert nicht) — entfernt."
    rm -f "$f" "/var/run/wireguard/${utun}.sock" 2>/dev/null
  fi
}

# Mount-LaunchAgent des Benutzers sofort anstossen (laeuft in dessen GUI-Domain, nicht als root).
kick_mount_agent() {
  local owner uid
  owner="$(stat -f '%Su' "$CONF" 2>/dev/null)"
  [ -n "$owner" ] || return 0
  uid="$(id -u "$owner" 2>/dev/null)"
  [ -n "$uid" ] || return 0
  # BEWUSST ohne -k: ein gerade laufender Mount-Lauf soll nicht mitten im Mounten
  # abgeschossen werden (haette dessen Lock verwaist zurueckgelassen).
  launchctl kickstart "gui/${uid}/${MOUNT_AGENT}" >/dev/null 2>&1 \
    && log "Mount-Agent angestossen (gui/${uid}/${MOUNT_AGENT}) — Laufwerke kommen sofort." \
    || log "Mount-Agent nicht anstossbar (gui/${uid}/${MOUNT_AGENT}) — LaunchAgent-Takt uebernimmt."
}

bring_up() {
  clean_stale_state
  wg-quick down "$CONF" >>"$LOG" 2>&1   # idempotent aufraeumen, Fehler sind hier normal
  if wg-quick up "$CONF" >>"$LOG" 2>&1; then
    log "WireGuard-Tunnel hochgefahren ($CONF)"
    kick_mount_agent
    return 0
  fi
  log "WireGuard-Start FEHLGESCHLAGEN — naechster Versuch in ${INTERVAL}s (siehe Ausgabe oben)"
  return 1
}

rotate_log
log "wireguard-up Watchdog gestartet — Version ${SCRIPT_VERSION}, Prueftakt ${INTERVAL}s"

if [ ! -f "$CONF" ]; then
  # Kein Endlos-Spin bei fehlender Konfig: einmal melden, dann traege weiterpruefen
  # (die Konfig kann nachtraeglich auftauchen, z.B. wenn ~/SK noch nicht bereit war).
  log "WARNUNG: WireGuard-Konfig fehlt: $CONF — warte darauf."
fi

fails=0
last_state=""
while true; do
  rotate_log

  if [ ! -f "$CONF" ]; then
    [ "$last_state" = "noconf" ] || { log "WireGuard-Konfig fehlt weiterhin: $CONF"; last_state="noconf"; }
    sleep 60
    continue
  fi

  TUNIP="$(tunnel_ip)"
  if [ -z "$TUNIP" ]; then
    [ "$last_state" = "noip" ] || { log "FEHLER: keine Address-Zeile in $CONF gefunden."; last_state="noip"; }
    sleep 60
    continue
  fi
  SERVER="${TUNIP%.*}.1"   # Gehirn-Server ist die .1 im Tunnelnetz

  if ! iface_up "$TUNIP"; then
    log "Tunnel-Interface (${TUNIP}) fehlt — baue auf."
    # Bei Erfolg direkt auf "ok" setzen: bring_up hat den Mount-Agent schon angestossen,
    # der Gesundheits-Zweig unten soll das nicht gleich nochmal tun (kein Doppel-Kick).
    if bring_up; then last_state="ok"; else last_state="down"; fi
    fails=0
    sleep "$INTERVAL"
    continue
  fi

  # Interface da — aber lebt die Strecke? (toter Tunnel nach Sleep/Wake oder IP-Wechsel)
  # Zwei unabhaengige Proben: ICMP ODER der SMB-Port. Wird ICMP unterwegs mal gefiltert,
  # gilt der Tunnel trotzdem als gesund -> kein unnoetiger Neuaufbau (Flapping-Schutz).
  if ping -c 1 -W 2000 "$SERVER" >/dev/null 2>&1 || nc -z -G 3 "$SERVER" 445 >/dev/null 2>&1; then
    if [ "$last_state" != "ok" ]; then
      log "Tunnel gesund (${TUNIP} -> ${SERVER})."
      last_state="ok"
      kick_mount_agent
    fi
    fails=0
  else
    fails=$((fails + 1))
    log "Tunnel antwortet nicht (${SERVER}, Fehlschlag ${fails}/${FAIL_LIMIT})."
    if [ "$fails" -ge "$FAIL_LIMIT" ]; then
      log "Tunnel tot trotz vorhandenem Interface — baue neu auf."
      if bring_up; then last_state="ok"; else last_state="down"; fi
      fails=0
    fi
  fi

  sleep "$INTERVAL"
done
