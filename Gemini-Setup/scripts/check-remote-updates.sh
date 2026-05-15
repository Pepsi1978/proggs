#!/usr/bin/env bash
# check-remote-updates.sh (Gemini)
set -uo pipefail

REPO_ROOT="C:\Users\barwa\GeminiCLI"
cd "$REPO_ROOT"

echo "[$(date)] Pr├╝fe GitHub auf Updates..."

# Hintergrund-Fetch, um die Session nicht zu blockieren (max 5 Sek)
git fetch --quiet origin &
FETCH_PID=$!

# Kurzes Warten auf den Fetch
sleep 2

if ps -p $FETCH_PID > /dev/null; then
  echo "  (Fetch l├ñuft noch im Hintergrund...)"
else
  LOCAL=$(git rev-parse @)
  REMOTE=$(git rev-parse @{u} 2>/dev/null || echo "@")
  BASE=$(git merge-base @ @{u} 2>/dev/null || echo "@")

  if [ "$LOCAL" = "$REMOTE" ]; then
    echo "Ô£à Dein lokales Repo ist auf dem neuesten Stand."
  elif [ "$LOCAL" = "$BASE" ]; then
    echo "­ƒÆí Intelligenz-Vorschlag (Gemini): Neue Daten auf GitHub verf├╝gbar! -> Bitte f├╝hre 'git pull' aus, um die neuesten Erkenntnisse zu laden. ÔÇö Soll ich das tun?"
  elif [ "$REMOTE" = "$BASE" ]; then
    echo "  (Du hast lokale ├änderungen, die noch nicht gepusht wurden.)"
  else
    echo "  (Repo ist divergiert.)"
  fi
fi
