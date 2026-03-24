#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="C:\Users\barwa\GeminiCLI"
SCRIPT_DIR="$REPO_ROOT/Gemini-Setup/scripts"

# Führe den Whiteboard-Merge aus
node "$SCRIPT_DIR/whiteboard-bridge.mjs" merge-sentinels --workspace "$REPO_ROOT"

echo "✅ Whiteboard-Writeback abgeschlossen."
