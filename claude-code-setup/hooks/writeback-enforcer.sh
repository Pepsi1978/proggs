#!/usr/bin/env bash
# writeback-enforcer.sh — SubagentStop Hook for Write-Back Enforcement (C1, macOS/Linux)
# Checks for sentinel files written by senior agents, merges findings into MEMORY.md.
# v3.1: Added subagent-failure + tool-failure to section map (K10).
#       Added section-field override: sentinel JSON may contain "section" to route correctly.
#       flock-based file locking (matches PS1 mutex) + hook-log.sh integration.

# Source hook-log.sh for structured logging
source "$(dirname "$0")/hook-log.sh"

SENTINEL_DIR="${TMPDIR:-/tmp}"
MEMORY_FILE="$HOME/proggs/.claude/agent-memory/shared/MEMORY.md"
LOCK_FILE="/tmp/claude-whiteboard.lock"

[ ! -f "$MEMORY_FILE" ] && exit 0

# Section map: agent name -> target section header
get_section() {
    case "$1" in
        code-reviewer|batch-reviewer|mar-reviewer) echo "## Erkenntnisse aus Code Reviews" ;;
        tester|quality-gate) echo "## Erkenntnisse aus Tests" ;;
        architect|challenger) echo "## Architektur-Entscheidungen" ;;
        debugger) echo "## Debugging-Muster" ;;
        coder) echo "## Erkenntnisse aus Code Reviews" ;;
        optimizer) echo "## Performance & Optimierung" ;;
        ui-polisher) echo "## UI/UX-Patterns" ;;
        researcher|intelligence-researcher) echo "## Forschung & Intelligence" ;;
        evolution-analyst|env-checker) echo "## Systemzustand" ;;
        # K10: failure sentinels from SubagentStop and PostToolUseFailure hooks
        subagent-failure|tool-failure) echo "## Offene Fehler & Probleme" ;;
        # v4: intelligence checker and learning loop sentinels
        intelligence-checker|learning-loop) echo "## Meta-Intelligenz & Selbstverbesserung" ;;
        *) echo "## Erkenntnisse aus Code Reviews" ;;
    esac
}

# Find sentinel files
shopt -s nullglob
sentinels=("$SENTINEL_DIR"/agent-writeback-*.json)
shopt -u nullglob

[ ${#sentinels[@]} -eq 0 ] && exit 0

hook_log "found ${#sentinels[@]} sentinel file(s) — acquiring lock"

# flock-based locking to prevent concurrent read-modify-write on MEMORY.md.
# Matches the PS1 version's named mutex (Global\ClaudeWhiteboardMutex) with 5s timeout.
# On macOS, flock may not be pre-installed. Install via: brew install flock
# Fallback: proceed without locking (with a warning) so the hook never silently fails.
_do_merge() {
    for f in "${sentinels[@]}"; do
        agent=$(python3 -c "import json,sys; print(json.load(open(sys.argv[1])).get('agent','unknown'))" "$f" 2>/dev/null || echo "unknown")
        findings=$(python3 -c "import json,sys; print(json.load(open(sys.argv[1])).get('findings','(no findings)'))" "$f" 2>/dev/null || echo "(no findings)")
        timestamp=$(python3 -c "import json,sys; print(json.load(open(sys.argv[1])).get('timestamp',''))" "$f" 2>/dev/null || echo "")
        section_override=$(python3 -c "import json,sys; print(json.load(open(sys.argv[1])).get('section',''))" "$f" 2>/dev/null || echo "")
        ts=$(date +"%Y-%m-%d %H:%M")
        entry="- **[$ts] $agent**: $findings"
        # K10: If sentinel contains a "section" field, use it as override over the map.
        # This enables quality-gate (3 named sentinels) and failure hooks to route correctly.
        if [ -n "$section_override" ]; then
            # Prepend "## " if not already present
            case "$section_override" in
                "## "*) target_section="$section_override" ;;
                *) target_section="## $section_override" ;;
            esac
        else
            target_section=$(get_section "$agent")
        fi

        # Use Python for reliable in-place editing (no BSD/GNU sed incompatibility, no
        # delimiter issues with slashes in file paths or findings text).
        python3 - "$MEMORY_FILE" "$target_section" "$entry" "$agent" <<'PYEOF'
import sys, re

memory_file    = sys.argv[1]
target_section = sys.argv[2]  # e.g. "## Erkenntnisse aus Code Reviews"
entry          = sys.argv[3]
agent_name     = sys.argv[4]
placeholder    = "_Noch keine Eintraege._"

with open(memory_file, 'r', encoding='utf-8') as fh:
    lines = fh.readlines()

# Prefix match so "## Systemzustand (aktuell — Stand: 2026-03-20)" is also found.
section_idx = -1
for i, line in enumerate(lines):
    stripped = line.rstrip()
    if stripped == target_section or stripped.startswith(target_section):
        section_idx = i
        break

if section_idx < 0:
    print(f"WriteBack-Enforcer [Python]: section '{target_section}' not found for agent '{agent_name}' — appending at end as last resort", file=sys.stderr)
    # Append at end as last resort so data is never lost
    lines.append(entry + "\n")
else:
    # Scan inside the section to find placeholder or insertion point
    insert_idx = section_idx + 1
    placeholder_idx = -1
    for j in range(section_idx + 1, len(lines)):
        stripped = lines[j].rstrip()
        if stripped.startswith("## ") or stripped == "---":
            break
        if lines[j].strip() == placeholder:
            placeholder_idx = j
            break
        insert_idx = j + 1

    if placeholder_idx >= 0:
        lines[placeholder_idx] = entry + "\n"
    else:
        lines.insert(insert_idx, entry + "\n")

with open(memory_file, 'w', encoding='utf-8') as fh:
    fh.writelines(lines)
PYEOF

        py_exit=$?
        if [ $py_exit -ne 0 ]; then
            hook_log_warn "Python merge failed (exit $py_exit) for agent '$agent'"
        fi

        if [ $py_exit -eq 0 ]; then
            rm -f "$f"
            hook_log "merged $agent findings into '$target_section'"
        else
            hook_log_warn "Python merge failed for '$f' — sentinel preserved for retry"
        fi
        echo "WriteBack-Enforcer: Merged $agent findings into '$target_section'"
    done
}

if command -v flock >/dev/null 2>&1; then
    # Acquire exclusive lock with 5-second timeout (matching PS1 mutex WaitOne(5000))
    exec 9>"$LOCK_FILE"
    if flock -w 5 9; then
        hook_log "lock acquired"
        _do_merge
        flock -u 9
        hook_log "lock released"
    else
        hook_log_warn "flock timeout after 5s — skipping write"
        echo "WriteBack-Enforcer: lock timeout — skipping write" >&2
        exit 1
    fi
    exec 9>&-
else
    # flock not available (macOS without Homebrew) — proceed without locking
    echo "WriteBack-Enforcer: flock not found — proceeding without lock (install via: brew install flock)" >&2
    hook_log_warn "flock not available — proceeding without file locking"
    _do_merge
fi

exit 0
