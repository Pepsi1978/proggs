#!/usr/bin/env bash
# hook-exit0-guard: Pre-commit check that ALL hooks end with exit 0
# Runs as PreToolUse hook — triggers on Bash tool when git commit is detected
# stdout -> AI context (system-reminder), stderr -> user terminal
# Platform: macOS/Linux

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
. "$SCRIPT_DIR/hook-log.sh"

trap 'hook_log_warn "hook-exit0-guard: Error at line $LINENO"' ERR

input=$(cat)
tool_name=$(echo "$input" | jq -r '.tool_name // empty' 2>/dev/null)
tool_input_command=$(echo "$input" | jq -r '.tool_input.command // empty' 2>/dev/null)

# Only trigger on Bash tool with git commit commands
if [ "$tool_name" != "Bash" ]; then exit 0; fi
if ! echo "$tool_input_command" | grep -q 'git commit'; then exit 0; fi

# Check if any hook files are staged
repo_dir="$HOME/proggs"
staged=$(git -C "$repo_dir" diff --cached --name-only 2>/dev/null || true)
if [ -z "$staged" ]; then exit 0; fi

hook_files=$(echo "$staged" | grep -E '\.(ps1|sh)$' | grep -i 'hook' || true)
if [ -z "$hook_files" ]; then exit 0; fi

# Check each staged hook file for exit 0
problems=""
while IFS= read -r file; do
    [ -z "$file" ] && continue
    full_path="$repo_dir/$file"
    [ ! -f "$full_path" ] && continue

    # Check if file ends with exit 0 (allowing trailing whitespace/newlines)
    if ! tail -5 "$full_path" | grep -qE '^exit\s+0'; then
        problems="$problems\n  - $file"
    fi

    # Check for exit 1 in SessionStart hooks
    if echo "$file" | grep -qiE '(session|auto-sync|invariant)'; then
        if grep -qE 'exit\s+1' "$full_path"; then
            problems="$problems\n  - $file (contains exit 1 — forbidden in SessionStart hooks!)"
        fi
    fi
done <<< "$hook_files"

if [ -n "$problems" ]; then
    msg="Hook-Exit0-Guard: WARNUNG — folgende Hook-Dateien haben kein 'exit 0' am Ende:$problems\nBitte 'exit 0' am Ende jeder Hook-Datei hinzufuegen!"
    echo -e "$msg"
    echo -e "$msg" >&2
    hook_log_warn "Hook files missing exit 0:$problems"
fi

exit 0
