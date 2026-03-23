#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="/Users/frank/GeminiCLI"
cd "$REPO_ROOT"

echo "=== Gemini Setup Validierung ==="

required_files=(
  "Gemini-Setup/rules/global.md"
  "Gemini-Setup/rules/SUPERINTELLIGENZ.md"
  "Gemini-Setup/rules/SELBSTBEOBACHTUNG.md"
  "Gemini-Setup/rules/RESILIENT_BUGFIXING.md"
  "Gemini-Setup/agent-memory/shared/MEMORY.md"
  "Gemini-Setup/bridges/bridge-registry.json"
  "Gemini-Setup/bridges/codex-delta-bridge.json"
  "Gemini-Setup/bridges/claude-code-delta-bridge.json"
  "Gemini-Setup/bridges/environment-fix-exchange-bridge.json"
  "Gemini-Setup/bridges/intelligence-suggestion-exchange-bridge.json"
  "Gemini-Setup/state/environment-fixes.json"
  "Gemini-Setup/state/implemented-intelligence-suggestions.json"
  "Gemini-Setup/scripts/bridge-registry.mjs"
  "Gemini-Setup/scripts/bootstrap-report.mjs"
  "Gemini-Setup/scripts/audit-codex-delta.mjs"
  "Gemini-Setup/scripts/audit-claude-delta.mjs"
  "Gemini-Setup/scripts/register-environment-fix.mjs"
  "Gemini-Setup/scripts/register-intelligence-suggestion.mjs"
  "Gemini-Setup/scripts/check-code-search-health.mjs"
  "Gemini-Setup/scripts/session-start-audit.sh"
  "Gemini-Setup/scripts/whiteboard-bridge.mjs"
  "Gemini-Setup/scripts/writeback-enforcer.sh"
  "Gemini-Setup/scripts/plugin-health-check.sh"
  "Gemini-Setup/scripts/validate-mcp-paths.mjs"
  "Gemini-Setup/hooks/mcp-path-guard.sh"
  "Gemini-Setup/scripts/auto-sync.sh"
  "Gemini-Setup/scripts/bootstrap-gemini-setup.ps1"
  "Gemini-Setup/scripts/validate-gemini-setup.ps1"
  "Gemini-Setup/scripts/session-start-audit.ps1"
  "Gemini-Setup/scripts/plugin-health-check.ps1"
  "Gemini-Setup/scripts/writeback-enforcer.ps1"
  "Gemini-Setup/hooks/reindex-codebase.sh"
  "Gemini-Setup/skills/self-improve/SKILL.md"
)

missing=0
for file in "${required_files[@]}"; do
  if [[ ! -f "$file" ]]; then
    echo "❌ Fehlt: $file"
    missing=$((missing + 1))
  else
    echo "✅ Vorhanden: $file"
  fi
done

echo "[5/6] Führe AGENTS.md Konsistenz-Check aus..."
node Gemini-Setup/scripts/sync-agents-rules.mjs

if [[ $missing -eq 0 ]]; then
  echo "---"
  echo "🚀 Alle Gemini-Setup-Komponenten sind vorhanden."
  exit 0
else
  echo "---"
  echo "⚠️ $missing Komponenten fehlen."
  exit 1
fi
