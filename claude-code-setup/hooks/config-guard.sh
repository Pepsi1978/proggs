#!/bin/bash
# Config Guard: Verifies protected settings after any config change
# Hook event: ConfigChange
HOOK_NAME="config-guard" source "$HOME/.claude/hooks/hook-log.sh" 2>/dev/null

SETTINGS="$HOME/.claude/settings.json"
if [ ! -f "$SETTINGS" ]; then
    echo "Config-Guard: settings.json nicht gefunden."
    exit 0
fi

# Check protected settings using python3 (available on macOS, Linux, Termux)
VIOLATIONS=$(python3 -c "
import json, sys
try:
    d = json.load(open('$SETTINGS'))
    v = []
    if d.get('effortLevel') != 'high':
        v.append(f\"effortLevel={d.get('effortLevel')}(erwartet:high)\")
    env = d.get('env', {})
    if env.get('CLAUDE_CODE_EFFORT_LEVEL') != 'high':
        v.append(f\"CLAUDE_CODE_EFFORT_LEVEL={env.get('CLAUDE_CODE_EFFORT_LEVEL')}(erwartet:high)\")
    if env.get('CLAUDE_CODE_SUBAGENT_MODEL') != 'sonnet':
        v.append(f\"CLAUDE_CODE_SUBAGENT_MODEL={env.get('CLAUDE_CODE_SUBAGENT_MODEL')}(erwartet:sonnet)\")
    print(' '.join(v) if v else '')
except Exception as e:
    print(f'PARSE_ERROR: {e}', file=sys.stderr)
" 2>/dev/null)

if [ -n "$VIOLATIONS" ]; then
    echo "CONFIG-GUARD: WARNUNG — Geschuetzte Settings geaendert: $VIOLATIONS"
    exit 1
fi

echo "Config-Guard: Alle geschuetzten Einstellungen intakt."
exit 0
