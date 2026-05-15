#!/usr/bin/env bash
# git-multi-session-lock.sh: Lock-Mechanismus fuer parallele Claude-Sessions am gleichen Git-Repo
# Hinweis: Im Gegensatz zur PS1-Version ist auf macOS/Linux KEINE Pfad-Konvertierung
# noetig, da das cwd-Feld bereits Unix-Pfade enthaelt (/Users/... oder /home/...).
# Event: PreToolUse (Bash matcher)
# Zweck: Wenn zwei Sessions gleichzeitig git add/commit/push/reset machen wollen,
#         wartet die zweite Session bis die erste fertig ist. Verhindert die
#         klassische Race-Condition: Session A committet fremde Dateien von B,
#         merkt es, restored, killt damit Bs zwischenzeitlich committeten Stand.
#
# Logik:
#   1. Pruefen ob Befehl eine git-Schreibebene ist (add/commit/push/reset/...)
#   2. Repo-Root via 'git rev-parse --show-toplevel' ermitteln
#   3. Lock-Datei: <repo>/.git/claude-multi-session.lock
#   4. Falls Lock von anderer Session: warten (max 120s, stale-takeover nach 90s)
#   5. Falls Lock von uns selbst: weiter (gleiche Sequenz)
#   6. Lock mit eigener SessionID schreiben, exit 0
#
# Lock-Lifetime: 90 Sekunden TTL.
# Platform: macOS/Linux
# stdout -> AI context, stderr -> user terminal

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck disable=SC1091
. "$SCRIPT_DIR/hook-log.sh" 2>/dev/null || true

trap 'hook_log_warn "git-multi-session-lock: Error at line $LINENO" 2>/dev/null || true' ERR

# Stdin einlesen
input=$(cat 2>/dev/null || true)
if [ -z "$input" ]; then exit 0; fi

# JSON parsen ueber Python (jq nicht ueberall verfuegbar)
tool_name=$(printf '%s' "$input" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    print(d.get('tool_name', ''))
except Exception:
    print('')
" 2>/dev/null)

if [ "$tool_name" != "Bash" ]; then exit 0; fi

cmd=$(printf '%s' "$input" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    print(d.get('tool_input', {}).get('command', ''))
except Exception:
    print('')
" 2>/dev/null)

if [ -z "$cmd" ]; then exit 0; fi

# Git-Schreiboperation?
if ! printf '%s' "$cmd" | grep -qE '\bgit[[:space:]]+(add|commit|push|fetch|rebase|pull|reset|restore|stash|am|cherry-pick|revert|merge|tag|branch[[:space:]]+-[Dd])\b'; then
    exit 0
fi

# Working Directory aus Tool-Input oder PWD
cwd=$(printf '%s' "$input" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    print(d.get('tool_input', {}).get('cwd', ''))
except Exception:
    print('')
" 2>/dev/null)
[ -z "$cwd" ] && cwd="$PWD"

# Repo-Root ermitteln
if [ ! -d "$cwd" ]; then exit 0; fi
repo_root=$(cd "$cwd" 2>/dev/null && git rev-parse --show-toplevel 2>/dev/null) || true
if [ -z "$repo_root" ] || [ ! -d "$repo_root" ]; then
    hook_log "git-multi-session-lock: kein Git-Repo unter $cwd, skip" 2>/dev/null || true
    exit 0
fi

lockfile="$repo_root/.git/claude-multi-session.lock"
my_session_id="${CLAUDE_CODE_SESSION_ID:-$$-$(date +%Y%m%d%H%M%S)}"

max_wait=120
stale_threshold=90
waited=0
lock_owner_info=""

while [ -f "$lockfile" ]; do
    lock_data=$(cat "$lockfile" 2>/dev/null || true)
    if [ -z "$lock_data" ]; then
        break  # leere/korrupte Datei, ueberschreiben
    fi

    lock_session=$(printf '%s' "$lock_data" | python3 -c "
import sys, json
try:
    d = json.loads(sys.stdin.read())
    print(d.get('sessionId', ''))
except Exception:
    print('')
" 2>/dev/null)

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

    if [ -z "$lock_session" ]; then break; fi

    # Stale-Check
    age=0
    if [ -n "$lock_acquired" ]; then
        age=$(python3 -c "
from datetime import datetime, timezone
try:
    a = datetime.fromisoformat('$lock_acquired'.replace('Z', '+00:00'))
    n = datetime.now(timezone.utc)
    print(int((n - a).total_seconds()))
except Exception:
    print(999)
" 2>/dev/null)
    fi

    if [ "$age" -gt "$stale_threshold" ]; then
        hook_log_warn "git-multi-session-lock: Stale Lock (${age}s alt) von $lock_session wird ueberschrieben" 2>/dev/null || true
        break
    fi

    # Eigener Lock?
    if [ "$lock_session" = "$my_session_id" ]; then break; fi

    # Anderer Lock: warten
    if [ "$waited" -eq 0 ]; then
        lock_owner_info="Session $lock_session (PID $lock_pid, seit ${age}s)"
        echo "git-multi-session-lock: Andere Claude-Session blockiert git gerade ($lock_owner_info). Warte..." >&2
        hook_log "git-multi-session-lock: Warte auf $lock_owner_info" 2>/dev/null || true
    fi

    sleep 2
    waited=$((waited + 2))

    if [ "$waited" -ge "$max_wait" ]; then
        hook_log_warn "git-multi-session-lock: Timeout nach ${max_wait}s, Lock von $lock_session ignoriert" 2>/dev/null || true
        echo "WARNUNG: Multi-Session-Lock-Timeout nach ${max_wait}s. Andere Session haengt vermutlich. Fahre fort." >&2
        break
    fi

    if [ $((waited % 10)) -eq 0 ]; then
        echo "git-multi-session-lock: Warte weiter auf andere Session (${waited}s)..." >&2
    fi
done

# Lock schreiben (oder erneuern)
cmd_short=$(printf '%s' "$cmd" | head -c 200)
now=$(python3 -c "from datetime import datetime, timezone; print(datetime.now(timezone.utc).isoformat())" 2>/dev/null || date -u +"%Y-%m-%dT%H:%M:%S.000+00:00")

python3 -c "
import json
data = {
    'sessionId': '$my_session_id',
    'acquired': '$now',
    'pid': $$,
    'command': '''$cmd_short''',
    'repo': '$repo_root'
}
print(json.dumps(data))
" > "$lockfile" 2>/dev/null || {
    hook_log_warn "git-multi-session-lock: Konnte Lock nicht schreiben" 2>/dev/null || true
}

hook_log "git-multi-session-lock: Lock gesetzt fuer cmd '$cmd_short'" 2>/dev/null || true

exit 0
