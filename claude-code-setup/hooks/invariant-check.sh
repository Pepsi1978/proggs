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

# --- Invariant 5: ~/CLAUDE.md darf NICHT existieren (Geloescht 2026-04-04) ---
# Frueher wurde Sync zwischen ~/proggs/CLAUDE.md und ~/CLAUDE.md geprueft.
# Seit 2026-04-04 gibt es keine ~/CLAUDE.md mehr (Duplikat entfernt fuer Token-Ersparnis).
# Poka-Yoke Stufe 3 (2026-05-30): identisches Duplikat wird AUTOMATISCH geheilt
# (geloescht) statt nur gemeldet. Abweichende Datei wird weiterhin nur gemeldet,
# damit eigener Inhalt nicht verloren geht.
CLAUDE_HOME="$HOME/CLAUDE.md"
CLAUDE_REPO="$HOME/proggs/CLAUDE.md"
if [ -f "$CLAUDE_HOME" ]; then
    if [ -f "$CLAUDE_REPO" ] && cmp -s "$CLAUDE_HOME" "$CLAUDE_REPO"; then
        rm -f "$CLAUDE_HOME" 2>/dev/null \
            && violations+=("CLAUDE.MD: ~/CLAUDE.md (identisches Duplikat) automatisch entfernt — Token-Ersparnis wiederhergestellt.") \
            || violations+=("CLAUDE.MD: ~/CLAUDE.md existiert, Auto-Loeschung fehlgeschlagen. Bitte manuell loeschen.")
    else
        violations+=("CLAUDE.MD: ~/CLAUDE.md existiert UND weicht von der Repo-Version ab — NICHT automatisch geloescht. Bitte pruefen.")
    fi
fi

# --- Invariant 6: Heartbeat-Status ---
HEARTBEAT_STATUS="$HOME/.claude/heartbeat-status.json"
if [ -f "$HEARTBEAT_STATUS" ]; then
    hb_status=$(python3 -c "import json; print(json.load(open('$HEARTBEAT_STATUS')).get('status',''))" 2>/dev/null)
    if [ "$hb_status" = "CRITICAL" ]; then
        violations+=("HEARTBEAT: KRITISCHE Probleme zwischen Sessions erkannt!")
    fi
fi

# --- Invariant 8: Whiteboard-Versions-Drift (P2, 2026-05-10) ---
# Prueft ob die im Whiteboard vermerkte Claude-Code-Version mit der lokalen uebereinstimmt.
# Verhindert dass Whiteboard 24 Versionen drifftet (entdeckt 2026-05-10: v2.1.114 im
# Whiteboard, v2.1.138 lokal — 24 Versionen unbemerkt).
if [ -f "$WHITEBOARD" ]; then
    wb_version=$(grep -oE 'Claude Code\s*\*?\*?\s*v[0-9]+\.[0-9]+\.[0-9]+' "$WHITEBOARD" | head -1 | grep -oE '[0-9]+\.[0-9]+\.[0-9]+')
    local_version=$(claude --version 2>/dev/null | grep -oE '[0-9]+\.[0-9]+\.[0-9]+' | head -1)
    if [ -n "$wb_version" ] && [ -n "$local_version" ] && [ "$wb_version" != "$local_version" ]; then
        violations+=("WHITEBOARD-VERSIONS-DRIFT: Stand sagt v${wb_version}, lokal lauft v${local_version} — Systemzustand aktualisieren!")
    fi
fi

# --- Invariant 9: Merge-Konflikt-Marker in MEMORY.md (P1, 2026-05-10) ---
# Heute entdeckt: Merge-Konflikt-Marker koennen 17+ Tage ungeloest in MEMORY.md liegen
# und das ganze System verfaelschen. Beim SessionStart sofort lautstark warnen.
if [ -f "$WHITEBOARD" ]; then
    if grep -qE '<<<<<<< (Updated upstream|HEAD)|>>>>>>> Stashed changes|\|\|\|\|\|\|\| Stash base' "$WHITEBOARD"; then
        violations+=("WHITEBOARD-KONFLIKT: MEMORY.md enthaelt Merge-Konflikt-Marker — SOFORT manuell aufloesen!")
    fi
fi

# --- Invariant 10: Hook-Drift aktiv<->repo (inhaltlich, EOL/BOM-normalisiert) (2026-06-15) ---
# Faengt ab, wenn ein aktiver Hook (~/.claude/hooks/) inhaltlich von der Repo-Spiegelung
# (claude-code-setup/hooks/) abweicht (real 2026-06-15: subagent-context 1 Monat alt + flaches
# Schema -> Subagenten erbten das System nicht, monatelang unbemerkt). INHALTS-Vergleich (BOM via
# utf-8-sig + CRLF->LF + trailing-newline normalisiert), damit reiner EOL-Drift KEINEN Fehlalarm
# ausloest — sonst piept der Waechter dauernd und wird ignoriert (agent-knowledge-system.md S4).
REPO_HOOKS="$HOME/proggs/claude-code-setup/hooks"
if [ -d "$HOOKS_DIR" ] && [ -d "$REPO_HOOKS" ]; then
    drift_hooks=""
    drift_count=0
    for rf in "$REPO_HOOKS"/*.ps1 "$REPO_HOOKS"/*.sh; do
        [ -f "$rf" ] || continue
        name=$(basename "$rf")
        active="$HOOKS_DIR/$name"
        [ -f "$active" ] || continue
        if ! python3 - "$active" "$rf" 2>/dev/null <<'PYEOF'
import sys
def norm(p):
    t = open(p, encoding='utf-8-sig').read()
    return t.replace('\r\n', '\n').rstrip('\n')
sys.exit(0 if norm(sys.argv[1]) == norm(sys.argv[2]) else 1)
PYEOF
        then
            drift_hooks="$drift_hooks $name"
            drift_count=$((drift_count + 1))
        fi
    done
    if [ "$drift_count" -gt 0 ]; then
        violations+=("HOOK-DRIFT: $drift_count Hook(s) weichen aktiv<->repo ab (Inhalt):$drift_hooks — Repo<->aktiv spiegeln!")
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

# Almanach-/Harness-Self-Tests buendeln (bugs/health.py) — nur Hinweise, keine harte Verletzung
HEALTH_SCRIPT="$HOME/proggs/bugs/health.py"
if [ -f "$HEALTH_SCRIPT" ] && command -v python3 >/dev/null 2>&1; then
    health_out=$(python3 "$HEALTH_SCRIPT" --quiet 2>/dev/null)
    if [ -n "$health_out" ]; then
        echo ""
        echo "Almanach-Self-Test (bugs/health.py):"
        echo "$health_out" | sed 's/^/  /'
    fi
fi

exit 0
