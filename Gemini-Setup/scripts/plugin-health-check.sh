#!/usr/bin/env bash
# plugin-health-check.sh (Gemini)
set +e

REPO_ROOT="C:\Users\barwa\GeminiCLI"
LOG_FILE="$REPO_ROOT/Gemini-Setup/logs/plugin-health.log"
mkdir -p "$(dirname "$LOG_FILE")"

echo "[$(date)] Starte Plugin-Check..." >> "$LOG_FILE"

# CHECK 1: Code-Search MCP Health
echo "Pr├╝fe Code-Search MCP..."
if node "$REPO_ROOT/Gemini-Setup/scripts/check-code-search-health.mjs" >> "$LOG_FILE" 2>&1; then
  echo "Ô£à Code-Search MCP ist gesund."
else
  echo "ÔÜá´©Å Code-Search MCP hat Probleme (siehe Log)."
fi

# CHECK 2: Ollama (f├╝r lokale Embeddings)
if curl -sf --connect-timeout 2 "http://localhost:11434/api/tags" >/dev/null 2>&1; then
  echo "Ô£à Ollama ist erreichbar." >> "$LOG_FILE"
else
  echo "ÔÜá´©Å Ollama ist nicht aktiv." >> "$LOG_FILE"
fi

echo "---" >> "$LOG_FILE"
exit 0
