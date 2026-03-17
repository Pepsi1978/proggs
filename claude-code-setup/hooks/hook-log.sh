#!/bin/bash
# hook-log.sh — Error logging for Claude Code hooks
# Source this at the top of every hook script.
#
# Provides:
#   hook_log "message"       — log info message
#   hook_log_error "message" — log error with details
#
# Logs to: ~/.claude/logs/hooks/YYYY-MM-DD.log
# Log rotation: files older than 14 days are auto-deleted
#
# DESIGN: NO exit trap. Previous versions used an EXIT trap that caused
# phantom "hook error" messages when intermediate commands had non-zero
# exit codes. Each hook handles its own errors explicitly.

_HOOK_LOG_DIR="$HOME/.claude/logs/hooks"
_HOOK_LOG_NAME="${HOOK_NAME:-$(basename "${BASH_SOURCE[1]:-$0}" .sh)}"
_HOOK_LOG_FILE="$_HOOK_LOG_DIR/$(date +%Y-%m-%d).log"

mkdir -p "$_HOOK_LOG_DIR" 2>/dev/null

# Clean up log files older than 14 days
find "$_HOOK_LOG_DIR" -name "*.log" -mtime +14 -delete 2>/dev/null || true

hook_log() {
    echo "[$(date '+%H:%M:%S')] $_HOOK_LOG_NAME: $*" >> "$_HOOK_LOG_FILE" 2>/dev/null
}

hook_log_error() {
    echo "[$(date '+%H:%M:%S')] ERROR $_HOOK_LOG_NAME: $*" >> "$_HOOK_LOG_FILE" 2>/dev/null
}

# Kept for API compatibility but does nothing — no exit trap anymore
hook_expect_exit() { :; }
