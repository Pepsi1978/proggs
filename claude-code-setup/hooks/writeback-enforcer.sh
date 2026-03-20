#!/usr/bin/env bash
# writeback-enforcer.sh — SubagentStop Hook for Write-Back Enforcement (C1, macOS/Linux)
# Checks for sentinel files written by senior agents, merges findings into MEMORY.md.
# v3: Added flock-based file locking (matches PS1 mutex) + hook-log.sh integration.

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
        debugger|coder) echo "## Debugging-Muster" ;;
        optimizer) echo "## Performance & Optimierung" ;;
        ui-polisher) echo "## UI/UX-Patterns" ;;
        researcher|intelligence-researcher) echo "## Forschung & Intelligence" ;;
        evolution-analyst|env-checker) echo "## Systemzustand (aktuell)" ;;
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
        agent=$(python3 -c "import json; print(json.load(open('$f')).get('agent','unknown'))" 2>/dev/null || echo "unknown")
        findings=$(python3 -c "import json; print(json.load(open('$f')).get('findings','(no findings)'))" 2>/dev/null || echo "(no findings)")
        ts=$(date +"%Y-%m-%d %H:%M")
        entry="- **[$ts] $agent**: $findings"
        target_section=$(get_section "$agent")

        # Find the section header line number
        section_line=$(grep -n "^${target_section}" "$MEMORY_FILE" | head -1 | cut -d: -f1)

        if [ -n "$section_line" ]; then
            # Search from section_line to next ## or --- or end of file
            section_end=$(tail -n +"$((section_line + 1))" "$MEMORY_FILE" | grep -n '^## \|^---' | head -1 | cut -d: -f1)
            if [ -z "$section_end" ]; then
                section_end=999999
            fi

            # Check if placeholder exists within this section range
            placeholder_line=$(tail -n +"$((section_line + 1))" "$MEMORY_FILE" | head -n "$section_end" | grep -n "_Noch keine Eintraege._" | head -1 | cut -d: -f1)

            if [ -n "$placeholder_line" ]; then
                actual_line=$((section_line + placeholder_line))
                sed -i.bak "${actual_line}s/_Noch keine Eintraege._/${entry}/" "$MEMORY_FILE"
                rm -f "$MEMORY_FILE.bak"
            else
                # Insert at end of section (before next section header)
                actual_insert=$((section_line + section_end))
                sed -i.bak "${actual_insert}i\\${entry}" "$MEMORY_FILE"
                rm -f "$MEMORY_FILE.bak"
            fi
        else
            hook_log_warn "section '$target_section' not found for agent '$agent' — skipping"
        fi

        rm -f "$f"
        hook_log "merged $agent findings into '$target_section'"
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
