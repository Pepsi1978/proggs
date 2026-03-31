#!/usr/bin/env bash
# invariant-check.sh — SessionStart Hook (macOS/Linux)
# Proaktive Pruefung von System-Invarianten bei jedem Start
# Inspiriert von Cursor Invariant Sentinel Pattern (R8 Finding 3, 2026-03-31)

set +e
violations=()

# --- Invariant 1: Stale OFFEN-Eintraege (>7 Tage) ---
WHITEBOARD="$HOME/proggs/.claude/agent-memory/shared/MEMORY.md"
if [ -f "$WHITEBOARD" ]; then
    today_epoch=$(date +%s)
    stale_count=0
    while IFS= read -r line; do
        date_str=$(echo "$line" | grep -oE '[0-9]{4}-[0-9]{2}-[0-9]{2}' | head -1)
        if [ -n "$date_str" ]; then
            entry_epoch=$(date -j -f "%Y-%m-%d" "$date_str" +%s 2>/dev/null || date -d "$date_str" +%s 2>/dev/null)
            if [ -n "$entry_epoch" ]; then
                age=$(( (today_epoch - entry_epoch) / 86400 ))
                if [ "$age" -gt 7 ]; then
                    stale_count=$((stale_count + 1))
                fi
            fi
        fi
    done < <(grep -i "Status: OFFEN" "$WHITEBOARD")
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
