#!/usr/bin/env bash
# writeback-enforcer.sh — SubagentStop Hook for Write-Back Enforcement (C1, macOS/Linux)
# Checks for sentinel files written by senior agents, merges findings into MEMORY.md.
# v2: Added section-based routing (matches writeback-enforcer.ps1 logic).

SENTINEL_DIR="${TMPDIR:-/tmp}"
MEMORY_FILE="$HOME/proggs/.claude/agent-memory/shared/MEMORY.md"

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

for f in "${sentinels[@]}"; do
    agent=$(python3 -c "import json; print(json.load(open('$f')).get('agent','unknown'))" 2>/dev/null || echo "unknown")
    findings=$(python3 -c "import json; print(json.load(open('$f')).get('findings','(no findings)'))" 2>/dev/null || echo "(no findings)")
    ts=$(date +"%Y-%m-%d %H:%M")
    entry="- **[$ts] $agent**: $findings"
    target_section=$(get_section "$agent")

    # Try to insert into the correct section
    # Find section header line number
    section_line=$(grep -n "^${target_section}" "$MEMORY_FILE" | head -1 | cut -d: -f1)

    if [ -n "$section_line" ]; then
        # Look for placeholder within this section
        # Search from section_line to next ## or --- or end of file
        section_end=$(tail -n +"$((section_line + 1))" "$MEMORY_FILE" | grep -n '^## \|^---' | head -1 | cut -d: -f1)
        if [ -z "$section_end" ]; then
            section_end=999999
        fi

        # Check if placeholder exists in this section range
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
    fi

    rm -f "$f"
    echo "WriteBack-Enforcer: Merged $agent findings into '$target_section'"
done

exit 0
