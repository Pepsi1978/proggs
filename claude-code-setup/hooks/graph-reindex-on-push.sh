#!/bin/bash
# graph-reindex-on-push.sh — PostToolUse Hook (async, Bash matcher)
# Triggers incremental codebase-memory-mcp reindex after every git push.
# Runs fully detached in the background — does not block the session.
# Platform: macOS / Linux

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
[ -f "$SCRIPT_DIR/hook-log.sh" ] && source "$SCRIPT_DIR/hook-log.sh"

# Read JSON from stdin
hook_input="$(cat)"

# Extract the Bash command
if ! command -v python3 &>/dev/null; then
    exit 0
fi

cmd="$(printf '%s' "$hook_input" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    print(d.get('tool_input', {}).get('command', ''))
except Exception:
    print('')
" 2>/dev/null)"

# Only trigger on git push commands (not git pull, git fetch, etc.)
if ! printf '%s' "$cmd" | grep -Eq 'git[[:space:]]+push'; then
    exit 0
fi

# Find the codebase-memory-mcp binary
CMM_BIN=""
if [ -x "/usr/local/bin/codebase-memory-mcp" ]; then
    CMM_BIN="/usr/local/bin/codebase-memory-mcp"
elif command -v codebase-memory-mcp &>/dev/null; then
    CMM_BIN="$(command -v codebase-memory-mcp)"
fi

REPO_PATH="$HOME/proggs"

# --- GitNexus incremental reindex (nur geaenderte Dateien) ---
GITNEXUS_BIN=""
if command -v gitnexus &>/dev/null; then
    GITNEXUS_BIN="$(command -v gitnexus)"
elif [ -x "/opt/homebrew/bin/gitnexus" ]; then
    GITNEXUS_BIN="/opt/homebrew/bin/gitnexus"
fi

if [ -n "$GITNEXUS_BIN" ]; then
    nohup "$GITNEXUS_BIN" analyze "$REPO_PATH" --skip-agents-md \
        > /tmp/gitnexus-reindex.log 2>&1 &
    disown $! 2>/dev/null || true
fi

# --- codebase-memory-mcp reindex (falls installiert) ---
if [ -n "$CMM_BIN" ]; then
    nohup "$CMM_BIN" cli index_repository "{\"repo_path\": \"$REPO_PATH\"}" \
        > /tmp/graph-reindex.log 2>&1 &
    disown $! 2>/dev/null || true
fi

exit 0
