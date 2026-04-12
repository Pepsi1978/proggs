#!/bin/bash
# path-health-check.sh -- Lightweight PATH health check at session start
# Event: SessionStart (runs every session, must be fast < 2 seconds)
# Platform: macOS/Linux
#
# PURPOSE: Detects orphaned Python installations and ghost PATH entries.
# Does NOT do full tool verification (that's path-verify.sh --fix).
#
# POKA-YOKE STUFE 3: Automatic detection at every session start.

set +e
issues=()

# ===== Check 1: Orphaned Python (pip without python) =====
IFS=':' read -ra path_dirs <<< "$PATH"
for dir in "${path_dirs[@]}"; do
    if [ -d "$dir" ]; then
        if [ -f "$dir/pip" ] || [ -f "$dir/pip3" ]; then
            parent_dir=$(dirname "$dir")
            has_python=false
            [ -f "$parent_dir/python" ] && has_python=true
            [ -f "$parent_dir/python3" ] && has_python=true
            [ -f "$dir/python" ] && has_python=true
            [ -f "$dir/python3" ] && has_python=true
            if ! $has_python; then
                issues+=("ORPHANED PYTHON: $dir has pip but no python. Fix: bash ~/.claude/hooks/path-verify.sh --fix")
            fi
        fi
    fi
done

# ===== Check 2: Ghost PATH entries (non-existent tool directories) =====
for dir in "${path_dirs[@]}"; do
    if [ -n "$dir" ] && [ ! -d "$dir" ]; then
        case "$dir" in
            *Python*|*python*|*node*|*npm*|*cargo*|*rustup*|*Go*|*go/bin*|*gradle*|*kotlin*|*Android*|*bun*)
                issues+=("GHOST-PATH: $dir does not exist")
                ;;
        esac
    fi
done

# ===== Output =====
if [ ${#issues[@]} -gt 0 ]; then
    echo "PATH-Health-Check: ${#issues[@]} Problem(e) gefunden:"
    for issue in "${issues[@]}"; do
        echo "  - $issue"
    done
    echo "Reparatur: bash ~/.claude/hooks/path-verify.sh --fix"
else
    echo "PATH-Health-Check: OK"
fi

exit 0
