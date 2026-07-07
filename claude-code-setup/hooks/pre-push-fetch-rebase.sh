#!/bin/bash
# Pre-push hook: automatic fetch + rebase before push
# Prevents push rejections when other CLIs (Codex, Gemini) pushed in the meantime
#
# Installation:
#   cp claude-code-setup/hooks/pre-push-fetch-rebase.sh .git/hooks/pre-push
#   chmod +x .git/hooks/pre-push

REMOTE="$1"

# Only for pushes to origin
if [ "$REMOTE" != "origin" ]; then
    exit 0
fi

echo "[pre-push] Syncing with origin/main..."

# Fetch latest
if ! git fetch origin 2>/dev/null; then
    echo "[pre-push] Warning: fetch failed, continuing with push."
    exit 0
fi

# Check if rebase is needed
REMOTE_MAIN=$(git rev-parse origin/main 2>/dev/null) || exit 0
BASE=$(git merge-base HEAD origin/main 2>/dev/null) || exit 0

if [ "$BASE" = "$REMOTE_MAIN" ]; then
    echo "[pre-push] Already up-to-date."
    exit 0
fi

echo "[pre-push] Behind origin/main — rebasing..."

# Stash dirty changes (e.g. Whiteboard auto-updates)
DIRTY=0
if ! git diff --quiet 2>/dev/null || ! git diff --cached --quiet 2>/dev/null; then
    git stash --quiet 2>/dev/null
    DIRTY=1
fi

# Rebase
if git rebase origin/main --quiet 2>/dev/null; then
    echo "[pre-push] Rebase successful."
else
    echo "[pre-push] ERROR: Rebase conflict! Push aborted."
    echo "[pre-push] Fix manually: git rebase --abort && git fetch origin && git rebase origin/main"
    git rebase --abort 2>/dev/null
    [ "$DIRTY" -eq 1 ] && git stash pop --quiet 2>/dev/null
    exit 1
fi

# Restore stashed changes
[ "$DIRTY" -eq 1 ] && git stash pop --quiet 2>/dev/null

exit 0
