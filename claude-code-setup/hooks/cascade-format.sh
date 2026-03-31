#!/bin/bash
# Cascade Format Hook: Lint-Pruefung nach jedem Edit/Write auf Code-Dateien
# Quelle: Superintelligenz Finding 3 — Cascade Hooks Pattern (Windsurf-inspiriert)
# Event: PostToolUse (Edit|Write)
# Zweck: Fuehrt Linting auf geaenderten Code-Dateien aus und meldet Fehler.
#
# UNTERSCHIED zu auto-format:
#   auto-format = Formatierung (Whitespace) — ASYNC, Fehler ignoriert
#   cascade-format = LINT (Syntax, Imports) — SYNC, Fehler gemeldet

set +e  # Nie crashen

INPUT=$(cat)

FILE_PATH=$(echo "$INPUT" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('tool_input',{}).get('file_path',''))" 2>/dev/null)

if [[ -z "$FILE_PATH" || ! -f "$FILE_PATH" ]]; then
    exit 0
fi

EXT="${FILE_PATH##*.}"
EXT=$(echo "$EXT" | tr '[:upper:]' '[:lower:]')
LINT_CMD=""
LINT_FAILED=0

case "$EXT" in
    kt|kts)
        if command -v ktlint &>/dev/null; then
            LINT_CMD="ktlint"
            RESULT=$(ktlint --relative "$FILE_PATH" 2>&1)
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
        if python3 -c "import json; json.load(open('$FILE_PATH', encoding='utf-8'))" 2>/dev/null; then
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
            RESULT=$(go vet "$DIR/..." 2>&1 | head -5)
            [[ $? -ne 0 ]] && LINT_FAILED=1
        fi
        ;;
    rs)
        CARGO=$(find "$(dirname "$FILE_PATH")" -maxdepth 3 -name "Cargo.toml" -print -quit 2>/dev/null)
        if [[ -n "$CARGO" ]] && command -v cargo &>/dev/null; then
            LINT_CMD="cargo check"
            RESULT=$(cargo check --manifest-path "$CARGO" 2>&1 | head -5)
            [[ $? -ne 0 ]] && LINT_FAILED=1
        fi
        ;;
esac

if [[ $LINT_FAILED -eq 1 && -n "$LINT_CMD" ]]; then
    BASENAME=$(basename "$FILE_PATH")
    echo "CASCADE-LINT [$LINT_CMD]: Fehler in $BASENAME"
    echo "$RESULT" | head -5
    echo "Bitte den Lint-Fehler fixen bevor weitergearbeitet wird."
fi

exit 0
