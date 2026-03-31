#!/bin/bash
# Cascade Format Hook: Lint-Pruefung nach jedem Edit/Write auf Code-Dateien
# Quelle: Superintelligenz Finding 3 — Cascade Hooks Pattern (Windsurf-inspiriert)
# Event: PostToolUse (Edit|Write)
#
# FIXED: Pipeline-Exit-Code-Bugs bei cargo/go (Audit Finding #1, #3)
# FIXED: Shell-Injection via FILE_PATH (Audit Finding #2)
# FIXED: ktlint --relative entfernt (Audit Finding #4)
# FIXED: Tampermonkey .user.js Ausnahme (Audit Finding #6)
# FIXED: Leerer stdin behandelt (Audit Finding #13)

set +e  # Nie crashen

INPUT=$(cat)
[[ -z "$INPUT" ]] && exit 0

FILE_PATH=$(echo "$INPUT" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('tool_input',{}).get('file_path',''))" 2>/dev/null)

[[ -z "$FILE_PATH" || ! -f "$FILE_PATH" ]] && exit 0

EXT="${FILE_PATH##*.}"
EXT=$(echo "$EXT" | tr '[:upper:]' '[:lower:]')
LINT_CMD=""
LINT_FAILED=0
RESULT=""

# Tampermonkey .user.js: ueberspringen — nutzt eslint per CLAUDE.md
[[ "$FILE_PATH" == *.user.js ]] && exit 0

case "$EXT" in
    kt|kts)
        if command -v ktlint &>/dev/null; then
            LINT_CMD="ktlint"
            RESULT=$(ktlint "$FILE_PATH" 2>&1)
            [[ $? -ne 0 ]] && LINT_FAILED=1
        fi
        ;;
    js|jsx|ts|tsx|mjs|mts)
        if command -v biome &>/dev/null; then
            LINT_CMD="biome lint"
            RESULT=$(biome lint "$FILE_PATH" 2>&1)
            [[ $? -ne 0 ]] && LINT_FAILED=1
        fi
        ;;
    json)
        # Sicher: FILE_PATH als Argument, nie interpoliert
        if python3 -c "import json,sys; json.load(open(sys.argv[1], encoding='utf-8'))" -- "$FILE_PATH" 2>/dev/null; then
            :  # Valid JSON
        else
            LINT_CMD="json-validate"
            RESULT="Invalid JSON"
            LINT_FAILED=1
        fi
        ;;
    py)
        if command -v python3 &>/dev/null; then
            LINT_CMD="py_compile"
            RESULT=$(python3 -m py_compile "$FILE_PATH" 2>&1)
            [[ $? -ne 0 ]] && LINT_FAILED=1
        fi
        ;;
    go)
        if command -v go &>/dev/null; then
            DIR=$(dirname "$FILE_PATH")
            LINT_CMD="go vet"
            # Fix: Exit-Code von go vet erfassen, nicht von head
            FULL_RESULT=$(go vet "$DIR/..." 2>&1)
            GO_EXIT=$?
            RESULT=$(echo "$FULL_RESULT" | head -5)
            [[ $GO_EXIT -ne 0 ]] && LINT_FAILED=1
        fi
        ;;
    rs)
        # Rust: ausgeschlossen — cargo check zu langsam fuer 15s Hook-Timeout
        # Manuell ausfuehren: cargo check
        ;;
esac

if [[ $LINT_FAILED -eq 1 && -n "$LINT_CMD" ]]; then
    BASENAME=$(basename "$FILE_PATH")
    echo "CASCADE-LINT [$LINT_CMD]: Fehler in $BASENAME"
    echo "$RESULT" | head -5
    echo "Bitte den Lint-Fehler fixen bevor weitergearbeitet wird."
fi

exit 0
