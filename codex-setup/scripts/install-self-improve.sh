#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
SOURCE_DIR="$REPO_ROOT/codex-setup/skills/self-improve"
TARGET_DIR="$HOME/.codex/skills/self-improve"
CACHE_DIR="$HOME/.codex/self-improve-cache"

mkdir -p "$TARGET_DIR" "$CACHE_DIR"

if command -v rsync >/dev/null 2>&1; then
  rsync -a --delete "$SOURCE_DIR"/ "$TARGET_DIR"/
else
  rm -rf "$TARGET_DIR"
  mkdir -p "$TARGET_DIR"
  cp -R "$SOURCE_DIR"/. "$TARGET_DIR"/
fi

echo "Installed Codex self-improve skill to $TARGET_DIR"
