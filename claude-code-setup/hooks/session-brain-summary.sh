#!/usr/bin/env bash
# session-brain-summary: SessionEnd — schickt die Session-Rohdaten (Prompts, Commits, Dateien)
# an den Second-Brain-Agenten (POST /session-log), der sie per LLM zu "gemacht/entschieden/
# gelernt" verdichtet und unter Programmierung/Sessions ins Gehirn legt + den Kern-Block
# "Woran Frank gerade baut" nachzieht (Gruppe D 27/28/32). Duenner Wrapper um
# session-brain-summary.py (EINE Logik fuer beide Plattformen).
# Runs as SessionEnd hook (command, async). Fail-open: blockiert ein Session-Ende NIE.
# Platform: macOS/Linux

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
. "$SCRIPT_DIR/hook-log.sh"

# exit 0 IM trap (PFLICHT) — sonst beendet set -e bei einem Fehler mit non-zero (claude-hooks.md 13.4).
trap 'hook_log_warn "session-brain-summary: Unexpected error at line $LINENO"; exit 0' ERR

py="$SCRIPT_DIR/session-brain-summary.py"
if command -v python3 >/dev/null 2>&1 && [ -f "$py" ]; then
    input=$(cat || true)
    if [ -n "$input" ]; then
        printf '%s' "$input" | python3 "$py" >/dev/null 2>&1 || true
    fi
fi

# PFLICHT: Jeder Hook MUSS mit exit 0 enden — ausnahmslos.
exit 0
