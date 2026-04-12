#!/usr/bin/env bash
# doctor-lite.sh — Lightweight SessionStart health check (macOS version)
# Checks: cloud MCP feature flag, dead MCP servers
# Created: 2026-03-26 | Runs in < 2 seconds

warnings=()

# 1. Check if cloud MCP feature flag was reset by server
claude_json="$HOME/.claude.json"
if [ -f "$claude_json" ]; then
    if grep -q '"tengu_claudeai_mcp_connectors": true' "$claude_json" 2>/dev/null; then
        warnings+=("Cloud-MCP-Flag wurde vom Server zurueckgesetzt (tengu_claudeai_mcp_connectors=true). Cloud-Connectoren sind wieder aktiv (~21K Token). Bitte auf claude.ai > Settings > Integrations dauerhaft trennen.")
    fi
fi

# 2. Check sequential-thinking not sneaked back in
if [ -f "$claude_json" ] && grep -q '"sequential-thinking"' "$claude_json" 2>/dev/null; then
    warnings+=("sequential-thinking MCP-Server ist wieder konfiguriert (redundant mit eingebautem Thinking).")
fi

# Output
if [ ${#warnings[@]} -gt 0 ]; then
    echo "Doctor-Lite: ${#warnings[@]} Problem(e):"
    for w in "${warnings[@]}"; do
        echo "  ! $w"
    done
else
    echo "Doctor-Lite: Alle Checks OK."
fi
exit 0
