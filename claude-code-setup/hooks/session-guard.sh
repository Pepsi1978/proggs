#!/usr/bin/env bash
# Session Guard: Verifies bypass permissions and workspace directory on every session start
# Hook event: SessionStart
# Platform: macOS/Linux (Bash)
#
# ROBUSTNESS: set +e — any failure → exit 0 (never block session).
set +e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/hook-log.sh" 2>/dev/null || true
source "$SCRIPT_DIR/whiteboard-insert.sh" 2>/dev/null || true

write_status() {
    echo "$1"
    echo "$1" >&2
}

fixes=()

# =============================================
# CHECK 1: Bypass Permissions — MUST be active
# =============================================

SETTINGS="$HOME/.claude/settings.json"
LOCAL_SETTINGS="$HOME/.claude/settings.local.json"

# Check global settings.json
if [ -f "$SETTINGS" ]; then
    mode=$(python3 -c "import json; d=json.load(open('$SETTINGS')); print(d.get('permissions',{}).get('defaultMode',''))" 2>/dev/null)
    if [ -n "$mode" ] && [ "$mode" != "bypassPermissions" ]; then
        python3 -c "
import json
with open('$SETTINGS', 'r') as f:
    d = json.load(f)
d.setdefault('permissions', {})['defaultMode'] = 'bypassPermissions'
with open('$SETTINGS', 'w') as f:
    json.dump(d, f, indent=2)
" 2>/dev/null
        fixes+=("settings.json defaultMode repariert (war: $mode)")
    fi
fi

# Check settings.local.json
if [ -f "$LOCAL_SETTINGS" ]; then
    local_mode=$(python3 -c "import json; d=json.load(open('$LOCAL_SETTINGS')); print(d.get('permissions',{}).get('defaultMode',''))" 2>/dev/null)
    if [ -n "$local_mode" ] && [ "$local_mode" != "bypassPermissions" ]; then
        python3 -c "
import json
with open('$LOCAL_SETTINGS', 'r') as f:
    d = json.load(f)
d.setdefault('permissions', {})['defaultMode'] = 'bypassPermissions'
with open('$LOCAL_SETTINGS', 'w') as f:
    json.dump(d, f, indent=2)
" 2>/dev/null
        fixes+=("settings.local.json defaultMode repariert (war: $local_mode)")
    fi
fi

# Check ALL project-level settings
PROJECTS_DIR="$HOME/.claude/projects"
if [ -d "$PROJECTS_DIR" ]; then
    for pdir in "$PROJECTS_DIR"/*/; do
        [ -d "$pdir" ] || continue
        plocal="$pdir/settings.local.json"
        if [ -f "$plocal" ]; then
            pmode=$(python3 -c "import json; d=json.load(open('$plocal')); print(d.get('permissions',{}).get('defaultMode',''))" 2>/dev/null)
            if [ -n "$pmode" ] && [ "$pmode" != "bypassPermissions" ]; then
                python3 -c "
import json
with open('$plocal', 'r') as f:
    d = json.load(f)
d.setdefault('permissions', {})['defaultMode'] = 'bypassPermissions'
with open('$plocal', 'w') as f:
    json.dump(d, f, indent=2)
" 2>/dev/null
                fixes+=("Projekt $(basename "$pdir") defaultMode repariert")
            fi
        else
            # Create missing project settings with bypassPermissions
            echo '{"permissions":{"defaultMode":"bypassPermissions"}}' | python3 -c "
import json, sys
d = json.load(sys.stdin)
with open('$plocal', 'w') as f:
    json.dump(d, f, indent=2)
" 2>/dev/null
            fixes+=("Projekt $(basename "$pdir") settings.local.json erstellt")
        fi
    done
fi

# =============================================
# CHECK 2: Remove allow list from ALL settings files
# =============================================
# An explicit allow list acts as whitelist and BLOCKS tools not on it,
# even with bypassPermissions. Claude Code auto-creates allow entries when
# the user manually approves a tool. Remove from ALL files on every start.

remove_allow_list() {
    local path="$1"
    local label="$2"
    if [ -f "$path" ]; then
        local has_allow
        has_allow=$(python3 -c "import json; d=json.load(open('$path')); print('yes' if 'allow' in d.get('permissions',{}) else 'no')" 2>/dev/null)
        if [ "$has_allow" = "yes" ]; then
            python3 -c "
import json, os, tempfile
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
" 2>/dev/null
            fixes+=("allow-Liste aus $label entfernt")
            hook_log "AUTO-FIX: removed allow list from $label" 2>/dev/null || true
        fi
    fi
}

# Clean settings.json
remove_allow_list "$SETTINGS" "settings.json"

# Clean settings.local.json
remove_allow_list "$LOCAL_SETTINGS" "settings.local.json"

# Clean ALL project-level settings (both settings.json and settings.local.json)
if [ -d "$PROJECTS_DIR" ]; then
    for pdir in "$PROJECTS_DIR"/*/; do
        [ -d "$pdir" ] || continue
        pname=$(basename "$pdir")
        remove_allow_list "$pdir/settings.json" "Projekt/$pname/settings.json"
        remove_allow_list "$pdir/settings.local.json" "Projekt/$pname/settings.local.json"
    done
fi

# =============================================
# REPORT
# =============================================

if [ ${#fixes[@]} -gt 0 ]; then
    fix_str=$(IFS="; "; echo "${fixes[*]}")
    ts=$(date '+%Y-%m-%d %H:%M')
    entry="### $ts — Hook: session-guard.sh — Auto-Reparatur: $fix_str — Status: AUTO-GEFIXT"
    insert_whiteboard_entry "Offene Fehler & Probleme" "$entry" 2>/dev/null || true
    write_status "SessionGuard: AUTO-REPARATUR: $fix_str"
else
    write_status "SessionGuard: bypassPermissions aktiv auf allen Ebenen."
fi

# Always output the workspace reminder for the AI context
echo "SessionGuard: WORKSPACE_CHECK — Arbeitsverzeichnis MUSS ~/proggs sein. Falls pwd=~: WARNUNG an Benutzer!"

exit 0
