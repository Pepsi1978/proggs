#!/usr/bin/env bash
# invariant-check.sh — SessionStart Hook (macOS/Linux)
# Proaktive Pruefung von System-Invarianten bei jedem Start
# Inspiriert von Cursor Invariant Sentinel Pattern (R8 Finding 3, 2026-03-31)

set +e
violations=()

# --- Invariant 1: Stale OFFEN-Eintraege (>7 Tage) ---
# BUG FIX 2026-03-31: Datum und Status stehen auf VERSCHIEDENEN Zeilen.
# Alter grep-Ansatz fand Status-Zeile, aber KEIN Datum auf dieser Zeile.
# Neuer Ansatz: grep -B20 holt Kontext VOR der Status-Zeile, dann Datum extrahieren.
WHITEBOARD="$HOME/proggs/.claude/agent-memory/shared/MEMORY.md"
if [ -f "$WHITEBOARD" ]; then
    today_epoch=$(date +%s)
    stale_count=0
    # BUG FIX #5 (2026-03-31): Pattern must match Markdown **Status:** OFFEN
    # but NOT **Status:** DESIGN-OFFEN. Use literal "Status:** OFFEN" for exact match.
    # stale_count computed via subshell (pipe creates subshell, variable won't propagate)
    stale_count=$(grep -B20 'Status:\*\* OFFEN' "$WHITEBOARD" 2>/dev/null | grep -E '^### [0-9]{4}-[0-9]{2}-[0-9]{2}' | while IFS= read -r header_line; do
        date_str=$(echo "$header_line" | grep -oE '[0-9]{4}-[0-9]{2}-[0-9]{2}' | head -1)
        if [ -n "$date_str" ]; then
            entry_epoch=$(date -j -f "%Y-%m-%d" "$date_str" +%s 2>/dev/null || date -d "$date_str" +%s 2>/dev/null)
            if [ -n "$entry_epoch" ]; then
                age=$(( (today_epoch - entry_epoch) / 86400 ))
                if [ "$age" -gt 7 ]; then echo "stale"; fi
            fi
        fi
    done | wc -l | tr -d ' ')
    if [ "$stale_count" -gt 0 ]; then
        violations+=("WHITEBOARD: $stale_count OFFEN-Eintraege aelter als 7 Tage — /self-improve starten!")
    fi
fi

# --- Invariant 2: bypassPermissions aktiv ---
SETTINGS_LOCAL="$HOME/.claude/settings.local.json"
if [ -f "$SETTINGS_LOCAL" ]; then
    mode=$(python3 -c "import json; d=json.load(open('$SETTINGS_LOCAL')); print(d.get('permissions',{}).get('defaultMode',''))" 2>/dev/null)
    if [ "$mode" != "bypassPermissions" ]; then
        violations+=("PERMISSIONS: bypassPermissions NICHT aktiv in settings.local.json!")
    fi
fi

# --- Invariant 3: Hook-Paare ---
HOOKS_DIR="$HOME/.claude/hooks"
if [ -d "$HOOKS_DIR" ]; then
    missing_count=0
    platform_only="notify mcp-auth-check plugin-health-check subagent-context"
    for ps1 in "$HOOKS_DIR"/*.ps1; do
        [ -f "$ps1" ] || continue
        base=$(basename "$ps1" .ps1)
        echo "$platform_only" | grep -qw "$base" && continue
        if [ ! -f "$HOOKS_DIR/$base.sh" ]; then
            missing_count=$((missing_count + 1))
        fi
    done
    if [ "$missing_count" -gt 0 ]; then
        violations+=("HOOKS: $missing_count .ps1-Hooks ohne .sh-Gegenstueck")
    fi
fi

# --- Invariant 4: Systemzustand-Alter ---
if [ -f "$WHITEBOARD" ]; then
    state_date=$(grep -oE 'Stand: [0-9]{4}-[0-9]{2}-[0-9]{2}' "$WHITEBOARD" | grep -oE '[0-9]{4}-[0-9]{2}-[0-9]{2}' | head -1)
    if [ -n "$state_date" ]; then
        state_epoch=$(date -j -f "%Y-%m-%d" "$state_date" +%s 2>/dev/null || date -d "$state_date" +%s 2>/dev/null)
        if [ -n "$state_epoch" ]; then
            state_age=$(( ($(date +%s) - state_epoch) / 86400 ))
            if [ "$state_age" -gt 14 ]; then
                violations+=("SYSTEMZUSTAND: Letzte Aktualisierung vor ${state_age} Tagen — veraltet!")
            fi
        fi
    fi
fi

# --- Invariant 5: CLAUDE.md Sync ---
CLAUDE_REPO="$HOME/proggs/CLAUDE.md"
CLAUDE_HOME="$HOME/CLAUDE.md"
if [ -f "$CLAUDE_REPO" ] && [ -f "$CLAUDE_HOME" ]; then
    hash_repo=$(shasum -a 256 "$CLAUDE_REPO" | cut -d' ' -f1)
    hash_home=$(shasum -a 256 "$CLAUDE_HOME" | cut -d' ' -f1)
    if [ "$hash_repo" != "$hash_home" ]; then
        violations+=("CLAUDE.MD: Repo-Version und Home-Version sind NICHT synchron!")
    fi
fi

# --- Invariant 6: Heartbeat-Status (between-session health) ---
HEARTBEAT_STATUS="$HOME/.claude/heartbeat-status.json"
if [ -f "$HEARTBEAT_STATUS" ]; then
    hb_status=$(python3 -c "import json; d=json.load(open('$HEARTBEAT_STATUS')); print(d.get('status','UNKNOWN'))" 2>/dev/null)
    hb_time=$(python3 -c "import json; d=json.load(open('$HEARTBEAT_STATUS')); print(d.get('timestamp','?'))" 2>/dev/null)
    if [ "$hb_status" = "CRITICAL" ]; then
        # Extract critical check details
        hb_details=$(python3 -c "
import json
d = json.load(open('$HEARTBEAT_STATUS'))
for name, info in d.get('checks', {}).items():
    if info.get('status') == 'CRITICAL':
        print(f'  {name}: {info.get(\"detail\", \"?\")}')" 2>/dev/null)
        violations+=("HEARTBEAT ($hb_time): KRITISCHE Probleme zwischen Sessions erkannt:")
        if [ -n "$hb_details" ]; then
            while IFS= read -r line; do
                violations+=("$line")
            done <<< "$hb_details"
        fi
    elif [ "$hb_status" = "WARNING" ]; then
        hb_warnings=$(python3 -c "
import json
d = json.load(open('$HEARTBEAT_STATUS'))
warns = [f'{n}: {i.get(\"detail\",\"?\")}' for n,i in d.get('checks',{}).items() if i.get('status')=='WARNING']
print('; '.join(warns))" 2>/dev/null)
        violations+=("HEARTBEAT ($hb_time): Warnungen: $hb_warnings")
    fi
fi

# --- Output ---
if [ ${#violations[@]} -gt 0 ]; then
    echo ""
    echo "Invariant-Check: ${#violations[@]} Verletzung(en):"
    for v in "${violations[@]}"; do
        echo "  - $v"
    done
else
    echo "Invariant-Check: Alle Pruefungen bestanden."
fi

exit 0
