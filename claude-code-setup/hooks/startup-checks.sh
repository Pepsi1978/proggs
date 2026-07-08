#!/usr/bin/env bash
# startup-checks.sh — Consolidated SessionStart Health Checks
# Event: SessionStart
# Type: command
# Platform: macOS/Linux
#
# CONSOLIDATION (2026-04-12): Vereint 6 einfache SessionStart-Hooks in einer Datei:
#   1. disk-guard        — Speicherplatz pruefen
#   2. path-health-check — PATH auf Orphans und Ghost-Eintraege pruefen
#   3. doctor-lite       — Cloud-MCP, .mcp.json, sequential-thinking
#   5. mcp-auth-check    — MCP-Server-Authentifizierung pruefen
#   6. mirror-check      — Mirror-Ledger ausstehende Eintraege
#
# ROBUSTNESS: Non-critical. Jeder Fehler → weitermachen. Am Ende immer exit 0.

set +e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
. "$SCRIPT_DIR/hook-log.sh" 2>/dev/null || true

warnings=()
ok_checks=()

# ============================================================
# CHECK 1: Speicherplatz (ehemals disk-guard.sh)
# ============================================================
check_disk_space() {
    if [ "$(uname)" = "Darwin" ]; then
        free_gb=$(df -g / 2>/dev/null | awk 'NR==2 {print $4}')
        usage_pct=$(df / 2>/dev/null | awk 'NR==2 {gsub(/%/,""); print $5}')
    else
        free_gb=$(df -BG / 2>/dev/null | awk 'NR==2 {gsub(/G/,""); print $4}')
        usage_pct=$(df / 2>/dev/null | awk 'NR==2 {gsub(/%/,""); print $5}')
    fi

    if [ -z "$usage_pct" ]; then
        warnings+=("Disk-Check fehlgeschlagen")
        return
    fi

    if [ "$usage_pct" -ge 95 ] 2>/dev/null; then
        warnings+=("Speicherplatz KRITISCH: ${usage_pct}% belegt, nur ${free_gb}GB frei!")
        # Cooldown 2026-05-10: gestufte Eskalation gegen Spam
        #   95-96% → max 1 Eintrag pro Tag (24h)
        #   97-98% → max 1 Eintrag pro 4h
        #   99%+ → bei jedem Lauf
        cooldown_file="$HOME/.claude/.disk-warn-last"
        should_log=1
        if [ "$usage_pct" -lt 99 ] && [ -f "$cooldown_file" ]; then
            now=$(date +%s)
            mtime=$(stat -f %m "$cooldown_file" 2>/dev/null || stat -c %Y "$cooldown_file" 2>/dev/null)
            if [ -n "$mtime" ]; then
                hours_since=$(( (now - mtime) / 3600 ))
                if [ "$usage_pct" -le 96 ] && [ "$hours_since" -lt 24 ]; then should_log=0; fi
                if [ "$usage_pct" -le 98 ] && [ "$usage_pct" -ge 97 ] && [ "$hours_since" -lt 4 ]; then should_log=0; fi
            fi
        fi
        if [ "$should_log" -eq 1 ] && type insert_whiteboard_entry >/dev/null 2>&1; then
            entry="### $(date '+%Y-%m-%d %H:%M') — Hook: startup-checks.sh — Speicherplatz KRITISCH bei ${usage_pct}%"
            insert_whiteboard_entry "Offene Fehler & Probleme" "$entry" 2>/dev/null || true
            echo "$usage_pct" > "$cooldown_file" 2>/dev/null || true
        fi
    elif [ "$usage_pct" -ge 90 ] 2>/dev/null; then
        warnings+=("Speicherplatz bei ${usage_pct}% — ${free_gb}GB frei (Achtung)")
    else
        ok_checks+=("Disk: ${free_gb}GB frei (${usage_pct}%)")
    fi
}

# ============================================================
# CHECK 2: PATH-Gesundheit (ehemals path-health-check.sh)
# ============================================================
check_path_health() {
    local issues=0

    # Check for orphaned Python (pip without python)
    for dir in $(echo "$PATH" | tr ':' '\n' | grep -i python); do
        if [ -f "$dir/pip" ] || [ -f "$dir/pip3" ]; then
            parent=$(dirname "$dir")
            if [ ! -f "$parent/python3" ] && [ ! -f "$parent/python" ]; then
                warnings+=("PATH: ORPHANED PYTHON: $dir hat pip aber kein python")
                issues=$((issues + 1))
            fi
        fi
    done

    if [ "$issues" -eq 0 ]; then
        ok_checks+=("PATH: OK")
    fi
}

# ============================================================
# CHECK 3: Doctor-Lite (ehemals doctor-lite.sh)
# ============================================================
check_doctor_lite() {
    local local_warnings=0
    local claude_json="$HOME/.claude.json"
    if [ -f "$claude_json" ]; then
        # Cloud MCP connectors
        if grep -q '"tengu_claudeai_mcp_connectors": true' "$claude_json" 2>/dev/null; then
            if ! grep -q '"claude\.ai' "$claude_json" 2>/dev/null; then
                warnings+=("Cloud-MCP-Connectoren aktiv und NICHT blockiert (~21K Token)")
                local_warnings=$((local_warnings + 1))
            fi
        fi
        # sequential-thinking
        if grep -q '"sequential-thinking"' "$claude_json" 2>/dev/null; then
            warnings+=("sequential-thinking MCP-Server ist wieder konfiguriert (redundant)")
            local_warnings=$((local_warnings + 1))
        fi
    fi

    # Check .mcp.json for Windows paths on macOS
    local mcp_json="$HOME/.mcp.json"
    if [ -f "$mcp_json" ] && [ "$(uname)" = "Darwin" ]; then
        if grep -q 'C:\\Users\\' "$mcp_json" 2>/dev/null; then
            warnings+=(".mcp.json: Windows-Pfade auf macOS gefunden")
            local_warnings=$((local_warnings + 1))
        fi
    fi

    if [ "$local_warnings" -eq 0 ]; then
        ok_checks+=("Doctor-Lite: OK")
    fi
}

# ============================================================
# CHECK 5: MCP-Auth (DISABLED 2026-04-12)
# ============================================================
# REMOVED: "claude mcp list" spawns orphan MCP server processes on every call.
# This was a root cause of the process leak and session destruction bug.
# See observation #4072: "MCP Server Duplication Root Cause: claude mcp list Spawns Orphans"
check_mcp_auth() {
    ok_checks+=("MCP-Auth: Pruefung deaktiviert (spawnte Orphans)")
}

# ============================================================
# CHECK 6: Mirror-Ledger (ehemals mirror-check.sh)
# ============================================================
check_mirror_ledger() {
    local ledger="$HOME/proggs/claude-code-setup/mirror-ledger.md"
    [ ! -f "$ledger" ] && return

    local platform="macos"
    [ "$(uname)" != "Darwin" ] && platform="linux"

    local pattern="APPLIED: ${platform}/claude-code=PENDING"
    local count
    # grep -c gibt bei 0 Treffern "0" UND exit 1 → "|| echo 0" haengte ein zweites "0"
    # an (count="0\n0") → "[ : integer expression expected". "|| count=0" ergibt EINE Zahl.
    count=$(grep -cF "$pattern" "$ledger" 2>/dev/null) || count=0

    if [ "$count" -gt 0 ]; then
        warnings+=("Mirror-Bridge: $count ausstehende Eintraege von anderen Plattformen")
    fi
}

# ============================================================
# MAIN: Alle Checks ausfuehren
# ============================================================

# Reihenfolge: Schnelle lokale Checks zuerst, langsamer externer Check (MCP-Auth) zuletzt.
check_disk_space
check_path_health
check_doctor_lite
check_mirror_ledger
check_mcp_auth  # LETZTER Check — ruft claude mcp list auf (kann bis zu 5s dauern)

# ============================================================
# OUTPUT: Kompakte Zusammenfassung
# ============================================================

if [ "${#warnings[@]}" -gt 0 ]; then
    echo "Startup-Checks: ${#warnings[@]} Problem(e):"
    for w in "${warnings[@]}"; do
        echo "  ! $w"
    done
fi

if [ "${#warnings[@]}" -eq 0 ] && [ "${#ok_checks[@]}" -gt 0 ]; then
    echo "Startup-Checks: Alle OK (${#ok_checks[@]} Pruefungen bestanden)"
fi

exit 0
