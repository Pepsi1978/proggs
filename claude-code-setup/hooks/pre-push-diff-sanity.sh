#!/usr/bin/env bash
# pre-push-diff-sanity.sh: Vor jedem git push die zu pushenden Commits analysieren
# und auffaellige Diffs (viele Loeschungen, viele Dateien) als Warnung anzeigen.
# Verhindert dass Rebase-Artefakte oder Loeschungen aus parallelen Sessions
# unbemerkt gepusht werden.
#
# Event: PreToolUse (Bash matcher)
# Schwellen: >7 Dateien geaendert, >3 Datei-Loeschungen, oder >500 Zeilen geloescht.
# Verhalten: NICHT blockierend (exit 0) — nur Warnung als additionalContext.
#
# Direktive #3 Resilient Bugfixing (Poka-Yoke Stufe 1 — Warnung).
# Verwandt: feedback_no_panic_restore_in_parallel_sessions, parallel-sessions-git.md
#
# Platform: macOS/Linux

set +e   # NICHT set -e — wir wollen NIEMALS abbrechen, sondern immer exit 0

# --- Input lesen ---
hook_input=$(cat)
[ -z "$hook_input" ] && exit 0

# --- Schneller Vorab-Check ---
echo "$hook_input" | grep -qE '\bgit[[:space:]]+push\b' || exit 0

# --- JSON parsen (cwd + command extrahieren) ---
# python3 statt jq — jq ist auf manchen Macs nicht installiert, python3 immer
parse_json() {
    python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    cmd = d.get('tool_input', {}).get('command', '') or ''
    cwd = d.get('tool_input', {}).get('cwd', '') or ''
    print(f'{cmd}|||CWD|||{cwd}')
except Exception:
    print('')
" 2>/dev/null
}

parsed=$(echo "$hook_input" | parse_json)
[ -z "$parsed" ] && exit 0

cmd="${parsed%|||CWD|||*}"
cwd="${parsed##*|||CWD|||}"

# Final-Check auf dem geparsten command
echo "$cmd" | grep -qE '\bgit[[:space:]]+push\b' || exit 0

# --- Hook-Log laden (nach skip-checks) ---
SCRIPT_DIR="$(cd "$(dirname "$0")" 2>/dev/null && pwd)" || SCRIPT_DIR=""
if [ -n "$SCRIPT_DIR" ] && [ -f "$SCRIPT_DIR/hook-log.sh" ]; then
    # shellcheck source=/dev/null
    . "$SCRIPT_DIR/hook-log.sh" 2>/dev/null || true
fi
hook_log_safe() {
    if command -v hook_log >/dev/null 2>&1; then hook_log "$@"; fi
}
hook_log_warn_safe() {
    if command -v hook_log_warn >/dev/null 2>&1; then hook_log_warn "$@"; fi
}

# --- cwd ermitteln (Fallback: PWD) ---
[ -z "$cwd" ] && cwd="$PWD"

# Git-Bash-Pfade in Unix-Pfade — auf macOS/Linux meist nicht noetig, aber sicher
# (auf macOS keine Konvertierung noetig — bleibt wie ist)
[ ! -d "$cwd" ] && exit 0

# --- Upstream-Branch ermitteln ---
upstream=$(git -C "$cwd" rev-parse --abbrev-ref --symbolic-full-name '@{upstream}' 2>/dev/null)
if [ -z "$upstream" ]; then
    # Kein Upstream gesetzt -> First push, kein Diff verfuegbar
    exit 0
fi

# --- Zu pushende Commits ---
commits_raw=$(git -C "$cwd" log "${upstream}..HEAD" --format='%H||%s' 2>/dev/null)
[ -z "$commits_raw" ] && exit 0

# --- Schwellen ---
THRESHOLD_FILES=7
THRESHOLD_DELETED_FILES=3
THRESHOLD_LINES_DELETED=500

# --- Pro Commit analysieren ---
warnings=""
summary=""
warning_count=0
commit_count=0

while IFS= read -r line; do
    [ -z "$line" ] && continue
    [[ "$line" != *"||"* ]] && continue

    commit_count=$((commit_count + 1))
    hash="${line%%||*}"
    msg="${line#*||}"
    [ -z "$hash" ] && continue

    # Diff-Stat (Summary-Zeile am Ende)
    stat_out=$(git -C "$cwd" diff --stat "${hash}^..${hash}" 2>/dev/null) || continue
    [ -z "$stat_out" ] && continue

    stat_last=$(echo "$stat_out" | tail -n 1)
    [ -z "$stat_last" ] && continue

    # "N files changed" extrahieren
    files_changed=$(echo "$stat_last" | grep -oE '[0-9]+ files? changed' | grep -oE '[0-9]+' | head -n 1)
    [ -z "$files_changed" ] && continue

    deletions=$(echo "$stat_last" | grep -oE '[0-9]+ deletions?\(-\)' | grep -oE '[0-9]+' | head -n 1)
    [ -z "$deletions" ] && deletions=0
    insertions=$(echo "$stat_last" | grep -oE '[0-9]+ insertions?\(\+\)' | grep -oE '[0-9]+' | head -n 1)
    [ -z "$insertions" ] && insertions=0

    # Anzahl komplett geloeschter Dateien (D-Filter)
    deleted_files=$(git -C "$cwd" diff --diff-filter=D --name-only "${hash}^..${hash}" 2>/dev/null | grep -v '^$' || true)
    deleted_count=0
    if [ -n "$deleted_files" ]; then
        deleted_count=$(echo "$deleted_files" | wc -l | tr -d ' ')
    fi

    short_hash="${hash:0:7}"
    summary="${summary}  ${short_hash} : ${files_changed} files, +${insertions}/-${deletions} lines, ${deleted_count} deletions  (${msg})"$'\n'

    # Auffaellig?
    reasons=""
    if [ "$files_changed" -gt "$THRESHOLD_FILES" ]; then
        reasons="${files_changed} Dateien geaendert (Schwelle: ${THRESHOLD_FILES})"
    fi
    if [ "$deleted_count" -gt "$THRESHOLD_DELETED_FILES" ]; then
        if [ -n "$reasons" ]; then reasons="${reasons}, "; fi
        reasons="${reasons}${deleted_count} Datei-Loeschungen (Schwelle: ${THRESHOLD_DELETED_FILES})"
    fi
    if [ "$deletions" -gt "$THRESHOLD_LINES_DELETED" ]; then
        if [ -n "$reasons" ]; then reasons="${reasons}, "; fi
        reasons="${reasons}${deletions} Zeilen geloescht (Schwelle: ${THRESHOLD_LINES_DELETED})"
    fi

    if [ -n "$reasons" ]; then
        warning_count=$((warning_count + 1))
        block="  Commit ${short_hash} : ${msg}"$'\n'"    Grund: ${reasons}"
        if [ "$deleted_count" -gt 0 ]; then
            # Top 10 geloeschte Dateien anzeigen
            top10=$(echo "$deleted_files" | head -n 10 | sed 's/^/      - /')
            block="${block}"$'\n'"    Geloeschte Dateien:"$'\n'"${top10}"
            if [ "$deleted_count" -gt 10 ]; then
                rest=$((deleted_count - 10))
                block="${block}"$'\n'"      (+ ${rest} weitere)"
            fi
        fi
        if [ -n "$warnings" ]; then warnings="${warnings}"$'\n\n'; fi
        warnings="${warnings}${block}"
    fi
done <<< "$commits_raw"

# --- Ausgabe ---
if [ "$warning_count" -gt 0 ]; then
    msg="[pre-push-diff-sanity] AUFFAELLIGE COMMITS in diesem git push:

${warnings}

Push-Uebersicht (alle ${commit_count} Commit(s)):
${summary}
Wenn diese Loeschungen unbeabsichtigt sind oder aus einer parallelen Session
stammen, JETZT pruefen BEVOR der push durchlaeuft:
  git log -1 --stat        # genaue Aenderungen sehen
  git reset --soft HEAD~1  # letzten Commit zurueckziehen (NICHT bei bereits gepushten)

Memory-Hinweis: feedback_no_panic_restore_in_parallel_sessions.md beschreibt
das typische Muster wenn Loeschungen aus paralleler Session in den eigenen
Commit-Diff wandern."

    echo "$msg"
    echo "$msg" >&2
    hook_log_safe "pre-push-diff-sanity: ${warning_count} auffaellige Commit(s) (von ${commit_count})"
else
    hook_log_safe "pre-push-diff-sanity: ${commit_count} Commit(s) ok"
fi

# PFLICHT: exit 0 — Hook darf push NICHT blockieren
exit 0
