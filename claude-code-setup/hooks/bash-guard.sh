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
# PART 1b: 'git add -A' / 'git add .' blockieren (parallele Sessions)
# Wuerde fremde unfertige Dateien anderer Sessions mitcommitten.
# Praezise: blockiert -A/--all/././ auch nach Flags, aber NICHT
# spezifische Pfade ('git add .claude/x', 'git add foo.txt') und
# nicht ueber && hinweg ('... && grep -A 3'). grep -E ohne -i =
# case-sensitiv, damit '-A' nicht auch '-a' matcht.
# ============================================================

check_git_add_all() {
    if printf '%s' "$cmd" | grep -qE 'git[[:space:]]+add[[:space:]]+([^&|;<>]*[[:space:]]+)?(-A|--all|\.|\./)([[:space:]]|$)' 2>/dev/null; then
        hook_log_error "BLOCKED 'git add -A/.': ${cmd:0:100}" 2>/dev/null || true
        echo "bash-guard: BLOCKIERT — 'git add -A' / 'git add .' ist verboten. Parallele Sessions wuerden fremde unfertige Dateien mitcommitten. Stage deine eigenen Dateien namentlich: git add pfad/datei1 pfad/datei2" >&2
        echo '{"error":"BLOCKED: git add -A/. verboten (parallele Sessions). Stage eigene Dateien namentlich: git add pfad/datei"}'
        exit 2
    fi
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
            echo '{"reason":"BLOCKIERT: ~/Codex/ ist gesperrt. Verwende ~/proggs/."}'
            exit 2
        fi

        # Codex directory — file operations
        if echo "$stripped" | grep -qE '^(ls|cat|rm|mv|cp|touch|mkdir|chmod|head|tail|wc|stat|file)[[:space:]]' 2>/dev/null; then
            if echo "$stripped" | grep -qE '[~/]Codex[/]' 2>/dev/null; then
                echo '{"reason":"BLOCKIERT: ~/Codex/ ist gesperrt. Verwende ~/proggs/."}'
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
    # FIX 2026-08-26: Der Test war der LETZTE Befehl der Funktion -> im Normalfall
    # (nichts blockiert) gab die Funktion 1 zurueck. Der ERR-Trap aus hook-log.sh
    # wertete das als Fehler -> 138 Falschmeldungen pro Session im Hook-Log, echte
    # Fehler gingen darin unter. Explizites return 0 macht den Rueckgabewert bewusst.
    if [ $? -eq 2 ]; then exit 2; fi
    return 0
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
    fi
}

# ============================================================
# PART 5: Hook-Exit0-Guard (ex hook-exit0-guard.sh, konsolidiert 2026-05-10)
# Loop-3 ESK-4: vermeidet 2. Bash-Spawn bei jedem Bash-Call.
# Triggert nur auf 'git commit', prueft staged Hook-Dateien auf exit 0.
# ============================================================

check_exit0_guard() {
    if ! echo "$cmd" | grep -q 'git commit' 2>/dev/null; then return 0; fi

    local repo_dir="$HOME/proggs"
    local staged
    staged=$(git -C "$repo_dir" diff --cached --name-only 2>/dev/null || true)
    [ -z "$staged" ] && return 0

    local hook_files
    hook_files=$(echo "$staged" | grep -E '\.(ps1|sh)$' | grep -i 'hook' || true)
    [ -z "$hook_files" ] && return 0

    local problems=""
    while IFS= read -r file; do
        [ -z "$file" ] && continue
        local full_path="$repo_dir/$file"
        [ ! -f "$full_path" ] && continue

        if ! tail -5 "$full_path" | grep -qE '^exit[[:space:]]+0'; then
            problems="$problems\n  - $file"
        fi

        if echo "$file" | grep -qiE '(session|auto-sync|invariant)'; then
            if grep -qE 'exit[[:space:]]+1' "$full_path"; then
                problems="$problems\n  - $file (contains exit 1 — forbidden in SessionStart hooks!)"
            fi
        fi
    done <<< "$hook_files"

    if [ -n "$problems" ]; then
        local msg="Hook-Exit0-Guard: WARNUNG — folgende Hook-Dateien haben kein 'exit 0' am Ende:$problems\nBitte 'exit 0' am Ende jeder Hook-Datei hinzufuegen!"
        echo -e "$msg"
        echo -e "$msg" >&2
        hook_log_warn "Hook files missing exit 0:$problems" 2>/dev/null || true
    fi
}

# Run all checks
check_dangerous
check_git_add_all
check_forbidden
check_shell_updates
check_settings_write
check_exit0_guard

exit 0
