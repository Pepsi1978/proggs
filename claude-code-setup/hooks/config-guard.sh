#!/bin/bash
# Config Guard: Verifies protected settings after any config change
# Hook event: ConfigChange
# effortLevel: WARNING only (user may intentionally set to medium to save tokens)
HOOK_NAME="config-guard" source "$HOME/.claude/hooks/hook-log.sh" 2>/dev/null

SETTINGS="$HOME/.claude/settings.json"
if [ ! -f "$SETTINGS" ]; then
    echo "Config-Guard: settings.json nicht gefunden."
    exit 0
fi

# Check protected settings using python3 (available on macOS, Linux, Termux)
RESULT=$(python3 -c "
import json, sys
try:
    d = json.load(open('$SETTINGS'))
    warnings = []
    blocks = []
    # effortLevel: Default is medium. Only warn if changed to something unexpected.
    eff = d.get('effortLevel', 'medium')
    if eff not in ('medium', 'high'):
        warnings.append(f'effortLevel={eff}(erwartet:medium oder high)')
    env = d.get('env', {})
    # CLAUDE_CODE_EFFORT_LEVEL in env: default is medium
    env_eff = env.get('CLAUDE_CODE_EFFORT_LEVEL', 'medium')
    if env_eff not in ('medium', 'high'):
        warnings.append(f'CLAUDE_CODE_EFFORT_LEVEL={env_eff}(erwartet:medium oder high)')
    # SUBAGENT_MODEL: BLOCK if changed (critical for cost/quality)
    if env.get('CLAUDE_CODE_SUBAGENT_MODEL') != 'sonnet':
        blocks.append(f\"CLAUDE_CODE_SUBAGENT_MODEL={env.get('CLAUDE_CODE_SUBAGENT_MODEL')}(erwartet:sonnet)\")
    # AUTOCOMPACT: BLOCK if below 95
    acp = env.get('CLAUDE_AUTOCOMPACT_PCT_OVERRIDE', '95')
    if acp and int(acp) < 95:
        blocks.append(f'AUTOCOMPACT={acp}(minimum:95)')
    print(f'WARN:{\" \".join(warnings)}|BLOCK:{\" \".join(blocks)}')
except Exception as e:
    print(f'PARSE_ERROR: {e}', file=sys.stderr)
" 2>/dev/null)

WARNINGS=$(echo "$RESULT" | sed 's/WARN:\(.*\)|BLOCK:.*/\1/')
BLOCKS=$(echo "$RESULT" | sed 's/WARN:.*|BLOCK:\(.*\)/\1/')

if [ -n "$BLOCKS" ] && [ "$BLOCKS" != " " ]; then
    echo "CONFIG-GUARD: BLOCKIERT — Geschuetzte Settings geaendert: $BLOCKS"
    exit 1
fi

if [ -n "$WARNINGS" ] && [ "$WARNINGS" != " " ]; then
    echo "CONFIG-GUARD: WARNUNG — Unerwartete Setting-Aenderung: $WARNINGS"
    echo "Hinweis: Beim naechsten Session-Start wird automatisch auf 'medium' zurueckgesetzt."
    exit 0
fi

echo "Config-Guard: Alle geschuetzten Einstellungen intakt."
exit 0
