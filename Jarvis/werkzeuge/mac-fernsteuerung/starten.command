#!/bin/zsh
# Startet den Fernsteuerungs-Dienst im Hintergrund und hält ihn am Leben. Muss aus Terminal.app heraus laufen
# (Doppelklick, LaunchAgent über "open -a Terminal" oder aus einer Terminal-Sitzung), denn nur so erbt er dessen
# Rechte für Bildschirmaufnahme und Bedienungshilfen.
bin="$HOME/Library/Application Support/JarvisFernsteuerung/jarvis-fernsteuerung"
logdir="$HOME/Library/Logs/JarvisFernsteuerung"; mkdir -p "$logdir"
if pgrep -f "JarvisFernsteuerung/jarvis-fernsteuerung" >/dev/null; then echo "läuft bereits"; exit 0; fi
nohup zsh -c "while true; do \"$bin\" >> \"$logdir/dienst.log\" 2>&1; sleep 3; done" >/dev/null 2>&1 &
disown
echo "Fernsteuerung gestartet"
