#!/usr/bin/env bash
# session-presence-warn.sh: UserPromptSubmit — warnt, wenn eine andere lebende Session im SELBEN
# Projekt arbeitet (Awareness gegen versehentliches Ueberschreiben). Duenner Wrapper um
# session-presence.py (eine Logik fuer beide Plattformen). stdout -> AI context (additionalContext).
# Platform: macOS/Linux.
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
. "$SCRIPT_DIR/hook-log.sh"
trap 'hook_log_warn "session-presence-warn: error at line $LINENO"; exit 0' ERR

py="$SCRIPT_DIR/session-presence.py"
if command -v python3 >/dev/null 2>&1 && [ -f "$py" ]; then
    input=$(cat)
    if [ -n "$input" ]; then
        out=$(printf '%s' "$input" | python3 "$py" warn 2>/dev/null || true)
        if [ -n "$out" ]; then printf '%s\n' "$out"; fi
    fi
fi

# PFLICHT: passiver Awareness-Hook endet immer graceful.
exit 0
