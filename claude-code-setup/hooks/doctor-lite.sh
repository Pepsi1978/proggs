#!/usr/bin/env bash
# doctor-lite.sh — Lightweight SessionStart health check (macOS version)
# Self-healing: auto-fixes known issues instead of just warning
# Created: 2026-03-26 | Updated: 2026-04-03
# FIX: Use Python JSON parsing instead of grep to avoid false positives
# from cachedGrowthBookFeatures (server-side cache, not user settings).

set +e
claude_json="$HOME/.claude.json"

result=$(python3 -c "
import json, sys

fixes = []
warnings = []
path = '$claude_json'
try:
    with open(path) as f:
        data = json.load(f)
except Exception:
    print('Doctor-Lite: .claude.json nicht lesbar')
    sys.exit(0)

changed = False

# 1. Check top-level cloud MCP flag (not the cached server flag)
if data.get('tengu_claudeai_mcp_connectors') == True:
    data['tengu_claudeai_mcp_connectors'] = False
    changed = True
    fixes.append('Cloud-MCP-Flag automatisch deaktiviert')

# 2. Check mcpServers for sequential-thinking
mcp = data.get('mcpServers', {})
if 'sequential-thinking' in mcp:
    del mcp['sequential-thinking']
    changed = True
    fixes.append('sequential-thinking MCP-Server automatisch entfernt (redundant)')

if changed:
    with open(path, 'w') as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write('\n')

if fixes or warnings:
    total = len(fixes) + len(warnings)
    print(f'Doctor-Lite: {total} Problem(e):')
    for f in fixes:
        print(f'  fix {f}')
    for w in warnings:
        print(f'  ! {w}')
else:
    print('Doctor-Lite: Alle Checks OK.')
" 2>&1)

echo "$result"
exit 0
