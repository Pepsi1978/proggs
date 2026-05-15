#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="C:\Users\barwa\GeminiCLI"
cd "$REPO_ROOT"

echo "=== Gemini CLI Bootstrap ==="
echo "Workspace: $REPO_ROOT"

echo "[1/2] Validiere Gemini-Setup-Struktur..."
if bash Gemini-Setup/scripts/validate-gemini-setup.sh; then
  echo "Ô£à Validierung erfolgreich."
else
  echo "ÔØî Validierung fehlgeschlagen. Bitte fehlende Komponenten pr├╝fen."
  exit 1
fi

echo "[2/2] Generiere Bootstrap-Report..."
node Gemini-Setup/scripts/bootstrap-report.mjs

echo "[3/4] F├╝hre Cross-CLI Intelligenz-Audit aus..."
bash Gemini-Setup/scripts/session-start-audit.sh

echo "[4/5] F├╝hre Plugin-Health-Check aus..."
bash Gemini-Setup/scripts/plugin-health-check.sh

echo "[5/5] F├╝hre MCP-Pfad-Check aus..."
bash Gemini-Setup/hooks/mcp-path-guard.sh

echo "[6/7] F├╝hre automatischen Repository-Sync (Pull & Diff) aus..."
bash Gemini-Setup/scripts/auto-sync.sh

echo "[7/7] Triggere automatische Hintergrund-Indexierung..."
bash Gemini-Setup/hooks/reindex-codebase.sh

echo "---"
echo "Gemini CLI Bootstrap abgeschlossen."
echo "N├ñchste Schritte:"
echo "- Starte bitte die Bruecke zu Codex"
echo "- node Gemini-Setup/scripts/audit-codex-delta.mjs"
