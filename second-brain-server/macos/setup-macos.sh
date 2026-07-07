#!/bin/bash
# setup-macos.sh — richtet den dauerhaften Auto-Start fuer die Gehirn-Laufwerke auf macOS ein.
# Pendant zu windows/wg-drive-reconnect.ps1 + geplanter Aufgabe.
#
# Zwei Teile:
#   USER-Teil  (ohne sudo): LaunchAgent fuer den SMB-Mount (Login + alle 5 Min) + Sofort-Mount.
#   DAEMON-Teil (mit sudo):  LaunchDaemon fuer den WireGuard-Tunnel beim Boot.
#
# Aufruf:
#   bash setup-macos.sh            -> richtet den USER-Teil ein und zeigt den sudo-Befehl fuer den Boot-Tunnel
#   sudo bash setup-macos.sh --daemon  -> richtet den Boot-Tunnel (LaunchDaemon) ein
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
AGENT_PLIST="de.frank.secondbrain.drivemount.plist"
DAEMON_PLIST="de.frank.secondbrain.wireguard.plist"

if [ "$1" = "--daemon" ]; then
  # ---- DAEMON-Teil (root) ----
  if [ "$(id -u)" -ne 0 ]; then echo "Bitte mit sudo aufrufen: sudo bash setup-macos.sh --daemon"; exit 1; fi
  install -m 644 -o root -g wheel "$DIR/$DAEMON_PLIST" "/Library/LaunchDaemons/$DAEMON_PLIST"
  launchctl bootout system "/Library/LaunchDaemons/$DAEMON_PLIST" 2>/dev/null || true
  launchctl bootstrap system "/Library/LaunchDaemons/$DAEMON_PLIST"
  echo "OK: WireGuard-Boot-Tunnel (LaunchDaemon) installiert + gestartet."
  exit 0
fi

# ---- USER-Teil (kein sudo) ----
if [ "$(id -u)" -eq 0 ]; then echo "Den USER-Teil bitte OHNE sudo ausfuehren (nur --daemon braucht sudo)."; exit 1; fi

chmod +x "$DIR/wg-drive-mount.sh" "$DIR/wireguard-up.sh"

AGENTS_DIR="$HOME/Library/LaunchAgents"
mkdir -p "$AGENTS_DIR"
cp -f "$DIR/$AGENT_PLIST" "$AGENTS_DIR/$AGENT_PLIST"

# (Neu) laden
launchctl bootout "gui/$(id -u)/de.frank.secondbrain.drivemount" 2>/dev/null || true
launchctl bootstrap "gui/$(id -u)" "$AGENTS_DIR/$AGENT_PLIST" 2>/dev/null \
  || launchctl load -w "$AGENTS_DIR/$AGENT_PLIST"

# Sofort einmal mounten (falls Tunnel schon steht)
"$DIR/wg-drive-mount.sh" || true

echo "OK: Mount-LaunchAgent installiert (Login + alle 5 Min)."
echo
echo "Fuer den Boot-Tunnel (einmalig, braucht Admin-Passwort):"
echo "  sudo bash \"$DIR/setup-macos.sh\" --daemon"
