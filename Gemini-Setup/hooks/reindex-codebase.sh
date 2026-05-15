#!/usr/bin/env bash
# reindex-codebase.sh (Gemini) ÔÇö Hintergrund-Indexierung
set -euo pipefail

REPO_ROOT="C:\Users\barwa\GeminiCLI"
REINDEX_SCRIPT="$REPO_ROOT/mcp-code-search/src/reindex.ts"

echo "[$(date)] Starte Hintergrund-Reindex (inkrementell)..."
# Nutze bun direkt fuer bessere Performance und Paritaet zur Windows-Version
nohup bun "$REINDEX_SCRIPT" "$REPO_ROOT" > /dev/null 2>&1 &

echo "Ô£à Reindex-Job im Hintergrund gestartet."
