#!/usr/bin/env bash
# self-improve-sync.sh
# Syncs local Claude Code configuration files to the proggs repo and pushes.
#
# Usage:
#   ./self-improve-sync.sh [COMMIT_MESSAGE]
#
# Default commit message: "Self-improve sync (bash)"
#
# What this syncs:
#   - ~/.claude/rules/        -> ~/proggs/claude-code-setup/rules/
#   - ~/.claude/agents/       -> ~/proggs/claude-code-setup/agents/
#   - ~/.claude/commands/     -> ~/proggs/claude-code-setup/commands/
#   - ~/.claude/hooks/        -> ~/proggs/claude-code-setup/hooks/
#   - ~/.claude/skills/       -> ~/proggs/claude-code-setup/skills/
#   - ~/.claude/agent-memory/shared/MEMORY.md -> ~/proggs/claude-code-setup/agent-memory/shared/
#   - ~/CLAUDE.md             -> ~/proggs/CLAUDE.md

set -euo pipefail

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------
COMMIT_MSG="${1:-Self-improve sync (bash)}"
PROGGS_DIR="$HOME/proggs"
CLAUDE_DIR="$HOME/.claude"
SETUP_DIR="$PROGGS_DIR/claude-code-setup"

# ---------------------------------------------------------------------------
# Validation
# ---------------------------------------------------------------------------
if [[ ! -d "$PROGGS_DIR/.git" ]]; then
    echo "ERROR: $PROGGS_DIR is not a git repository" >&2
    exit 1
fi

if [[ ! -d "$CLAUDE_DIR" ]]; then
    echo "ERROR: $CLAUDE_DIR does not exist" >&2
    exit 1
fi

echo "==> Syncing Claude Code configuration to repo..."
echo "    Source:  $CLAUDE_DIR"
echo "    Target:  $SETUP_DIR"
echo "    Repo:    $PROGGS_DIR"
echo ""

# ---------------------------------------------------------------------------
# Copy files
# ---------------------------------------------------------------------------

# Rules (Markdown, platform-independent)
echo "--> Copying rules..."
mkdir -p "$SETUP_DIR/rules"
cp "$CLAUDE_DIR/rules/"*.md "$SETUP_DIR/rules/" 2>/dev/null || echo "    (no .md files in rules/)"

# Agents
echo "--> Copying agents..."
mkdir -p "$SETUP_DIR/agents"
cp "$CLAUDE_DIR/agents/"*.md "$SETUP_DIR/agents/" 2>/dev/null || echo "    (no .md files in agents/)"

# Commands (self-improve.md + reference directory)
echo "--> Copying commands..."
mkdir -p "$SETUP_DIR/commands"
cp "$CLAUDE_DIR/commands/self-improve.md" "$SETUP_DIR/commands/" 2>/dev/null || echo "    (self-improve.md not found)"
if [[ -d "$CLAUDE_DIR/commands/self-improve-ref" ]]; then
    cp -r "$CLAUDE_DIR/commands/self-improve-ref/" "$SETUP_DIR/commands/self-improve-ref/" 2>/dev/null || true
fi

# Hooks (all variants: .sh, .ps1, .ts + subdirectories)
echo "--> Copying hooks..."
mkdir -p "$SETUP_DIR/hooks"
cp "$CLAUDE_DIR/hooks/"*.sh  "$SETUP_DIR/hooks/" 2>/dev/null || true
cp "$CLAUDE_DIR/hooks/"*.ps1 "$SETUP_DIR/hooks/" 2>/dev/null || true
cp "$CLAUDE_DIR/hooks/"*.ts  "$SETUP_DIR/hooks/" 2>/dev/null || true
# Subdirectory: prompt-injection-defender
if [[ -d "$CLAUDE_DIR/hooks/prompt-injection-defender" ]]; then
    cp -r "$CLAUDE_DIR/hooks/prompt-injection-defender/" "$SETUP_DIR/hooks/prompt-injection-defender/" 2>/dev/null || true
fi

# Skills
echo "--> Copying skills..."
if [[ -d "$CLAUDE_DIR/skills" ]]; then
    cp -r "$CLAUDE_DIR/skills/" "$SETUP_DIR/skills/" 2>/dev/null || echo "    (skills copy failed)"
fi

# CLAUDE.md (root of repo)
echo "--> Copying CLAUDE.md..."
cp "$HOME/CLAUDE.md" "$PROGGS_DIR/CLAUDE.md" 2>/dev/null || echo "    (~/CLAUDE.md not found)"

# Shared knowledge hub (MEMORY.md only — no separate files)
echo "--> Copying MEMORY.md..."
mkdir -p "$SETUP_DIR/agent-memory/shared"
cp "$CLAUDE_DIR/agent-memory/shared/MEMORY.md" "$SETUP_DIR/agent-memory/shared/" 2>/dev/null || echo "    (MEMORY.md not found)"

echo ""

# ---------------------------------------------------------------------------
# Git sync
# ---------------------------------------------------------------------------
echo "==> Pulling latest changes..."
cd "$PROGGS_DIR"
git pull --rebase || {
    echo "ERROR: git pull --rebase failed — resolve conflicts first" >&2
    exit 1
}

echo ""
echo "==> Staging changes..."
git add "$SETUP_DIR/" "$PROGGS_DIR/CLAUDE.md"

# Re-stage after auto-formatters may have modified staged files (race condition fix)
sleep 1
git add "$SETUP_DIR/" "$PROGGS_DIR/CLAUDE.md"

# Check if there is anything to commit
if ! git status --porcelain "$SETUP_DIR/" "$PROGGS_DIR/CLAUDE.md" | grep -q .; then
    echo "==> Nothing to commit — configuration is already up to date."
    exit 0
fi

echo "==> Committing..."
# Determine next commit number
LAST_NUM=$(git log --oneline | grep -oP '^[a-f0-9]+ #\K[0-9]+' | sort -n | tail -1 || echo "0")
NEXT_NUM=$(printf "%03d" $((LAST_NUM + 1)))

git commit -m "#$NEXT_NUM - $COMMIT_MSG"

echo ""
echo "==> Pushing..."
git push

echo ""
echo "==> Done. Configuration synced and pushed."
