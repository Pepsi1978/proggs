#!/usr/bin/env bash
# codex-git-multi-session-lock.sh
# Codex PreToolUse:Bash lock for shared Git write operations.
# Bash 3.2 compatible.

set +e
set -u

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

hook_log() { :; }
hook_log_warn() { :; }

git_write_pattern='\bgit[[:space:]]+(add|commit|push|fetch|rebase|pull|reset|restore|checkout|switch|rm|mv|clean|gc|maintenance|stash|am|cherry-pick|revert|merge|tag[[:space:]]+[^[:space:]]|branch[[:space:]]+(-[Dd]|--delete|--force|--move|-m[[:space:]]+[^[:space:]]|-M[[:space:]]+[^[:space:]])|remote[[:space:]]+(add|remove|rm|rename|set-url|prune|update)|worktree[[:space:]]+(add|remove|move|prune)|submodule[[:space:]]+(update|add|deinit))\b'

input="$(cat 2>/dev/null || true)"
cmd="${CODEX_GIT_LOCK_COMMAND:-}"
cwd="${CODEX_GIT_LOCK_CWD:-}"
session_id="${CODEX_GIT_LOCK_SESSION_ID:-}"
explicit_git_common_dir="${CODEX_GIT_LOCK_COMMON_DIR:-}"

normalize_windows_path_for_bash() {
    local value="$1"
    case "$value" in
        [A-Za-z]:\\*)
            local drive rest
            drive="$(printf '%s' "${value:0:1}" | tr '[:upper:]' '[:lower:]')"
            rest="${value:3}"
            rest="${rest//\\//}"
            printf '/%s/%s' "$drive" "$rest"
            ;;
        *)
            printf '%s' "$value"
            ;;
    esac
}

resolve_real_git() {
    if [ -n "${CODEX_GIT_LOCK_REAL_GIT:-}" ] && [ -x "${CODEX_GIT_LOCK_REAL_GIT:-}" ]; then
        printf '%s' "$CODEX_GIT_LOCK_REAL_GIT"
        return
    fi

    if command -v git.exe >/dev/null 2>&1; then
        command -v git.exe
        return
    fi

    local candidates candidate home_bin home_local_bin script_git
    candidates="$(which -a git 2>/dev/null || command -v git 2>/dev/null || true)"
    home_bin="${HOME:-}/bin/git"
    home_local_bin="${HOME:-}/.local/bin/git"
    script_git="$SCRIPT_DIR/git"

    for candidate in $candidates; do
        case "$candidate" in
            "$home_bin"|"$home_local_bin"|"$script_git") continue ;;
        esac
        if [ -x "$candidate" ]; then
            printf '%s' "$candidate"
            return
        fi
    done

    printf 'git'
}

if [ -n "$input" ]; then
    if ! [[ $input =~ $git_write_pattern ]]; then
        exit 0
    fi

    parsed="$(printf '%s' "$input" | python3 -c '
import json, sys
try:
    d = json.loads(sys.stdin.read())
    tool = d.get("tool_name", "") or ""
    ti = d.get("tool_input") or {}
    if not isinstance(ti, dict):
        ti = {}
    cmd = ti.get("command", "") or ""
    cwd = ti.get("cwd", "") or d.get("cwd", "") or ""
    sid = d.get("session_id", "") or ""
    print(tool + "\x1f" + cmd + "\x1f" + cwd + "\x1f" + sid, end="")
except Exception:
    print("\x1f\x1f\x1f", end="")
' 2>/dev/null)"

    old_ifs="$IFS"
    IFS="$(printf '\037')"
    read -r tool_name cmd cwd session_id <<< "$parsed"
    IFS="$old_ifs"

    if [ "$tool_name" != "Bash" ]; then
        exit 0
    fi
fi

if [ -z "$cmd" ]; then
    exit 0
fi

if ! [[ $cmd =~ $git_write_pattern ]]; then
    exit 0
fi

# shellcheck disable=SC1091
. "$SCRIPT_DIR/hook-log.sh" 2>/dev/null || true
type hook_log >/dev/null 2>&1 || hook_log() { :; }
type hook_log_warn >/dev/null 2>&1 || hook_log_warn() { :; }

[ -z "$cwd" ] && cwd="$PWD"
cwd="$(normalize_windows_path_for_bash "$cwd")"
[ -d "$cwd" ] || exit 0

real_git="$(resolve_real_git)"
git_common_dir="$explicit_git_common_dir"
if [ -z "$git_common_dir" ]; then
    git_common_dir="$("$real_git" -C "$cwd" rev-parse --git-common-dir 2>/dev/null || true)"
fi
if [ -z "$git_common_dir" ]; then
    hook_log "codex-git-multi-session-lock: no git repo at $cwd" 2>/dev/null || true
    exit 0
fi

git_common_dir="$(normalize_windows_path_for_bash "$git_common_dir")"

case "$git_common_dir" in
    /*) ;;
    [A-Za-z]:[/\\]*) ;;
    *) git_common_dir="$cwd/$git_common_dir" ;;
esac

[ -d "$git_common_dir" ] || exit 0
lockfile="$git_common_dir/claude-multi-session.lock"

if [ -z "$session_id" ]; then
    if [ -n "${CODEX_SESSION_ID:-}" ]; then
        session_id="$CODEX_SESSION_ID"
    else
        session_id="ppid-$PPID"
    fi
fi

max_wait="${CODEX_GIT_LOCK_MAX_WAIT_SECONDS:-120}"
if ! [[ $max_wait =~ ^[0-9]+$ ]]; then
    max_wait=120
fi
stale_threshold=180
max_corrupt_retries=5
corrupt_retries=0
waited=0
cmd_short="${cmd:0:200}"
lock_owner_pid="${CODEX_GIT_LOCK_OWNER_PID:-$$}"
if ! [[ $lock_owner_pid =~ ^[0-9]+$ ]] || [ "$lock_owner_pid" -le 0 ] 2>/dev/null; then
    lock_owner_pid="$$"
fi

make_lock_json() {
    local session="$1"
    local repo="$2"
    local pid_value="$3"
    local command_value="$4"
    printf '%s\t%s\t%s\t%s' "$session" "$repo" "$pid_value" "$command_value" | python3 -c '
import json, sys
from datetime import datetime, timezone
try:
    raw = sys.stdin.read()
    session, repo, pid_value, command = raw.split("\t", 3)
    data = {
        "sessionId": session,
        "acquired": datetime.now(timezone.utc).isoformat(),
        "pid": int(pid_value),
        "command": command,
        "repo": repo,
    }
    print(json.dumps(data, separators=(",", ":")), end="")
except Exception:
    sys.exit(1)
' 2>/dev/null
}

write_lock_create_new() {
    local target="$1"
    local json_content="$2"
    if [ -z "$json_content" ]; then
        return 1
    fi
    (set -o noclobber; printf '%s' "$json_content" > "$target") 2>/dev/null
}

write_lock_refresh() {
    local target="$1"
    local json_content="$2"
    local temp_path
    temp_path="${target}.tmp.$$.$RANDOM"
    if [ -z "$json_content" ]; then
        return 1
    fi
    if ! printf '%s' "$json_content" > "$temp_path" 2>/dev/null; then
        rm -f "$temp_path" 2>/dev/null || true
        return 1
    fi
    mv -f "$temp_path" "$target" 2>/dev/null || {
        rm -f "$temp_path" 2>/dev/null || true
        return 1
    }
}

is_process_alive() {
    local pid_value="$1"
    if [ -z "$pid_value" ] || ! [[ $pid_value =~ ^[0-9]+$ ]] || [ "$pid_value" -le 0 ] 2>/dev/null; then
        return 1
    fi
    kill -0 "$pid_value" 2>/dev/null
}

parse_lock() {
    local raw="$1"
    if [ -z "$raw" ]; then
        printf '\037\037\037999'
        return
    fi
    printf '%s' "$raw" | python3 -c '
import json, sys
from datetime import datetime, timezone
try:
    d = json.loads(sys.stdin.read())
    session = d.get("sessionId", "") or ""
    acquired = d.get("acquired", "") or ""
    pid_value = str(d.get("pid", "0"))
    age = 999
    if acquired:
        try:
            dt = datetime.fromisoformat(acquired.replace("Z", "+00:00"))
            age = int((datetime.now(timezone.utc) - dt).total_seconds())
        except Exception:
            age = 999
    print(session + "\x1f" + acquired + "\x1f" + pid_value + "\x1f" + str(age), end="")
except Exception:
    print("\x1f\x1f\x1f999", end="")
' 2>/dev/null
}

while [ "$waited" -lt "$max_wait" ]; do
    new_json="$(make_lock_json "$session_id" "$git_common_dir" "$lock_owner_pid" "$cmd_short" || true)"

    if write_lock_create_new "$lockfile" "$new_json"; then
        hook_log "codex-git-multi-session-lock: acquired for '$cmd_short'" 2>/dev/null || true
        exit 0
    fi

    lock_raw=""
    if [ -f "$lockfile" ]; then
        lock_raw="$(cat "$lockfile" 2>/dev/null || true)"
    fi

    parsed="$(parse_lock "$lock_raw")"
    old_ifs="$IFS"
    IFS="$(printf '\037')"
    read -r lock_session _lock_acquired lock_pid age <<< "$parsed"
    IFS="$old_ifs"

    if [ -z "$lock_raw" ] || [ -z "$lock_session" ]; then
        corrupt_retries=$((corrupt_retries + 1))
        if [ "$corrupt_retries" -gt "$max_corrupt_retries" ]; then
            hook_log_warn "codex-git-multi-session-lock: corrupt retry limit reached; fail-open" 2>/dev/null || true
            exit 0
        fi
        rm -f "$lockfile" 2>/dev/null || true
        sleep 0.1
        continue
    fi

    if ! [[ $age =~ ^-?[0-9]+$ ]]; then
        age=999
    fi

    is_own=0
    [ "$lock_session" = "$session_id" ] && is_own=1

    pid_alive=0
    if [ "$is_own" -eq 1 ]; then
        pid_alive=1
    elif is_process_alive "$lock_pid"; then
        pid_alive=1
    fi

    is_stale=0
    if [ "$age" -gt "$stale_threshold" ] 2>/dev/null; then
        is_stale=1
    elif [ "$pid_alive" -eq 0 ]; then
        is_stale=1
    fi

    if [ "$is_stale" -eq 1 ]; then
        hook_log_warn "codex-git-multi-session-lock: stale lock from $lock_session; delete and retry" 2>/dev/null || true
        rm -f "$lockfile" 2>/dev/null || true
        corrupt_retries=0
        sleep 0.1
        continue
    fi

    if [ "$is_own" -eq 1 ]; then
        if write_lock_refresh "$lockfile" "$new_json"; then
            hook_log "codex-git-multi-session-lock: refreshed for '$cmd_short'" 2>/dev/null || true
        else
            hook_log_warn "codex-git-multi-session-lock: refresh failed; fail-open" 2>/dev/null || true
        fi
        exit 0
    fi

    if [ "$waited" -eq 0 ]; then
        echo "codex-git-multi-session-lock: another session holds git lock (session $lock_session, pid $lock_pid, age ${age}s). Waiting..." >&2
        hook_log "codex-git-multi-session-lock: waiting for $lock_session" 2>/dev/null || true
    elif [ $((waited % 10)) -eq 0 ]; then
        echo "codex-git-multi-session-lock: still waiting (${waited}s)..." >&2
    fi

    sleep 2
    waited=$((waited + 2))
    corrupt_retries=0
done

hook_log_warn "codex-git-multi-session-lock: timeout after ${waited}s; fail-open" 2>/dev/null || true
echo "WARNING: codex-git-multi-session-lock timeout after ${waited}s; continuing without lock." >&2
exit 0
