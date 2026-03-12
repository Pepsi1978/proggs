#!/data/data/com.termux/files/usr/bin/bash
# Auto-format hook for Termux
# Formats files after Claude edits them
# Reads JSON from stdin with the edited file path

INPUT=$(cat)
FILE=$(echo "$INPUT" | jq -r '.tool_input.file_path // .tool_input.filePath // empty' 2>/dev/null)

[ -z "$FILE" ] || [ ! -f "$FILE" ] && exit 0

EXT="${FILE##*.}"

case "$EXT" in
  js|jsx|ts|tsx|json|css)
    # Prettier if available (npx or local)
    if command -v npx &>/dev/null && [ -f "$(dirname "$FILE")/node_modules/.bin/prettier" ]; then
      npx prettier --write "$FILE" 2>/dev/null
    fi
    ;;
  rs)
    # Rustfmt for Rust
    if command -v rustfmt &>/dev/null; then
      rustfmt "$FILE" 2>/dev/null
    fi
    ;;
  go)
    # Gofmt for Go
    if command -v gofmt &>/dev/null; then
      gofmt -w "$FILE" 2>/dev/null
    fi
    ;;
  sh|bash)
    # Syntax check with bash -n
    bash -n "$FILE" 2>/dev/null
    ;;
esac

exit 0
