#!/bin/zsh
# Einmalig: Token anlegen (falls fehlt), Dienst bauen, Autostart eintragen und starten.
# Danach den Token aufs Handy: adb -s <IP>:5555 shell "run-as de.frank.jarvis sh -c 'cat > files/fernsteuerung-token.txt'" < ~/SK/Jarvis/fernsteuerung-token.txt
set -e
cd "${0:A:h}"
t="$HOME/SK/Jarvis/fernsteuerung-token.txt"
[ -f "$t" ] || { openssl rand -hex 24 > "$t"; chmod 600 "$t"; }
./bauen.sh
cp de.frank.jarvis.fernsteuerung.plist ~/Library/LaunchAgents/
launchctl bootout gui/$(id -u)/de.frank.jarvis.fernsteuerung 2>/dev/null || true
launchctl bootstrap gui/$(id -u) ~/Library/LaunchAgents/de.frank.jarvis.fernsteuerung.plist
# RunAtLoad im LaunchAgent startet den Dienst (starten.command), hier nicht noch einmal.
