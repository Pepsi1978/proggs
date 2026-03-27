#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

if [[ $# -lt 2 ]]; then
  echo "Usage: $(basename "$0") <section> <entry>" >&2
  exit 1
fi

DIRECTIVE_TOKEN="$(node "$SCRIPT_DIR/whiteboard-bridge.mjs" directive-token --workspace "$REPO_ROOT")"

node "$SCRIPT_DIR/whiteboard-bridge.mjs" insert --workspace "$REPO_ROOT" --directive-token "$DIRECTIVE_TOKEN" --section "$1" --entry "$2"
