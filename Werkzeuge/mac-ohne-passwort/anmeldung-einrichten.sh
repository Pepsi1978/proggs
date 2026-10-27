#!/bin/zsh
# Richtet die Anmeldung ohne Passwort ein (Einschalten, Neustart, Sperrbildschirm), Touch ID bleibt an:
# Passwort einmal ablegen (nur root lesbar), Apples Bildschirmfreigabe einschalten, Dienst auto-anmelden.rb starten.
#   sudo zsh anmeldung-einrichten.sh
#   sudo zsh anmeldung-einrichten.sh --zurueck   (Dienst entfernen, Passwort löschen)
set -e
[[ $EUID -eq 0 ]] || exec sudo zsh "$0" "$@"
hier="${0:A:h}"
ordner="/Library/Application Support/MacOhnePasswort"
plist="/Library/LaunchDaemons/de.frank.mac-auto-anmelden.plist"

if [[ "$1" == "--zurueck" ]]; then
  launchctl bootout system "$plist" 2>/dev/null || true
  rm -f "$plist" "$ordner/auto-anmelden.rb" "$ordner/authrestart.plist"
  echo "Entfernt: Der Mac fragt wieder nach dem Passwort."
  exit 0
fi

[[ -f "$ordner/authrestart.plist" ]] || zsh "$hier/neustart.sh" --einrichten

# Bildschirmfreigabe (nimmt der Sperrbildschirm im Gegensatz zu anderen Programmen an)
launchctl enable system/com.apple.screensharing
launchctl bootstrap system /System/Library/LaunchDaemons/com.apple.screensharing.plist 2>/dev/null || true

# Skript nach root-eigenem Ort kopieren, damit der root-Dienst nichts ausführt, was der Benutzer ändern kann.
install -m 700 -o root -g wheel "$hier/auto-anmelden.rb" "$ordner/auto-anmelden.rb"
install -m 644 -o root -g wheel "$hier/de.frank.mac-auto-anmelden.plist" "$plist"
launchctl bootout system "$plist" 2>/dev/null || true
launchctl bootstrap system "$plist"
sleep 1
/usr/bin/ruby "$ordner/auto-anmelden.rb" --pruefen
