#!/usr/bin/env bash
# reindex-codebase.sh — SessionStart Hook (async)
# v4: Incremental reindex — only re-indexes changed files.
#
# Architecture:
# - Single DB file (index.db) updated in-place, no more pointer-swap
# - reindex.ts detects changed files via mtime comparison and only
#   re-embeds those — typically <1 minute instead of 30-60 minutes
# - Full reindex only when DB doesn't exist (first run)
# - nohup + disown detaches from hook process group (survives timeout)
# - Lock file prevents parallel reindex processes

HOOKS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$HOOKS_DIR/hook-log.sh"
source "$HOOKS_DIR/whiteboard-insert.sh"

ROOT_DIR="$HOME/proggs"
DB_DIR="$ROOT_DIR/.code-search"
DB_PATH="$DB_DIR/index.db"
STAMP_FILE="$DB_DIR/.last-index-time"
LOCK_FILE="$DB_DIR/.reindex.lock"
MCP_DIR="$ROOT_DIR/mcp-code-search"
REINDEX_SCRIPT="$MCP_DIR/src/reindex.ts"
LOG_FILE="$DB_DIR/.reindex.log"

# Use absolute path for tsx (macOS needs it for hooks/cron)
if [ -x "/opt/homebrew/bin/tsx" ]; then
    RUNNER="/opt/homebrew/bin/tsx"
elif command -v tsx > /dev/null 2>&1; then
    RUNNER="tsx"
else
    hook_log_warn "tsx not found — semantic search reindex disabled"
    exit 0
fi

# Exit if the reindex script doesn't exist
if [ ! -f "$REINDEX_SCRIPT" ]; then
    hook_log_warn "reindex.ts not found at $REINDEX_SCRIPT"
    exit 0
fi

# Ensure .mcp.json in home directory is up-to-date
PROGGS_MCP="$ROOT_DIR/.mcp.json"
if [ -f "$PROGGS_MCP" ]; then
    if ! diff -q "$PROGGS_MCP" "$HOME/.mcp.json" > /dev/null 2>&1; then
        cp "$PROGGS_MCP" "$HOME/.mcp.json"
    fi
fi

# Ensure dependencies are installed
if [ ! -d "$MCP_DIR/node_modules/better-sqlite3" ]; then
    hook_log "installing mcp-code-search dependencies"
    npm install --prefix "$MCP_DIR" 2>/dev/null
fi

# --- Check if Ollama is running, start if needed ---
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
            hook_log_warn "Ollama failed to start — skipping reindex"
            exit 0
        fi
    else
        hook_log_warn "Ollama not installed — skipping reindex"
        exit 0
    fi
fi

# Ensure nomic-embed-text model is available
MODELS_JSON=$(curl -s --max-time 2 http://localhost:11434/api/tags 2>/dev/null)
if [ $? -eq 0 ] && [ -n "$MODELS_JSON" ]; then
    if ! echo "$MODELS_JSON" | grep -q "nomic-embed-text"; then
        ollama pull nomic-embed-text 2>/dev/null || {
            hook_log_warn "ollama pull nomic-embed-text failed"
            exit 0
        }
    fi
fi

# --- Quick check if reindex is needed (avoid spawning worker for nothing) ---
mkdir -p "$DB_DIR"

if [ -f "$STAMP_FILE" ] && [ -f "$DB_PATH" ]; then
    NEWER=$(find "$ROOT_DIR" \
        \( -name "*.ts" -o -name "*.kt" -o -name "*.rs" -o -name "*.go" \
           -o -name "*.cs" -o -name "*.swift" -o -name "*.py" \
           -o -name "*.js" -o -name "*.json" -o -name "*.md" \
           -o -name "*.yaml" -o -name "*.ps1" -o -name "*.sh" \) \
        -newer "$STAMP_FILE" \
        -not -path "*/node_modules/*" \
        -not -path "*/.git/*" \
        -not -path "*/.gradle/*" \
        -not -path "*/build/*" \
        -not -path "*/dist/*" \
        -not -path "*/target/*" \
        -not -path "*/.cache/*" \
        -not -path "*/.code-search/*" \
        2>/dev/null | head -1)
    if [ -z "$NEWER" ]; then
        hook_log "index is up to date — skipping reindex"
        exit 0
    fi
fi

# --- Lock: prevent parallel reindex ---
MAX_LOCK_AGE_SECONDS=1800  # 30 minutes — any worker older than this is considered stuck

if [ -f "$LOCK_FILE" ]; then
    LOCK_PID=$(cat "$LOCK_FILE" 2>/dev/null)
    LOCK_AGE=0
    if [ "$(uname)" = "Darwin" ]; then
        LOCK_CREATED=$(stat -f %m "$LOCK_FILE" 2>/dev/null || echo 0)
    else
        LOCK_CREATED=$(stat -c %Y "$LOCK_FILE" 2>/dev/null || echo 0)
    fi
    NOW=$(date +%s)
    LOCK_AGE=$(( NOW - LOCK_CREATED ))

    if [ "$LOCK_AGE" -gt "$MAX_LOCK_AGE_SECONDS" ]; then
        # Lock is older than 30 minutes — worker is definitely stuck
        if [ -n "$LOCK_PID" ] && [ "$LOCK_PID" != "pending" ] && kill -0 "$LOCK_PID" 2>/dev/null; then
            kill "$LOCK_PID" 2>/dev/null
            hook_log_warn "killed stuck worker (PID $LOCK_PID, age ${LOCK_AGE}s) and removed stale lock"
        else
            hook_log_warn "removed stale lock file (age ${LOCK_AGE}s, PID $LOCK_PID no longer exists)"
        fi
        rm -f "$LOCK_FILE"
    elif [ "$LOCK_PID" = "pending" ]; then
        hook_log "reindex is pending start — skipping"
        exit 0
    elif [ -n "$LOCK_PID" ] && kill -0 "$LOCK_PID" 2>/dev/null; then
        hook_log "reindex already running (PID $LOCK_PID, age ${LOCK_AGE}s) — skipping"
        exit 0
    else
        rm -f "$LOCK_FILE"
        hook_log_warn "removed stale lock file (PID $LOCK_PID no longer exists)"
    fi
fi

# --- Migrate from old numbered DBs to single index.db ---
# One-time migration: if index.db doesn't exist but old numbered DBs do,
# rename the current one to index.db and delete the rest.
if [ ! -f "$DB_PATH" ]; then
    CURRENT_OLD=$(cat "$DB_DIR/current.txt" 2>/dev/null || echo "")
    if [ -n "$CURRENT_OLD" ] && [ -f "$DB_DIR/$CURRENT_OLD" ]; then
        mv "$DB_DIR/$CURRENT_OLD" "$DB_PATH"
        hook_log "migrated $CURRENT_OLD → index.db"
    fi
    # Clean up ALL old numbered DBs and their WAL/SHM
    for f in "$DB_DIR"/index-*.db "$DB_DIR"/index-*.db-wal "$DB_DIR"/index-*.db-shm; do
        [ -f "$f" ] && rm -f "$f"
    done
    rm -f "$DB_DIR/current.txt"
fi

# --- Start reindex as fully detached background process ---
# Set lock BEFORE forking to close the race window between stale-check and worker start.
# The worker will overwrite it with its own PID once running.
echo "pending" > "$LOCK_FILE"
hook_log "starting incremental reindex (background)"

WORKER_SCRIPT="$DB_DIR/.reindex-worker.sh"
WORKER_TIMEOUT=1800  # 30 minutes max runtime
cat > "$WORKER_SCRIPT" << WORKER_EOF
#!/usr/bin/env bash
# Auto-generated reindex worker — runs detached from hook process
echo \$\$ > "$LOCK_FILE"

cd "$MCP_DIR" || exit 1

# Run with timeout to prevent stuck workers
if command -v timeout > /dev/null 2>&1; then
    timeout "$WORKER_TIMEOUT" "$RUNNER" "$REINDEX_SCRIPT" "$ROOT_DIR" "$DB_PATH" "$STAMP_FILE" > "$LOG_FILE" 2>&1
    EXIT_CODE=\$?
    if [ "\$EXIT_CODE" -eq 124 ]; then
        echo "Reindex TIMED OUT after ${WORKER_TIMEOUT}s" >> "$LOG_FILE"
    fi
elif command -v gtimeout > /dev/null 2>&1; then
    gtimeout "$WORKER_TIMEOUT" "$RUNNER" "$REINDEX_SCRIPT" "$ROOT_DIR" "$DB_PATH" "$STAMP_FILE" > "$LOG_FILE" 2>&1
    EXIT_CODE=\$?
    if [ "\$EXIT_CODE" -eq 124 ]; then
        echo "Reindex TIMED OUT after ${WORKER_TIMEOUT}s" >> "$LOG_FILE"
    fi
else
    # No timeout command available — run without timeout
    "$RUNNER" "$REINDEX_SCRIPT" "$ROOT_DIR" "$DB_PATH" "$STAMP_FILE" > "$LOG_FILE" 2>&1
    EXIT_CODE=\$?
fi

if [ "\$EXIT_CODE" -eq 0 ]; then
    # Atomic stamp write: write to temp, then rename (survives crashes)
    STAMP_TMP="$STAMP_FILE.tmp.\$\$"
    date -Iseconds > "\$STAMP_TMP" 2>/dev/null || date > "\$STAMP_TMP"
    mv "\$STAMP_TMP" "$STAMP_FILE"
else
    # Log failure but keep existing DB intact (incremental = safe)
    echo "Reindex failed with exit code \$EXIT_CODE at \$(date -Iseconds 2>/dev/null || date)" >> "$LOG_FILE"
fi

rm -f "$LOCK_FILE"
rm -f "$WORKER_SCRIPT"
WORKER_EOF
chmod +x "$WORKER_SCRIPT"

# Launch fully detached: nohup + redirect all FDs + disown
nohup bash "$WORKER_SCRIPT" </dev/null >/dev/null 2>&1 &
disown $! 2>/dev/null || true

# The hook returns immediately — the detached worker handles the rest.
exit 0
