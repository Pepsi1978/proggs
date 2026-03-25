#!/usr/bin/env bash
# plugin-health-check.sh — SessionStart diagnostic for plugin services
#
# Runs AFTER all other SessionStart hooks to detect and log plugin failures.
# When Claude Code shows "startup hook error" without details, this log
# tells you exactly which plugin service was unhealthy at startup.
#
# Hook event: SessionStart (MUST be the LAST SessionStart hook group)
# Platform: macOS / Linux

set +e  # Never exit on errors — diagnostic only

HOOKS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$HOOKS_DIR/hook-log.sh" 2>/dev/null || {
    # Minimal fallback if hook-log.sh fails
    _HOOK_LOG_DIR="$HOME/.claude/logs/hooks"
    _HOOK_LOG_FILE="$_HOOK_LOG_DIR/$(date '+%Y-%m-%d').log"
    mkdir -p "$_HOOK_LOG_DIR"
    hook_log() { echo "[$(date '+%H:%M:%S')] plugin-health-check: $1" >> "$_HOOK_LOG_FILE" 2>/dev/null; }
    hook_log_error() { echo "[$(date '+%H:%M:%S')] ERROR plugin-health-check: $1" >> "$_HOOK_LOG_FILE" 2>/dev/null; }
    hook_log_warn() { echo "[$(date '+%H:%M:%S')] WARN  plugin-health-check: $1" >> "$_HOOK_LOG_FILE" 2>/dev/null; }
    hook_log "started"
}

errors=0
checks=0

# ============================================================================
# CHECK 1: claude-mem worker (HTTP service on port 37777)
# ============================================================================
CMEM_PORT=37777
CMEM_HOST="127.0.0.1"
CMEM_SETTINGS="$HOME/.claude-mem/settings.json"
if [ -f "$CMEM_SETTINGS" ]; then
    _p=$(grep -o '"CLAUDE_MEM_WORKER_PORT"[[:space:]]*:[[:space:]]*"[0-9]*"' "$CMEM_SETTINGS" 2>/dev/null | grep -o '[0-9]*')
    [ -n "$_p" ] && CMEM_PORT="$_p"
fi

checks=$((checks + 1))
if curl -sf --connect-timeout 2 --max-time 3 "http://${CMEM_HOST}:${CMEM_PORT}/api/health" >/dev/null 2>&1; then
    hook_log "PASS claude-mem-worker (${CMEM_HOST}:${CMEM_PORT})"
else
    hook_log_error "FAIL claude-mem-worker (${CMEM_HOST}:${CMEM_PORT}) — not responding"
    errors=$((errors + 1))
fi

# ============================================================================
# CHECK 2: Ollama (needed by code-search MCP server)
# ============================================================================
checks=$((checks + 1))
if curl -sf --connect-timeout 2 --max-time 3 "http://localhost:11434/api/tags" >/dev/null 2>&1; then
    hook_log "PASS ollama (localhost:11434)"
else
    hook_log_warn "SKIP ollama (localhost:11434) — not running (code-search will be unavailable)"
fi

# ============================================================================
# CHECK 3: Plugin hooks.json integrity (are expected files present?)
# ============================================================================
PLUGIN_CACHE="$HOME/.claude/plugins/cache"

# claude-mem plugin hooks
CMEM_HOOKS="$PLUGIN_CACHE/thedotmack/claude-mem"
if [ -d "$CMEM_HOOKS" ]; then
    LATEST_VER=$(ls -1d "$CMEM_HOOKS"/*/ 2>/dev/null | sort -V | tail -1)
    if [ -n "$LATEST_VER" ]; then
        checks=$((checks + 1))
        HOOKS_FILE="${LATEST_VER}hooks/hooks.json"
        if [ -f "$HOOKS_FILE" ]; then
            # Check if our fallback patch is present
            if grep -q "sleep.*curl.*api/health.*exit" "$HOOKS_FILE" 2>/dev/null; then
                hook_log "PASS claude-mem hooks.json (patched with fallback)"
            else
                hook_log_warn "claude-mem hooks.json exists but fallback patch missing — next plugin update will re-patch"
            fi
        else
            hook_log_error "FAIL claude-mem hooks.json missing at $HOOKS_FILE"
            errors=$((errors + 1))
        fi
    fi
fi

# ============================================================================
# CHECK 4: Key MCP server dependencies
# ============================================================================

# code-search needs: tsx, better-sqlite3, the index script
MCP_DIR="$HOME/proggs/mcp-code-search"
if [ -d "$MCP_DIR" ]; then
    checks=$((checks + 1))
    missing=""
    [ ! -f "$MCP_DIR/src/index.ts" ] && missing="$missing index.ts"
    [ ! -f "$MCP_DIR/src/db-state.ts" ] && missing="$missing db-state.ts"
    [ ! -f "$MCP_DIR/src/reindex-core.ts" ] && missing="$missing reindex-core.ts"
    [ ! -d "$MCP_DIR/node_modules/better-sqlite3" ] && missing="$missing node_modules/better-sqlite3"

    if [ -z "$missing" ]; then
        hook_log "PASS code-search MCP (all source files present)"
    else
        hook_log_error "FAIL code-search MCP — missing:$missing"
        errors=$((errors + 1))
    fi
fi

# ============================================================================
# CHECK 5: Plugin hook script execute permissions (auto-repair)
# ============================================================================
# Some plugins install .sh hook scripts without +x permission.
# This auto-repairs them to prevent "Permission denied" errors at SessionStart.
PLUGIN_DIRS="$HOME/.claude/plugins/marketplaces $HOME/.claude/plugins/cache"
for pdir in $PLUGIN_DIRS; do
    if [ -d "$pdir" ]; then
        repaired=0
        while IFS= read -r -d '' sh_file; do
            if [ ! -x "$sh_file" ]; then
                chmod +x "$sh_file"
                repaired=$((repaired + 1))
                hook_log "auto-repaired +x permission: $(basename "$sh_file")"
            fi
        done < <(find "$pdir" -name "*.sh" -type f -print0 2>/dev/null)
        if [ "$repaired" -gt 0 ]; then
            hook_log "repaired $repaired plugin hook scripts with missing +x permission"
            # Write to whiteboard so /self-improve can detect the pattern
            source "$HOOKS_DIR/whiteboard-insert.sh" 2>/dev/null || true
            if command -v insert_whiteboard_entry &>/dev/null; then
                entry="### $(date '+%Y-%m-%d %H:%M') — Hook: plugin-health-check.sh — Auto-repaired $repaired plugin .sh files with missing +x permission in $pdir — Status: AUTO-GEFIXT"
                insert_whiteboard_entry "Offene Fehler & Probleme" "$entry" 2>/dev/null || true
            fi
        fi
    fi
done

# ============================================================================
# CHECK 6: launchd agents (macOS only)
# ============================================================================
if [ "$(uname)" = "Darwin" ]; then
    for plist_label in "com.claude-mem.worker" "com.parry.daemon"; do
        checks=$((checks + 1))
        if launchctl list "$plist_label" >/dev/null 2>&1; then
            hook_log "PASS launchd $plist_label (loaded)"
        else
            hook_log_warn "launchd $plist_label not loaded"
        fi
    done
fi

# ============================================================================
# SUMMARY
# ============================================================================
if [ "$errors" -eq 0 ]; then
    hook_log "health check complete: $checks checks passed, 0 errors"
else
    hook_log_error "health check complete: $errors/$checks FAILED — check log above for details"
fi

# Always exit 0 — this is diagnostic only, never blocks the session
exit 0
