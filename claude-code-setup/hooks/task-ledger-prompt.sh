#!/usr/bin/env bash
# UserPromptSubmit Hook: schreibt einen neuen Task-Eintrag in den Ledger.
# Resilient: Input-Validation, Graceful Failure, IMMER exit 0.

set +e

stdin_input=$(cat)
if [ -z "$stdin_input" ]; then exit 0; fi

# Validate: muss session_id enthalten
session_id=$(printf '%s' "$stdin_input" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    print(d.get('session_id', '') or '')
except Exception:
    print('')
" 2>/dev/null)

if [ -z "$session_id" ]; then exit 0; fi

HELPER="$HOME/.claude/hooks/task-ledger-helper.py"
if [ ! -f "$HELPER" ]; then exit 0; fi

export PYTHONIOENCODING=utf-8
printf '%s' "$stdin_input" | python3 "$HELPER" add >/dev/null 2>&1 || true

exit 0
