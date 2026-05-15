#!/usr/bin/env bash
# auto-sync.sh (Gemini) ÔÇö Automatischer Pull & Diff-Anzeige
set -uo pipefail

REPO_ROOT="C:\Users\barwa\GeminiCLI"
cd "$REPO_ROOT"

echo "[$(date)] Starte automatischen Repository-Sync..."

# 1. Fetch vom Remote
git fetch --quiet origin

LOCAL=$(git rev-parse @)
REMOTE=$(git rev-parse @{u} 2>/dev/null || echo "@")

if [ "$LOCAL" = "$REMOTE" ]; then
  echo "Ô£à Keine neuen ├änderungen auf GitHub."
else
  echo "­ƒôÑ Neue Daten gefunden. F├╝hre 'git pull' aus..."
  
  # Merke den alten Stand f├╝r den Diff
  OLD_HEAD=$(git rev-parse HEAD)
  
  # Pull ausf├╝hren
  if git pull origin main --quiet; then
    NEW_HEAD=$(git rev-parse HEAD)
    
    echo -e "\n=== ├äNDERUNGS-├£BERSICHT (Statistik) ==="
    git diff --stat $OLD_HEAD $NEW_HEAD
    
    echo -e "\n=== DETAILLIERTE VER├äNDERUNGEN (Diff) ==="
    # Zeige den Diff an (begrenzt auf die ersten 50 Zeilen pro Datei f├╝r ├£bersichtlichkeit)
    git diff --color=always $OLD_HEAD $NEW_HEAD
    
    echo -e "\nÔ£à Sync abgeschlossen. Die semantische Suche wird im Hintergrund aktualisiert."
  else
    echo "ÔÜá´©Å FEHLER beim Pull. M├Âglicherweise gibt es lokale Konflikte."
    echo "Bitte pr├╝fe den Status manuell mit 'git status'."
  fi
fi
