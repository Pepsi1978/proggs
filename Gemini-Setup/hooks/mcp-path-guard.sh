#!/usr/bin/env bash
# mcp-path-guard.sh (Gemini)
set -uo pipefail

REPO_ROOT="C:\Users\barwa\GeminiCLI"

echo "[$(date)] Prüfe MCP-Pfadintegrität..."
if ! node "$REPO_ROOT/Gemini-Setup/scripts/validate-mcp-paths.mjs"; then
  echo "❌ MCP-PFAD-FEHLER ERKANNT! Bitte .mcp.json prüfen."
  # Wir blockieren die Session nicht, geben aber eine laute Warnung aus.
fi
