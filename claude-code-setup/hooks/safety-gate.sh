#!/bin/bash
# Safety Gate: Block dangerous commands before execution
# Hook event: PreToolUse (Bash)
# Platform: macOS / Linux (bash)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/hook-log.sh"
# shellcheck source=whiteboard-insert.sh
source "$SCRIPT_DIR/whiteboard-insert.sh"

# ---------------------------------------------------------------------------
# Read JSON from stdin
# ---------------------------------------------------------------------------
hook_input="$(cat)"

# Guard: if python3 is unavailable, allow the command (fail-open for non-critical hooks)
if ! command -v python3 &>/dev/null; then
    hook_log_warn "python3 not found — safety-gate cannot parse input, allowing command"
    exit 0
fi

tool_name="$(printf '%s' "$hook_input" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('tool_name',''))" 2>/dev/null)"
cmd="$(printf '%s' "$hook_input"       | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('tool_input',{}).get('command',''))" 2>/dev/null)"

# Only check Bash tool calls
if [ "$tool_name" != "Bash" ]; then
    exit 0
fi

if [ -z "$cmd" ]; then
    exit 0
fi

# ---------------------------------------------------------------------------
# Dangerous patterns to block — split into case-sensitive (ERE) and
# case-insensitive arrays so we avoid PCRE (?i) which is not valid in ERE.
# ---------------------------------------------------------------------------

# Case-sensitive block patterns (grep -E)
declare -a PATTERNS=(
    'rm[[:space:]]+-rf[[:space:]]+[/~]'                     # rm -rf / or ~
    'git[[:space:]]+push[[:space:]]+--force[[:space:]]+.*main'  # force-push to main
    '^\s*git[[:space:]]+reset[[:space:]]+--hard'            # hard reset
    '^\s*git[[:space:]]+restore[[:space:]]+\.'              # git restore . (discard all changes)
    '^\s*git[[:space:]]+branch[[:space:]]+-D'               # git branch -D (force-delete)
    '^\s*git[[:space:]]+init'                               # (#42) block creating new repos — only Pepsi1978/proggs allowed
    '^\s*gh[[:space:]]+repo[[:space:]]+create'              # (#42) block creating GitHub repos
    '^\s*git[[:space:]]+remote[[:space:]]+add'              # (#42) block adding new remotes
)

# Case-insensitive block patterns (grep -iE) — replaces the old PCRE (?i) entries
declare -a PATTERNS_CI=(
    'DROP[[:space:]]+TABLE'
    'DROP[[:space:]]+DATABASE'
    'TRUNCATE[[:space:]]+TABLE'
)

# Shell update patterns — WARNING only (exit 0), not blocking
# (#49) Warn when shell tools are updated mid-session (can kill running terminals)
declare -a SHELL_UPDATE_PATTERNS=(
    'rustup[[:space:]]+update'
    'npm[[:space:]]+install[[:space:]]+-g'
    'pip[[:space:]]+install[[:space:]]+--upgrade'
)

# Case-sensitive block check
for pattern in "${PATTERNS[@]}"; do
    if printf '%s' "$cmd" | grep -Eq "$pattern"; then
        short_cmd="${cmd:0:100}"
        hook_log_error "BLOCKED dangerous command: $pattern — cmd: $short_cmd"
        entry="### $(date '+%Y-%m-%d %H:%M') — Hook: safety-gate.sh — Befehl blockiert: $pattern"
        insert_whiteboard_entry "Offene Fehler & Probleme" "$entry" || true
        printf '{"error":"BLOCKED: Dangerous command detected — %s"}\n' "$pattern"
        exit 2
    fi
done

# Case-insensitive block check (SQL keywords)
for pattern in "${PATTERNS_CI[@]}"; do
    if printf '%s' "$cmd" | grep -Eiq "$pattern"; then
        short_cmd="${cmd:0:100}"
        hook_log_error "BLOCKED dangerous command (ci): $pattern — cmd: $short_cmd"
        entry="### $(date '+%Y-%m-%d %H:%M') — Hook: safety-gate.sh — Befehl blockiert (ci): $pattern"
        insert_whiteboard_entry "Offene Fehler & Probleme" "$entry" || true
        printf '{"error":"BLOCKED: Dangerous command detected — %s"}\n' "$pattern"
        exit 2
    fi
done

# (#49) Shell update warning — allow but warn user
for pattern in "${SHELL_UPDATE_PATTERNS[@]}"; do
    if printf '%s' "$cmd" | grep -Eq "$pattern"; then
        hook_log_warn "Shell-update detected mid-session: $pattern"
        printf '%s\n' "WARNING (safety-gate): Shell/Tool-Update erkannt. Laut CLAUDE.md-Regel muessen Shell-Updates NACH Abschluss aller Aufgaben erfolgen — sie koennen alle offenen Terminals killen. Bitte erst alle Aufgaben beenden und committen, dann updaten."
        exit 0  # allow, but user has been warned
    fi
done

# (#43) settings.json write-via-Bash warning — allow but warn
if printf '%s' "$cmd" | grep -Eq '>[[:space:]]*.*settings\.json|echo.*>.*settings\.json|cat.*>.*settings\.json'; then
    hook_log_warn "Bash command writing to settings.json detected — config-guard will verify afterwards"
    printf '%s\n' "WARNING (safety-gate): Schreibzugriff auf settings.json per Bash erkannt. config-guard.sh prueft die Datei nach der Aenderung auf geschuetzte Settings (effortLevel, defaultMode, AUTOCOMPACT, SUBAGENT_MODEL)."
    exit 0  # allow — config-guard PostToolUse will catch violations
fi

hook_log "command passed all checks"
exit 0
