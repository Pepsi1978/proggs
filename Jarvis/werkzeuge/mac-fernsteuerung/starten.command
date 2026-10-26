#!/bin/zsh
# Startet den Fernsteuerungs-Dienst im Hintergrund und hält ihn am Leben. Muss aus Terminal.app heraus laufen
# (Doppelklick, LaunchAgent über "open -a Terminal" oder aus einer Terminal-Sitzung), denn nur so erbt er dessen
# Rechte für Bildschirmaufnahme und Bedienungshilfen.
bin="$HOME/Library/Application Support/JarvisFernsteuerung/jarvis-fernsteuerung"
logdir="$HOME/Library/Logs/JarvisFernsteuerung"; mkdir -p "$logdir"
# Muster verankert: sonst trifft pgrep auch fremde Befehlszeilen, in denen der Name nur vorkommt.
dienst_laeuft() { pgrep -f "^zsh -c while true; do \"$bin\"" >/dev/null; }
weiterleitung_laeuft() { pgrep -f "^zsh -c while true; do /usr/bin/ruby .*weiterleitung\.rb" >/dev/null; }
if dienst_laeuft && weiterleitung_laeuft; then echo "läuft bereits"; exit 0; fi
if ! dienst_laeuft; then
  nohup zsh -c "while true; do \"$bin\" >> \"$logdir/dienst.log\" 2>&1; sleep 3; done" >/dev/null 2>&1 &
  disown
fi
# Weiterleitung aus dem Tunnel (siehe weiterleitung.rb)
rb="${0:A:h}/weiterleitung.rb"
if ! weiterleitung_laeuft; then
  nohup zsh -c "while true; do /usr/bin/ruby \"$rb\" >> \"$logdir/weiterleitung.log\" 2>&1; sleep 3; done" >/dev/null 2>&1 &
  disown
fi
echo "Fernsteuerung gestartet"
