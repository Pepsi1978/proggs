#!/usr/bin/env bash
# MCP Auth Check: Detects expired cloud connector sessions at startup
# Hook event: SessionStart (async)
# Platform: macOS/Linux
#
# Checks all MCP servers for "Needs authentication" or "Failed to connect"
# and outputs actionable messages instead of cryptic status codes.

set +e

output=$(claude mcp list 2>&1)

auth_needed=""
failed=""
auth_count=0
fail_count=0

while IFS= read -r line; do
    if echo "$line" | grep -q "Needs authentication"; then
        name=$(echo "$line" | sed 's/\s*[-:].*//; s/\s*$//')
        auth_needed="${auth_needed}${auth_needed:+, }${name}"
        auth_count=$((auth_count + 1))
    elif echo "$line" | grep -q "Failed to connect"; then
        name=$(echo "$line" | sed 's/\s*[-:].*//; s/\s*$//')
        failed="${failed}${failed:+, }${name}"
        fail_count=$((fail_count + 1))
    fi
done <<< "$output"

if [ $auth_count -gt 0 ]; then
    msg="MCP-Auth-Check: ${auth_count} Cloud-Connector(s) haben eine abgelaufene Session: ${auth_needed}. Loesung: claude.ai/settings oeffnen und die Verbindung erneuern (Re-Login)."
    echo "$msg"
    echo "⚠ $msg" >&2
fi

if [ $fail_count -gt 0 ]; then
    msg="MCP-Auth-Check: ${fail_count} MCP-Server nicht erreichbar: ${failed}. Moeglicherweise fehlt ein API-Token oder der Server ist offline."
    echo "$msg"
    echo "⚠ $msg" >&2
fi

if [ $auth_count -eq 0 ] && [ $fail_count -eq 0 ]; then
    echo "MCP-Auth-Check: Alle MCP-Server verbunden."
fi

exit 0
