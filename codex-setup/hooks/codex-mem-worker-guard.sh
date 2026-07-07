#!/usr/bin/env bash
# codex-mem-worker-guard.sh -- compatibility shim for Codex hook settings
set +e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TARGET="$SCRIPT_DIR/claude-mem-worker-guard.sh"

if [ -f "$TARGET" ]; then
    bash "$TARGET" "$@" >/dev/null 2>&1 || true
fi

exit 0
