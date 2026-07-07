#!/usr/bin/env bash
# claude-mem-worker-guard.sh — Universal service readiness & plugin hooks hardener
#
# PRIMARY PURPOSE: Prevent SessionStart hook errors from race conditions between
# MCP servers and plugin hooks. Currently handles claude-mem's worker daemon,
# but designed to be extended for ANY future plugin that starts background services.
#
# ARCHITECTURE (4 phases):
#   Phase 0: Auto-patch ALL plugin hooks.json files (self-healing after updates)
#   Phase 1: Quick health check for all known services
#   Phase 2: Passive wait for services to become ready
#   Phase 3: Active start of services if still not running
#
# ROBUSTNESS FEATURES:
#   - Dynamic port detection from ~/.claude-mem/settings.json
#   - Multiple health check methods (curl → nc → /dev/tcp)
#   - Auto-patches plugin hooks.json on EVERY start (survives plugin updates)
#   - Finds plugin root across cache versions and marketplace paths
#   - General-purpose pattern: add new services to SERVICES array
#   - Always exits 0 (NEVER blocks the session)
#
# Hook event: SessionStart (must be the FIRST SessionStart hook in settings.json)
# Platform: macOS / Linux

set +e  # Never exit on errors — this hook must always succeed

HOOKS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$HOOKS_DIR/hook-log.sh" 2>/dev/null || hook_log() { :; }

# ============================================================================
# SECTION 1: SERVICE REGISTRY
# Add new services here when new plugins need pre-warming.
# Format: "name|host|port|plugin_cache_path_pattern|start_command_pattern"
# ============================================================================

# Read claude-mem port dynamically
CMEM_PORT=37777
CMEM_HOST="127.0.0.1"
CMEM_SETTINGS="$HOME/.claude-mem/settings.json"
if [ -f "$CMEM_SETTINGS" ]; then
    _p=$(grep -o '"CLAUDE_MEM_WORKER_PORT"[[:space:]]*:[[:space:]]*"[0-9]*"' "$CMEM_SETTINGS" 2>/dev/null | grep -o '[0-9]*')
    [ -n "$_p" ] && CMEM_PORT="$_p"
    _h=$(grep -o '"CLAUDE_MEM_WORKER_HOST"[[:space:]]*:[[:space:]]*"[^"]*"' "$CMEM_SETTINGS" 2>/dev/null | sed 's/.*"\([^"]*\)"$/\1/')
    [ -n "$_h" ] && CMEM_HOST="$_h"
fi

# Service registry — extend this list for future plugins
# Each entry: name|host|port
SERVICES=(
    "claude-mem-worker|${CMEM_HOST}|${CMEM_PORT}"
    # Future services go here, e.g.:
    # "some-other-plugin|127.0.0.1|8080"
)

# ============================================================================
# SECTION 2: PLUGIN ROOT RESOLVER
# ============================================================================

find_plugin_root() {
    local vendor="$1"  # e.g. "thedotmack/claude-mem"
    local marker="$2"  # file that must exist, e.g. "scripts/worker-service.cjs"
    local root=""

    # Method 1: Plugin cache (survives across versions)
    local cache_dir="$HOME/.claude/plugins/cache/$vendor"
    if [ -d "$cache_dir" ]; then
        root=$(ls -1d "$cache_dir"/*/ 2>/dev/null | sort -V | tail -1)
        if [ -n "$root" ] && [ -f "${root}${marker}" ]; then
            echo "$root"
            return 0
        fi
    fi

    # Method 2: Marketplace path (older install format)
    local vendor_short=$(echo "$vendor" | cut -d'/' -f1)
    local mkt_dir="$HOME/.claude/plugins/marketplaces/$vendor_short/plugin"
    if [ -f "$mkt_dir/$marker" ]; then
        echo "$mkt_dir/"
        return 0
    fi

    return 1
}

# ============================================================================
# SECTION 3: HEALTH CHECK FUNCTIONS (multiple methods for max reliability)
# ============================================================================

check_http() {
    local url="$1"
    curl -s --connect-timeout 1 --max-time 2 "$url" >/dev/null 2>&1
}

check_port_nc() {
    local host="$1" port="$2"
    nc -z -w 1 "$host" "$port" >/dev/null 2>&1
}

check_port_tcp() {
    local host="$1" port="$2"
    (echo > "/dev/tcp/${host}/${port}") >/dev/null 2>&1
}

service_healthy() {
    local host="$1" port="$2"
    check_http "http://${host}:${port}/api/health" && return 0
    check_port_nc "$host" "$port" && return 0
    check_port_tcp "$host" "$port" && return 0
    return 1
}

# ============================================================================
# SECTION 4: AUTO-PATCHER (self-healing after plugin updates)
# Scans ALL plugin hooks.json files and adds resilience to service commands.
# ============================================================================

patch_plugin_hooks() {
    local hooks_json="$1"
    local host="$2"
    local port="$3"

    [ -f "$hooks_json" ] || return 0

    # Already patched? (look for our fallback signature)
    if grep -q "curl -sf.*:${port}/api/health.*exit 0" "$hooks_json" 2>/dev/null; then
        return 0
    fi

    # Contains service start commands that could race?
    if ! grep -q 'worker-service\|daemon\|server.*start' "$hooks_json" 2>/dev/null; then
        return 0
    fi

    hook_log "auto-patching $(basename "$(dirname "$(dirname "$hooks_json")")")/hooks.json" 2>/dev/null || true

    local tmp_file="${hooks_json}.guard-tmp"
    local fallback="|| { sleep 5 \&\& curl -sf http://${host}:${port}/api/health >\/dev\/null 2>\&1 \&\& exit 0 || exit 1; }"

    # Patch worker-service start and context commands
    sed \
        -e "s|worker-service\.cjs\" start\"|worker-service.cjs\" start ${fallback}\"|" \
        -e "s|worker-service\.cjs\" hook claude-code context\"|worker-service.cjs\" hook claude-code context ${fallback}\"|" \
        "$hooks_json" > "$tmp_file" 2>/dev/null

    if [ -s "$tmp_file" ] && ! diff -q "$hooks_json" "$tmp_file" >/dev/null 2>&1; then
        mv "$tmp_file" "$hooks_json" 2>/dev/null
        hook_log "hooks.json patched successfully" 2>/dev/null || true
    else
        rm -f "$tmp_file" 2>/dev/null
    fi
}

# ============================================================================
# SECTION 5: MAIN EXECUTION
# ============================================================================

all_healthy=true

for service_entry in "${SERVICES[@]}"; do
    IFS='|' read -r svc_name svc_host svc_port <<< "$service_entry"

    # --- Check if service's plugin is installed ---
    case "$svc_name" in
        claude-mem-worker)
            PLUGIN_ROOT=$(find_plugin_root "thedotmack/claude-mem" "scripts/worker-service.cjs")
            [ -z "$PLUGIN_ROOT" ] && continue  # Plugin not installed
            # Phase 0: Auto-patch
            patch_plugin_hooks "${PLUGIN_ROOT}hooks/hooks.json" "$svc_host" "$svc_port"
            ;;
        *)
            # Generic — just check the port
            ;;
    esac

    # --- Phase 1: Quick check ---
    if service_healthy "$svc_host" "$svc_port"; then
        hook_log "$svc_name healthy (${svc_host}:${svc_port})" 2>/dev/null || true
        continue
    fi

    all_healthy=false
    hook_log "$svc_name not ready — starting pre-warm for ${svc_host}:${svc_port}" 2>/dev/null || true

    # --- Phase 2: Passive wait (3 seconds) ---
    ready=false
    for i in $(seq 1 6); do
        sleep 0.5
        if service_healthy "$svc_host" "$svc_port"; then
            hook_log "$svc_name healthy after passive wait" 2>/dev/null || true
            ready=true
            break
        fi
    done
    $ready && continue

    # --- Phase 3: Active start ---
    case "$svc_name" in
        claude-mem-worker)
            if [ -n "$PLUGIN_ROOT" ] && [ -f "${PLUGIN_ROOT}scripts/worker-service.cjs" ]; then
                BUN=""
                for candidate in /opt/homebrew/bin/bun "$HOME/.bun/bin/bun" /usr/local/bin/bun /home/linuxbrew/.linuxbrew/bin/bun; do
                    [ -x "$candidate" ] && BUN="$candidate" && break
                done
                [ -z "$BUN" ] && command -v bun >/dev/null 2>&1 && BUN="bun"

                if [ -n "$BUN" ]; then
                    hook_log "actively starting $svc_name via $BUN" 2>/dev/null || true
                    "$BUN" "${PLUGIN_ROOT}scripts/worker-service.cjs" --daemon >/dev/null 2>&1 &
                    disown 2>/dev/null || true

                    for i in $(seq 1 8); do
                        sleep 0.5
                        if service_healthy "$svc_host" "$svc_port"; then
                            hook_log "$svc_name healthy after active start" 2>/dev/null || true
                            ready=true
                            break
                        fi
                    done
                fi
            fi
            ;;
        *)
            hook_log "$svc_name: no active start handler — waiting only" 2>/dev/null || true
            # Generic: just wait a bit more
            for i in $(seq 1 4); do
                sleep 0.5
                service_healthy "$svc_host" "$svc_port" && break
            done
            ;;
    esac
done

exit 0
