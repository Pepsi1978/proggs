#!/usr/bin/env bash
# claude-mem-worker-launcher.sh — Launchd wrapper for claude-mem worker
#
# Dynamically finds the latest plugin version and starts the worker.
# Used by com.claude-mem.worker.plist (launchd agent).
#
# RESILIENCE FEATURES:
# - Resolves plugin path at runtime (survives plugin updates)
# - Searches multiple bun locations (survives bun version updates)
# - Waits patiently if plugin is temporarily missing (during updates)
# - Checks if worker is already running (prevents duplicate processes)
# - Logs to /tmp/claude-mem-worker.log for debugging
# - Never exit 1 immediately — wait and retry to avoid launchd crash-loop

set -e

LOG_PREFIX() { echo "$(date -Iseconds 2>/dev/null || date)"; }

# --- Pre-flight: check if worker already running on expected port ---
CMEM_PORT=37777
CMEM_SETTINGS="$HOME/.claude-mem/settings.json"
if [ -f "$CMEM_SETTINGS" ]; then
    _p=$(grep -o '"CLAUDE_MEM_WORKER_PORT"[[:space:]]*:[[:space:]]*"[0-9]*"' "$CMEM_SETTINGS" 2>/dev/null | grep -o '[0-9]*')
    [ -n "$_p" ] && CMEM_PORT="$_p"
fi

if curl -sf --connect-timeout 1 --max-time 2 "http://127.0.0.1:${CMEM_PORT}/api/health" >/dev/null 2>&1; then
    echo "$(LOG_PREFIX) Worker already running on port $CMEM_PORT — exiting cleanly"
    # Sleep long to prevent launchd from restarting us immediately
    sleep 300
    exit 0
fi

# --- Find plugin directory (wait up to 60s if temporarily missing during update) ---
PLUGIN_DIR="$HOME/.claude/plugins/cache/thedotmack/claude-mem"
MAX_WAIT=60
waited=0

while [ ! -d "$PLUGIN_DIR" ] && [ "$waited" -lt "$MAX_WAIT" ]; do
    echo "$(LOG_PREFIX) Waiting for plugin directory ($waited/${MAX_WAIT}s)..." >&2
    sleep 5
    waited=$((waited + 5))
done

if [ ! -d "$PLUGIN_DIR" ]; then
    echo "$(LOG_PREFIX) Plugin not installed after ${MAX_WAIT}s — sleeping 5min before retry" >&2
    sleep 300
    exit 0  # Exit 0 so KeepAlive respawns us after ThrottleInterval
fi

LATEST_VERSION=$(ls -1d "$PLUGIN_DIR"/*/ 2>/dev/null | sort -V | tail -1)
if [ -z "$LATEST_VERSION" ]; then
    echo "$(LOG_PREFIX) No plugin versions found — sleeping 5min before retry" >&2
    sleep 300
    exit 0
fi

WORKER_SCRIPT="${LATEST_VERSION}scripts/worker-service.cjs"
if [ ! -f "$WORKER_SCRIPT" ]; then
    echo "$(LOG_PREFIX) worker-service.cjs not found at $WORKER_SCRIPT — sleeping 5min" >&2
    sleep 300
    exit 0
fi

# --- Find bun binary ---
BUN=""
for candidate in /opt/homebrew/bin/bun /opt/homebrew/Cellar/bun/*/bin/bun "$HOME/.bun/bin/bun" /usr/local/bin/bun; do
    for resolved in $candidate; do
        [ -x "$resolved" ] && BUN="$resolved" && break 2
    done
done
if [ -z "$BUN" ]; then
    command -v bun >/dev/null 2>&1 && BUN="bun"
fi
# Fallback: try node if bun not available
if [ -z "$BUN" ]; then
    for node_candidate in /opt/homebrew/bin/node /usr/local/bin/node; do
        [ -x "$node_candidate" ] && BUN="$node_candidate" && break
    done
    command -v node >/dev/null 2>&1 && BUN="node"
fi

if [ -z "$BUN" ]; then
    echo "$(LOG_PREFIX) Neither bun nor node found — sleeping 5min" >&2
    sleep 300
    exit 0
fi

echo "$(LOG_PREFIX) Starting claude-mem worker: $BUN $WORKER_SCRIPT --daemon"
exec "$BUN" "$WORKER_SCRIPT" --daemon
