#!/bin/bash
# Auto-format: runs the appropriate code formatter after file edits
HOOK_NAME="auto-format" source "$HOME/.claude/hooks/hook-log.sh" 2>/dev/null
input=$(cat)
file_path=$(echo "$input" | yq -r '.tool_input.file_path // empty' 2>/dev/null)
[[ -z "$file_path" || ! -f "$file_path" ]] && exit 0

case "${file_path##*.}" in
  swift) swift-format --in-place "$file_path" 2>/dev/null ;;
  go)    gofmt -w "$file_path" 2>/dev/null ;;
  rs)    rustfmt "$file_path" 2>/dev/null ;;
  js|ts|tsx|mts|mjs|jsx|json|css)
    # Format JS/TS/JSON/CSS with Biome if available, else Prettier
    dir=$(dirname "$file_path")
    if command -v biome &>/dev/null; then
      biome format --write "$file_path" 2>/dev/null
    elif [[ -f "$dir/node_modules/.bin/prettier" ]]; then
      "$dir/node_modules/.bin/prettier" --write "$file_path" 2>/dev/null
    fi
    ;;
  cs)
    # Format C# with dotnet format if inside a project
    proj_dir=$(dirname "$file_path")
    while [[ "$proj_dir" != "/" ]]; do
      if ls "$proj_dir"/*.csproj "$proj_dir"/*.sln 2>/dev/null | head -1 >/dev/null 2>&1; then
        dotnet format whitespace "$proj_dir" --include "$file_path" 2>/dev/null
        break
      fi
      proj_dir=$(dirname "$proj_dir")
    done
    ;;
  c|cpp|h|hpp)
    # Format C/C++ with clang-format if available
    if command -v clang-format &>/dev/null; then
      clang-format -i "$file_path" 2>/dev/null
    fi
    ;;
  kt|kts)
    # Format Kotlin with ktfmt (kotlinlang style)
    if command -v ktfmt &>/dev/null; then
      ktfmt --kotlinlang-style "$file_path" 2>/dev/null
    fi
    ;;
  java)
    # Format Java with google-java-format
    if command -v google-java-format &>/dev/null; then
      google-java-format --replace "$file_path" 2>/dev/null
    fi
    ;;
esac
exit 0
