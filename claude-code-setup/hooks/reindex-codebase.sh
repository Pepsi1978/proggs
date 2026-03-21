#!/usr/bin/env bash
# reindex-codebase.sh — SessionStart Hook (async)
# v3: Complete rewrite for reliability.
#
# Key improvements over v2:
# - Uses a permanent reindex.ts script instead of HEREDOC generation
# - nohup background process — immune to hook timeout (300s)
# - Lock file prevents parallel reindex processes
# - Bash-based cleanup (no more HEREDOC regex bugs)
# - Cleans up WAL/SHM files from crashed indexers

HOOKS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$HOOKS_DIR/hook-log.sh"
source "$HOOKS_DIR/whiteboard-insert.sh"

ROOT_DIR="$HOME/proggs"
DB_DIR="$ROOT_DIR/.code-search"
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

# --- Check if reindex is needed ---
mkdir -p "$DB_DIR"

if [ -f "$STAMP_FILE" ]; then
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
if [ -f "$LOCK_FILE" ]; then
    LOCK_PID=$(cat "$LOCK_FILE" 2>/dev/null)
    if [ -n "$LOCK_PID" ] && kill -0 "$LOCK_PID" 2>/dev/null; then
        hook_log "reindex already running (PID $LOCK_PID) — skipping"
        exit 0
    else
        # Stale lock file — remove it
        rm -f "$LOCK_FILE"
        hook_log_warn "removed stale lock file (PID $LOCK_PID no longer exists)"
    fi
fi

# --- Determine next DB filename ---
CURRENT_DB=$(cat "$DB_DIR/current.txt" 2>/dev/null || echo "")
MAX_N=0
for dbfile in "$DB_DIR"/index-*.db; do
    [ -f "$dbfile" ] || continue
    BASENAME=$(basename "$dbfile")
    if [[ "$BASENAME" =~ ^index-([0-9]+)\.db$ ]]; then
        N="${BASH_REMATCH[1]}"
        [ "$N" -gt "$MAX_N" ] && MAX_N=$N
    fi
done
NEXT_N=$((MAX_N + 1))
NEW_DB_NAME="index-${NEXT_N}.db"
NEW_DB_PATH="$DB_DIR/$NEW_DB_NAME"

# --- Clean up orphaned WAL/SHM files from crashed previous runs ---
for f in "$DB_DIR"/index-*.db-wal "$DB_DIR"/index-*.db-shm; do
    [ -f "$f" ] || continue
    BASE_DB="${f%-*}"  # Remove -wal or -shm suffix
    # If the base DB exists but is NOT the current one, clean up
    if [ -f "$BASE_DB" ] && [ "$(basename "$BASE_DB")" != "$CURRENT_DB" ]; then
        rm -f "$f"
    fi
done

# --- Start reindex as fully detached background process ---
# Must use nohup + setsid/disown to survive hook process group termination.
# Claude Code kills the hook's process group after timeout, so a simple ( ... ) &
# is NOT enough — the subshell would be killed too.
hook_log "starting reindex (background): $NEW_DB_NAME"

# Write a worker script that runs independently of this hook
WORKER_SCRIPT="$DB_DIR/.reindex-worker.sh"
cat > "$WORKER_SCRIPT" << WORKER_EOF
#!/usr/bin/env bash
# Auto-generated reindex worker — runs detached from hook process
echo \$\$ > "$LOCK_FILE"

cd "$MCP_DIR" || exit 1
"$RUNNER" "$REINDEX_SCRIPT" "$ROOT_DIR" "$NEW_DB_PATH" > "$LOG_FILE" 2>&1
EXIT_CODE=\$?

if [ "\$EXIT_CODE" -eq 0 ]; then
    date -Iseconds > "$STAMP_FILE" 2>/dev/null || date > "$STAMP_FILE"
    # Clean up old DB files (keep only the new one)
    CURRENT_AFTER=\$(cat "$DB_DIR/current.txt" 2>/dev/null || echo "")
    for f in "$DB_DIR"/index-*.db "$DB_DIR"/index-*.db-wal "$DB_DIR"/index-*.db-shm; do
        [ -f "\$f" ] || continue
        BASENAME=\$(basename "\$f")
        case "\$BASENAME" in
            "\${CURRENT_AFTER}"*) continue ;;
        esac
        rm -f "\$f" 2>/dev/null || true
    done
    # Clean up leftover reindex-*.ts temp files from v2
    find "$MCP_DIR" -maxdepth 1 -name "reindex-*.ts" ! -name "reindex.ts" -delete 2>/dev/null || true
else
    # Failed: remove incomplete DB, keep old one
    rm -f "$NEW_DB_PATH" "${NEW_DB_PATH}-wal" "${NEW_DB_PATH}-shm" 2>/dev/null || true
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
