#!/usr/bin/env bash
# reindex-codebase.sh — SessionStart Hook (async)
# Re-indexes the codebase for semantic search if files changed since last index.
# Uses pointer-based DB switching: writes to index-N.db, then updates current.txt.
# The old DB stays untouched and available for searches during the entire reindex.
# Old DBs are cleaned up on a best-effort basis.
# v2: Uses whiteboard-insert.sh for section-based error logging (echo >> is FORBIDDEN).

source "$(dirname "$0")/hook-log.sh"
source "$(dirname "$0")/whiteboard-insert.sh"

ROOT_DIR="$HOME/proggs"
DB_DIR="$ROOT_DIR/.code-search"
STAMP_FILE="$DB_DIR/.last-index-time"
POINTER_FILE="$DB_DIR/current.txt"
MCP_DIR="$ROOT_DIR/mcp-code-search"
# Use tsx (Node.js TypeScript runner) on ALL platforms.
# bun doesn't support better-sqlite3 (native C++ addon) — Bun bug #4290.
if command -v tsx > /dev/null 2>&1; then
    RUNNER="tsx"
elif command -v npx > /dev/null 2>&1; then
    RUNNER="npx tsx"
else
    echo "Reindex-Hook: tsx nicht gefunden (npm i -g tsx) — Semantic Search deaktiviert."
    exit 0
fi

# Exit if the MCP indexer source doesn't exist
if [ ! -f "$MCP_DIR/src/index.ts" ]; then
    exit 0
fi

# Ensure .mcp.json exists in home directory (Claude Code reads it from working dir)
HOME_MCP="$HOME/.mcp.json"
PROGGS_MCP="$ROOT_DIR/.mcp.json"
if [ -f "$PROGGS_MCP" ] && [ ! -f "$HOME_MCP" ]; then
    cp "$PROGGS_MCP" "$HOME_MCP"
fi

# Ensure dependencies are installed (npm for native addon compatibility)
if [ ! -d "$MCP_DIR/node_modules" ]; then
    npm install --prefix "$MCP_DIR" 2>/dev/null
fi

# Auto-start Ollama if not running
if ! curl -s --max-time 2 http://localhost:11434/api/tags > /dev/null 2>&1; then
    # Try to find and start ollama
    OLLAMA_BIN=""
    if command -v ollama > /dev/null 2>&1; then
        OLLAMA_BIN="ollama"
    elif [ -f "/usr/local/bin/ollama" ]; then
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
        ollama pull nomic-embed-text 2>/dev/null || hook_log_warn "ollama pull nomic-embed-text failed"
    fi
fi

# Check if re-index is needed
NEEDS_REINDEX=0
if [ ! -f "$STAMP_FILE" ]; then
    NEEDS_REINDEX=1
else
    STAMP_TIME=$(stat -c %Y "$STAMP_FILE" 2>/dev/null || stat -f %m "$STAMP_FILE" 2>/dev/null)
    # Find any code file newer than the stamp (exclude common non-code dirs)
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
        2>/dev/null | head -1)
    if [ -n "$NEWER" ]; then
        NEEDS_REINDEX=1
    fi
fi

if [ "$NEEDS_REINDEX" -eq 0 ]; then
    exit 0
fi

# Determine next DB filename (index-N.db)
MAX_N=0
if [ -d "$DB_DIR" ]; then
    while IFS= read -r dbfile; do
        BASENAME=$(basename "$dbfile")
        if [[ "$BASENAME" =~ ^index-([0-9]+)\.db$ ]]; then
            N="${BASH_REMATCH[1]}"
            if [ "$N" -gt "$MAX_N" ]; then
                MAX_N=$N
            fi
        fi
    done < <(find "$DB_DIR" -maxdepth 1 -name "index-*.db" 2>/dev/null)
fi
NEXT_N=$((MAX_N + 1))
NEW_DB_NAME="index-${NEXT_N}.db"

# Build the TypeScript indexer script
TEMP_FILE="$MCP_DIR/reindex-$(uuidgen 2>/dev/null || cat /proc/sys/kernel/random/uuid 2>/dev/null || date +%s).ts"

# Escape the root dir path for embedding in the TS heredoc
ROOT_DIR_ESCAPED="${ROOT_DIR//\\/\/}"

cat > "$TEMP_FILE" << TSEOF
import { findCodeFiles, chunkFile } from '${MCP_DIR}/src/indexer.ts';
import { generateEmbeddings } from '${MCP_DIR}/src/ollama.ts';
import { VectorStore } from '${MCP_DIR}/src/store.ts';
import { resolve, join } from 'path';
import { mkdirSync, existsSync, writeFileSync, readdirSync, unlinkSync } from 'fs';

async function main() {
  const rootDir = resolve('${ROOT_DIR_ESCAPED}');
  const dbDir = join(rootDir, '.code-search');
  if (!existsSync(dbDir)) mkdirSync(dbDir, { recursive: true });

  const newDbName = '${NEW_DB_NAME}';
  const newDbPath = join(dbDir, newDbName);
  const store = new VectorStore(newDbPath);

  const files = await findCodeFiles(rootDir);
  const allChunks: any[] = [];
  for (const file of files) {
    const chunks = await chunkFile(file, rootDir);
    allChunks.push(...chunks);
  }

  const BATCH = 32;
  for (let i = 0; i < allChunks.length; i += BATCH) {
    const batch = allChunks.slice(i, i + BATCH);
    const embeddings = await generateEmbeddings(batch.map((c: any) => c.content));
    store.insertBatch(batch, embeddings);
  }
  store.close();

  // Update pointer — this is the "switch" moment
  writeFileSync(join(dbDir, 'current.txt'), newDbName);

  // Clean up old DB files (best-effort — skip if locked)
  for (const f of readdirSync(dbDir)) {
    if (f.match(/^index-\d+\\.db/) && !f.startsWith(newDbName.replace('.db', ''))) {
      try { unlinkSync(join(dbDir, f)); } catch {}
    }
  }
}

main().catch(e => { console.error(e); process.exit(1); });
TSEOF

# Run the indexer
cd "$MCP_DIR" && $RUNNER "$TEMP_FILE" 2>/dev/null
EXIT_CODE=$?

# Clean up temp file (and any orphaned ones from previous crashed runs)
rm -f "$TEMP_FILE"
find "$MCP_DIR" -maxdepth 1 -name "reindex-*.ts" -delete 2>/dev/null || true

TIMESTAMP=$(date '+%Y-%m-%d %H:%M')

if [ "$EXIT_CODE" -eq 0 ]; then
    mkdir -p "$DB_DIR"
    date -Iseconds > "$STAMP_FILE" 2>/dev/null || date > "$STAMP_FILE"
    echo "Reindex-Hook: Codebase neu indexiert ($NEW_DB_NAME, pointer-swap)."
else
    if [ "$EXIT_CODE" -eq 143 ]; then
        EXIT_INFO="SIGTERM — process killed by 300s timeout"
    else
        EXIT_INFO="Unknown error (ExitCode: $EXIT_CODE)"
    fi
    hook_log_error "Indexer failed — ExitCode $EXIT_CODE: $EXIT_INFO"
    ENTRY="### $TIMESTAMP — Hook: reindex-codebase.sh — ExitCode $EXIT_CODE: $EXIT_INFO — Status: OFFEN"
    insert_whiteboard_entry "Offene Fehler & Probleme" "$ENTRY"
    echo "Reindex-Hook: FEHLER — Bun ExitCode $EXIT_CODE. Siehe Shared Whiteboard."
fi
