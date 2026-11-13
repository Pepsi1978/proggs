#!/bin/zsh
# Macht das MacBook zum Dauerrechner für Jarvis (zugeklappt, Display aus, rund um die Uhr erreichbar). Mehrfach ausführbar.
#   zsh einrichten.sh          einrichten und prüfen (sudo ohne Passwort aus Werkzeuge/mac-ohne-passwort)
#   zsh einrichten.sh pruefen  nur prüfen
# Einrichten:
# - pmset: nie schlafen (auch zugeklappt: disablesleep), kein Standby, Display darf nach 10 min aus, Wake on LAN.
#   autorestart kennt ein MacBook nicht (pmset -g cap): Fällt der Strom aus, hält der Akku den Mac an; ist er leer, startet
#   ein Apple-Silicon-MacBook beim Wiederkommen des Stroms von selbst (AutoBoot, Werkseinstellung).
# - Alle LaunchAgents für Jarvis einrichten: Brücke (Aufträge, Victron), Vorwärmen, adb-Wachhund (Handy und Tablet).
# Prüfen: Auto-Anmeldung (Werkzeuge/mac-ohne-passwort, Dienst de.frank.mac-auto-anmelden), FileVault aus, Tunnel 10.8.0.6,
# Fernwartungsport 47800, Stromquelle.
set -u
PROGGS="$HOME/proggs"
ok() { print "OK      $1"; }
warn() { print "ACHTUNG $1"; }

if [[ "${1:-}" != pruefen ]]; then
  if sudo -n true 2>/dev/null; then
    sudo pmset -a sleep 0 standby 0 disablesleep 1 displaysleep 10 womp 1 tcpkeepalive 1
  else
    warn "sudo fragt nach dem Passwort: pmset übersprungen (erst Werkzeuge/mac-ohne-passwort/einrichten.sh)"
  fi
  bash "$PROGGS/Jarvis/werkzeuge/pc-bruecke/einrichten.sh" >/dev/null && ok "Jarvis-Brücke eingerichtet"
  bash "$PROGGS/Werkzeuge/vorwaermen/einrichten.sh" >/dev/null && ok "Vorwärmen eingerichtet"
  bash "$PROGGS/Werkzeuge/adb-wlan/wachhund-einrichten.sh" >/dev/null && ok "adb-Wachhund eingerichtet (Handy und Tablet)"
  /opt/homebrew/bin/python3 -c "import bleak, cryptography" 2>/dev/null \
    || /opt/homebrew/bin/python3 -m pip install --quiet --break-system-packages bleak cryptography
fi

[[ "$(pmset -g | awk '/SleepDisabled/{print $2}')" == 1 ]] && ok "Ruhezustand aus (auch zugeklappt)" || warn "Ruhezustand nicht abgeschaltet"
pmset -g batt | grep -q "AC Power" && ok "am Netzteil" || warn "läuft auf Akku: als Dauerrechner ans Netzteil (Vorwärmen baut auf Akku nur ab 30 % Ladung)"
fdesetup status | grep -q "Off" && ok "FileVault aus (Anmeldung nach Neustart ohne Passworteingabe möglich)" || warn "FileVault an: nach einem Neustart wartet der Mac auf das Passwort"
sudo -n launchctl print system/de.frank.mac-auto-anmelden >/dev/null 2>&1 && ok "Auto-Anmeldung aktiv" || warn "Auto-Anmeldung fehlt: sudo zsh $PROGGS/Werkzeuge/mac-ohne-passwort/anmeldung-einrichten.sh"
for d in de.frank.jarvis.pc-bruecke de.frank.vorwaermen de.frank.adb-wlan.wachhund de.frank.jarvis.fernsteuerung; do
  launchctl print "gui/$(id -u)/$d" >/dev/null 2>&1 && ok "LaunchAgent $d geladen" || warn "LaunchAgent $d fehlt"
done
ifconfig | grep -q "inet 10.8.0.6 " && ok "WireGuard-Tunnel 10.8.0.6 steht" || warn "WireGuard-Tunnel 10.8.0.6 fehlt (WireGuard-App, Tunnel „mac“, On-Demand an)"
nc -z -G2 127.0.0.1 47800 2>/dev/null && ok "Fernwartungsport 47800 offen" || warn "Fernwartungsport 47800 geschlossen"
[[ -f "$HOME/SK/Victron/victron.json" ]] && ok "Victron-Schlüssel vorhanden" || warn "~/SK/Victron/victron.json fehlt: Solarbatterie wird vom Mac nicht gemeldet"
