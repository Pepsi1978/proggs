#!/usr/bin/env bash
# git-multi-session-lock.sh: Lock-Mechanismus fuer parallele Gemini-Sessions am gleichen Git-Repo
# Event: PreToolUse (Bash matcher)
# Zweck: Wenn zwei Sessions gleichzeitig git add/commit/push/reset machen wollen,
#         wartet die zweite Session bis die erste fertig ist. Verhindert die
#         klassische Race-Condition: Session A committet fremde Dateien von B,
#         merkt es, restored, killt damit Bs zwischenzeitlich committeten Stand.
#
# Atomarer Lock (Direktive #3, Resilient Bugfixing, Loop 3 + Perf-Loop 3):
#   1. Pruefen ob Befehl eine git-Schreibebene ist (add/commit/push/reset/...)
#   2. Git-Common-Dir via 'git -C $cwd rev-parse --git-common-dir' (worktree-safe)
#   3. Lock-Datei: <git-common-dir>/Gemini-multi-session.lock
#   4. Atomares Create-Or-Fail via 'set -o noclobber' (POSIX O_EXCL)
#   5. Korrupte/leere Lock-Files werden bei Read-Failure SOFORT geloescht
#   6. Stale-Lock-Takeover bei TTL ODER wenn lockender Prozess tot (kill -0)
#
# Performance-Optimierungen (Perf-Loop 1+2+3):
#   - parse_input_all: 1x python3 statt 3x parse_field (spart ~100-150ms)
#   - parse_lock_all: 1x python3 statt 4x (spart ~150-200ms)
#   - cmd_short via bash ${var:0:200} statt python3 (spart ~50ms)
#   - JSON-build via bash printf statt python3 (spart ~50ms in write-path)
#   - $(< file) bash builtin statt cat (spart ~1ms x N reads)
#   - bash builtin regex [[ =~ ]] statt grep -qE (spart ~5ms je skip)
#   - hook-log lazy-loaded NACH skip-checks (spart ~2-3ms je skip)
#   - git -C $cwd statt cd + git (spart 1 subshell-fork)
#
# Defense-in-Depth-Schichten:
#   1. Praevention: O_EXCL via 'set -o noclobber' — atomar fail-if-exists
#   2. Reaktiv: Cleanup bei Schreibfehler (rm), Cleanup bei Korruption (rm)
#   3. Selbstheilend: Stale-by-time UND stale-by-dead-pid
#   4. Worktree-aware: git-common-dir statt repo-root/.git
#
# Lock-Lifetime: 180 Sekunden TTL (langlebig genug fuer grosse Pushes).
# Platform: macOS/Linux/Git-Bash

# Bewusst kein 'set -e': mehrere Befehle duerfen optional fehlschlagen.
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Hilfs-Stubs die immer existieren (echte werden ggf. spaeter via dot-source ueberschrieben)
hook_log() { :; }
hook_log_warn() { :; }

# Perf-Loop 1: hook-log lazy-load NACH den skip-checks (95% der Aufrufe sind skip).
# Wir laden es erst nach Pattern-Match wenn wir wirklich im write-path sind.

# Debug-Loop 1: zurueck zu cat — bash 3.2 (macOS-Default) interpretiert
# `read -d ''` anders als bash 4+ (liest nur bis newline statt bis NUL).
# Bei multi-line JSON katastrophal. cat ist universal kompatibel.
# Kosten: ~5-10ms Subprocess-Overhead je Aufruf (akzeptabel fuer Safety).
input=$(cat 2>/dev/null || true)
if [ -z "$input" ]; then exit 0; fi

# Perf-Loop 2: Schneller grep-skip VOR python3-Parse. 95% der Aufrufe sind
# non-git-Bash-Befehle — bei denen sparen wir den python3-Start (~45ms je skip).
# Pattern entspricht 1:1 dem git_write_pattern unten — wenn ein git-write
# Befehl im input vorkommt, gehen wir in den vollen parse-pfad. Sonst skip.
git_write_pattern='\bgit[[:space:]]+(add|commit|push|fetch|rebase|pull|reset|restore|stash|am|cherry-pick|revert|merge|tag[[:space:]]+[^[:space:]]|branch[[:space:]]+(-[Dd]|--delete|--force|--move|-m[[:space:]]+[^[:space:]]|-M[[:space:]]+[^[:space:]])|worktree[[:space:]]+(add|remove|move|prune)|submodule[[:space:]]+(update|add|deinit))\b'
if ! [[ $input =~ $git_write_pattern ]]; then
    exit 0
fi

# Perf-Loop 1: ALLE Felder in EINEM python3-Aufruf parsen (statt 3 Aufrufen).
# Trenner: \x1f (Unit Separator, kommt in normalem Text nie vor).
# Format: tool_name<US>command<US>cwd
parsed=$(printf '%s' "$input" | python3 -c "
import sys, json
try:
    d = json.loads(sys.stdin.read())
    tool = d.get('tool_name', '') or ''
    ti = d.get('tool_input') or {}
    if not isinstance(ti, dict):
        ti = {}
    cmd = ti.get('command', '') or ''
    cwd = ti.get('cwd', '') or ''
    print(tool + '\x1f' + cmd + '\x1f' + cwd, end='')
except Exception:
    print('\x1f\x1f', end='')
" 2>/dev/null)

# Split via Bash-internen Mechanismus (read mit IFS)
IFS=$'\x1f' read -r tool_name cmd cwd <<<"$parsed"

if [ "$tool_name" != "Bash" ]; then exit 0; fi
if [ -z "$cmd" ]; then exit 0; fi

# Final-Check: bash builtin regex auf dem exakten command-Feld (statt nur input-blob).
# Verhindert false-positives wenn 'git commit' im Pfad oder Argument vorkommt
# aber nicht der echte command ist.
if ! [[ $cmd =~ $git_write_pattern ]]; then
    exit 0
fi

# === Ab hier: write-path. hook-log lazy laden. ===
# shellcheck disable=SC1091
. "$SCRIPT_DIR/hook-log.sh" 2>/dev/null || true
# Falls hook-log keine Funktionen exportiert, bleiben die Stubs aktiv.
type hook_log >/dev/null 2>&1 || hook_log() { :; }
type hook_log_warn >/dev/null 2>&1 || hook_log_warn() { :; }

# cwd fallback
[ -z "$cwd" ] && cwd="$PWD"
if [ ! -d "$cwd" ]; then exit 0; fi

# Perf-Loop 1: git -C $cwd statt cd + git (spart 1 subshell-fork)
git_common_dir=$(git -C "$cwd" rev-parse --git-common-dir 2>/dev/null) || true
if [ -z "$git_common_dir" ]; then
    hook_log "git-multi-session-lock: kein Git-Repo unter $cwd, skip" 2>/dev/null || true
    exit 0
fi

# Relative Pfade (z.B. ".git") absolut machen
case "$git_common_dir" in
    /*) ;;
    [A-Za-z]:[/\\]*) ;;  # Windows-absolute (z.B. C:/Users/...)
    *) git_common_dir="$cwd/$git_common_dir" ;;
esac

if [ ! -d "$git_common_dir" ]; then
    hook_log "git-multi-session-lock: git-common-dir nicht gefunden: $git_common_dir, skip" 2>/dev/null || true
    exit 0
fi

lockfile="$git_common_dir/Gemini-multi-session.lock"

# Session-ID — primaer aus gemini-setup-Env. Fallback: stabile Parent-PID.
my_session_id="${Gemini_CODE_SESSION_ID:-${Gemini_SESSION_ID:-ppid-$PPID}}"

max_wait=120
stale_threshold=180
global_waited=0
corrupt_retry_count=0
max_corrupt_retries=5

# Perf-Loop 1: cmd_short via bash builtin (UTF-8-byte-truncation, kein python3 fork).
# Hinweis: in UTF-8 kann ${var:0:200} mitten in einem Multibyte-Zeichen schneiden;
# das macht keinen syntaktischen Fehler weil der String nur im JSON-command-Feld
# eingebettet wird (Python json escaped bytes als \uXXXX wenn ungueltig).
cmd_short="${cmd:0:200}"

# JSON-Escape Helfer fuer Bash-built JSON (Perf-Loop 1).
# Wir nutzen Python einmal pro write um den finalen JSON-String zu bauen — das ist
# unausweichlich weil Bash UTF-8/Sonderzeichen nicht sauber escaped. ABER: wir
# konsolidieren write_lock_atomic_create und write_lock_refresh um nur EINEN
# python-call zu machen statt einen pro Operation.

# Atomares Create-Or-Fail via O_EXCL (set -o noclobber).
# Perf-Loop 1: Cleanup bei Schreibfehler integriert.
write_lock_atomic_create() {
    local target="$1" session="$2" repo="$3" pid_val="$4" cmd_val="$5"

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

    # Atomares Create-Or-Fail via noclobber in Subshell.
    if (set -o noclobber; printf '%s' "$json_content" > "$target") 2>/dev/null; then
        return 0
    else
        return 1
    fi
}

# Self-Refresh: temp + atomic rename (mv ist atomar via rename(2) auf POSIX).
# Debug-Loop 2 Fix: direct redirect `> target` truncate-on-open ist NICHT atomar
# aus reader-Sicht — andere Sessions koennen partial content sehen, als korrupt
# klassifizieren und unseren Lock LOESCHEN. mv (rename(2)) macht den target erst
# nach komplettem temp-write sichtbar.
# Direktive 3 Schicht 1 (Praevention) MUSS bleiben.
write_lock_refresh() {
    local target="$1" session="$2" repo="$3" pid_val="$4" cmd_val="$5"
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
    if [ ! -s "$tempfile" ]; then
        rm -f "$tempfile" 2>/dev/null
        return 1
    fi
    mv -f "$tempfile" "$target" 2>/dev/null || {
        rm -f "$tempfile" 2>/dev/null
        return 1
    }
    return 0
}

# Liveness-Check: kill -0 sendet kein Signal, prueft nur Existenz + Permission.
is_process_alive() {
    local pid_val="$1"
    if [ -z "$pid_val" ] || [ "$pid_val" = "0" ] || [ "$pid_val" = "?" ]; then
        return 0  # unbekannt = vorsichtig "alive" annehmen
    fi
    if ! [[ $pid_val =~ ^[0-9]+$ ]]; then
        return 0
    fi
    kill -0 "$pid_val" 2>/dev/null
}

# Perf-Loop 1: parse_lock_all — 1 python3-Call statt 4 separater Calls fuer
# session/acquired/pid/age. Trenner: \x1f. Format: session<US>acquired<US>pid<US>age
parse_lock_all() {
    local lock_raw="$1"
    if [ -z "$lock_raw" ]; then
        printf '\x1f\x1f\x1f999'
        return
    fi
    printf '%s' "$lock_raw" | python3 -c "
import sys, json
from datetime import datetime, timezone
try:
    raw = sys.stdin.read()
    if not raw:
        print('\x1f\x1f\x1f999', end='')
        sys.exit(0)
    d = json.loads(raw)
    session = d.get('sessionId', '') or ''
    acquired = d.get('acquired', '') or ''
    pid_val = str(d.get('pid', '?'))
    age = 999
    if acquired:
        try:
            a = datetime.fromisoformat(acquired.strip().replace('Z', '+00:00'))
            age = int((datetime.now(timezone.utc) - a).total_seconds())
        except Exception:
            age = 999
    print(session + '\x1f' + acquired + '\x1f' + pid_val + '\x1f' + str(age), end='')
except Exception:
    print('\x1f\x1f\x1f999', end='')
" 2>/dev/null
}

# --- Atomare Lock-Aquise ---
while [ $global_waited -lt $max_wait ]; do
    # Versuch 1: atomar Create-Or-Fail
    if write_lock_atomic_create "$lockfile" "$my_session_id" "$git_common_dir" "$$" "$cmd_short"; then
        hook_log "git-multi-session-lock: Lock atomar erworben fuer cmd '$cmd_short'" 2>/dev/null || true
        exit 0
    fi

    # Versuch 2: Datei existiert. Lies sie und entscheide.
    # Perf-Loop 2: $(< file) bash builtin statt cat (kein fork)
    lock_data=""
    if [ -f "$lockfile" ]; then
        lock_data=$(< "$lockfile") 2>/dev/null || lock_data=""
    fi

    # Korrupte/leere Datei SOFORT loeschen.
    if [ -z "$lock_data" ]; then
        corrupt_retry_count=$((corrupt_retry_count + 1))
        if [ $corrupt_retry_count -gt $max_corrupt_retries ]; then
            hook_log_warn "git-multi-session-lock: $max_corrupt_retries Korrupt-Retries — gebe auf" 2>/dev/null || true
            exit 0
        fi
        hook_log_warn "git-multi-session-lock: Leerer Lock — loesche (Retry $corrupt_retry_count/$max_corrupt_retries)" 2>/dev/null || true
        rm -f "$lockfile" 2>/dev/null || true
        sleep 0.1
        continue
    fi

    # Perf-Loop 1: ALLE 4 Felder in EINEM python3-Call parsen.
    parsed=$(parse_lock_all "$lock_data")
    IFS=$'\x1f' read -r lock_session lock_acquired lock_pid age <<<"$parsed"

    # JSON kaputt erkennen: leere session = unparsable
    if [ -z "$lock_session" ]; then
        corrupt_retry_count=$((corrupt_retry_count + 1))
        if [ $corrupt_retry_count -gt $max_corrupt_retries ]; then
            hook_log_warn "git-multi-session-lock: $max_corrupt_retries Korrupt-Retries (JSON) — gebe auf" 2>/dev/null || true
            exit 0
        fi
        hook_log_warn "git-multi-session-lock: Kaputtes JSON — loesche (Retry $corrupt_retry_count/$max_corrupt_retries)" 2>/dev/null || true
        rm -f "$lockfile" 2>/dev/null || true
        sleep 0.1
        continue
    fi

    # Defensiv: age muss numerisch sein
    if ! [[ $age =~ ^-?[0-9]+$ ]]; then
        age=999
    fi

    # PID-Liveness-Check ergaenzend zum Zeit-Check
    # (nur fuer FREMDE Sessions — die eigene Session ueberlebt sich selbst)
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
        corrupt_retry_count=0
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
        echo "git-multi-session-lock: Andere Gemini-Session blockiert git gerade ($info). Warte..." >&2
        hook_log "git-multi-session-lock: Warte auf $info" 2>/dev/null || true
    elif [ $((global_waited % 10)) -eq 0 ]; then
        echo "git-multi-session-lock: Warte weiter (${global_waited}s)..." >&2
    fi

    sleep 2
    global_waited=$((global_waited + 2))
    corrupt_retry_count=0
done

# Timeout erreicht — fail-open (besser ohne Lock als haengende Session)
hook_log_warn "git-multi-session-lock: Timeout nach ${global_waited}s, fahre ohne Lock fort" 2>/dev/null || true
echo "WARNUNG: Multi-Session-Lock-Timeout nach ${global_waited}s. Andere Session haengt vermutlich." >&2

exit 0

