#!/usr/bin/env bash
# bug-almanac-hint.sh: passiver UserPromptSubmit-Trigger — scannt Prompt auf Almanach-Bereichs-Stichwoerter
# Runs as UserPromptSubmit hook
# stdout -> AI context (hookSpecificOutput.additionalContext JSON), stderr -> user terminal
# Platform: macOS/Linux. Logik in bug-almanac-hint.py (Cross-Platform-Konsistenz, eine Stichwort-Map).
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
. "$SCRIPT_DIR/hook-log.sh"

trap 'hook_log_warn "bug-almanac-hint: error at line $LINENO"' ERR

py="$SCRIPT_DIR/bug-almanac-hint.py"
if command -v python3 >/dev/null 2>&1 && [ -f "$py" ]; then
    input=$(cat)
    if [ -n "$input" ]; then
        out=$(printf '%s' "$input" | python3 "$py" 2>/dev/null || true)
        if [ -n "$out" ]; then printf '%s\n' "$out"; fi
    fi
fi

# PFLICHT: passiver Hint-Hook endet immer graceful.
exit 0
