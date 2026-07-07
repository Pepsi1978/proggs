#!/usr/bin/env bash
# hook-log.sh — Error logging for Claude Code hooks (bash/macOS/Linux)
# Source this at the top of every hook script:
#   source "$(dirname "$0")/hook-log.sh"
#
# Provides:
#   hook_log "message"       — log info message
#   hook_log_error "message" — log error with details
#   hook_log_warn "message"  — log warning
#
# Logs to: ~/.claude/logs/hooks/YYYY-MM-DD.log
# Log rotation: files older than 14 days are auto-deleted

_HOOK_LOG_DIR="$HOME/.claude/logs/hooks"
_HOOK_LOG_NAME="$(basename "${BASH_SOURCE[1]:-unknown-hook}" .sh)"
_HOOK_LOG_FILE="$_HOOK_LOG_DIR/$(date '+%Y-%m-%d').log"

# Ensure log directory exists
mkdir -p "$_HOOK_LOG_DIR"

# Clean up log files older than 14 days (macOS uses -mtime +14, Linux too)
find "$_HOOK_LOG_DIR" -name "*.log" -mtime +14 -delete 2>/dev/null || true

hook_log() {
    local ts
    ts=$(date '+%H:%M:%S')
    echo "[$ts] $_HOOK_LOG_NAME: $1" >> "$_HOOK_LOG_FILE" 2>/dev/null || true
}

hook_log_error() {
    local ts
    ts=$(date '+%H:%M:%S')
    echo "[$ts] ERROR $_HOOK_LOG_NAME: $1" >> "$_HOOK_LOG_FILE" 2>/dev/null || true
}

hook_log_warn() {
    local ts
    ts=$(date '+%H:%M:%S')
    echo "[$ts] WARN  $_HOOK_LOG_NAME: $1" >> "$_HOOK_LOG_FILE" 2>/dev/null || true
}

# Trap handler for ERR signal — logs the failing command and line number
_hook_log_trap_handler() {
    local line_no="$1"
    local cmd="${BASH_COMMAND}"
    hook_log_error "command failed at line ${line_no} — ${cmd}"
}

# IMPORTANT: The ERR trap is suppressed by `|| true` constructs in sourcing hooks.
# Hooks that use `command || true` will NOT trigger this trap on failure.
# For reliable error detection, hooks should check $? explicitly after critical commands.

# Register ERR trap: fires whenever a command exits with non-zero status
trap '_hook_log_trap_handler $LINENO' ERR

# Register EXIT trap: logs unexpected non-zero exit codes (mirrors PS1's PowerShell.Exiting handler)
# Expected exit codes: 0 = OK, 1 = intentional denial (e.g., resource locked), 2 = intentional block (e.g., safety-gate)
# Only codes >= 3 are logged as truly unexpected failures.
_HOOK_EXPECTED_EXIT_CODES=(0 1 2)
_hook_log_exit_handler() {
    local exit_code="$?"
    local expected=false
    for code in "${_HOOK_EXPECTED_EXIT_CODES[@]}"; do
        if [[ "$exit_code" -eq "$code" ]]; then
            expected=true
            break
        fi
    done
    if [[ "$expected" = false ]]; then
        hook_log_error "unexpected exit code ${exit_code}"
    fi
}
trap '_hook_log_exit_handler' EXIT

hook_log "started"
