#!/bin/bash
# Safety Gate: Block dangerous commands before execution
# Works on: macOS, Linux, Termux
HOOK_NAME="safety-gate" source "$HOME/.claude/hooks/hook-log.sh" 2>/dev/null || true

INPUT=$(cat)

# Parse tool name and command — try python3 first, then jq
if command -v python3 &>/dev/null; then
    TOOL_NAME=$(echo "$INPUT" | python3 -c "import sys,json; print(json.load(sys.stdin).get('tool_name',''))" 2>/dev/null || echo "")
    COMMAND=$(echo "$INPUT" | python3 -c "import sys,json; print(json.load(sys.stdin).get('tool_input',{}).get('command',''))" 2>/dev/null || echo "")
elif command -v jq &>/dev/null; then
    TOOL_NAME=$(echo "$INPUT" | jq -r '.tool_name // empty' 2>/dev/null || echo "")
    COMMAND=$(echo "$INPUT" | jq -r '.tool_input.command // empty' 2>/dev/null || echo "")
else
    exit 0
fi

# Only check Bash commands
[[ "$TOOL_NAME" != "Bash" ]] && exit 0
[[ -z "$COMMAND" ]] && exit 0

# Dangerous patterns to block
PATTERNS=(
    'rm[[:space:]]+-rf[[:space:]]+'
    'git[[:space:]]+push[[:space:]]+--force'
    'git[[:space:]]+push[[:space:]]+-f[[:space:]]'
    'git[[:space:]]+reset[[:space:]]+--hard'
    'git[[:space:]]+clean[[:space:]]+-[a-z]*f'
    'git[[:space:]]+checkout[[:space:]]+--[[:space:]]+\.'
    'DROP[[:space:]]+TABLE'
    'DROP[[:space:]]+DATABASE'
    'TRUNCATE[[:space:]]+TABLE'
)

for pattern in "${PATTERNS[@]}"; do
    if echo "$COMMAND" | grep -qE "$pattern" 2>/dev/null; then
        echo "{\"error\":\"BLOCKED: Dangerous command detected — $pattern\"}"
        exit 2
    fi
done
exit 0
