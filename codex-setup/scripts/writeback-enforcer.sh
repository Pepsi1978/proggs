#!/usr/bin/env bash
set -euo pipefail

workspace="${1:-$(pwd)}"
sentinel_dir="${2:-}"

if [[ -n "$sentinel_dir" ]]; then
  node "$(dirname "$0")/whiteboard-bridge.mjs" merge-sentinels --workspace "$workspace" --dir "$sentinel_dir"
else
  node "$(dirname "$0")/whiteboard-bridge.mjs" merge-sentinels --workspace "$workspace"
fi
