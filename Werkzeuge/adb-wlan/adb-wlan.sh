#!/usr/bin/env bash
# Verbindet das per USB angeschlossene Android-Handy zusätzlich per WLAN mit adb.
# Danach kann das Kabel ab. Ohne Kabel: versucht die zuletzt bekannte IP erneut.
set -u
PORT=5555
STATE="$HOME/.adb-wlan-last-ip"

USB=$(adb devices | awk 'NR>1 && $2=="device" && $1 !~ /:/ {print $1; exit}')

if [ -n "$USB" ]; then
  adb -s "$USB" shell svc wifi enable >/dev/null 2>&1
  IP=""
  for _ in $(seq 1 15); do
    IP=$(adb -s "$USB" shell ip -f inet addr show wlan0 2>/dev/null | awk '/inet /{print $2}' | cut -d/ -f1)
    [ -n "$IP" ] && break
    sleep 2
  done
  [ -z "$IP" ] && { echo "Handy hat keine WLAN-IP."; exit 1; }
  echo "$IP" > "$STATE"
  adb -s "$USB" tcpip "$PORT" >/dev/null
  sleep 3
else
  IP=$(cat "$STATE" 2>/dev/null || true)
  [ -z "$IP" ] && { echo "Kein USB-Gerät und keine bekannte IP. Einmal per Kabel anschließen."; exit 1; }
fi

if ! adb connect "$IP:$PORT" | grep -q "connected"; then
  # macOS blockiert den alten adb-Server manchmal im lokalen Netz ("No route to host")
  adb kill-server; adb start-server >/dev/null 2>&1; sleep 1
  adb connect "$IP:$PORT"
fi
adb devices -l
