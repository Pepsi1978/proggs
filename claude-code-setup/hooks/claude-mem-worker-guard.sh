#!/usr/bin/env bash
# claude-mem-worker-guard.sh — Pre-warm claude-mem worker before plugin hooks
#
# Problem: When Claude Code starts, both the MCP server and SessionStart hooks
# try to start the worker daemon on port 37777 simultaneously. The second one
# fails with "port in use", causing 2 hook errors on every first session start.
# On subsequent starts the worker is already running, so no errors occur.
#
# Solution: Three-phase approach:
#   Phase 1: Quick health check (instant return if worker already running)
#   Phase 2: Wait 3 seconds for MCP server to start the worker
#   Phase 3: Actively start the worker ourselves if still not running
#
# Hook event: SessionStart (must be the FIRST SessionStart hook in settings.json)
# Platform: macOS / Linux

HOOKS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$HOOKS_DIR/hook-log.sh" 2>/dev/null || true

# --- Configuration ---
WORKER_PORT=37777
WORKER_URL="http://127.0.0.1:$WORKER_PORT"
MAX_WAIT_PASSIVE=6     # Passive wait iterations (6 * 0.5s = 3s)
MAX_WAIT_ACTIVE=8      # Active wait iterations after starting (8 * 0.5s = 4s)

# --- Guards ---

# Only run if claude-mem plugin exists
PLUGIN_CACHE="$HOME/.claude/plugins/cache/thedotmack/claude-mem"
if [ ! -d "$PLUGIN_CACHE" ]; then
    exit 0
fi

# Find latest plugin version in cache (survives plugin updates)
PLUGIN_ROOT=""
if [ -d "$PLUGIN_CACHE" ]; then
    PLUGIN_ROOT=$(ls -1d "$PLUGIN_CACHE"/*/ 2>/dev/null | sort -V | tail -1)
fi

# Fallback to marketplace path
if [ -z "$PLUGIN_ROOT" ] || [ ! -f "${PLUGIN_ROOT}scripts/worker-service.cjs" ]; then
    PLUGIN_MKT="$HOME/.claude/plugins/marketplaces/thedotmack/plugin"
    if [ -d "$PLUGIN_MKT" ] && [ -f "$PLUGIN_MKT/scripts/worker-service.cjs" ]; then
        PLUGIN_ROOT="$PLUGIN_MKT/"
    fi
fi

# --- Health check function ---
worker_healthy() {
    curl -s --connect-timeout 1 --max-time 2 "$WORKER_URL/api/health" >/dev/null 2>&1
}

# --- Phase 1: Quick check ---
if worker_healthy; then
    hook_log "claude-mem worker already healthy" 2>/dev/null || true
    exit 0
fi

hook_log "claude-mem worker not ready — starting pre-warm sequence" 2>/dev/null || true

# --- Phase 2: Passive wait (MCP server starts the worker) ---
for i in $(seq 1 $MAX_WAIT_PASSIVE); do
    sleep 0.5
    if worker_healthy; then
        hook_log "claude-mem worker healthy after passive wait ($(echo "$i * 0.5" | bc)s)" 2>/dev/null || true
        exit 0
    fi
done

# --- Phase 3: Active start (if MCP server didn't start it) ---
if [ -n "$PLUGIN_ROOT" ] && [ -f "${PLUGIN_ROOT}scripts/worker-service.cjs" ]; then
    # Find Bun runtime
    BUN=""
    for candidate in /opt/homebrew/bin/bun "$HOME/.bun/bin/bun" /usr/local/bin/bun /home/linuxbrew/.linuxbrew/bin/bun; do
        if [ -x "$candidate" ]; then
            BUN="$candidate"
            break
        fi
    done

    if [ -n "$BUN" ]; then
        hook_log "actively starting claude-mem worker daemon" 2>/dev/null || true
        # Start worker as daemon in background
        "$BUN" "${PLUGIN_ROOT}scripts/worker-service.cjs" --daemon >/dev/null 2>&1 &
        disown 2>/dev/null || true

        # Wait for it to become healthy
        for i in $(seq 1 $MAX_WAIT_ACTIVE); do
            sleep 0.5
            if worker_healthy; then
                hook_log "claude-mem worker healthy after active start ($(echo "$i * 0.5" | bc)s)" 2>/dev/null || true
                exit 0
            fi
        done
    fi
fi

# --- Fallback: Worker didn't start, but don't block the session ---
hook_log "claude-mem worker not ready after full pre-warm — continuing" 2>/dev/null || true
exit 0
