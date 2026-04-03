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

    # FIX 2026-04-03: Heartbeat runs every 30min via launchd, but during long
    # Claude sessions it can be delayed. Only alert if launchd agent is NOT loaded
    # OR heartbeat is older than 3 hours (indicating agent actually crashed).
    if [ -n "$hb_time" ] && [ "$hb_time" != "?" ]; then
        hb_epoch=$(date -j -f "%Y-%m-%dT%H:%M:%SZ" "$hb_time" +%s 2>/dev/null || echo "")
        if [ -n "$hb_epoch" ]; then
            hb_age_min=$(( ($(date +%s) - hb_epoch) / 60 ))
            hb_loaded=$(launchctl list 2>/dev/null | grep -c "claude-heartbeat")
            if [ "$hb_loaded" -eq 0 ]; then
                violations+=("HEARTBEAT: launchd Agent ist NICHT geladen! Reparieren: launchctl load ~/Library/LaunchAgents/com.frank.claude-heartbeat.plist")
            elif [ "$hb_age_min" -gt 180 ]; then
                violations+=("HEARTBEAT: Letzter Check vor ${hb_age_min} Minuten (>3h) — Agent haengt moeglicherweise!")
            fi
        fi
    fi
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

# --- Invariant 7: Hook type/field consistency (Self-Healing) ---
# A hook with type:"prompt" MUST have a "prompt" field (string).
# A hook with type:"command" MUST have a "command" field.
# If type:"prompt" has "command" but no "prompt", auto-fix to type:"command".
SETTINGS_MAIN="$HOME/.claude/settings.json"
if [ -f "$SETTINGS_MAIN" ]; then
    fix_result=$(python3 -c "
import json, sys
path = '$SETTINGS_MAIN'
with open(path) as f:
    data = json.load(f)
fixed = 0
hooks = data.get('hooks', {})
for event, entries in hooks.items():
    for entry in entries:
        for hook in entry.get('hooks', []):
            if hook.get('type') == 'prompt' and 'command' in hook and 'prompt' not in hook:
                hook['type'] = 'command'
                fixed += 1
if fixed > 0:
    with open(path, 'w') as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write('\n')
print(fixed)
" 2>/dev/null)
    if [ "$fix_result" -gt 0 ] 2>/dev/null; then
        violations+=("HOOKS-SCHEMA: $fix_result Hook(s) mit type:prompt+command statt type:command gefunden und AUTO-REPARIERT!")
    fi
fi

# --- Invariant 8: Stale-Exit-Scanner (Hook-Dateien auf stille Deaktivierung pruefen) ---
# FIX 2026-04-03: Previous pattern (grep age|stale + grep exit 0) was too broad and
# matched nearly every hook. Now specifically looks for the actual bug pattern:
# a time comparison that leads directly to exit 0 (silently disabling the hook).
# Pattern: "exit 0" on the same or next line after a time/age comparison.
HOOKS_DIR_SCAN="$HOME/.claude/hooks"
if [ -d "$HOOKS_DIR_SCAN" ]; then
    stale_exit_hooks=""
    for hookfile in "$HOOKS_DIR_SCAN"/*.sh; do
        [ -f "$hookfile" ] || continue
        base=$(basename "$hookfile")
        # Skip the checker itself and already-fixed hooks
        [ "$base" = "invariant-check.sh" ] && continue
        # Only flag hooks where a staleness check directly causes exit 0
        if grep -q -E '(age|stale|expired).*(exit 0)|(exit 0).*(age|stale|expired)' "$hookfile" 2>/dev/null; then
            stale_exit_hooks="$stale_exit_hooks $base"
        fi
    done
    stale_exit_hooks=$(echo "$stale_exit_hooks" | xargs)
    if [ -n "$stale_exit_hooks" ]; then
        violations+=("STALE-EXIT: Hooks mit potenziellem Stale-Exit-Bug: $stale_exit_hooks")
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
