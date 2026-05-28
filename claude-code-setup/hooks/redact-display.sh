#!/bin/bash
# redact-display.sh — MessageDisplay hook: redact secrets from the DISPLAYED assistant text
# Hook event: MessageDisplay (added in Claude Code v2.1.152)
# Platform: macOS / Linux
#
# Strategy:
#   1. Read the assistant message_text from stdin (MessageDisplay event)
#   2. If it contains known secret patterns, replace them with <REDACTED>
#   3. Emit hookSpecificOutput.displayContent so ONLY the on-screen display changes.
#      Per the MessageDisplay contract the transcript file and Claude's own context
#      keep the ORIGINAL text — this hook never alters what Claude sees.
#
# Companion to redact-secrets.sh (PreToolUse, guards file writes). This one guards
# the visible answer text — e.g. a token Claude echoed back into its reply.
#
# ROBUSTNESS: safe-fail. Any error, empty input, or no match → exit 0 WITHOUT
# emitting displayContent → the ORIGINAL text is shown unchanged. A broken hook
# must NEVER blank or mangle the user's view.

set +e  # Hooks muessen IMMER weiterlaufen, nie bei Fehler abbrechen

HOOK_INPUT=$(cat)
if [ -z "$HOOK_INPUT" ]; then exit 0; fi

printf '%s' "$HOOK_INPUT" | python3 -c "
import sys, json, re
try:
    d = json.load(sys.stdin)
except Exception:
    sys.exit(0)

# Input-Guard: nur auf echte MessageDisplay-Events reagieren
if d.get('hook_event_name') != 'MessageDisplay':
    sys.exit(0)

text = d.get('message_text', '')
if not text:
    sys.exit(0)

patterns = [
    r'ghp_[A-Za-z0-9]{36,}',          # GitHub Personal Access Token
    r'gho_[A-Za-z0-9]{36,}',          # GitHub OAuth Token
    r'ghu_[A-Za-z0-9]{36,}',          # GitHub User-to-Server Token
    r'ghs_[A-Za-z0-9]{36,}',          # GitHub Server-to-Server Token
    r'github_pat_[A-Za-z0-9_]{36,}',  # GitHub Fine-Grained PAT
    r'sk-ant-[A-Za-z0-9_-]{20,}',     # Anthropic API Key
    r'sk-[A-Za-z0-9]{40,}',           # OpenAI-style API Key
    r'xoxb-[0-9]+-[A-Za-z0-9-]+',     # Slack Bot Token
    r'AIza[A-Za-z0-9_-]{30,}',        # Google API Key
]

redacted = text
for p in patterns:
    try:
        redacted = re.sub(p, '<REDACTED>', redacted)
    except Exception:
        pass

# Kein Treffer -> Original anzeigen (kein displayContent ausgeben)
if redacted == text:
    sys.exit(0)

try:
    print(json.dumps({
        'hookSpecificOutput': {
            'hookEventName': 'MessageDisplay',
            'displayContent': redacted
        }
    }))
except Exception:
    pass
sys.exit(0)
" 2>/dev/null

exit 0
