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
