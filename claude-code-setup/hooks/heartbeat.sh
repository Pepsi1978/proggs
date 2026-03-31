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
        if ! python3 -c "import json; json.load(open('$SETTINGS_JSON'))" 2>/dev/null; then
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
        if ! python3 -c "import json; json.load(open('$SETTINGS_LOCAL'))" 2>/dev/null; then
            issues=$((issues + 1))
            details="${details}settings.local.json CORRUPT; "
            notify_critical "settings.local.json ist beschaedigt!"
        fi
    fi

    # Check .mcp.json
    if [ -f "$MCP_JSON" ]; then
        if ! python3 -c "import json; json.load(open('$MCP_JSON'))" 2>/dev/null; then
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
    if ! command -v /opt/homebrew/bin/brew &>/dev/null; then
        echo '"brew_outdated": {"status": "UNKNOWN", "detail": "Homebrew not found"}'
        return
    fi

    local count
    count=$(/opt/homebrew/bin/brew outdated --quiet 2>/dev/null | wc -l | tr -d ' ')

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
# Run all checks
# ─────────────────────────────────────────────────────────────────────
log "Heartbeat check started"

c_disk=$(check_disk_space)
c_config=$(check_config_integrity)
c_whiteboard=$(check_whiteboard)
c_brew=$(check_brew_outdated)
c_worker=$(check_claude_mem_worker)
c_dirs=$(check_key_directories)

# Determine overall status
overall="OK"
for check_result in "$c_disk" "$c_config" "$c_whiteboard" "$c_brew" "$c_worker" "$c_dirs"; do
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
    $c_dirs
  }
}
ENDJSON

    # Validate the JSON we just wrote before replacing the status file
    if python3 -c "import json; json.load(open('$TMP_STATUS'))" 2>/dev/null; then
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
