#!/usr/bin/env bash
# bash-guard.sh — Consolidated PreToolUse:Bash guard
# Event: PreToolUse
# Matcher: Bash
# Type: command
# Platform: macOS/Linux
#
# CONSOLIDATION (2026-04-12): Vereint safety-gate.sh + silent-corrector.sh
# Alle Bash-Kommando-Pruefungen in einer Datei.
#
# Hook-Protokoll: exit 0 = erlauben, exit 2 + JSON = blockieren
# ROBUSTNESS: Fail-open — bei jedem Fehler exit 0

set +e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
. "$SCRIPT_DIR/hook-log.sh" 2>/dev/null || true

# Read stdin JSON
input_data=$(cat)
if [ -z "$input_data" ] || [ ${#input_data} -lt 5 ]; then exit 0; fi

tool_name=$(echo "$input_data" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('tool_name',''))" 2>/dev/null)
if [ "$tool_name" != "Bash" ]; then exit 0; fi

cmd=$(echo "$input_data" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('tool_input',{}).get('command',''))" 2>/dev/null)
if [ -z "$cmd" ]; then exit 0; fi

# ============================================================
# PART 1: Gefaehrliche Befehle blockieren (ex safety-gate)
# ============================================================

check_dangerous() {
    local patterns=(
        'rm[[:space:]]+-rf[[:space:]]+[/~]'
        'git[[:space:]]+push[[:space:]]+--force[[:space:]]+.*main'
        'git[[:space:]]+reset[[:space:]]+--hard'
        'git[[:space:]]+restore[[:space:]]+\.'
        'git[[:space:]]+branch[[:space:]]+-D'
        'DROP[[:space:]]+TABLE'
        'DROP[[:space:]]+DATABASE'
        'TRUNCATE[[:space:]]+TABLE'
        'git[[:space:]]+init'
        'gh[[:space:]]+repo[[:space:]]+create'
        'git[[:space:]]+remote[[:space:]]+add'
    )

    for pattern in "${patterns[@]}"; do
        if echo "$cmd" | grep -qiE "$pattern" 2>/dev/null; then
            echo "{\"error\":\"BLOCKED: Dangerous command — $pattern\"}"
            exit 2
        fi
    done
}

# ============================================================
# PART 2: Codex-Verzeichnis + sed-auf-JSON blockieren (ex silent-corrector)
# ============================================================

check_forbidden() {
    # Codex directory
    if echo "$cmd" | grep -qE 'cd[[:space:]]+.*[~/]Codex[/]?' 2>/dev/null; then
        echo '{"reason":"BLOCKIERT: ~/Codex/ ist gesperrt. Verwende ~/proggs/."}'
        exit 2
    fi
    if echo "$cmd" | grep -qE '(ls|cat|rm|mv|cp|touch|mkdir)[[:space:]]+.*[~/]Codex[/]' 2>/dev/null; then
        echo '{"reason":"BLOCKIERT: ~/Codex/ ist gesperrt. Verwende ~/proggs/."}'
        exit 2
    fi

    # sed on JSON
    if echo "$cmd" | grep -qE '^sed[[:space:]]' 2>/dev/null && echo "$cmd" | grep -qE '\.json' 2>/dev/null; then
        echo '{"reason":"BLOCKIERT: sed auf JSON verboten. Benutze Edit-Tool oder python3 json.load/dump."}'
        exit 2
    fi
}

# ============================================================
# PART 3: Shell-Update-Warnung (ex safety-gate)
# ============================================================

check_shell_updates() {
    local patterns=(
        'npm[[:space:]]+install[[:space:]]+-g[[:space:]]+@anthropic'
        'brew[[:space:]]+upgrade'
        'rustup[[:space:]]+update'
    )
    for pattern in "${patterns[@]}"; do
        if echo "$cmd" | grep -qE "$pattern" 2>/dev/null; then
            echo "WARNING: Shell-Update erkannt. Laut Regeln muessen Shell-Updates NACH allen Aufgaben erfolgen."
            exit 0
        fi
    done
}

# Run all checks
check_dangerous
check_forbidden
check_shell_updates

exit 0
