#!/bin/bash
# wg-endpoint-monitor.sh — protokolliert Endpoint-Wechsel der WireGuard-Peers auf dem VPS.
#
# Zweck (Direktive #3 / observability-first, 2026-06-29): Beim Vorfall am 2026-06-29 21:19
# flappte der Tunnel zu Franks Mac (60 % Paketverlust), die SMB-Laufwerke fielen ab. Der VPS
# war nachweislich kerngesund (sar: CPU idle 98 %, eth0 0 Fehler, kein OOM) -> der Verlust
# entstand auf der Strecke/Client-Seite. Da der Mac-Endpoint eine DYNAMISCHE Telekom-IP ist,
# lautet die Haupthypothese: kurzer IP-/NAT-Wechsel der Internetleitung. Diese Hypothese liess
# sich nachtraeglich nicht mehr beweisen (Spuren verfluechtigt). Dieser Monitor schliesst die
# Luecke: er protokolliert JEDEN Endpoint-Wechsel mit Zeitstempel -> beim naechsten Vorfall ist
# beweisbar, OB und WANN die Client-IP gewechselt hat.
#
# Laeuft per systemd-Timer alle 30 s. Reines Logging, keine Aenderung am Tunnel. exit 0 immer.
set +e

IFACE="wg0"
LOG="/var/log/wg-endpoint-monitor.log"
STATE_DIR="/run/wg-endpoint-monitor"
mkdir -p "$STATE_DIR" 2>/dev/null

command -v wg >/dev/null 2>&1 || exit 0
wg show "$IFACE" >/dev/null 2>&1 || exit 0

NOW=$(date '+%Y-%m-%d %H:%M:%S')

# Map: pubkey -> allowed-ip (zur lesbaren Peer-Benennung), pubkey -> endpoint
# wg show <if> endpoints:     <pubkey>\t<ip:port|(none)>
# wg show <if> allowed-ips:   <pubkey>\t<10.8.0.x/32 ...>
while IFS=$'\t' read -r peer endpoint; do
  [ -z "$peer" ] && continue
  [ "$endpoint" = "(none)" ] && continue
  allowed=$(wg show "$IFACE" allowed-ips 2>/dev/null | awk -v p="$peer" '$1==p{print $2}')
  # kurzer, stabiler Dateiname pro Peer
  key=$(printf '%s' "$peer" | tr -c 'A-Za-z0-9' '_' | cut -c1-24)
  statefile="$STATE_DIR/$key"
  last=$(cat "$statefile" 2>/dev/null)
  if [ "$endpoint" != "$last" ]; then
    if [ -n "$last" ]; then
      echo "$NOW  ENDPOINT-WECHSEL  peer=${allowed:-$peer}  alt='${last}'  neu='${endpoint}'" >> "$LOG"
    else
      echo "$NOW  ENDPOINT-START   peer=${allowed:-$peer}  endpoint='${endpoint}'" >> "$LOG"
    fi
    printf '%s' "$endpoint" > "$statefile"
  fi
done < <(wg show "$IFACE" endpoints 2>/dev/null)

# Log-Rotation: ab ~1 MB neu anfangen
if [ -f "$LOG" ] && [ "$(wc -c <"$LOG" 2>/dev/null || echo 0)" -gt 1048576 ]; then
  mv -f "$LOG" "$LOG.1" 2>/dev/null
fi
exit 0
