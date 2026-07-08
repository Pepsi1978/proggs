#!/usr/bin/env bash
# rule-size-guard: PreToolUse-Blocker fuer Regel-Dateien > 2 KB (rule-size-limit.md).
# Logik in rule-size-guard.py (gemeinsam mit der .ps1-Version). Blockt via permissionDecision=deny.
# Fail-open: faellt Python aus, wird nicht blockiert (kein Crash, kein Hook-Fehler).
input=$(cat)
[ -z "$input" ] && exit 0
printf '%s' "$input" | python3 "$(dirname "$0")/rule-size-guard.py" 2>/dev/null || true
exit 0
