#!/usr/bin/env bash
# Stop Hook: setzt aktuellen Task auf paused/committed/done.
# Resilient: Input-Validation, Graceful Failure, IMMER exit 0.

set +e

stdin_input=$(cat)
if [ -z "$stdin_input" ]; then exit 0; fi

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
printf '%s' "$stdin_input" | python3 "$HELPER" close >/dev/null 2>&1 || true

exit 0
