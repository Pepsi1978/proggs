#!/usr/bin/env bash
# build-ablauf-guard: PreToolUse-Blocker: Android-Bau vor Versions-Bump und Kill-Befehle, die die eigene Shell treffen.
# Logik in build-ablauf-guard.py (Windows ruft sie direkt auf, Mac und Server über diese Hülle). Blockt via permissionDecision=deny.
# Fail-open: faellt Python aus, wird nicht blockiert (kein Crash, kein Hook-Fehler).
input=$(cat)
[ -z "$input" ] && exit 0
printf '%s' "$input" | python3 "$(dirname "$0")/build-ablauf-guard.py" 2>/dev/null || true
exit 0
