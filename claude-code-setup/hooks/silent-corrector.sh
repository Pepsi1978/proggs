#!/usr/bin/env bash
# silent-corrector.sh — PreToolUse Hook: Silently corrects known error patterns
# Uses the updatedInput feature (Claude Code v2.0.10+) to fix tool inputs without
# blocking the session. Falls back gracefully if python3 is unavailable.
#
# Corrections implemented:
#   1. /Codex/ paths → /proggs/ paths (Claude must never touch ~/Codex/)
#   2. sed on JSON files → BLOCK with helpful error (cannot auto-convert)
#
# Intentionally NOT implemented:
#   3. git push without fetch+rebase — handled by poka-yoke-git-push.sh already
#
# How updatedInput works (Claude Code v2.0.10+):
#   - Hook receives full tool invocation as JSON on stdin
#   - Format: {"tool_name": "Bash", "tool_input": {"command": "..."}}
#   - To silently fix: output {"updatedInput": {"command": "fixed command"}} + exit 0
#   - To pass through unchanged: output nothing + exit 0
#   - To block: output {"reason": "explanation"} + exit 2
#
# Defense in Depth (Directive #3):
#   - set +e: never abort on error
#   - Every check in its own handler with graceful fallback
#   - Corrections logged to /tmp/silent-corrector.log for auditability
#   - exit 0 at all non-blocking paths
#
# Source: Direktiven-Recherche 2026-04-06, Erkenntnis 3 (updatedInput)

set +e  # Never exit on error — this hook must NEVER break a session

LOG_FILE="/tmp/silent-corrector.log"
TIMESTAMP="$(date '+%Y-%m-%d %H:%M:%S')"

# ─────────────────────────────────────────────────────────────────────
# Helper: log a correction or block event
# ─────────────────────────────────────────────────────────────────────
log_event() {
    local event_type="$1"
    local detail="$2"
    echo "[$TIMESTAMP] $event_type: $detail" >> "$LOG_FILE" 2>/dev/null || true
}

# ─────────────────────────────────────────────────────────────────────
# Read stdin — the full tool invocation JSON
# ─────────────────────────────────────────────────────────────────────
INPUT=""
INPUT=$(cat 2>/dev/null)
if [ -z "$INPUT" ]; then
    # No input means nothing to check — pass through silently
    exit 0
fi

# ─────────────────────────────────────────────────────────────────────
# Determine tool name — only Bash commands need correction
# ─────────────────────────────────────────────────────────────────────
TOOL_NAME=""
TOOL_NAME=$(echo "$INPUT" | python3 -c \
    "import sys, json; print(json.load(sys.stdin).get('tool_name', ''))" \
    2>/dev/null) || true

# If we can't parse the tool name, pass through to avoid false positives
if [ -z "$TOOL_NAME" ]; then
    exit 0
fi

# Only intercept Bash tool calls — other tools don't run shell commands
if [ "$TOOL_NAME" != "Bash" ]; then
    exit 0
fi

# ─────────────────────────────────────────────────────────────────────
# Extract the bash command string from tool_input
# ─────────────────────────────────────────────────────────────────────
COMMAND=""
COMMAND=$(echo "$INPUT" | python3 -c \
    "import sys, json; d = json.load(sys.stdin); print(d.get('tool_input', {}).get('command', ''))" \
    2>/dev/null) || true

# If we can't extract the command, pass through safely
if [ -z "$COMMAND" ]; then
    exit 0
fi

# ─────────────────────────────────────────────────────────────────────
# CORRECTION 1: /Codex/ path → /proggs/ path
#
# Rule: ~/Codex/ is exclusively Codex's workspace. Claude must NEVER
# touch it. This correction silently redirects accidental Codex paths
# to the correct proggs workspace (rule: codex-directory-forbidden.md).
#
# Examples corrected:
#   /Users/frank/Codex/foo.sh → /Users/frank/proggs/foo.sh
#   ~/Codex/CLAUDE.md         → ~/proggs/CLAUDE.md
#   /Codex/hooks/bar.sh       → /proggs/hooks/bar.sh
# ─────────────────────────────────────────────────────────────────────
if echo "$COMMAND" | grep -qE '(/Codex/|~/Codex/)' 2>/dev/null; then
    # Build corrected command: replace all /Codex/ and ~/Codex/ occurrences
    CORRECTED_COMMAND=""
    CORRECTED_COMMAND=$(python3 -c "
import sys, json, re

command = sys.stdin.read()

# Replace ~/Codex/ with ~/proggs/ (tilde form, must come first)
command = command.replace('~/Codex/', '~/proggs/')

# Replace absolute path forms: /Users/<user>/Codex/ → /Users/<user>/proggs/
# Also handles bare /Codex/ paths
command = re.sub(r'(/Users/[^/]+)/Codex/', r'\1/proggs/', command)
command = re.sub(r'\bCodigo/', 'proggs/', command)  # typo guard
# Bare /Codex/ not preceded by /Users/something — replace directly
command = re.sub(r'(?<!/Users/[^/]{1,50})/Codex/', '/proggs/', command)

print(command, end='')
" <<< "$COMMAND" 2>/dev/null) || true

    if [ -n "$CORRECTED_COMMAND" ] && [ "$CORRECTED_COMMAND" != "$COMMAND" ]; then
        log_event "CORRECTED /Codex/→/proggs/" "$COMMAND"

        # Emit updatedInput JSON — python3 ensures proper JSON escaping
        python3 -c "
import json, sys

corrected = sys.stdin.read()
output = json.dumps({'updatedInput': {'command': corrected}})
print(output)
" <<< "$CORRECTED_COMMAND" 2>/dev/null || true

        exit 0
    fi
fi

# ─────────────────────────────────────────────────────────────────────
# CORRECTION 2: sed on JSON files → BLOCK
#
# Rule: sed must never modify JSON files (no-sed-on-json.md).
# sed does not understand JSON syntax and can corrupt files silently.
# Auto-converting sed to python3 is error-prone; blocking is safer here
# and forces use of the Edit tool or python3 json.load/dump.
#
# Pattern: command contains "sed" AND a .json filename
# Examples blocked:
#   sed -i 's/foo/bar/' settings.json
#   sed 's/"key": "old"/"key": "new"/' ~/.mcp.json > /tmp/new.json && mv ...
# ─────────────────────────────────────────────────────────────────────
if echo "$COMMAND" | grep -q '\bsed\b' 2>/dev/null; then
    if echo "$COMMAND" | grep -qE '\.json\b' 2>/dev/null; then
        log_event "BLOCKED sed-on-JSON" "$COMMAND"

        # Output block reason as JSON for Claude Code to display
        python3 -c "
import json
reason = (
    'sed auf JSON-Dateien ist verboten (no-sed-on-json.md). '
    'Bitte das Edit-Tool verwenden oder: '
    'python3 -c \"import json; d=json.load(open(DATEI)); d[KEY]=VALUE; '
    'json.dump(d, open(DATEI, w), indent=2)\"'
)
print(json.dumps({'reason': reason}))
" 2>/dev/null || echo '{"reason": "sed auf JSON verboten — benutze Edit-Tool oder python3 json.load/dump"}'

        exit 2
    fi
fi

# ─────────────────────────────────────────────────────────────────────
# No correction needed — pass through unchanged
# ─────────────────────────────────────────────────────────────────────
exit 0
