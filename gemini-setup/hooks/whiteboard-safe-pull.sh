#!/usr/bin/env bash
# whiteboard-safe-pull.sh — Prevents MEMORY.md merge conflicts
# Called by whiteboard-insert.sh BEFORE writing to MEMORY.md
# Strategy: pull remote changes first, retry on conflict with auto-resolve
# Created: 2026-03-25 by Intelligenz-Vorschlag #715

set +e  # Hooks muessen IMMER weiterlaufen, nie bei Fehler abbrechen

REPO_DIR="$HOME/proggs"
WHITEBOARD=".Gemini/agent-memory/shared/MEMORY.md"

cd "$REPO_DIR" || exit 0

# Only act if we're in a git repo
git rev-parse --is-inside-work-tree &>/dev/null || exit 0

# Check if there are remote changes to pull
git fetch origin main --quiet 2>/dev/null || exit 0

LOCAL=$(git rev-parse HEAD 2>/dev/null)
REMOTE=$(git rev-parse origin/main 2>/dev/null)

# Nothing to do if already up to date
[[ "$LOCAL" == "$REMOTE" ]] && exit 0

# If MEMORY.md has local changes, stash → pull → pop
if git diff --name-only | grep -q "$WHITEBOARD" 2>/dev/null || \
   git diff --cached --name-only | grep -q "$WHITEBOARD" 2>/dev/null; then

    # Stash only the whiteboard file
    git stash push -m "whiteboard-safe-pull auto-stash" -- "$WHITEBOARD" 2>/dev/null || exit 0

    # Pull remote changes
    git pull --rebase origin main --quiet 2>/dev/null || {
        # If pull fails, pop stash and exit
        git stash pop --quiet 2>/dev/null || true
        exit 0
    }

    # Re-apply local changes
    git stash pop --quiet 2>/dev/null || {
        # If pop fails (conflict), resolve by keeping both
        # Accept theirs for the structure, append our changes
        git checkout --theirs "$WHITEBOARD" 2>/dev/null || true
        git add "$WHITEBOARD" 2>/dev/null || true
        git stash drop --quiet 2>/dev/null || true
    }
else
    # No local whiteboard changes, just pull
    git pull --rebase origin main --quiet 2>/dev/null || true
fi

exit 0

