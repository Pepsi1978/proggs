#!/bin/zsh
# Richtet den Dienst hotspot-suche ein: Der Mac sucht ohne WLAN-Verbindung dauerhaft weiter nach dem Hotspot.
#   sudo zsh einrichten.sh
#   sudo zsh einrichten.sh --zurueck   (Dienst entfernen)
set -e
[[ $EUID -eq 0 ]] || exec sudo zsh "$0" "$@"
hier="${0:A:h}"
ordner="/Library/Application Support/HotspotSuche"
plist="/Library/LaunchDaemons/de.frank.hotspot-suche.plist"

if [[ "$1" == "--zurueck" ]]; then
  launchctl bootout system "$plist" 2>/dev/null || true
  rm -rf "$plist" "$ordner" /Library/Logs/hotspot-suche.log
  echo "Entfernt: macOS sucht wieder nur nach eigenem Ermessen."
  exit 0
fi

# Skript nach root-eigenem Ort kopieren, damit der root-Dienst nichts ausführt, was der Benutzer ändern kann.
mkdir -p "$ordner"
install -m 700 -o root -g wheel "$hier/hotspot-suche.sh" "$ordner/hotspot-suche.sh"
install -m 644 -o root -g wheel "$hier/de.frank.hotspot-suche.plist" "$plist"
launchctl bootout system "$plist" 2>/dev/null || true
# bootout läuft asynchron: warten, bis der alte Dienst weg ist, sonst scheitert bootstrap mit Fehler 5
for i in {1..20}; do launchctl print system/de.frank.hotspot-suche >/dev/null 2>&1 || break; sleep 0.5; done
launchctl bootstrap system "$plist"
echo "Eingerichtet: hotspot-suche läuft alle 30 s."
