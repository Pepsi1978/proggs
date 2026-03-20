#!/usr/bin/env bash
# reindex-codebase.sh — SessionStart Hook (async)
# Re-indexes the codebase for semantic search if files changed since last index.
# Uses pointer-based DB switching: writes to index-N.db, then updates current.txt.
# The old DB stays untouched and available for searches during the entire reindex.
# Cross-platform counterpart to reindex-codebase.ps1 (Windows).

ROOT_DIR="$HOME/proggs"
DB_DIR="$ROOT_DIR/.code-search"
STAMP_FILE="$DB_DIR/.last-index-time"
POINTER_FILE="$DB_DIR/current.txt"
MCP_DIR="$ROOT_DIR/mcp-code-search"

# Abort if MCP server source doesn't exist
[ -f "$MCP_DIR/src/index.ts" ] || exit 0

# Abort if Ollama is not running
curl -s --max-time 2 http://localhost:11434/api/tags >/dev/null 2>&1 || exit 0

# Check if bun is available
if command -v bun >/dev/null 2>&1; then
    BUN_EXE="bun"
elif [ -x "$HOME/.bun/bin/bun" ]; then
    BUN_EXE="$HOME/.bun/bin/bun"
else
    # No bun found — skip reindex silently
    exit 0
fi

# Ensure dependencies are installed
if [ ! -d "$MCP_DIR/node_modules" ]; then
    (cd "$MCP_DIR" && "$BUN_EXE" install --frozen-lockfile 2>/dev/null || "$BUN_EXE" install 2>/dev/null) || exit 0
fi

# Check if re-index is needed
NEEDS_REINDEX=false
if [ ! -f "$STAMP_FILE" ]; then
    NEEDS_REINDEX=true
else
    # Find any source file newer than the stamp
    NEWER=$(find "$ROOT_DIR" \
        -type f \( -name "*.ts" -o -name "*.kt" -o -name "*.rs" -o -name "*.go" \
                   -o -name "*.cs" -o -name "*.swift" -o -name "*.py" -o -name "*.js" \
                   -o -name "*.json" -o -name "*.md" -o -name "*.yaml" -o -name "*.ps1" \
                   -o -name "*.sh" \) \
        -not -path "*/node_modules/*" \
        -not -path "*/.git/*" \
        -not -path "*/.gradle/*" \
        -not -path "*/build/*" \
        -not -path "*/dist/*" \
        -not -path "*/target/*" \
        -not -path "*/.cache/*" \
        -newer "$STAMP_FILE" \
        -print -quit 2>/dev/null)
    [ -n "$NEWER" ] && NEEDS_REINDEX=true
fi

[ "$NEEDS_REINDEX" = false ] && exit 0

# Determine next DB filename (index-N.db)
mkdir -p "$DB_DIR"
MAX_N=0
for f in "$DB_DIR"/index-*.db; do
    [ -e "$f" ] || continue
    basename="$(basename "$f")"
    n="${basename#index-}"
    n="${n%.db}"
    if [ "$n" -gt "$MAX_N" ] 2>/dev/null; then
        MAX_N="$n"
    fi
done
NEXT_N=$((MAX_N + 1))
NEW_DB_NAME="index-${NEXT_N}.db"

# Create temp script in mcp-code-search dir (Bun resolves imports relative to source file)
TEMP_SCRIPT="$MCP_DIR/reindex-$(head -c 8 /dev/urandom | xxd -p).ts"
cat > "$TEMP_SCRIPT" << 'REINDEX_EOF'
import { findCodeFiles, chunkFile } from './src/indexer.ts';
import { generateEmbeddings } from './src/ollama.ts';
import { VectorStore } from './src/store.ts';
import { resolve, join } from 'path';
import { mkdirSync, existsSync, writeFileSync, readdirSync, unlinkSync } from 'fs';

const rootDir = resolve(process.env.REINDEX_ROOT!);
const dbDir = join(rootDir, '.code-search');
if (!existsSync(dbDir)) mkdirSync(dbDir, { recursive: true });

const newDbName = process.env.REINDEX_DB_NAME!;
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

// Clean up old DB files (best-effort)
const currentBase = newDbName.replace('.db', '');
for (const f of readdirSync(dbDir)) {
  if (f.match(/^index-\d+\.db/) && !f.startsWith(currentBase)) {
    try { unlinkSync(join(dbDir, f)); } catch {}
  }
}
REINDEX_EOF

# Run the reindex
export REINDEX_ROOT="$ROOT_DIR"
export REINDEX_DB_NAME="$NEW_DB_NAME"
cleanup_temp() {
    rm -f "$TEMP_SCRIPT"
    # Also clean up any orphaned temp files from previous crashed runs
    rm -f "$MCP_DIR"/reindex-*.ts 2>/dev/null
}
trap cleanup_temp EXIT

WHITEBOARD="$ROOT_DIR/.claude/agent-memory/shared/MEMORY.md"

if (cd "$MCP_DIR" && "$BUN_EXE" run "$TEMP_SCRIPT" 2>/dev/null); then
    date -u +"%Y-%m-%dT%H:%M:%SZ" > "$STAMP_FILE"
    echo "Reindex-Hook: Codebase neu indexiert ($NEW_DB_NAME, pointer-swap)."
else
    EXIT_CODE=$?
    if [ -f "$WHITEBOARD" ]; then
        cat >> "$WHITEBOARD" << WBEOF

### $(date +"%Y-%m-%d %H:%M") — Hook: reindex-codebase.sh — Indexierung ExitCode $EXIT_CODE
**Quelle:** Hook: reindex-codebase.sh (SessionStart, async, macOS)
**Symptom:** Semantische Indexierung fehlgeschlagen mit ExitCode $EXIT_CODE
**Ursache:** Bun-Prozess beendet mit Code $EXIT_CODE. Moeglich: Timeout, fehlende Abhaengigkeiten, Ollama nicht erreichbar.
**Betroffene Dateien:** ~/.claude/hooks/reindex-codebase.sh, mcp-code-search/
**Fix-Vorschlag:** Ollama-Status pruefen (curl localhost:11434), bun install in mcp-code-search/, Hook-Timeout erhoehen.
**Status:** OFFEN
WBEOF
    fi
    echo "Reindex-Hook: FEHLER — ExitCode $EXIT_CODE. Siehe Shared Whiteboard."
fi
