#!/bin/bash
# Config Guard: Verifies protected settings after any config change
# Hook event: ConfigChange
HOOK_NAME="config-guard" source "$HOME/.claude/hooks/hook-log.sh" 2>/dev/null || true

SETTINGS="$HOME/.claude/settings.json"
if [ ! -f "$SETTINGS" ]; then
    echo "Config-Guard: settings.json nicht gefunden."
    exit 0
fi

if ! command -v python3 &>/dev/null; then
    echo "Config-Guard: python3 nicht verfuegbar — uebersprungen."
    exit 0
fi

RESULT=$(python3 -c "
import json, sys
try:
    d = json.load(open('$SETTINGS'))
    warnings = []
    blocks = []
    eff = d.get('effortLevel', 'medium')
    if eff not in ('medium', 'high'):
        warnings.append(f'effortLevel={eff}(erwartet:medium oder high)')
    env = d.get('env', {})
    env_eff = env.get('CLAUDE_CODE_EFFORT_LEVEL', 'medium')
    if env_eff not in ('medium', 'high'):
        warnings.append(f'CLAUDE_CODE_EFFORT_LEVEL={env_eff}(erwartet:medium oder high)')
    if env.get('CLAUDE_CODE_SUBAGENT_MODEL') != 'sonnet':
        blocks.append(f\"CLAUDE_CODE_SUBAGENT_MODEL={env.get('CLAUDE_CODE_SUBAGENT_MODEL')}(erwartet:sonnet)\")
    acp = env.get('CLAUDE_AUTOCOMPACT_PCT_OVERRIDE', '95')
    try:
        if acp and int(acp) < 95:
            blocks.append(f'AUTOCOMPACT={acp}(minimum:95)')
    except (ValueError, TypeError):
        pass
    print(f'WARN:{chr(31).join(warnings)}|BLOCK:{chr(31).join(blocks)}')
except Exception as e:
    print('WARN:|BLOCK:')
" 2>/dev/null || echo "WARN:|BLOCK:")

WARNINGS=$(echo "$RESULT" | sed 's/WARN:\(.*\)|BLOCK:.*/\1/' | tr $'\x1f' ' ')
BLOCKS=$(echo "$RESULT" | sed 's/WARN:.*|BLOCK:\(.*\)/\1/' | tr $'\x1f' ' ')

if [ -n "$BLOCKS" ] && [ "$BLOCKS" != " " ] && [ "$BLOCKS" != "" ]; then
    echo "CONFIG-GUARD: BLOCKIERT — Geschuetzte Settings geaendert: $BLOCKS"
    exit 1
fi

if [ -n "$WARNINGS" ] && [ "$WARNINGS" != " " ] && [ "$WARNINGS" != "" ]; then
    echo "CONFIG-GUARD: WARNUNG — Unerwartete Setting-Aenderung: $WARNINGS"
    exit 0
fi

echo "Config-Guard: Alle geschuetzten Einstellungen intakt."
exit 0
