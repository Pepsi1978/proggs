#!/bin/bash
# redact-secrets.sh — PreToolUse hook: block Write/Edit if content contains secrets
# Hook event: PreToolUse (Write, Edit)
# Platform: macOS / Linux
#
# Strategy:
#   1. Intercept Write/Edit tool calls targeting sensitive files
#   2. Check content for known secret patterns
#   3. If secrets found: BLOCK the write and print a clear warning
#
# ROBUSTNESS: Entire script is safe-fail. Any error → exit 0 (allow).

set -euo pipefail

# Read stdin into variable
HOOK_INPUT=$(cat)

# Empty input → allow
if [ -z "$HOOK_INPUT" ]; then exit 0; fi

# Parse tool name and file path (requires python3 for robust JSON parsing)
TOOL_NAME=$(echo "$HOOK_INPUT" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    print(d.get('tool_name', ''))
except Exception:
    print('')
" 2>/dev/null || true)

# Only intercept Write and Edit
if [ "$TOOL_NAME" != "Write" ] && [ "$TOOL_NAME" != "Edit" ]; then exit 0; fi

# Extract file path and content
read -r FILE_PATH CONTENT <<< "$(echo "$HOOK_INPUT" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    ti = d.get('tool_input', {})
    fp = ti.get('file_path', '')
    if d.get('tool_name') == 'Write':
        content = ti.get('content', '')
    else:
        content = (ti.get('old_string', '') + '\n' + ti.get('new_string', ''))
    # Print as two tab-separated lines for read
    import base64
    enc = base64.b64encode(content.encode('utf-8', errors='replace')).decode('ascii')
    print(fp + '\t' + enc)
except Exception:
    print('\t')
" 2>/dev/null || echo $'\t')"

if [ -z "$FILE_PATH" ]; then exit 0; fi

# Scope: only check sensitive config files
SENSITIVE=false
if echo "$FILE_PATH" | grep -qE 'settings-reference\.json$|settings\.json$|settings\.local\.json$|claude-code-setup/.*\.json$'; then
    SENSITIVE=true
fi

if [ "$SENSITIVE" != "true" ]; then exit 0; fi

# Decode content back from base64
DECODED_CONTENT=$(echo "$CONTENT" | python3 -c "
import sys, base64
enc = sys.stdin.read().strip()
if enc:
    try:
        print(base64.b64decode(enc).decode('utf-8', errors='replace'))
    except Exception:
        print('')
" 2>/dev/null || true)

if [ -z "$DECODED_CONTENT" ]; then exit 0; fi

# Check for secret patterns
FOUND_SECRETS=""

check_pattern() {
    local pattern="$1"
    local label="$2"
    if echo "$DECODED_CONTENT" | grep -qoE "$pattern" 2>/dev/null; then
        PREVIEW=$(echo "$DECODED_CONTENT" | grep -oE "$pattern" 2>/dev/null | head -1 | cut -c1-12)
        FOUND_SECRETS="$FOUND_SECRETS $label($PREVIEW...)"
    fi
}

check_pattern 'ghp_[A-Za-z0-9]{36,}' 'ghp'
check_pattern 'gho_[A-Za-z0-9]{36,}' 'gho'
check_pattern 'github_pat_[A-Za-z0-9_]{36,}' 'github_pat'
check_pattern 'ghs_[A-Za-z0-9]{36,}' 'ghs'
check_pattern 'sk-[A-Za-z0-9]{40,}' 'sk'
check_pattern 'xoxb-[0-9]+-[A-Za-z0-9-]+' 'xoxb'

if [ -z "$FOUND_SECRETS" ]; then exit 0; fi

# Block the write
FILENAME=$(basename "$FILE_PATH")
python3 -c "
import json, sys
msg = '''WARNUNG: Secret erkannt in '$FILENAME' — Write blockiert.
Gefundene Muster: $FOUND_SECRETS
Bitte redacte die Secrets vor dem Schreiben mit: '<REDACTED — set locally>'
Alternativ: python3 ~/.claude/scripts/redact-secrets.py \"$FILE_PATH\"'''
print(json.dumps({'decision': 'block', 'reason': msg}))
" 2>/dev/null || echo '{"decision":"block","reason":"WARNUNG: Secret erkannt — Write blockiert."}'

exit 0
