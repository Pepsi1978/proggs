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

wlan_an() { networksetup -getairportpower "$dev" | grep -q ': On'; }
ohne_seit="$ordner/ohne-verbindung-seit"

# WLAN von Frank ausgeschaltet, schon verbunden oder anderes Netz aktiv (Kabel, USB-Tethering): nichts tun
wlan_an || exit 0
if verbunden; then rm -f "$ohne_seit"; exit 0; fi
standard=$(route -n get default 2>/dev/null | awk '/interface:/{print $2}')
[[ -n "$standard" && "$standard" != "$dev" ]] && exit 0

# Erst nach 60 s ohne Verbindung eingreifen, damit macOS nach Aufwecken oder Start selbst verbinden kann
mkdir -p "$ordner"
jetzt=$(date +%s)
[[ -f "$ohne_seit" ]] || echo "$jetzt" > "$ohne_seit"
(( jetzt - $(cat "$ohne_seit") >= 60 )) || exit 0

# Gezielte Suche nach dem Hotspot und Verbindung mit dem gespeicherten Zugang
antwort=$(networksetup -setairportnetwork "$dev" "$netz" 2>&1)
sleep 5
if verbunden; then
  echo "$(date '+%d.%m.%Y %H:%M:%S') verbunden über gezielte Suche" >> "$log"
  exit 0
fi
# Fehler, die nicht nur "Hotspot nicht in Reichweite" bedeuten, festhalten
[[ -n "$antwort" && "$antwort" != *"Could not find"* ]] && echo "$(date '+%d.%m.%Y %H:%M:%S') gezielte Suche: $antwort" >> "$log"

# Rückfall höchstens alle 2 Minuten: WLAN aus und an startet die automatische Verbindung von macOS neu
# (danach hat sich der Mac am 27.10.2026 binnen 9 s verbunden). Ohne Verbindung geht dabei nichts verloren.
stempel="$ordner/letzter-neustart"
zuletzt=$(cat "$stempel" 2>/dev/null || echo 0)
(( jetzt - zuletzt >= 120 )) || exit 0
if ! wlan_an || verbunden; then exit 0; fi
echo "$jetzt" > "$stempel"
echo "$(date '+%d.%m.%Y %H:%M:%S') WLAN-Neustart" >> "$log"
networksetup -setairportpower "$dev" off
sleep 2
networksetup -setairportpower "$dev" on
sleep 15
verbunden && echo "$(date '+%d.%m.%Y %H:%M:%S') verbunden nach WLAN-Neustart" >> "$log"
exit 0
