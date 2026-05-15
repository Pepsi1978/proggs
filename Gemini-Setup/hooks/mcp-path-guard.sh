#!/usr/bin/env bash
# mcp-path-guard.sh (Gemini)
set -uo pipefail

REPO_ROOT="C:\Users\barwa\GeminiCLI"

echo "[$(date)] Pr├╝fe MCP-Pfadintegrit├ñt..."
if ! node "$REPO_ROOT/Gemini-Setup/scripts/validate-mcp-paths.mjs"; then
  echo "ÔØî MCP-PFAD-FEHLER ERKANNT! Bitte .mcp.json pr├╝fen."
  # Wir blockieren die Session nicht, geben aber eine laute Warnung aus.
fi
