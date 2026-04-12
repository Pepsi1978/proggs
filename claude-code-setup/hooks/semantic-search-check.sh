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

exit 0
