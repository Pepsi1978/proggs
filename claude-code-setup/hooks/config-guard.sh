#!/bin/bash
# Config Guard: Verifies protected settings after any config change
# Hook event: PostToolUse (Edit|Write on settings.json)
# Platform: macOS / Linux (bash)
#
# Output: JSON for hook compatibility.
#   Block:   {"error":"CONFIG-GUARD: BLOCKIERT — ..."}
#   OK:      no output (exit 0)

HOOK_NAME="config-guard"
LOG_DIR="$HOME/.claude/logs/hooks"
LOG_FILE="$LOG_DIR/$(date +%Y-%m-%d).log"

# ---------------------------------------------------------------------------
# Logging helpers (mirrors hook-log.ps1 behaviour)
# ---------------------------------------------------------------------------
_hook_log() {
    local level="$1"
    local msg="$2"
    local ts
    ts="$(date +%H:%M:%S)"
    mkdir -p "$LOG_DIR"
    echo "[$ts] $level $HOOK_NAME: $msg" >> "$LOG_FILE" 2>/dev/null || true
    find "$LOG_DIR" -name "*.log" -mtime +14 -delete 2>/dev/null || true
}
hook_log()       { _hook_log ""      "$1"; }
hook_log_error() { _hook_log "ERROR" "$1"; }
hook_log_warn()  { _hook_log "WARN " "$1"; }

# ---------------------------------------------------------------------------
# Read JSON from stdin
# ---------------------------------------------------------------------------
hook_input="$(cat)"

# Extract file_path (Write tool) or path (fallback) from tool_input
file_path="$(printf '%s' "$hook_input" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    ti = d.get('tool_input', {})
    print(ti.get('file_path') or ti.get('path') or '')
except Exception:
    print('')
" 2>/dev/null)"

if [ -z "$file_path" ]; then
    exit 0
fi

# Normalize backslashes and check if the target is settings.json
normalized="${file_path//\\//}"
if [[ "$normalized" != *".claude/settings.json" ]]; then
    exit 0
fi

SETTINGS="$HOME/.claude/settings.json"
if [ ! -f "$SETTINGS" ]; then
    hook_log_warn "settings.json not found at $SETTINGS"
    exit 0
fi

# ---------------------------------------------------------------------------
# Parse and validate protected settings using Python (reliable JSON parsing)
# ---------------------------------------------------------------------------
violations="$(python3 - "$SETTINGS" <<'PYEOF'
import sys, json

settings_path = sys.argv[1]
try:
    with open(settings_path, 'r', encoding='utf-8') as f:
        data = json.load(f)
except Exception as e:
    sys.stderr.write(f"failed to parse settings.json: {e}\n")
    sys.exit(0)

blocks = []

# effortLevel: MUST be "high" — BLOCK anything else (CLAUDE.md requirement)
eff = data.get('effortLevel')
if eff and eff != 'high':
    blocks.append(f"effortLevel={eff} (MUSS 'high' sein — CLAUDE.md-Regel)")

env_data = data.get('env', {})
if env_data:
    # CLAUDE_CODE_EFFORT_LEVEL in env
    env_eff = env_data.get('CLAUDE_CODE_EFFORT_LEVEL')
    if env_eff and env_eff != 'high':
        blocks.append(f"CLAUDE_CODE_EFFORT_LEVEL={env_eff} (MUSS 'high' sein)")

    # SUBAGENT_MODEL: BLOCK if changed from sonnet (critical for cost/quality)
    sub_model = env_data.get('CLAUDE_CODE_SUBAGENT_MODEL')
    if sub_model and sub_model != 'sonnet':
        blocks.append(f"CLAUDE_CODE_SUBAGENT_MODEL={sub_model} (erwartet: sonnet)")

    # AUTOCOMPACT: BLOCK if below 95
    acp = env_data.get('CLAUDE_AUTOCOMPACT_PCT_OVERRIDE')
    if acp is not None:
        try:
            if int(acp) < 95:
                blocks.append(f"AUTOCOMPACT={acp} (minimum: 95)")
        except (ValueError, TypeError):
            pass

print('; '.join(blocks))
PYEOF
)"

if [ -n "$violations" ]; then
    hook_log_error "BLOCKED — protected settings changed: $violations"
    # Output valid JSON for hook compatibility
    # Use python to safely escape the message as JSON string
    json_out="$(python3 -c "import json, sys; print(json.dumps({'error': 'CONFIG-GUARD: BLOCKIERT — Geschuetzte Settings geaendert: ' + sys.argv[1]}))" "$violations" 2>/dev/null)"
    printf '%s\n' "$json_out"
    exit 2
fi

hook_log "all protected settings intact"
exit 0
