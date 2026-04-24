#!/usr/bin/env bash
set -euo pipefail

workspace="${1:-$(pwd)}"
cd "$workspace"

git fetch origin
git diff --stat HEAD..origin/main
git diff --name-status HEAD..origin/main
git pull --rebase --autostash origin main

if [[ -f "$workspace/codex-setup/mcp-macos.json" ]]; then
  cp "$workspace/codex-setup/mcp-macos.json" "$workspace/.mcp.json"
fi

repair_script="$workspace/codex-setup/scripts/repair_codex_skill_visibility.py"
report_path="$workspace/codex-setup/state/skill-visibility-report.json"
if [[ -f "$repair_script" ]]; then
  if command -v python3 >/dev/null 2>&1; then
    python3 "$repair_script" --report-path "$report_path" || \
      echo "Skill visibility repair failed." >&2
  elif command -v python >/dev/null 2>&1; then
    python "$repair_script" --report-path "$report_path" || \
      echo "Skill visibility repair failed." >&2
  fi
fi
