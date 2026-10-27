#!/bin/zsh
# Läuft alle 30 s als root-Dienst. macOS gibt die Suche nach bekannten Netzen nach rund 5 Minuten ohne WLAN auf
# und sucht erst nach Aufwecken oder WLAN-Einschalten wieder. Dieser Dienst sucht weiter, solange der Mac mit
# keinem Netz verbunden ist, und verbindet mit dem Hotspot, sobald er auftaucht. Andere Netze bleiben unberührt.
netz="Borussia Dortmund"
ordner="/Library/Application Support/HotspotSuche"
log="/Library/Logs/hotspot-suche.log"
dev=$(networksetup -listallhardwareports | awk '/Wi-Fi/{getline; print $2; exit}')
[[ -n "$dev" ]] || exit 0

verbunden() { ifconfig "$dev" | grep -q 'status: active'; }

# WLAN von Frank ausgeschaltet oder schon verbunden: nichts tun
networksetup -getairportpower "$dev" | grep -q ': On' || exit 0
verbunden && exit 0

# Gezielte Suche nach dem Hotspot und Verbindung mit dem gespeicherten Zugang
networksetup -setairportnetwork "$dev" "$netz" >/dev/null 2>&1
sleep 5
if verbunden; then
  echo "$(date '+%d.%m.%Y %H:%M:%S') verbunden über gezielte Suche" >> "$log"
  exit 0
fi

# Rückfall höchstens alle 2 Minuten: WLAN aus und an startet die automatische Verbindung von macOS neu
# (danach hat sich der Mac am 27.10.2026 binnen 9 s verbunden). Ohne Verbindung geht dabei nichts verloren.
mkdir -p "$ordner"
stempel="$ordner/letzter-neustart"
jetzt=$(date +%s)
zuletzt=$(cat "$stempel" 2>/dev/null || echo 0)
(( jetzt - zuletzt >= 120 )) || exit 0
echo "$jetzt" > "$stempel"
networksetup -setairportpower "$dev" off
sleep 2
networksetup -setairportpower "$dev" on
sleep 15
verbunden && echo "$(date '+%d.%m.%Y %H:%M:%S') verbunden nach WLAN-Neustart" >> "$log"
exit 0
