#!/bin/bash
# Auto-Sync: Syncs Claude Code config from GitHub on every session start
# Runs as SessionStart hook — output is visible to the user
# Works on: macOS, Linux, Termux (Android)
# DESIGN: This hook NEVER exits non-zero. Errors are logged and reported,
# but the hook always succeeds so Claude Code starts cleanly.

HOOK_NAME="auto-sync" source "$HOME/.claude/hooks/hook-log.sh" 2>/dev/null

REPO_DIR="$HOME/proggs"
SETUP_DIR="$REPO_DIR/claude-code-setup"
CLAUDE_DIR="$HOME/.claude"

# Check prerequisites
if ! command -v git &>/dev/null; then
    hook_log_error "git not found in PATH" 2>/dev/null
    echo "Auto-Sync: git nicht gefunden — uebersprungen."
    exit 0
fi

if [ ! -d "$REPO_DIR/.git" ]; then
    echo "Auto-Sync: ~/proggs Repo nicht gefunden — uebersprungen."
    exit 0
fi

cd "$REPO_DIR" || exit 0

# Skip if rebase or merge is in progress
if [ -d ".git/rebase-merge" ] || [ -d ".git/rebase-apply" ] || [ -f ".git/MERGE_HEAD" ]; then
    echo "Auto-Sync: Rebase/Merge in Arbeit — uebersprungen."
    hook_log "skipped: rebase/merge in progress" 2>/dev/null
    exit 0
fi

# Fetch latest from remote (quick metadata check)
if ! git fetch --quiet 2>/dev/null; then
    echo "Auto-Sync: Keine Internetverbindung — uebersprungen."
    exit 0
fi

# Compare local HEAD vs remote tracking branch
LOCAL=$(git rev-parse HEAD 2>/dev/null)
REMOTE=$(git rev-parse @{u} 2>/dev/null)

if [ -z "$REMOTE" ]; then
    echo "Auto-Sync: Kein Upstream-Branch konfiguriert — uebersprungen."
    exit 0
fi

if [ "$LOCAL" = "$REMOTE" ]; then
    echo "Auto-Sync: Alle Dateien aktuell."
    exit 0
fi

# Updates available!
BEHIND=$(git rev-list --count HEAD..@{u} 2>/dev/null || echo "?")
echo "Auto-Sync: $BEHIND neue Commits auf GitHub gefunden — aktualisiere..."

# Stash dirty changes before pull (prevents rebase conflicts)
STASHED=false
if ! git diff --quiet 2>/dev/null || ! git diff --cached --quiet 2>/dev/null; then
    if git stash push -m "auto-sync-$(date +%s)" --quiet 2>/dev/null; then
        STASHED=true
        hook_log "stashed dirty changes before pull" 2>/dev/null
    fi
fi

# Pull with rebase — capture output for error logging
PULL_OUTPUT=$(git pull --rebase --quiet 2>&1)
PULL_RC=$?

# Restore stashed changes
if [ "$STASHED" = true ]; then
    if ! git stash pop --quiet 2>/dev/null; then
        hook_log_error "stash pop failed — changes saved in git stash" 2>/dev/null
        echo "Auto-Sync: Hinweis — gestashte Aenderungen konnten nicht wiederhergestellt werden (git stash list)"
    fi
fi

if [ $PULL_RC -ne 0 ]; then
    hook_log_error "git pull failed (rc=$PULL_RC): $PULL_OUTPUT" 2>/dev/null
    echo "Auto-Sync: Pull fehlgeschlagen — Details: ~/.claude/logs/hooks/$(date +%Y-%m-%d).log"
    # Abort any leftover rebase from the failed pull
    git rebase --abort 2>/dev/null
    exit 0  # Never fail the hook
fi

echo "Auto-Sync: Git Pull erfolgreich."

# --- Sync config files from setup backup to active Claude Code config ---

SYNCED=""

# Rules
if [ -d "$SETUP_DIR/rules" ]; then
    mkdir -p "$CLAUDE_DIR/rules"
    COUNT=$(ls "$SETUP_DIR/rules/"*.md 2>/dev/null | wc -l | tr -d ' ')
    cp "$SETUP_DIR/rules/"*.md "$CLAUDE_DIR/rules/" 2>/dev/null
    [ "$COUNT" -gt 0 ] && SYNCED="$SYNCED Rules($COUNT)"
fi

# Agents
if [ -d "$SETUP_DIR/agents" ]; then
    mkdir -p "$CLAUDE_DIR/agents"
    COUNT=$(ls "$SETUP_DIR/agents/"*.md 2>/dev/null | wc -l | tr -d ' ')
    cp "$SETUP_DIR/agents/"*.md "$CLAUDE_DIR/agents/" 2>/dev/null
    [ "$COUNT" -gt 0 ] && SYNCED="$SYNCED Agents($COUNT)"
fi

# Commands (skills like /self-improve, /tool-check)
if [ -d "$SETUP_DIR/commands" ]; then
    mkdir -p "$CLAUDE_DIR/commands"
    COUNT=$(ls "$SETUP_DIR/commands/"*.md 2>/dev/null | wc -l | tr -d ' ')
    cp "$SETUP_DIR/commands/"*.md "$CLAUDE_DIR/commands/" 2>/dev/null
    [ "$COUNT" -gt 0 ] && SYNCED="$SYNCED Commands($COUNT)"
fi

# Hooks (shell scripts only — not JSON config, not this script itself during execution)
if [ -d "$SETUP_DIR/hooks" ]; then
    mkdir -p "$CLAUDE_DIR/hooks"
    HOOKS_COUNT=0
    for hook in "$SETUP_DIR/hooks/"*.sh; do
        [ -f "$hook" ] || continue
        cp "$hook" "$CLAUDE_DIR/hooks/" 2>/dev/null
        chmod +x "$CLAUDE_DIR/hooks/$(basename "$hook")" 2>/dev/null
        HOOKS_COUNT=$((HOOKS_COUNT + 1))
    done
    [ "$HOOKS_COUNT" -gt 0 ] && SYNCED="$SYNCED Hooks($HOOKS_COUNT)"
fi

# CLAUDE.md (both ~/CLAUDE.md and repo copy)
if [ -f "$SETUP_DIR/CLAUDE.md" ]; then
    cp "$SETUP_DIR/CLAUDE.md" "$HOME/CLAUDE.md" 2>/dev/null
    cp "$SETUP_DIR/CLAUDE.md" "$REPO_DIR/CLAUDE.md" 2>/dev/null
    SYNCED="$SYNCED CLAUDE.md"
fi

# .gitignore_global
if [ -f "$SETUP_DIR/.gitignore_global" ]; then
    cp "$SETUP_DIR/.gitignore_global" "$HOME/.gitignore_global" 2>/dev/null
    SYNCED="$SYNCED .gitignore"
fi

echo "Auto-Sync: Lokale Konfiguration aktualisiert:$SYNCED"
echo "Auto-Sync: Hinweis — CLAUDE.md und Rules werden erst nach Neustart von Claude Code wirksam."
