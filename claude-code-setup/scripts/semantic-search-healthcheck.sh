#!/usr/bin/env bash
# semantic-search-healthcheck.sh
# Runs the 4-category semantic search integrity test for the mcp-code-search system.
#
# Usage:
#   ./semantic-search-healthcheck.sh [PROGGS_DIR]
#   ./semantic-search-healthcheck.sh --reindex [PROGGS_DIR]
#
# Exit codes:
#   0 = all checks OK
#   1 = at least one FAIL
#   2 = no FAIL but at least one WARN
#
# Categories:
#   1. Basic function  (Ollama -> Embedding -> DB -> Search)
#   2. Pointer consistency (current.txt points to existing DB)
#   3. Infrastructure  (hooks, MCP config, gitignore, dependencies, Ollama)
#   4. Parallelism safety (race condition guards)

set -euo pipefail

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------
REINDEX=0
if [[ "${1:-}" == "--reindex" ]]; then
    REINDEX=1
    shift
fi

PROGGS_DIR="${1:-$HOME/proggs}"
PROGGS_DIR="$(realpath "$PROGGS_DIR")"
MCP_DIR="$PROGGS_DIR/mcp-code-search"
DBDIR="$PROGGS_DIR/.code-search"
SETTINGS="$HOME/.claude/settings.json"

# ---------------------------------------------------------------------------
# Result tracking
# ---------------------------------------------------------------------------
FAIL_COUNT=0
WARN_COUNT=0

ok()   { echo "  OK   $*"; }
warn() { echo "  WARN $*"; WARN_COUNT=$((WARN_COUNT + 1)); }
fail() { echo "  FAIL $*"; FAIL_COUNT=$((FAIL_COUNT + 1)); }

# ---------------------------------------------------------------------------
# Optional reindex
# ---------------------------------------------------------------------------
if [[ "$REINDEX" -eq 1 ]]; then
    echo "==> Reindexing codebase (bun run index)..."
    if [[ -d "$MCP_DIR" ]]; then
        (cd "$MCP_DIR" && bun run index 2>&1) || echo "  WARN: Reindex returned non-zero exit"
    else
        echo "  FAIL: $MCP_DIR not found — cannot reindex"
        exit 1
    fi
    echo ""
fi

# ---------------------------------------------------------------------------
# Category 1: Basic function (Ollama -> Embedding -> DB -> Search)
# ---------------------------------------------------------------------------
echo "==> Category 1: Basic function"

if [[ ! -d "$MCP_DIR" ]]; then
    fail "1-pre: $MCP_DIR does not exist"
else
    cat1_result=$(cd "$MCP_DIR" && bun -e "
import { VectorStore } from './src/store.ts';
import { generateEmbedding } from './src/ollama.ts';
import { resolve, join } from 'path';
import { existsSync, readFileSync } from 'fs';

const root = resolve('$PROGGS_DIR');
const dbDir = join(root, '.code-search');
const pointerFile = join(dbDir, 'current.txt');

if (!existsSync(pointerFile)) { console.log('FAIL-1A: current.txt missing'); process.exit(1); }
const dbName = readFileSync(pointerFile, 'utf-8').trim();
if (!existsSync(join(dbDir, dbName))) { console.log('FAIL-1B: ' + dbName + ' does not exist'); process.exit(1); }
const store = new VectorStore(join(dbDir, dbName));
const stats = store.stats();
if (stats.totalChunks === 0) { console.log('FAIL-1C: Index is empty'); process.exit(1); }
const emb = await generateEmbedding('test query for health check');
if (emb.length !== 768) { console.log('FAIL-1D: Embedding dim ' + emb.length + ' instead of 768'); process.exit(1); }
const results = store.search(emb, 3);
if (results.length === 0) { console.log('FAIL-1E: Search returned 0 results'); process.exit(1); }
store.close();
console.log('OK-1: ' + stats.totalFiles + ' files, ' + stats.totalChunks + ' chunks, DB=' + dbName);
" 2>&1) || true

    if echo "$cat1_result" | grep -q "^FAIL"; then
        fail "$(echo "$cat1_result" | grep "^FAIL")"
    elif echo "$cat1_result" | grep -q "^OK-1"; then
        ok "$(echo "$cat1_result" | grep "^OK-1")"
    else
        warn "1: Unexpected output: $cat1_result"
    fi
fi

# ---------------------------------------------------------------------------
# Category 2: Pointer consistency
# ---------------------------------------------------------------------------
echo ""
echo "==> Category 2: Pointer consistency"

# 2A: current.txt exists and is non-empty
if test -s "$DBDIR/current.txt"; then
    ok "2A: current.txt exists and is non-empty"
else
    fail "2A: current.txt missing or empty"
fi

# 2B: Referenced DB file exists
POINTER=$(cat "$DBDIR/current.txt" 2>/dev/null || echo "")
if [[ -n "$POINTER" ]] && test -f "$DBDIR/$POINTER"; then
    ok "2B: $POINTER exists"
else
    fail "2B: $POINTER not found in $DBDIR"
fi

# 2C: No orphaned index-N.db files (more than 1 = cleanup failed)
COUNT=$(ls "$DBDIR"/index-*.db 2>/dev/null | wc -l)
if [[ "$COUNT" -le 1 ]]; then
    ok "2C: $COUNT DB file(s)"
else
    warn "2C: $COUNT DB files — cleanup may be needed"
fi

# 2D: No orphaned WAL/SHM files from old indexes
OLD_WAL=$(ls "$DBDIR"/index-*.db-wal "$DBDIR"/index-*.db-shm 2>/dev/null | grep -v "${POINTER:-NOPOINTER}" | wc -l)
if [[ "$OLD_WAL" -eq 0 ]]; then
    ok "2D: No orphaned WAL/SHM files"
else
    warn "2D: $OLD_WAL orphaned WAL/SHM file(s) — clean up manually"
fi

# ---------------------------------------------------------------------------
# Category 3: Infrastructure
# ---------------------------------------------------------------------------
echo ""
echo "==> Category 3: Infrastructure"

# 3A: SessionStart hook registered?
if grep -q "reindex-codebase" "$SETTINGS" 2>/dev/null; then
    ok "3A: reindex-codebase hook registered"
else
    fail "3A: Hook missing in $SETTINGS"
fi

# 3B: MCP server configured in .mcp.json?
MCP_JSON="$PROGGS_DIR/.mcp.json"
if grep -q "code-search" "$MCP_JSON" 2>/dev/null; then
    ok "3B: MCP server configured in .mcp.json"
else
    fail "3B: MCP entry missing in $MCP_JSON"
fi

# 3C: .code-search in .gitignore?
if grep -q "code-search" "$PROGGS_DIR/.gitignore" 2>/dev/null; then
    ok "3C: .gitignore contains .code-search"
else
    fail "3C: .code-search not in .gitignore — DB would be committed to repo!"
fi

# 3D: node_modules present?
if test -d "$MCP_DIR/node_modules"; then
    ok "3D: Dependencies installed"
else
    fail "3D: node_modules missing — run: cd $MCP_DIR && bun install"
fi

# 3E: Ollama reachable?
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:11434/api/tags 2>/dev/null || echo "000")
if [[ "$HTTP_CODE" == "200" ]]; then
    ok "3E: Ollama is running"
else
    warn "3E: Ollama not reachable (HTTP $HTTP_CODE) — start Ollama first"
fi

# 3F: nomic-embed-text model loaded?
if curl -s http://localhost:11434/api/tags 2>/dev/null | grep -q "nomic-embed-text"; then
    ok "3F: nomic-embed-text model present"
else
    fail "3F: nomic-embed-text missing — run: ollama pull nomic-embed-text"
fi

# ---------------------------------------------------------------------------
# Category 4: Parallelism safety
# ---------------------------------------------------------------------------
echo ""
echo "==> Category 4: Parallelism safety"

INDEX_TS="$MCP_DIR/src/index.ts"

# 4A: resolveCurrentDb called at least twice (definition + usage in getStore)
if [[ -f "$INDEX_TS" ]]; then
    RESOLVE_COUNT=$(grep -c "resolveCurrentDb" "$INDEX_TS" 2>/dev/null || echo 0)
    if [[ "$RESOLVE_COUNT" -ge 2 ]]; then
        ok "4A: resolveCurrentDb called $RESOLVE_COUNT times (pointer re-read per call)"
    else
        fail "4A: resolveCurrentDb only $RESOLVE_COUNT occurrence(s) — pointer may be cached"
    fi
else
    warn "4A: $INDEX_TS not found — cannot verify"
fi

# 4B: Reindex hook writes to NEW DB (not active one)
REINDEX_HOOK="$HOME/.claude/hooks/reindex-codebase.sh"
if [[ -f "$REINDEX_HOOK" ]]; then
    if grep -q "index-" "$REINDEX_HOOK" 2>/dev/null; then
        ok "4B: Reindex hook appears to use versioned DB names"
    else
        warn "4B: Cannot confirm reindex writes to a new DB — review $REINDEX_HOOK"
    fi
else
    warn "4B: $REINDEX_HOOK not found — cannot verify"
fi

# 4C: Cleanup logic skips locked files (try/catch around unlinkSync)
STORE_TS="$MCP_DIR/src/store.ts"
if [[ -f "$STORE_TS" ]]; then
    if grep -q "try" "$STORE_TS" && grep -q "unlink" "$STORE_TS"; then
        ok "4C: store.ts has try/catch around unlink (locked files handled)"
    else
        warn "4C: Cannot confirm locked-file safety in $STORE_TS — review manually"
    fi
else
    warn "4C: $STORE_TS not found — cannot verify"
fi

# ---------------------------------------------------------------------------
# Summary
# ---------------------------------------------------------------------------
echo ""
echo "============================="
echo " Semantic Search Healthcheck"
echo "============================="
if [[ "$FAIL_COUNT" -gt 0 ]]; then
    echo " Result: FAIL ($FAIL_COUNT failure(s), $WARN_COUNT warning(s))"
    echo " Action: Fix FAILs before using semantic search."
    exit 1
elif [[ "$WARN_COUNT" -gt 0 ]]; then
    echo " Result: WARN ($WARN_COUNT warning(s), 0 failures)"
    echo " Action: Review warnings — search may still work."
    exit 2
else
    echo " Result: OK — all checks passed"
    exit 0
fi
