#!/usr/bin/env bash
# emulator-start-guard: PreToolUse-Blocker fuer nackte Android-Emulator-Starts.
# Logik in emulator-start-guard.py (gemeinsam mit der .ps1-Version). Blockt via permissionDecision=deny.
# Fail-open: faellt Python aus, wird nicht blockiert (kein Crash, kein Hook-Fehler).
input=$(cat)
[ -z "$input" ] && exit 0
printf '%s' "$input" | python3 "$(dirname "$0")/emulator-start-guard.py" 2>/dev/null || true
exit 0
