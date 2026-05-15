#!/usr/bin/env bash
# git-multi-session-lock.sh: Lock-Mechanismus fuer parallele Claude-Sessions am gleichen Git-Repo
# Event: PreToolUse (Bash matcher)
# Zweck: Wenn zwei Sessions gleichzeitig git add/commit/push/reset machen wollen,
#         wartet die zweite Session bis die erste fertig ist. Verhindert die
#         klassische Race-Condition: Session A committet fremde Dateien von B,
#         merkt es, restored, killt damit Bs zwischenzeitlich committeten Stand.
#
# Atomarer Lock (Direktive #3, Resilient Bugfixing, Loop 3):
#   1. Pruefen ob Befehl eine git-Schreibebene ist (add/commit/push/reset/...)
#   2. Git-Common-Dir via 'git rev-parse --git-common-dir' (worktree-safe)
#   3. Lock-Datei: <git-common-dir>/claude-multi-session.lock
#   4. Atomares Create-Or-Fail via 'set -o noclobber' (POSIX O_EXCL)
#   5. Korrupte/leere Lock-Files werden bei Read-Failure SOFORT geloescht
#   6. Stale-Lock-Takeover bei TTL ODER wenn lockender Prozess tot (kill -0)
#
# Defense-in-Depth-Schichten:
#   1. Praevention: O_EXCL via 'set -o noclobber' — atomar fail-if-exists
#   2. Reaktiv: Cleanup bei Schreibfehler (rm), Cleanup bei Korruption (rm)
#   3. Selbstheilend: Stale-by-time UND stale-by-dead-pid
#   4. Worktree-aware: git-common-dir statt repo-root/.git
#
# Lock-Lifetime: 180 Sekunden TTL (langlebig genug fuer grosse Pushes).
# Hinweis: cwd auf macOS/Linux ist bereits Unix-Style, keine Konvertierung noetig.
# Platform: macOS/Linux

# Bewusst kein 'set -e': mehrere Befehle duerfen optional fehlschlagen.
# Stattdessen explizite '|| true' Behandlung pro kritischer Stelle.
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck disable=SC1091
. "$SCRIPT_DIR/hook-log.sh" 2>/dev/null || true

# Hilfsfunktionen die immer existieren (auch wenn hook-log.sh fehlt)
type hook_log >/dev/null 2>&1 || hook_log() { :; }
type hook_log_warn >/dev/null 2>&1 || hook_log_warn() { :; }

# Stdin einlesen
input=$(cat 2>/dev/null || true)
if [ -z "$input" ]; then exit 0; fi

# JSON parsen ueber Python (jq nicht ueberall verfuegbar).
# Loop 1 Fix: field-name via stdin uebergeben (kein Shell-Injection-Risiko).
parse_field() {
    local field="$1"
    printf '%s\n%s' "$field" "$input" | python3 -c "
import sys, json
try:
    lines = sys.stdin.read().split('\n', 1)
    if len(lines) < 2:
        print('')
        sys.exit(0)
    field, raw = lines[0], lines[1]
    d = json.loads(raw)
    val = d
    for p in field.split('.'):
        if isinstance(val, dict):
            val = val.get(p, '')
        else:
            val = ''
            break
    print(val if val is not None else '')
except Exception:
    print('')
" 2>/dev/null
}

tool_name=$(parse_field "tool_name")
if [ "$tool_name" != "Bash" ]; then exit 0; fi

cmd=$(parse_field "tool_input.command")
if [ -z "$cmd" ]; then exit 0; fi

# Git-Schreiboperation? Loop 1: branch --delete/--force/--move + worktree + submodule erweitert
if ! printf '%s' "$cmd" | grep -qE '\bgit[[:space:]]+(add|commit|push|fetch|rebase|pull|reset|restore|stash|am|cherry-pick|revert|merge|tag[[:space:]]+[^[:space:]]|branch[[:space:]]+(-[Dd]|--delete|--force|--move|-m[[:space:]]+[^[:space:]]|-M[[:space:]]+[^[:space:]])|worktree[[:space:]]+(add|remove|move|prune)|submodule[[:space:]]+(update|add|deinit))\b'; then
    exit 0
fi

# Working Directory aus Tool-Input oder PWD
cwd=$(parse_field "tool_input.cwd")
[ -z "$cwd" ] && cwd="$PWD"

# Git-Common-Dir ermitteln (worktree-aware — Lock teilt sich zwischen allen Worktrees)
if [ ! -d "$cwd" ]; then exit 0; fi
git_common_dir=$(cd "$cwd" 2>/dev/null && git rev-parse --git-common-dir 2>/dev/null) || true
if [ -z "$git_common_dir" ]; then
    hook_log "git-multi-session-lock: kein Git-Repo unter $cwd, skip" 2>/dev/null || true
    exit 0
fi

# Relative Pfade (z.B. ".git") absolut machen
case "$git_common_dir" in
    /*) ;;  # absolut, OK
    *) git_common_dir="$cwd/$git_common_dir" ;;
esac

if [ ! -d "$git_common_dir" ]; then
    hook_log "git-multi-session-lock: git-common-dir nicht gefunden: $git_common_dir, skip" 2>/dev/null || true
    exit 0
fi

lockfile="$git_common_dir/claude-multi-session.lock"

# Session-ID — primaer aus Claude-Code-Env. Fallback: stabile Parent-PID
# (Hook-PID $$ variiert per Aufruf, PPID = Claude-Code-Prozess = stabil pro Session)
my_session_id="${CLAUDE_CODE_SESSION_ID:-${CLAUDE_SESSION_ID:-ppid-$PPID}}"

max_wait=120
stale_threshold=180  # Loop 3: erhoeht von 90 auf 180 — grosse Pushes koennen >90s dauern
global_waited=0
corrupt_retry_count=0
max_corrupt_retries=5  # Loop 1: harte Grenze gegen Endlos-Loop bei korruptem File

# UTF-8-Safe truncate (Python-basiert, keine Multibyte-Probleme)
cmd_short=$(printf '%s' "$cmd" | python3 -c "
import sys
s = sys.stdin.read()
if len(s) > 200:
    s = s[:200]
print(s, end='')
" 2>/dev/null || printf '%.200s' "$cmd")

# Atomares Create-Or-Fail via O_EXCL (set -o noclobber)
# Loop 1 Fix: mv -f clobberte fremde Locks — jetzt echtes atomic create-only-if-not-exists.
# Cleanup bei Schreibfehler verhindert dass leere Dateien zurueckbleiben (Korrupt-Loop).
write_lock_atomic_create() {
    local target="$1"
    local session="$2"
    local repo="$3"
    local pid_val="$4"
    local cmd_val="$5"

    # JSON bauen via Python (Apostroph/Sonderzeichen-safe)
    local json_content
    json_content=$(printf '%s\t%s\t%s\t%s' "$session" "$repo" "$pid_val" "$cmd_val" | python3 -c "
import sys, json
from datetime import datetime, timezone
try:
    raw = sys.stdin.read()
    parts = raw.split('\t', 3)
    if len(parts) < 4:
        sys.exit(1)
    data = {
        'sessionId': parts[0],
        'repo': parts[1],
        'pid': int(parts[2]) if parts[2].isdigit() else 0,
        'command': parts[3],
        'acquired': datetime.now(timezone.utc).isoformat()
    }
    print(json.dumps(data), end='')
except Exception:
    sys.exit(1)
" 2>/dev/null) || return 1

    if [ -z "$json_content" ]; then return 1; fi

    # Atomares Create-Or-Fail via noclobber.
    # In Subshell damit set -o noclobber nicht persistiert.
    # Wenn Schreiben mitten drin scheitert: Datei loeschen (Cleanup gegen leere Locks).
    if (set -o noclobber; printf '%s' "$json_content" > "$target") 2>/dev/null; then
        return 0
    else
        # Entweder Datei existierte schon (normaler Fall, naechster Loop liest sie),
        # ODER es war ein I/O-Fehler. In beiden Faellen: wenn wir versehentlich eine
        # leere Datei hinterlassen haben (sehr unwahrscheinlich bei noclobber-Fail),
        # raeumen wir NICHT auf — wir wissen nicht ob sie uns gehoert.
        return 1
    fi
}

# Self-Refresh: temp + atomic rename (mv ist atomar auf POSIX)
# Loop 1 Fix: bei Schreibfehler in temp-File aufraeumen (Cleanup gegen Muell-tempfiles).
write_lock_refresh() {
    local target="$1"
    local session="$2"
    local repo="$3"
    local pid_val="$4"
    local cmd_val="$5"
    local tempfile
    tempfile="${target}.tmp.$$.$RANDOM"

    if ! printf '%s\t%s\t%s\t%s' "$session" "$repo" "$pid_val" "$cmd_val" | python3 -c "
import sys, json
from datetime import datetime, timezone
try:
    raw = sys.stdin.read()
    parts = raw.split('\t', 3)
    if len(parts) < 4:
        sys.exit(1)
    data = {
        'sessionId': parts[0],
        'repo': parts[1],
        'pid': int(parts[2]) if parts[2].isdigit() else 0,
        'command': parts[3],
        'acquired': datetime.now(timezone.utc).isoformat()
    }
    print(json.dumps(data), end='')
except Exception:
    sys.exit(1)
" > "$tempfile" 2>/dev/null; then
        rm -f "$tempfile" 2>/dev/null
        return 1
    fi
    # Verify tempfile nicht leer (Cleanup-Schutz)
    if [ ! -s "$tempfile" ]; then
        rm -f "$tempfile" 2>/dev/null
        return 1
    fi
    # Atomarer Rename (mv ist atomar via rename(2) auf POSIX)
    mv -f "$tempfile" "$target" 2>/dev/null || {
        rm -f "$tempfile" 2>/dev/null
        return 1
    }
    return 0
}

# Loop 3: Liveness-Check fuer PID
# kill -0 sendet kein Signal sondern prueft nur ob Prozess existiert + wir Rechte haben.
# Exit 0 = lebt, Exit != 0 = tot oder fremd.
is_process_alive() {
    local pid_val="$1"
    if [ -z "$pid_val" ] || [ "$pid_val" = "0" ] || [ "$pid_val" = "?" ]; then
        return 0  # unbekannt = vorsichtig "alive" annehmen
    fi
    if ! printf '%s' "$pid_val" | grep -qE '^[0-9]+$'; then
        return 0
    fi
    kill -0 "$pid_val" 2>/dev/null
}

# --- Atomare Lock-Aquise ---
while [ $global_waited -lt $max_wait ]; do
    # Versuch 1: atomar Create-Or-Fail
    if write_lock_atomic_create "$lockfile" "$my_session_id" "$git_common_dir" "$$" "$cmd_short"; then
        hook_log "git-multi-session-lock: Lock atomar erworben fuer cmd '$cmd_short'" 2>/dev/null || true
        exit 0
    fi

    # Versuch 2: Datei existiert. Lies sie und entscheide.
    lock_data=$(cat "$lockfile" 2>/dev/null || true)

    # Loop 1 Fix: Korrupte/leere Datei SOFORT loeschen, nicht in Loop laufen lassen
    if [ -z "$lock_data" ]; then
        corrupt_retry_count=$((corrupt_retry_count + 1))
        if [ $corrupt_retry_count -gt $max_corrupt_retries ]; then
            hook_log_warn "git-multi-session-lock: $max_corrupt_retries Korrupt-Retries — gebe auf, fahre ohne Lock fort" 2>/dev/null || true
            exit 0
        fi
        hook_log_warn "git-multi-session-lock: Leerer Lock — loesche (Retry $corrupt_retry_count/$max_corrupt_retries)" 2>/dev/null || true
        rm -f "$lockfile" 2>/dev/null || true
        sleep 0.1
        continue
    fi

    # JSON-Felder einzeln parsen
    lock_session=$(printf '%s' "$lock_data" | python3 -c "
import sys, json
try:
    d = json.loads(sys.stdin.read())
    print(d.get('sessionId', ''))
except Exception:
    print('')
" 2>/dev/null)

    if [ -z "$lock_session" ]; then
        # JSON kaputt — selbe Behandlung wie leere Datei
        corrupt_retry_count=$((corrupt_retry_count + 1))
        if [ $corrupt_retry_count -gt $max_corrupt_retries ]; then
            hook_log_warn "git-multi-session-lock: $max_corrupt_retries Korrupt-Retries (JSON) — gebe auf" 2>/dev/null || true
            exit 0
        fi
        hook_log_warn "git-multi-session-lock: Kaputtes JSON im Lock — loesche (Retry $corrupt_retry_count/$max_corrupt_retries)" 2>/dev/null || true
        rm -f "$lockfile" 2>/dev/null || true
        sleep 0.1
        continue
    fi

    lock_acquired=$(printf '%s' "$lock_data" | python3 -c "
import sys, json
try:
    d = json.loads(sys.stdin.read())
    print(d.get('acquired', ''))
except Exception:
    print('')
" 2>/dev/null)

    lock_pid=$(printf '%s' "$lock_data" | python3 -c "
import sys, json
try:
    d = json.loads(sys.stdin.read())
    print(d.get('pid', '?'))
except Exception:
    print('?')
" 2>/dev/null)

    # Stale-Check by time
    age=999
    if [ -n "$lock_acquired" ]; then
        age=$(printf '%s' "$lock_acquired" | python3 -c "
from datetime import datetime, timezone
import sys
try:
    raw = sys.stdin.read().strip().replace('Z', '+00:00')
    a = datetime.fromisoformat(raw)
    n = datetime.now(timezone.utc)
    print(int((n - a).total_seconds()))
except Exception:
    print(999)
" 2>/dev/null)
        # Defensiv: falls Python was Komisches drucke
        if ! printf '%s' "$age" | grep -qE '^-?[0-9]+$'; then
            age=999
        fi
    fi

    # Loop 3: PID-Liveness-Check ergaenzend zum Zeit-Check
    # (nur fuer FREMDE Sessions — die eigene Session ueberlebt sich selbst per Definition)
    pid_dead=0
    if [ "$lock_session" != "$my_session_id" ]; then
        if ! is_process_alive "$lock_pid"; then
            pid_dead=1
        fi
    fi

    is_stale=0
    stale_reason=""
    if [ "$age" -gt "$stale_threshold" ] 2>/dev/null; then
        is_stale=1
        stale_reason="${age}s alt"
    elif [ "$pid_dead" -eq 1 ]; then
        is_stale=1
        stale_reason="PID $lock_pid tot"
    fi

    if [ "$is_stale" -eq 1 ]; then
        hook_log_warn "git-multi-session-lock: Stale Lock ($stale_reason) von $lock_session - delete + retry" 2>/dev/null || true
        rm -f "$lockfile" 2>/dev/null || true
        corrupt_retry_count=0  # Reset bei legitimer Stale-Detection
        sleep 0.1
        continue
    fi

    # Eigener Lock: refresh
    if [ "$lock_session" = "$my_session_id" ]; then
        if write_lock_refresh "$lockfile" "$my_session_id" "$git_common_dir" "$$" "$cmd_short"; then
            hook_log "git-multi-session-lock: Lock refreshed fuer cmd '$cmd_short'" 2>/dev/null || true
        else
            hook_log_warn "git-multi-session-lock: Lock-Refresh fehlgeschlagen, fahre fort" 2>/dev/null || true
        fi
        exit 0
    fi

    # Fremder aktiver Lock: warten
    if [ "$global_waited" -eq 0 ]; then
        info="Session $lock_session (PID $lock_pid, seit ${age}s)"
        echo "git-multi-session-lock: Andere Claude-Session blockiert git gerade ($info). Warte..." >&2
        hook_log "git-multi-session-lock: Warte auf $info" 2>/dev/null || true
    elif [ $((global_waited % 10)) -eq 0 ]; then
        echo "git-multi-session-lock: Warte weiter (${global_waited}s)..." >&2
    fi

    sleep 2
    global_waited=$((global_waited + 2))
    corrupt_retry_count=0  # Reset wenn wir auf normalen fremden Lock warten
done

# Timeout erreicht — fail-open (besser ohne Lock als haengende Session)
hook_log_warn "git-multi-session-lock: Timeout nach ${global_waited}s, fahre ohne Lock fort" 2>/dev/null || true
echo "WARNUNG: Multi-Session-Lock-Timeout nach ${global_waited}s. Andere Session haengt vermutlich." >&2

exit 0
