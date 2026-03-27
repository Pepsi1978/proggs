#!/usr/bin/env bash
# sync-clawi.sh - Sync Clawi's Identity/Memory between Repo and Local Workspace
set -e

MODE=${1:-}
REPO_DIR=$(cd "$(dirname "$0")" && pwd)
WORKSPACE_DIR="$HOME/.openclaw/workspace"
FILES_TO_SYNC=(
  "SOUL.md"
  "IDENTITY.md"
  "DIREKTIVEN.md"
  "AGENTS.md"
  "ENVIRONMENT-FIXES.md"
  "README.md"
  "HEARTBEAT.md"
  "MEMORY.md"
  "RECOVERY-OPENCLAW.md"
)

if [[ ! -d "$WORKSPACE_DIR" ]]; then
    echo "Local OpenClaw workspace not found at $WORKSPACE_DIR"
    exit 1
fi

copy_if_exists() {
    local src="$1"
    local dst="$2"
    if [[ -f "$src" ]]; then
        mkdir -p "$(dirname "$dst")"
        cp "$src" "$dst"
        echo "  Synced $(basename "$src")"
    fi
}

case "$MODE" in
    "pull")
        echo "Pulling Clawi's Identity from Repo to $WORKSPACE_DIR..."
        for file in "${FILES_TO_SYNC[@]}"; do
            copy_if_exists "$REPO_DIR/$file" "$WORKSPACE_DIR/$file"
        done

        if [[ -d "$REPO_DIR/memory" ]]; then
            mkdir -p "$WORKSPACE_DIR/memory"
            cp -R "$REPO_DIR/memory/." "$WORKSPACE_DIR/memory/"
            echo "  Updated Memory logs"
        fi

        if [[ -d "$REPO_DIR/hooks" ]]; then
            mkdir -p "$WORKSPACE_DIR/hooks"
            cp -R "$REPO_DIR/hooks/." "$WORKSPACE_DIR/hooks/"
            echo "  Updated hooks"
        fi

        echo "Clawi's Identity is now up to date in local workspace!"
        ;;
    "push")
        echo "Pushing Clawi's Identity from $WORKSPACE_DIR to Repo..."
        for file in "${FILES_TO_SYNC[@]}"; do
            copy_if_exists "$WORKSPACE_DIR/$file" "$REPO_DIR/$file"
        done

        if [[ -d "$WORKSPACE_DIR/memory" ]]; then
            mkdir -p "$REPO_DIR/memory"
            cp -R "$WORKSPACE_DIR/memory/." "$REPO_DIR/memory/"
            echo "  Saved Memory logs"
        fi

        if [[ -d "$WORKSPACE_DIR/hooks" ]]; then
            mkdir -p "$REPO_DIR/hooks"
            cp -R "$WORKSPACE_DIR/hooks/." "$REPO_DIR/hooks/"
            echo "  Saved hooks"
        fi

        echo "Clawi's Identity is now backed up in the repository!"
        ;;
    *)
        echo "Usage: $0 {pull|push}"
        exit 1
        ;;
esac
