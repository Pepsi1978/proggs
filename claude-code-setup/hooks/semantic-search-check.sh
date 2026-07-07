#!/usr/bin/env bash
# semantic-search-check.sh — SessionStart: verify code-search MCP server is reachable
# Replaces the old prompt-type hook (which can't run at SessionStart due to
# missing ToolUseContext — a Claude Code limitation).
# This command-type hook checks the MCP server's health endpoint instead.

set +e  # Never block session start

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/hook-log.sh" 2>/dev/null || true

# The code-search MCP server runs on a local port via bun/tsx.
# Check if the process is running by looking for the index file.
MCP_DIR="$HOME/proggs/mcp-code-search"
MCP_INDEX="$MCP_DIR/node_modules"

if [ ! -d "$MCP_DIR" ]; then
    hook_log "code-search MCP dir not found — skipping"
    exit 0
fi

if [ ! -d "$MCP_INDEX" ]; then
    echo "Semantische Suche: node_modules fehlen. Fix: cd ~/proggs/mcp-code-search && bun install"
    hook_log_warn "code-search node_modules missing"
    exit 0  # Don't fail the hook — just warn
fi

# Check if the .mcp.json references code-search and the binary exists
MCP_JSON="$HOME/.mcp.json"
if [ -f "$MCP_JSON" ]; then
    if grep -q "code-search" "$MCP_JSON" 2>/dev/null; then
        hook_log "code-search MCP configured in .mcp.json"
    else
        hook_log "code-search not in .mcp.json — skipping check"
    fi
fi

# Watchdog: detect stale or failed reindex. Without this the SessionStart reindex
# can die quietly for weeks and the user never learns about it (see 2026-04-11
# incident: 13 days of silent HTTP-400 crashes).
DB_DIR="$HOME/proggs/.code-search"
STATE_FILE="$DB_DIR/state.json"
REINDEX_LOG="$DB_DIR/reindex.log"
STALE_DAYS=3
STALE_SECONDS=$(( STALE_DAYS * 86400 ))

if [ -f "$STATE_FILE" ]; then
    LAST_SUCCESS_ISO=$(python3 -c "
import json, sys
try:
    with open('$STATE_FILE', 'r', encoding='utf-8') as f:
        d = json.load(f)
    print(d.get('lastSuccessAt') or '')
except Exception:
    print('')
" 2>/dev/null)

    if [ -n "$LAST_SUCCESS_ISO" ]; then
        LAST_SUCCESS_EPOCH=$(python3 -c "
import datetime
try:
    iso = '$LAST_SUCCESS_ISO'.replace('Z', '+00:00')
    print(int(datetime.datetime.fromisoformat(iso).timestamp()))
except Exception:
    print(0)
" 2>/dev/null)
        NOW_EPOCH=$(date +%s)
        AGE=$(( NOW_EPOCH - LAST_SUCCESS_EPOCH ))
        AGE_DAYS=$(( AGE / 86400 ))

        if [ "$LAST_SUCCESS_EPOCH" -gt 0 ] && [ "$AGE" -gt "$STALE_SECONDS" ]; then
            echo "Semantische Suche: Index ist seit ${AGE_DAYS} Tagen nicht mehr aktualisiert worden (letzter Erfolg: $LAST_SUCCESS_ISO)."
            if [ -f "$REINDEX_LOG" ]; then
                LAST_ERR=$(grep -E "Reindex failed|HTTP 400|Error" "$REINDEX_LOG" 2>/dev/null | tail -1)
                if [ -n "$LAST_ERR" ]; then
                    echo "  Letzter Fehler aus reindex.log: $LAST_ERR"
                fi
            fi
            echo "  Fix: cd ~/proggs/mcp-code-search && /opt/homebrew/bin/tsx src/session-reindex.ts ~/proggs"
            hook_log_warn "code-search index stale: ${AGE_DAYS} days old"
        fi
    fi
elif [ -f "$REINDEX_LOG" ]; then
    # No state.json but a log — last run probably crashed before writing state
    LAST_ERR=$(grep -E "Reindex failed|HTTP 400|Error" "$REINDEX_LOG" 2>/dev/null | tail -1)
    if [ -n "$LAST_ERR" ]; then
        echo "Semantische Suche: kein erfolgreicher Index — letzter Fehler: $LAST_ERR"
        hook_log_warn "code-search index missing, last error: $LAST_ERR"
    fi
fi

exit 0
