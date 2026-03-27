#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
DIRECTIVE_TOKEN="$(node "$SCRIPT_DIR/whiteboard-bridge.mjs" directive-token --workspace "$REPO_ROOT")"

if [[ $# -gt 0 ]]; then
  node "$SCRIPT_DIR/whiteboard-bridge.mjs" merge-sentinels --workspace "$REPO_ROOT" --directive-token "$DIRECTIVE_TOKEN" --sentinel-dir "$1"
else
  node "$SCRIPT_DIR/whiteboard-bridge.mjs" merge-sentinels --workspace "$REPO_ROOT" --directive-token "$DIRECTIVE_TOKEN"
fi
