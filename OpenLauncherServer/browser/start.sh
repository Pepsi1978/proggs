#!/bin/bash
# Unsichtbarer Bildschirm :99 mit Chrome, sichtbar über VNC (nur localhost; die Konsole reicht ihn hinter ihrer Anmeldung durch).
# Läuft als tmux-Sitzung „browser“, gestartet von launcher.browser_starten. Endet ein Teil, endet alles, und der nächste Bedarf startet neu.
export DISPLAY=:99 LANG=de_DE.UTF-8 LANGUAGE=de TZ=Europe/Berlin
trap 'kill $(jobs -p) 2>/dev/null' EXIT
rm -f /tmp/.X99-lock
Xvfb :99 -screen 0 1280x800x24 -nolisten tcp &
sleep 1
openbox &
x11vnc -display :99 -localhost -rfbport 5900 -shared -forever -nopw -quiet -noxrecord -ncache 0 &
google-chrome-stable --no-first-run --no-default-browser-check --password-store=basic --disable-dev-shm-usage \
  --remote-debugging-port=9222 --user-data-dir="$HOME/.openlauncher/chrome" --window-position=0,0 --window-size=1280,800 about:blank &
wait -n
