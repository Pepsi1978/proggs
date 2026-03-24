#!/usr/bin/env bash
# plugin-health-check.sh (Gemini)
set +e

REPO_ROOT="C:\Users\barwa\GeminiCLI"
LOG_FILE="$REPO_ROOT/Gemini-Setup/logs/plugin-health.log"
mkdir -p "$(dirname "$LOG_FILE")"

echo "[$(date)] Starte Plugin-Check..." >> "$LOG_FILE"

# CHECK 1: Code-Search MCP Health
echo "Prüfe Code-Search MCP..."
if node "$REPO_ROOT/Gemini-Setup/scripts/check-code-search-health.mjs" >> "$LOG_FILE" 2>&1; then
  echo "✅ Code-Search MCP ist gesund."
else
  echo "⚠️ Code-Search MCP hat Probleme (siehe Log)."
fi

# CHECK 2: Ollama (für lokale Embeddings)
if curl -sf --connect-timeout 2 "http://localhost:11434/api/tags" >/dev/null 2>&1; then
  echo "✅ Ollama ist erreichbar." >> "$LOG_FILE"
else
  echo "⚠️ Ollama ist nicht aktiv." >> "$LOG_FILE"
fi

echo "---" >> "$LOG_FILE"
exit 0
