#!/usr/bin/env bash
set -euo pipefail

workspace="${1:-$(pwd)}"
source_dir="$workspace/codex-setup/skills/self-improve"
target_dir="$HOME/.codex/skills/self-improve"

if [[ ! -d "$source_dir" ]]; then
  echo "Missing repo source: $source_dir" >&2
  exit 1
fi

mkdir -p "$target_dir"
cp -R "$source_dir"/. "$target_dir"/
echo "Deployed self-improve to $target_dir"
