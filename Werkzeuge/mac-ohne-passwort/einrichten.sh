#!/bin/zsh
# Mac ohne Passwort-Abfragen: sudo ohne Passwort und keine Admin-Fenster mehr (Systemeinstellungen, Installationen,
# Hilfsprogramme). Einmal ausführen mit:  sudo zsh einrichten.sh
# Rückgängig:                             sudo zsh einrichten.sh --zurueck
# Nach großen macOS-Updates erneut ausführen, Apple setzt die Rechte dabei manchmal zurück.
set -e
[[ $EUID -eq 0 ]] || { echo "Bitte mit sudo starten: sudo zsh $0"; exit 1; }

benutzer="${SUDO_USER:-frank}"
sudoers="/etc/sudoers.d/ohne-passwort"
sicherung="/Library/Application Support/MacOhnePasswort"
rechte=(
  system.preferences system.preferences.security system.preferences.energysaver system.preferences.accounts
  system.preferences.network system.preferences.sharing system.preferences.printing system.preferences.datetime
  system.preferences.startupdisk system.preferences.softwareupdate
  system.install.software system.privilege.admin
)

if [[ "$1" == "--zurueck" ]]; then
  rm -f "$sudoers"
  for r in $rechte; do
    [[ -f "$sicherung/$r.plist" ]] && security authorizationdb write "$r" < "$sicherung/$r.plist" 2>/dev/null
  done
  echo "Zurückgesetzt: sudo und Admin-Fenster fragen wieder nach dem Passwort."
  exit 0
fi

# 1. sudo ohne Passwort; erst prüfen, dann einsetzen, damit eine kaputte Datei sudo nie aussperrt.
tmp=$(mktemp)
echo "$benutzer ALL=(ALL) NOPASSWD: ALL" > "$tmp"
visudo -cf "$tmp" >/dev/null
install -m 440 -o root -g wheel "$tmp" "$sudoers"
rm -f "$tmp"
echo "sudo: ohne Passwort für $benutzer"

# 2. Admin-Fenster: Rechte auf "allow" setzen, Originale vorher einmal sichern.
mkdir -p "$sicherung"
for r in $rechte; do
  [[ -f "$sicherung/$r.plist" ]] || security authorizationdb read "$r" > "$sicherung/$r.plist" 2>/dev/null || true
  security authorizationdb write "$r" allow 2>/dev/null && echo "ohne Abfrage: $r" || echo "nicht änderbar: $r"
done
echo "Fertig."
