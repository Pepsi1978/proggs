#!/usr/bin/env bash
# reindex-codebase.sh — SessionStart Hook (async)
# Incrementally re-indexes the codebase for semantic search.
# Only re-embeds files changed since last index. Full reindex only on first run.
# v3: Uses session-reindex.ts (reindex-core.ts) instead of inline full-reindex script.
# nohup + disown detaches from hook process group (survives timeout).
# Lock file prevents parallel reindex processes.

HOOKS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
[ -f "$HOOKS_DIR/hook-log.sh" ] && source "$HOOKS_DIR/hook-log.sh"

ROOT_DIR="$HOME/proggs"
DB_DIR="$ROOT_DIR/.code-search"
LOCK_FILE="$DB_DIR/.reindex.lock"
MCP_DIR="$ROOT_DIR/mcp-code-search"
REINDEX_SCRIPT="$MCP_DIR/src/session-reindex.ts"
LOG_FILE="$DB_DIR/reindex.log"

# Use absolute path for tsx (macOS needs it for hooks/cron)
if [ -x "/opt/homebrew/bin/tsx" ]; then
    RUNNER="/opt/homebrew/bin/tsx"
elif command -v tsx > /dev/null 2>&1; then
    RUNNER="tsx"
else
    echo "tsx not found — semantic search reindex disabled"
    exit 0
fi

# Guard: required files must exist
if [ ! -f "$REINDEX_SCRIPT" ]; then
    exit 0
fi

# Ensure dependencies are installed
if [ ! -d "$MCP_DIR/node_modules/better-sqlite3" ]; then
    npm install --prefix "$MCP_DIR" 2>/dev/null
fi

# Auto-start Ollama if not running
if ! curl -s --max-time 2 http://localhost:11434/api/tags > /dev/null 2>&1; then
    OLLAMA_BIN=""
    if command -v ollama > /dev/null 2>&1; then
        OLLAMA_BIN="ollama"
    elif [ -x "/usr/local/bin/ollama" ]; then
        OLLAMA_BIN="/usr/local/bin/ollama"
    fi

    if [ -n "$OLLAMA_BIN" ]; then
        nohup "$OLLAMA_BIN" serve > /dev/null 2>&1 &
        sleep 5
        if ! curl -s --max-time 3 http://localhost:11434/api/tags > /dev/null 2>&1; then
            exit 0
        fi
    else
        exit 0
    fi
fi

# Ensure nomic-embed-text model is available
MODELS_JSON=$(curl -s --max-time 2 http://localhost:11434/api/tags 2>/dev/null)
if [ $? -eq 0 ] && [ -n "$MODELS_JSON" ]; then
    if ! echo "$MODELS_JSON" | grep -q "nomic-embed-text"; then
        ollama pull nomic-embed-text 2>/dev/null || exit 0
    fi
fi

# Lock: prevent parallel reindex
mkdir -p "$DB_DIR"
MAX_LOCK_AGE_SECONDS=1800  # 30 minutes

if [ -f "$LOCK_FILE" ]; then
    LOCK_PID=$(cat "$LOCK_FILE" 2>/dev/null)
    if [ "$(uname)" = "Darwin" ]; then
        LOCK_CREATED=$(stat -f %m "$LOCK_FILE" 2>/dev/null || echo 0)
    else
        LOCK_CREATED=$(stat -c %Y "$LOCK_FILE" 2>/dev/null || echo 0)
    fi
    NOW=$(date +%s)
    LOCK_AGE=$(( NOW - LOCK_CREATED ))

    if [ "$LOCK_AGE" -gt "$MAX_LOCK_AGE_SECONDS" ]; then
        if [ -n "$LOCK_PID" ] && [ "$LOCK_PID" != "pending" ] && kill -0 "$LOCK_PID" 2>/dev/null; then
            kill "$LOCK_PID" 2>/dev/null
        fi
        rm -f "$LOCK_FILE"
    elif [ -n "$LOCK_PID" ] && [ "$LOCK_PID" != "pending" ] && kill -0 "$LOCK_PID" 2>/dev/null; then
        exit 0  # Already running
    else
        rm -f "$LOCK_FILE"
    fi
fi

# Start reindex as fully detached background process
echo "pending" > "$LOCK_FILE"

WORKER_SCRIPT="$DB_DIR/.reindex-worker.sh"
WORKER_TIMEOUT=1800
cat > "$WORKER_SCRIPT" << WORKER_EOF
#!/usr/bin/env bash
echo \$\$ > "$LOCK_FILE"
cd "$MCP_DIR" || exit 1

if command -v timeout > /dev/null 2>&1; then
    timeout "$WORKER_TIMEOUT" "$RUNNER" "$REINDEX_SCRIPT" "$ROOT_DIR" > "$LOG_FILE" 2>&1
elif command -v gtimeout > /dev/null 2>&1; then
    gtimeout "$WORKER_TIMEOUT" "$RUNNER" "$REINDEX_SCRIPT" "$ROOT_DIR" > "$LOG_FILE" 2>&1
else
    "$RUNNER" "$REINDEX_SCRIPT" "$ROOT_DIR" > "$LOG_FILE" 2>&1
fi

rm -f "$LOCK_FILE"
rm -f "$WORKER_SCRIPT"
WORKER_EOF
chmod +x "$WORKER_SCRIPT"

nohup bash "$WORKER_SCRIPT" </dev/null >/dev/null 2>&1 &
disown $! 2>/dev/null || true

exit 0
