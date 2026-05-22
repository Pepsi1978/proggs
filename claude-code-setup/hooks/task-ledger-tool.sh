#!/usr/bin/env bash
# PostToolUse Hook: haengt Files/Commits an aktuellen Task-Eintrag.
# Matcher in settings.json beschraenkt auf Edit|Write|MultiEdit|Bash.
# Resilient: Input-Validation, Graceful Failure, IMMER exit 0.

set +e

stdin_input=$(cat)
if [ -z "$stdin_input" ]; then exit 0; fi

read_field() {
    printf '%s' "$stdin_input" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    print(d.get('$1', '') or '')
except Exception:
    print('')
" 2>/dev/null
}

session_id=$(read_field session_id)
tool_name=$(read_field tool_name)
if [ -z "$session_id" ] || [ -z "$tool_name" ]; then exit 0; fi

HELPER="$HOME/.claude/hooks/task-ledger-helper.py"
if [ ! -f "$HELPER" ]; then exit 0; fi

export PYTHONIOENCODING=utf-8
printf '%s' "$stdin_input" | python3 "$HELPER" update >/dev/null 2>&1 || true

exit 0
