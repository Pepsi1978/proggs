#!/bin/bash
# Config Guard: Verifies protected settings after any config change
# Hook event: PostToolUse (Edit|Write on settings.json)
# Platform: macOS / Linux (bash)
#
# Output: JSON for hook compatibility.
#   Block:   {"error":"CONFIG-GUARD: BLOCKIERT — ..."}
#   OK:      no output (exit 0)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/hook-log.sh"
source "$SCRIPT_DIR/whiteboard-insert.sh"

# ---------------------------------------------------------------------------
# Read JSON from stdin
# ---------------------------------------------------------------------------
hook_input="$(cat)"

# Guard: if python3 is unavailable, skip silently (config-guard is critical but can't parse without python3)
if ! command -v python3 &>/dev/null; then
    hook_log_warn "python3 not found — config-guard cannot validate settings"
    exit 0
fi

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

# Normalize backslashes and check if the target is settings.json or settings.local.json
normalized="${file_path//\\//}"
if [[ "$normalized" != *".claude/settings"*".json" ]]; then
    exit 0
fi

# ---------------------------------------------------------------------------
# DEFENSE: Auto-remove allow lists from ALL settings files
# ---------------------------------------------------------------------------
# Claude Code auto-creates allow entries when the user approves a tool.
# These act as whitelists and break bypassPermissions. Remove immediately.

remove_allow_list_silent() {
    local path="$1"
    if [ -f "$path" ]; then
        python3 -c "
import json, os, tempfile, sys
with open('$path', 'r') as f:
    d = json.load(f)
if 'allow' in d.get('permissions', {}):
    del d['permissions']['allow']
    dir_name = os.path.dirname('$path')
    fd, tmp = tempfile.mkstemp(dir=dir_name, suffix='.tmp')
    with os.fdopen(fd, 'w') as f:
        json.dump(d, f, indent=2)
        f.write('\n')
    os.replace(tmp, '$path')
    print('removed')
else:
    print('')
" 2>/dev/null
    fi
}

allow_fixed=false
result=$(remove_allow_list_silent "$HOME/.claude/settings.json")
[ "$result" = "removed" ] && allow_fixed=true && hook_log "AUTO-FIX: removed allow list from settings.json" 2>/dev/null || true

result=$(remove_allow_list_silent "$HOME/.claude/settings.local.json")
[ "$result" = "removed" ] && allow_fixed=true && hook_log "AUTO-FIX: removed allow list from settings.local.json" 2>/dev/null || true

# Also clean project-level settings
if [ -d "$HOME/.claude/projects" ]; then
    for pd in "$HOME/.claude/projects"/*/; do
        [ -d "$pd" ] || continue
        result=$(remove_allow_list_silent "$pd/settings.json")
        [ "$result" = "removed" ] && allow_fixed=true
        result=$(remove_allow_list_silent "$pd/settings.local.json")
        [ "$result" = "removed" ] && allow_fixed=true
    done
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

# defaultMode: MUST be "bypassPermissions" — BLOCK anything else (user requirement)
perms = data.get('permissions', {})
if perms:
    def_mode = perms.get('defaultMode', '')
    if def_mode and def_mode != 'bypassPermissions':
        blocks.append(f"defaultMode={def_mode} (MUSS 'bypassPermissions' sein — Benutzer-Regel)")

# effortLevel: User-controlled via /effort — only block invalid values
eff = data.get('effortLevel')
valid_efforts = ('high', 'medium', 'low', 'max')
if eff and eff not in valid_efforts:
    blocks.append(f"effortLevel={eff} (ungueltiger Wert — erlaubt: {', '.join(valid_efforts)})")

env_data = data.get('env', {})
if env_data:
    # CLAUDE_CODE_EFFORT_LEVEL env var MUST NOT EXIST — it overrides /effort command
    env_eff = env_data.get('CLAUDE_CODE_EFFORT_LEVEL')
    if env_eff is not None:
        blocks.append(f"CLAUDE_CODE_EFFORT_LEVEL darf NICHT als env var existieren (blockiert /effort) — entfernen!")

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
    # Log to whiteboard — wrapped in subshell so exit 2 is always reached
    entry="### $(date '+%Y-%m-%d %H:%M') — Hook: config-guard.sh — Settings-Aenderung blockiert: $violations"
    insert_whiteboard_entry "Offene Fehler & Probleme" "$entry" || true
    # Output valid JSON for hook compatibility
    # Use python to safely escape the message as JSON string
    json_out="$(python3 -c "import json, sys; print(json.dumps({'error': 'CONFIG-GUARD: BLOCKIERT — Geschuetzte Settings geaendert: ' + sys.argv[1]}))" "$violations" 2>/dev/null)"
    printf '%s\n' "$json_out"
    exit 2
fi

hook_log "all protected settings intact"
exit 0
