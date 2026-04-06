#!/usr/bin/env bash
# heartbeat.sh — Periodic health check for the Claude Code development environment
# Runs via launchd every 15 minutes. Completely silent when healthy.
# Only sends macOS notification for CRITICAL issues.
#
# Checks:
#   1. Disk space (CRITICAL if >95%)
#   2. Config file integrity (settings.json, .mcp.json)
#   3. Whiteboard exists and is readable
#   4. Homebrew outdated packages
#   5. claude-mem worker running
#   6. Key directories exist
#
# Output: Writes status to ~/.claude/heartbeat-status.json (atomic write)
# Notifications: Only for CRITICAL issues via osascript
# Logging: ~/.claude/logs/heartbeat/YYYY-MM-DD.log
#
# Defense in Depth (Directive #3):
#   - Every check wrapped in its own error handler
#   - Script ALWAYS exits 0 (never blocks anything)
#   - Atomic JSON write (temp file → rename)
#   - Notification failures don't crash the script
#   - Graceful degradation if tools are missing

set +e  # Never exit on error

# ─────────────────────────────────────────────────────────────────────
# PATH: launchd has minimal PATH (/usr/bin:/bin:/usr/sbin:/sbin).
# We must ensure python3 and brew are findable.
# FIX 2026-03-31: Explicit PATH for launchd compatibility (Bug #1)
# ─────────────────────────────────────────────────────────────────────
export PATH="/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin:$PATH"

# Resolve python3 — prefer system python3 (always available via Xcode CLT)
PYTHON3="/usr/bin/python3"
if [ ! -x "$PYTHON3" ]; then
    PYTHON3=$(command -v python3 2>/dev/null || echo "")
fi

# ─────────────────────────────────────────────────────────────────────
# Configuration
# ─────────────────────────────────────────────────────────────────────
HEARTBEAT_DIR="$HOME/.claude"
STATUS_FILE="$HEARTBEAT_DIR/heartbeat-status.json"
LOG_DIR="$HEARTBEAT_DIR/logs/heartbeat"
LOG_FILE="$LOG_DIR/$(date '+%Y-%m-%d').log"
PROGGS_DIR="$HOME/proggs"
WHITEBOARD="$PROGGS_DIR/.claude/agent-memory/shared/MEMORY.md"
SETTINGS_JSON="$HOME/.claude/settings.json"
SETTINGS_LOCAL="$HOME/.claude/settings.local.json"
MCP_JSON="$PROGGS_DIR/.mcp.json"

# Thresholds
DISK_CRITICAL_PCT=95
DISK_WARNING_PCT=90
BREW_OUTDATED_WARNING=5

# ─────────────────────────────────────────────────────────────────────
# Setup
# ─────────────────────────────────────────────────────────────────────
mkdir -p "$LOG_DIR" 2>/dev/null

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" >> "$LOG_FILE" 2>/dev/null
}

notify_critical() {
    local msg="$1"
    log "CRITICAL: $msg"
    # Only notify via osascript if available — never crash on notification failure
    if command -v osascript &>/dev/null; then
        local safe_msg="${msg//\\/\\\\}"
        safe_msg="${safe_msg//\"/\\\"}"
        osascript -e "display notification \"${safe_msg}\" with title \"Claude Heartbeat\" subtitle \"KRITISCH\"" 2>/dev/null || true
    fi
}

# ─────────────────────────────────────────────────────────────────────
# Check functions — each returns a JSON fragment
# ─────────────────────────────────────────────────────────────────────

check_disk_space() {
    local pct
    pct=$(df -h / 2>/dev/null | awk 'NR==2 {gsub(/%/,""); print $5}')
    if [ -z "$pct" ]; then
        echo '"disk_space": {"status": "UNKNOWN", "detail": "Could not read disk usage"}'
        return
    fi

    if [ "$pct" -ge "$DISK_CRITICAL_PCT" ] 2>/dev/null; then
        local free
        free=$(df -h / 2>/dev/null | awk 'NR==2 {print $4}')
        notify_critical "Speicherplatz KRITISCH: ${pct}% belegt, nur ${free} frei!"
        echo "\"disk_space\": {\"status\": \"CRITICAL\", \"detail\": \"${pct}% used, ${free} free\"}"
    elif [ "$pct" -ge "$DISK_WARNING_PCT" ] 2>/dev/null; then
        local free
        free=$(df -h / 2>/dev/null | awk 'NR==2 {print $4}')
        echo "\"disk_space\": {\"status\": \"WARNING\", \"detail\": \"${pct}% used, ${free} free\"}"
    else
        echo "\"disk_space\": {\"status\": \"OK\", \"detail\": \"${pct}% used\"}"
    fi
}

check_config_integrity() {
    local issues=0
    local details=""

    # Check settings.json
    if [ -f "$SETTINGS_JSON" ]; then
        if ! $PYTHON3 -c "import json; json.load(open('$SETTINGS_JSON'))" 2>/dev/null; then
            issues=$((issues + 1))
            details="${details}settings.json CORRUPT; "
            notify_critical "settings.json ist beschaedigt! Claude Code funktioniert nicht korrekt."
        fi
    else
        issues=$((issues + 1))
        details="${details}settings.json MISSING; "
    fi

    # Check settings.local.json
    if [ -f "$SETTINGS_LOCAL" ]; then
        if ! $PYTHON3 -c "import json; json.load(open('$SETTINGS_LOCAL'))" 2>/dev/null; then
            issues=$((issues + 1))
            details="${details}settings.local.json CORRUPT; "
            notify_critical "settings.local.json ist beschaedigt!"
        fi
    fi

    # Check .mcp.json
    if [ -f "$MCP_JSON" ]; then
        if ! $PYTHON3 -c "import json; json.load(open('$MCP_JSON'))" 2>/dev/null; then
            issues=$((issues + 1))
            details="${details}.mcp.json CORRUPT; "
            notify_critical ".mcp.json ist beschaedigt! MCP-Server starten nicht."
        fi
    fi

    if [ "$issues" -gt 0 ]; then
        echo "\"config_integrity\": {\"status\": \"CRITICAL\", \"detail\": \"${details%%; }\"}"
    else
        echo "\"config_integrity\": {\"status\": \"OK\", \"detail\": \"All config files valid\"}"
    fi
}

check_whiteboard() {
    if [ ! -f "$WHITEBOARD" ]; then
        notify_critical "Whiteboard (MEMORY.md) fehlt!"
        echo '"whiteboard": {"status": "CRITICAL", "detail": "MEMORY.md missing"}'
    elif [ ! -s "$WHITEBOARD" ]; then
        echo '"whiteboard": {"status": "WARNING", "detail": "MEMORY.md is empty"}'
    else
        local lines
        lines=$(wc -l < "$WHITEBOARD" 2>/dev/null | tr -d ' ')
        echo "\"whiteboard\": {\"status\": \"OK\", \"detail\": \"${lines} lines\"}"
    fi
}

check_brew_outdated() {
    if [ ! -x /opt/homebrew/bin/brew ]; then
        echo '"brew_outdated": {"status": "UNKNOWN", "detail": "Homebrew not found"}'
        return
    fi

    # FIX 2026-03-31: Bug #2+#3 — disable auto-update and add 30s timeout
    # brew outdated can trigger brew update (30+ seconds, needs network).
    # HOMEBREW_NO_AUTO_UPDATE prevents that. timeout kills if stuck.
    local count
    count=$(HOMEBREW_NO_AUTO_UPDATE=1 timeout 30 /opt/homebrew/bin/brew outdated --quiet 2>/dev/null | wc -l | tr -d ' ')
    # If timeout killed it, count will be empty or 0
    if [ -z "$count" ]; then count=0; fi

    if [ "$count" -ge "$BREW_OUTDATED_WARNING" ]; then
        echo "\"brew_outdated\": {\"status\": \"WARNING\", \"detail\": \"${count} packages outdated\"}"
    elif [ "$count" -gt 0 ]; then
        echo "\"brew_outdated\": {\"status\": \"OK\", \"detail\": \"${count} packages outdated\"}"
    else
        echo '"brew_outdated": {"status": "OK", "detail": "All packages up to date"}'
    fi
}

check_claude_mem_worker() {
    local loaded
    loaded=$(launchctl list 2>/dev/null | grep -c "com.claude-mem.worker" || echo "0")

    if [ "$loaded" -gt 0 ]; then
        echo '"claude_mem_worker": {"status": "OK", "detail": "Worker loaded in launchd"}'
    else
        echo '"claude_mem_worker": {"status": "WARNING", "detail": "Worker not loaded — run: launchctl load ~/Library/LaunchAgents/com.claude-mem.worker.plist"}'
    fi
}

check_key_directories() {
    local missing=0
    local details=""

    for dir in "$HOME/.claude/hooks" "$HOME/.claude/agents" "$HOME/.claude/rules" "$PROGGS_DIR/.claude/agent-memory/shared"; do
        if [ ! -d "$dir" ]; then
            missing=$((missing + 1))
            details="${details}$(basename "$dir") MISSING; "
        fi
    done

    if [ "$missing" -gt 0 ]; then
        echo "\"key_directories\": {\"status\": \"WARNING\", \"detail\": \"${details%%; }\"}"
    else
        echo '"key_directories": {"status": "OK", "detail": "All key directories exist"}'
    fi
}

# ─────────────────────────────────────────────────────────────────────
# Check 7: validator_compliance
# Reads the last 3 session scores from session-scores.jsonl and checks
# whether each ended with an overall score >= 3.0. A low score means
# Claude was operating below acceptable quality and corrections occurred.
# CRITICAL (notification) if 2+ sessions below threshold.
# ─────────────────────────────────────────────────────────────────────
check_validator_compliance() {
    local scores_file="$HOME/.claude/session-scores.jsonl"

    # Graceful degradation: file does not exist yet
    if [ ! -f "$scores_file" ]; then
        echo '"validator_compliance": {"status": "UNKNOWN", "detail": "session-scores.jsonl not found"}'
        return
    fi

    # Extract the last 3 overall scores using python3
    local result
    result=$($PYTHON3 - "$scores_file" <<'PYEOF' 2>/dev/null
import sys, json

scores_file = sys.argv[1]
scores = []
try:
    with open(scores_file, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            try:
                entry = json.loads(line)
                # Support both "overall" and nested {"dimensions": {...}, "overall": ...}
                overall = entry.get('overall', entry.get('score', None))
                if overall is not None:
                    scores.append(float(overall))
            except (json.JSONDecodeError, ValueError, TypeError):
                continue
except Exception:
    print('UNKNOWN|Could not read session-scores.jsonl')
    sys.exit(0)

if len(scores) < 3:
    print(f'UNKNOWN|Only {len(scores)} session score(s) available — need 3')
    sys.exit(0)

last3 = scores[-3:]
below = [s for s in last3 if s < 3.0]

if len(below) == 0:
    avg = sum(last3) / len(last3)
    print(f'OK|Last 3 session scores all >= 3.0 (avg {avg:.1f})')
elif len(below) == 1:
    print(f'WARNING|1 of last 3 sessions scored below 3.0: {below}')
else:
    print(f'CRITICAL|{len(below)} of last 3 sessions scored below 3.0: {below}')
PYEOF
) || result="UNKNOWN|python3 error reading session scores"

    local status="${result%%|*}"
    local detail="${result#*|}"

    # Normalize status — default to UNKNOWN if parse failed
    case "$status" in
        OK|WARNING|CRITICAL|UNKNOWN) ;;
        *) status="UNKNOWN"; detail="Unexpected output from score parser" ;;
    esac

    if [ "$status" = "CRITICAL" ]; then
        notify_critical "Session-Qualität KRITISCH: $detail"
    fi

    echo "\"validator_compliance\": {\"status\": \"$status\", \"detail\": \"$detail\"}"
}

# ─────────────────────────────────────────────────────────────────────
# Check 8: length_drift
# Detects monotonically falling session scores over the last 3 sessions.
# Three consecutively declining scores suggest a quality regression trend,
# not just a single bad session. Early warning before it becomes CRITICAL.
# ─────────────────────────────────────────────────────────────────────
check_length_drift() {
    local scores_file="$HOME/.claude/session-scores.jsonl"

    if [ ! -f "$scores_file" ]; then
        echo '"length_drift": {"status": "UNKNOWN", "detail": "session-scores.jsonl not found"}'
        return
    fi

    local result
    result=$($PYTHON3 - "$scores_file" <<'PYEOF' 2>/dev/null
import sys, json

scores_file = sys.argv[1]
scores = []
try:
    with open(scores_file, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            try:
                entry = json.loads(line)
                overall = entry.get('overall', entry.get('score', None))
                if overall is not None:
                    scores.append(float(overall))
            except (json.JSONDecodeError, ValueError, TypeError):
                continue
except Exception:
    print('UNKNOWN|Could not read session-scores.jsonl')
    sys.exit(0)

if len(scores) < 3:
    print(f'UNKNOWN|Only {len(scores)} score(s) available — need 3')
    sys.exit(0)

last3 = scores[-3:]
# Monotonically falling: each score strictly less than the previous
is_falling = last3[0] > last3[1] > last3[2]

if is_falling:
    print(f'WARNING|Monotonically falling scores: {last3[0]:.1f} → {last3[1]:.1f} → {last3[2]:.1f}')
else:
    print(f'OK|No consistent downward drift in last 3 scores: {last3}')
PYEOF
) || result="UNKNOWN|python3 error reading session scores"

    local status="${result%%|*}"
    local detail="${result#*|}"

    case "$status" in
        OK|WARNING|CRITICAL|UNKNOWN) ;;
        *) status="UNKNOWN"; detail="Unexpected output from drift parser" ;;
    esac

    echo "\"length_drift\": {\"status\": \"$status\", \"detail\": \"$detail\"}"
}

# ─────────────────────────────────────────────────────────────────────
# Check 9: session_regression
# Compares the average of the last 3 session scores against the average
# of the 5 sessions before those. A lower recent average means overall
# quality is regressing — the compound intelligence effect is reversing.
# Requires at least 8 sessions to produce a meaningful comparison.
# ─────────────────────────────────────────────────────────────────────
check_session_regression() {
    local scores_file="$HOME/.claude/session-scores.jsonl"

    if [ ! -f "$scores_file" ]; then
        echo '"session_regression": {"status": "UNKNOWN", "detail": "session-scores.jsonl not found"}'
        return
    fi

    local result
    result=$($PYTHON3 - "$scores_file" <<'PYEOF' 2>/dev/null
import sys, json

scores_file = sys.argv[1]
scores = []
try:
    with open(scores_file, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            try:
                entry = json.loads(line)
                overall = entry.get('overall', entry.get('score', None))
                if overall is not None:
                    scores.append(float(overall))
            except (json.JSONDecodeError, ValueError, TypeError):
                continue
except Exception:
    print('UNKNOWN|Could not read session-scores.jsonl')
    sys.exit(0)

# Need at least 8 data points: 5 baseline + 3 recent
if len(scores) < 8:
    print(f'UNKNOWN|Only {len(scores)} score(s) available — need 8 for regression check')
    sys.exit(0)

recent3  = scores[-3:]
baseline5 = scores[-8:-3]

avg_recent   = sum(recent3)   / len(recent3)
avg_baseline = sum(baseline5) / len(baseline5)

if avg_recent < avg_baseline:
    delta = avg_baseline - avg_recent
    print(f'WARNING|Recent avg {avg_recent:.2f} < baseline avg {avg_baseline:.2f} (Δ -{delta:.2f})')
else:
    delta = avg_recent - avg_baseline
    print(f'OK|Recent avg {avg_recent:.2f} >= baseline avg {avg_baseline:.2f} (Δ +{delta:.2f})')
PYEOF
) || result="UNKNOWN|python3 error reading session scores"

    local status="${result%%|*}"
    local detail="${result#*|}"

    case "$status" in
        OK|WARNING|CRITICAL|UNKNOWN) ;;
        *) status="UNKNOWN"; detail="Unexpected output from regression parser" ;;
    esac

    echo "\"session_regression\": {\"status\": \"$status\", \"detail\": \"$detail\"}"
}

# ─────────────────────────────────────────────────────────────────────
# Check 10: behavioral_drift
# Reads the most recent session score entry and checks the "corrections"
# field. More than 3 corrections in one session means Claude made too
# many mistakes or was too uncertain — a sign of behavioral drift.
# The "corrections" field is written by the session-scorer hook.
# ─────────────────────────────────────────────────────────────────────
check_behavioral_drift() {
    local scores_file="$HOME/.claude/session-scores.jsonl"

    if [ ! -f "$scores_file" ]; then
        echo '"behavioral_drift": {"status": "UNKNOWN", "detail": "session-scores.jsonl not found"}'
        return
    fi

    local result
    result=$($PYTHON3 - "$scores_file" <<'PYEOF' 2>/dev/null
import sys, json

scores_file = sys.argv[1]
last_entry = None
try:
    with open(scores_file, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            try:
                entry = json.loads(line)
                last_entry = entry
            except (json.JSONDecodeError, ValueError):
                continue
except Exception:
    print('UNKNOWN|Could not read session-scores.jsonl')
    sys.exit(0)

if last_entry is None:
    print('UNKNOWN|No session score entries found')
    sys.exit(0)

# "corrections" field: number of user corrections in that session
corrections = last_entry.get('corrections', None)

if corrections is None:
    # Field may not exist in older entries — treat as UNKNOWN
    print('UNKNOWN|No corrections field in last session entry')
    sys.exit(0)

try:
    corrections = int(corrections)
except (ValueError, TypeError):
    print(f'UNKNOWN|Cannot parse corrections value: {corrections}')
    sys.exit(0)

if corrections > 3:
    print(f'WARNING|Last session had {corrections} corrections (threshold: 3) — behavioral drift detected')
else:
    print(f'OK|Last session had {corrections} correction(s) — within normal range')
PYEOF
) || result="UNKNOWN|python3 error reading session scores"

    local status="${result%%|*}"
    local detail="${result#*|}"

    case "$status" in
        OK|WARNING|CRITICAL|UNKNOWN) ;;
        *) status="UNKNOWN"; detail="Unexpected output from behavioral drift parser" ;;
    esac

    echo "\"behavioral_drift\": {\"status\": \"$status\", \"detail\": \"$detail\"}"
}

# ─────────────────────────────────────────────────────────────────────
# Run all checks
# ─────────────────────────────────────────────────────────────────────
log "Heartbeat check started"

c_disk=$(check_disk_space)
c_config=$(check_config_integrity)
c_whiteboard=$(check_whiteboard)
c_brew=$(check_brew_outdated)
c_worker=$(check_claude_mem_worker)
c_dirs=$(check_key_directories)
c_compliance=$(check_validator_compliance)
c_drift=$(check_length_drift)
c_regression=$(check_session_regression)
c_behavioral=$(check_behavioral_drift)

# Determine overall status
overall="OK"
for check_result in "$c_disk" "$c_config" "$c_whiteboard" "$c_brew" "$c_worker" "$c_dirs" "$c_compliance" "$c_drift" "$c_regression" "$c_behavioral"; do
    if echo "$check_result" | grep -q '"CRITICAL"'; then
        overall="CRITICAL"
        break
    elif echo "$check_result" | grep -q '"WARNING"'; then
        overall="WARNING"
    fi
done

# ─────────────────────────────────────────────────────────────────────
# Write status file (atomic: temp → rename)
# ─────────────────────────────────────────────────────────────────────
TMP_STATUS=$(mktemp "${HEARTBEAT_DIR}/heartbeat-status.tmp.XXXXXX" 2>/dev/null)
if [ -n "$TMP_STATUS" ]; then
    cat > "$TMP_STATUS" <<ENDJSON
{
  "timestamp": "$(date -u '+%Y-%m-%dT%H:%M:%SZ')",
  "status": "$overall",
  "checks": {
    $c_disk,
    $c_config,
    $c_whiteboard,
    $c_brew,
    $c_worker,
    $c_dirs,
    $c_compliance,
    $c_drift,
    $c_regression,
    $c_behavioral
  }
}
ENDJSON

    # Validate the JSON we just wrote before replacing the status file
    if $PYTHON3 -c "import json; json.load(open('$TMP_STATUS'))" 2>/dev/null; then
        mv "$TMP_STATUS" "$STATUS_FILE" 2>/dev/null
    else
        log "ERROR: Generated status JSON is invalid — keeping old status file"
        rm -f "$TMP_STATUS" 2>/dev/null
    fi
else
    log "ERROR: Could not create temp file for status"
fi

log "Heartbeat check completed: $overall"

# ─────────────────────────────────────────────────────────────────────
# Log rotation: delete heartbeat logs older than 14 days
# ─────────────────────────────────────────────────────────────────────
find "$LOG_DIR" -name "*.log" -mtime +14 -delete 2>/dev/null

# ALWAYS exit 0 — heartbeat must never block anything
exit 0
