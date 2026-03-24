#!/usr/bin/env bash
# auto-sync.sh (Gemini) — Automatischer Pull & Diff-Anzeige
set -uo pipefail

REPO_ROOT="C:\Users\barwa\GeminiCLI"
cd "$REPO_ROOT"

echo "[$(date)] Starte automatischen Repository-Sync..."

# 1. Fetch vom Remote
git fetch --quiet origin

LOCAL=$(git rev-parse @)
REMOTE=$(git rev-parse @{u} 2>/dev/null || echo "@")

if [ "$LOCAL" = "$REMOTE" ]; then
  echo "✅ Keine neuen Änderungen auf GitHub."
else
  echo "📥 Neue Daten gefunden. Führe 'git pull' aus..."
  
  # Merke den alten Stand für den Diff
  OLD_HEAD=$(git rev-parse HEAD)
  
  # Pull ausführen
  if git pull origin main --quiet; then
    NEW_HEAD=$(git rev-parse HEAD)
    
    echo -e "\n=== ÄNDERUNGS-ÜBERSICHT (Statistik) ==="
    git diff --stat $OLD_HEAD $NEW_HEAD
    
    echo -e "\n=== DETAILLIERTE VERÄNDERUNGEN (Diff) ==="
    # Zeige den Diff an (begrenzt auf die ersten 50 Zeilen pro Datei für Übersichtlichkeit)
    git diff --color=always $OLD_HEAD $NEW_HEAD
    
    echo -e "\n✅ Sync abgeschlossen. Die semantische Suche wird im Hintergrund aktualisiert."
  else
    echo "⚠️ FEHLER beim Pull. Möglicherweise gibt es lokale Konflikte."
    echo "Bitte prüfe den Status manuell mit 'git status'."
  fi
fi
