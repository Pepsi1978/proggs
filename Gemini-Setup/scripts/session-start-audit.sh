#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="C:\Users\barwa\GeminiCLI"
cd "$REPO_ROOT"

echo "=== Cross-CLI Intelligenz-Audit (Start) ==="

# Codex Audit
echo "Pr├╝fe Codex CLI..."
codex_changes=$(node Gemini-Setup/scripts/audit-codex-delta.mjs | grep -c "\- A " || true)
if [ "$codex_changes" -gt 0 ]; then
  echo "­ƒÆí Intelligenz-Vorschlag (Gemini): Es gibt $codex_changes neue Erweiterungen bei Codex. -> Starte bitte die Bruecke zu Codex f├╝r Details. ÔÇö Soll ich das sp├ñter tun?"
else
  echo "Ô£à Codex ist synchron."
fi

# Claude Code Audit
echo "Pr├╝fe Claude Code..."
claude_changes=$(node Gemini-Setup/scripts/audit-claude-delta.mjs | grep -c "\- A " || true)
if [ "$claude_changes" -gt 0 ]; then
  echo "­ƒÆí Intelligenz-Vorschlag (Gemini): Es gibt $claude_changes neue Erweiterungen bei Claude Code. -> Starte bitte die Bruecke zu Claude Code f├╝r Details. ÔÇö Soll ich das sp├ñter tun?"
else
  echo "Ô£à Claude Code ist synchron."
fi

echo "---"
