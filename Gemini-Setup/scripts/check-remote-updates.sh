#!/usr/bin/env bash
# check-remote-updates.sh (Gemini)
set -uo pipefail

REPO_ROOT="C:\Users\barwa\GeminiCLI"
cd "$REPO_ROOT"

echo "[$(date)] Prüfe GitHub auf Updates..."

# Hintergrund-Fetch, um die Session nicht zu blockieren (max 5 Sek)
git fetch --quiet origin &
FETCH_PID=$!

# Kurzes Warten auf den Fetch
sleep 2

if ps -p $FETCH_PID > /dev/null; then
  echo "  (Fetch läuft noch im Hintergrund...)"
else
  LOCAL=$(git rev-parse @)
  REMOTE=$(git rev-parse @{u} 2>/dev/null || echo "@")
  BASE=$(git merge-base @ @{u} 2>/dev/null || echo "@")

  if [ "$LOCAL" = "$REMOTE" ]; then
    echo "✅ Dein lokales Repo ist auf dem neuesten Stand."
  elif [ "$LOCAL" = "$BASE" ]; then
    echo "💡 Intelligenz-Vorschlag (Gemini): Neue Daten auf GitHub verfügbar! -> Bitte führe 'git pull' aus, um die neuesten Erkenntnisse zu laden. — Soll ich das tun?"
  elif [ "$REMOTE" = "$BASE" ]; then
    echo "  (Du hast lokale Änderungen, die noch nicht gepusht wurden.)"
  else
    echo "  (Repo ist divergiert.)"
  fi
fi
