#!/usr/bin/env bash
set -euo pipefail

section="${1:-}"
text="${2:-}"
workspace="${3:-$(pwd)}"

if [[ -z "$section" || -z "$text" ]]; then
  echo "Usage: whiteboard-insert.sh <section> <text> [workspace]" >&2
  exit 1
fi

node "$(dirname "$0")/whiteboard-bridge.mjs" insert --workspace "$workspace" --section "$section" --text "$text"
