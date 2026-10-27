#!/bin/zsh
# Neustart ohne Passwort-Eingabe, Touch ID bleibt an: authentifizierter FileVault-Neustart (fdesetup authrestart),
# danach ist der Mac direkt angemeldet. Braucht eingeschaltetes FileVault.
#   Einmal einrichten:  sudo zsh neustart.sh --einrichten   (fragt das Mac-Passwort ab und legt es nur für root lesbar ab)
#   Neu starten:        neustart   (Alias in ~/.zshrc) oder  sudo zsh neustart.sh
set -e
[[ $EUID -eq 0 ]] || exec sudo zsh "$0" "$@"

benutzer="${SUDO_USER:-frank}"
ordner="/Library/Application Support/MacOhnePasswort"
datei="$ordner/authrestart.plist"

if [[ "$1" == "--einrichten" ]]; then
  read -rs "pw?Mac-Passwort von $benutzer: "; echo
  mkdir -p "$ordner"; chmod 700 "$ordner"
  umask 077
  pw=${pw//&/&amp;}; pw=${pw//</&lt;}; pw=${pw//>/&gt;}
  print -r -- "<?xml version=\"1.0\" encoding=\"UTF-8\"?>
<plist version=\"1.0\"><dict><key>Username</key><string>$benutzer</string><key>Password</key><string>$pw</string></dict></plist>" > "$datei"
  plutil -lint "$datei" >/dev/null
  chown root:wheel "$datei"; chmod 600 "$datei"
  echo "Gespeichert. Neu starten ab jetzt mit: neustart"
  exit 0
fi

[[ "$(fdesetup isactive)" == "true" ]] || { echo "FileVault ist aus: erst einschalten (sudo fdesetup enable)."; exit 1; }
[[ -f "$datei" ]] || { echo "Noch nicht eingerichtet: sudo zsh $0 --einrichten"; exit 1; }
fdesetup authrestart -inputplist < "$datei"
