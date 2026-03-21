#!/usr/bin/env bash
# auto-format.sh — PostToolUse Hook
# Runs the appropriate code formatter after file edits.
# Platform: macOS/Linux (bash)

HOOKS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$HOOKS_DIR/hook-log.sh"

# Read JSON input from stdin
HOOK_INPUT=$(cat)
if [ -z "$HOOK_INPUT" ]; then
    exit 0
fi

# Extract file_path from JSON input (Edit tool uses file_path, Write tool uses path)
FILE_PATH=$(echo "$HOOK_INPUT" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    ti = data.get('tool_input', {})
    fp = ti.get('file_path') or ti.get('path') or ''
    print(fp)
except Exception:
    print('')
" 2>/dev/null)

if [ -z "$FILE_PATH" ] || [ ! -f "$FILE_PATH" ]; then
    exit 0
fi

hook_log "formatting $FILE_PATH"

# Determine file extension (without leading dot)
EXT="${FILE_PATH##*.}"

case "$EXT" in
    go)
        gofmt -w "$FILE_PATH" 2>/dev/null
        if [ $? -ne 0 ]; then
            hook_log_warn "formatter gofmt failed (exit $?) on $FILE_PATH"
        fi
        ;;
    rs)
        rustfmt "$FILE_PATH" 2>/dev/null
        if [ $? -ne 0 ]; then
            hook_log_warn "formatter rustfmt failed (exit $?) on $FILE_PATH"
        fi
        ;;
    cs)
        DIR=$(dirname "$FILE_PATH")
        # Search UPWARD for a .csproj file (C# projects contain the file, not the other way around)
        dir="$DIR"
        PROJ=""
        while [ "$dir" != "/" ] && [ "$dir" != "." ]; do
            PROJ=$(find "$dir" -maxdepth 1 -name "*.csproj" 2>/dev/null | head -1)
            [ -n "$PROJ" ] && break
            dir=$(dirname "$dir")
        done
        if [ -n "$PROJ" ]; then
            PROJ_DIR=$(dirname "$PROJ")
            dotnet format whitespace "$PROJ_DIR" --include "$FILE_PATH" 2>/dev/null
            if [ $? -ne 0 ]; then
                hook_log_warn "formatter dotnet-format failed (exit $?) on $FILE_PATH"
            fi
        fi
        ;;
    ts|tsx|mts|js|jsx|mjs|json|css)
        if command -v biome > /dev/null 2>&1; then
            biome format --write "$FILE_PATH" 2>/dev/null
            if [ $? -ne 0 ]; then
                hook_log_warn "formatter biome failed (exit $?) on $FILE_PATH"
            fi
        elif command -v prettier > /dev/null 2>&1; then
            prettier --write "$FILE_PATH" 2>/dev/null
            if [ $? -ne 0 ]; then
                hook_log_warn "formatter prettier failed (exit $?) on $FILE_PATH"
            fi
        fi
        ;;
    c|cpp|h|hpp)
        if command -v clang-format > /dev/null 2>&1; then
            clang-format -i "$FILE_PATH" 2>/dev/null
            if [ $? -ne 0 ]; then
                hook_log_warn "formatter clang-format failed (exit $?) on $FILE_PATH"
            fi
        fi
        ;;
    kt|kts)
        if command -v ktfmt > /dev/null 2>&1; then
            ktfmt --kotlinlang-style "$FILE_PATH" 2>/dev/null
            if [ $? -ne 0 ]; then
                hook_log_warn "formatter ktfmt failed (exit $?) on $FILE_PATH"
            fi
        elif command -v ktlint > /dev/null 2>&1; then
            ktlint --format "$FILE_PATH" 2>/dev/null
            if [ $? -ne 0 ]; then
                hook_log_warn "formatter ktlint failed (exit $?) on $FILE_PATH"
            fi
        fi
        ;;
    java)
        if command -v google-java-format > /dev/null 2>&1; then
            google-java-format --replace "$FILE_PATH" 2>/dev/null
            if [ $? -ne 0 ]; then
                hook_log_warn "formatter google-java-format failed (exit $?) on $FILE_PATH"
            fi
        fi
        ;;
esac

exit 0
