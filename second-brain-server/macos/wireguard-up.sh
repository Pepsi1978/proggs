#!/bin/bash
# wireguard-up.sh — startet den WireGuard-Tunnel zum Gehirn-Server beim Boot.
# Wird vom LaunchDaemon de.frank.secondbrain.wireguard.plist als ROOT aufgerufen.
#
# Setzt den PATH explizit (LaunchDaemons erben kein Homebrew-PATH) und faehrt den
# Tunnel idempotent hoch: ist er schon oben, passiert nichts.
set +e

export PATH="/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin"

CONF="/Users/frank/SK/second-brain/wireguard/second-brain.conf"
LOG="/Users/frank/Library/Logs/wg-tunnel.log"

log() { echo "$(date '+%Y-%m-%d %H:%M:%S')  $*" >>"$LOG" 2>/dev/null; }

if [ ! -f "$CONF" ]; then
  log "FEHLER: WireGuard-Konfig fehlt: $CONF"
  exit 0
fi

# Schon ein Tunnel ins Gehirn-Netz aktiv? -> nichts tun. (IP aus der Konfig, nicht hart kodiert)
TUNIP=$(grep -E "^Address" "$CONF" | head -1 | sed -E "s/.*= *([0-9.]+).*/\1/")
if [ -n "$TUNIP" ] && ifconfig 2>/dev/null | grep -q "inet ${TUNIP} "; then
  log "Tunnel bereits aktiv (${TUNIP}) — ok"
  exit 0
fi

if wg-quick up "$CONF" >>"$LOG" 2>&1; then
  log "WireGuard-Tunnel hochgefahren ($CONF)"
else
  log "WireGuard-Start meldete einen Fehler (evtl. schon aktiv) — siehe oben"
fi
exit 0
