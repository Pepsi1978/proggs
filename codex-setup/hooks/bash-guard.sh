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
            echo "bash-guard: BLOCKIERT — gefaehrlicher Befehl erkannt (Pattern: $pattern). Bitte sichereren Befehl verwenden." >&2
            echo "{\"error\":\"BLOCKED: Dangerous command — $pattern\"}"
            exit 2
        fi
    done
}

# ============================================================
# PART 2: Codex-Verzeichnis + sed-auf-JSON blockieren (ex silent-corrector)
# ============================================================

check_forbidden() {
    # Befehle an && ; || aufsplitten und jeden Teilbefehl einzeln pruefen
    # (wie die PS1-Version — sonst werden verkettete Befehle nicht erkannt)
    local IFS_OLD="$IFS"
    echo "$cmd" | tr '&;|' '\n' | while IFS= read -r part; do
        local stripped
        stripped=$(echo "$part" | sed 's/^[[:space:]]*//' | sed 's/[[:space:]]*$//')
        [ -z "$stripped" ] && continue

        # Codex directory — cd
        if echo "$stripped" | grep -qE '^cd[[:space:]]+.*[~/]Codex[/]?' 2>/dev/null; then
            echo '{"reason":"BLOCKIERT: ~/Codex/ ist gesperrt. Verwende ~/Codex/."}'
            exit 2
        fi

        # Codex directory — file operations
        if echo "$stripped" | grep -qE '^(ls|cat|rm|mv|cp|touch|mkdir|chmod|head|tail|wc|stat|file)[[:space:]]' 2>/dev/null; then
            if echo "$stripped" | grep -qE '[~/]Codex[/]' 2>/dev/null; then
                echo '{"reason":"BLOCKIERT: ~/Codex/ ist gesperrt. Verwende ~/Codex/."}'
                exit 2
            fi
        fi

        # sed on JSON — jetzt pro Teilbefehl (nicht nur am Zeilenanfang)
        if echo "$stripped" | grep -qE '^sed[[:space:]]' 2>/dev/null; then
            if echo "$stripped" | grep -qE '\.json' 2>/dev/null; then
                echo '{"reason":"BLOCKIERT: sed auf JSON verboten. Benutze Edit-Tool oder python3 json.load/dump."}'
                exit 2
            fi
        fi
    done
    # Wenn die while-Subshell mit exit 2 beendet hat, ist $? = 2
    [ $? -eq 2 ] && exit 2
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

# ============================================================
# PART 4: settings.json-via-Bash-Warnung (ex safety-gate)
# ============================================================

check_settings_write() {
    if echo "$cmd" | grep -qE '>[[:space:]]*.*settings\.json' 2>/dev/null; then
        echo "WARNING: Schreibzugriff auf settings.json per Bash erkannt. config-guard prueft danach."
        exit 0
    fi
}

# Run all checks
check_dangerous
check_forbidden
check_shell_updates
check_settings_write

exit 0
