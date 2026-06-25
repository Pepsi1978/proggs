#!/usr/bin/env bash
# session-presence-guard.sh: PreToolUse (Edit|Write) — Datei-Waechter gegen Ueberschreiben.
# Wenn die Zieldatei GERADE von einer anderen lebenden Session bearbeitet wird, lehnt der Hook
# den Edit ab (permissionDecision:deny + Begruendung; kein Warten -> kein Deadlock). Duenner
# Wrapper um session-presence.py. stdout -> spec-konformes deny-JSON. Platform: macOS/Linux.
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
. "$SCRIPT_DIR/hook-log.sh"
# FAIL-OPEN: bei jedem internen Fehler graceful exit 0 (Guard blockiert die Session nie versehentlich).
trap 'hook_log_warn "session-presence-guard: error at line $LINENO"; exit 0' ERR

py="$SCRIPT_DIR/session-presence.py"
if command -v python3 >/dev/null 2>&1 && [ -f "$py" ]; then
    input=$(cat)
    if [ -n "$input" ]; then
        out=$(printf '%s' "$input" | python3 "$py" guard 2>/dev/null || true)
        if [ -n "$out" ]; then printf '%s\n' "$out"; fi
    fi
fi

exit 0
