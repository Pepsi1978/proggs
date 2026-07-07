#!/usr/bin/env bash
# subagent-stop-summarizer.sh — SubagentStop Hook (type: prompt via command)
# Event: SubagentStop
# Purpose: Injects summary prompt ONLY when a real subagent has completed.
#          Prevents infinite loop caused by type:"prompt" hooks firing on every Stop.
#
# HOW IT WORKS:
#   Gemini CLI passes subagent info via stdin as JSON.
#   We read stdin, check for subagent_id. If empty -> silent exit 0.
#   If real subagent -> output prompt text to stdout (Gemini reads it as injected prompt).
#
# POKA-YOKE: type:"command" hooks that output text inject that text as a prompt.
#   This only fires when Gemini CLI actually calls SubagentStop with a subagent_id.
#
# EXIT CODES:
#   0 = success (with or without output)
#   Non-0 = error (Gemini CLI may log but continues)

set -euo pipefail

# Source hook-log if available
if [ -f "$(dirname "$0")/hook-log.sh" ]; then
    # shellcheck source=/dev/null
    . "$(dirname "$0")/hook-log.sh" 2>/dev/null || true
fi

# Read stdin (subagent context passed by Gemini CLI)
INPUT_JSON=""
if [ -t 0 ]; then
    # No stdin available
    INPUT_JSON=""
else
    INPUT_JSON=$(cat 2>/dev/null || true)
fi

# Parse subagent_id from JSON (try multiple field names)
SUBAGENT_ID=""
if [ -n "$INPUT_JSON" ] && command -v python3 &>/dev/null; then
    SUBAGENT_ID=$(python3 -c "
import sys, json
try:
    d = json.loads(sys.stdin.read())
    sid = d.get('subagent_id') or d.get('id') or d.get('agent_id') or ''
    print(sid)
except:
    print('')
" <<< "$INPUT_JSON" 2>/dev/null || true)
fi

# GUARD: If no real subagent_id found, exit silently
# This prevents the infinite loop where type:"prompt" fires on every response
if [ -z "$SUBAGENT_ID" ]; then
    exit 0
fi

# Real subagent completed — inject summary prompt
cat <<EOF
Ein Subagent hat gerade seine Arbeit beendet (ID: $SUBAGENT_ID).
Fasse in einem Satz auf Deutsch zusammen, was er erreicht hat.

Falls der Subagent FEHLGESCHLAGEN ist oder einen Fehler zurueckgegeben hat:
1. Schreibe eine Sentinel-Datei: /tmp/agent-writeback-subagent-failure.json
   Format: {"agent": "subagent-failure", "section": "Offene Fehler & Probleme", "findings": "[Beschreibung]", "timestamp": "[ISO-Datum]"}
2. Die 'findings' muessen enthalten: Agent-Name, Symptom, Root Cause, betroffene Dateien, konkreter Fix-Vorschlag.
3. Fehler von ALLEN Quellen (custom agents, plugins, marketplace agents) MUESSEN geloggt werden.
4. NIEMALS direkt in MEMORY.md schreiben — immer die Sentinel-Datei verwenden.
EOF

exit 0

