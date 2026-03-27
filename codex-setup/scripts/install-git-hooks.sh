#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
HOOKS_DIR="$REPO_ROOT/codex-setup/hooks"

if [[ ! -f "$HOOKS_DIR/pre-commit" ]]; then
  echo "Missing Codex pre-commit hook: $HOOKS_DIR/pre-commit" >&2
  exit 1
fi

git config --local core.hooksPath "$HOOKS_DIR"
echo "Configured git hooks path to $HOOKS_DIR"
