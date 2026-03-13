#!/bin/bash
# Safety Gate: Block dangerous commands before execution (macOS/Linux version)
INPUT=$(cat)

TOOL_NAME=$(echo "$INPUT" | python3 -c "import sys,json; print(json.load(sys.stdin).get('tool_name',''))" 2>/dev/null)
COMMAND=$(echo "$INPUT" | python3 -c "import sys,json; print(json.load(sys.stdin).get('tool_input',{}).get('command',''))" 2>/dev/null)

# Only check Bash commands
[[ "$TOOL_NAME" != "Bash" ]] && exit 0
[[ -z "$COMMAND" ]] && exit 0

# Dangerous patterns to block
PATTERNS=(
    'rm[[:space:]]+-rf[[:space:]]+[/~]'
    'git[[:space:]]+push[[:space:]]+--force[[:space:]]+.*main'
    'git[[:space:]]+reset[[:space:]]+--hard'
    'DROP[[:space:]]+TABLE'
    'DROP[[:space:]]+DATABASE'
)

for pattern in "${PATTERNS[@]}"; do
    if echo "$COMMAND" | grep -qE "$pattern"; then
        echo "{\"error\":\"BLOCKED: Dangerous command detected — $pattern\"}"
        exit 2
    fi
done
exit 0
