#!/bin/bash
# StopFailure Hook — Logs API errors that terminate sessions to MEMORY.md
# NEW in Claude Code 2.1.78 — fires on rate limit, auth failure, network errors
# Wrapped with timeout to prevent blocking session cleanup

FAILURES_FILE="$HOME/.claude/agent-memory/shared/MEMORY.md"
DATE=$(date +%Y-%m-%d)
TIME=$(date +%H:%M:%S)

# Ensure MEMORY.md exists
if [ ! -f "$FAILURES_FILE" ]; then
  exit 0
fi

# Read error context from stdin (Claude Code pipes hook context as JSON)
ERROR_CONTEXT=$(cat 2>/dev/null || echo '{}')

# Try to extract error type from context
ERROR_TYPE="unknown"
if echo "$ERROR_CONTEXT" | grep -qi "rate.limit\|429"; then
  ERROR_TYPE="rate_limit"
elif echo "$ERROR_CONTEXT" | grep -qi "auth\|401\|403\|unauthorized"; then
  ERROR_TYPE="auth_failure"
elif echo "$ERROR_CONTEXT" | grep -qi "network\|timeout\|ECONNREFUSED\|ETIMEDOUT"; then
  ERROR_TYPE="network_error"
elif echo "$ERROR_CONTEXT" | grep -qi "overloaded\|503\|529"; then
  ERROR_TYPE="api_overloaded"
fi

# Append to MEMORY.md
cat >> "$FAILURES_FILE" << EOF

### [$DATE] API: Session terminated by $ERROR_TYPE ($TIME)
- **Symptom**: Session ended unexpectedly due to API error
- **Root Cause**: $ERROR_TYPE (auto-detected by StopFailure hook)
- **Fix**: Automatic retry on next session start
- **Prevention**: StopFailure hook monitors for recurring patterns
- **Context**: $(echo "$ERROR_CONTEXT" | head -c 200)
- **Status**: AUTO-LOGGED (incomplete context — review manually if recurring)
EOF

exit 0
