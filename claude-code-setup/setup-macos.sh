#!/bin/bash
# Claude Code Setup - macOS
# Copies all configuration files to the correct locations

set -e
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "=== Claude Code Setup (macOS) ==="

# Create directories
mkdir -p ~/.claude/{rules,agents,commands,hooks}

# Copy configs
cp "$SCRIPT_DIR/settings.json" ~/.claude/settings.json
cp "$SCRIPT_DIR/rules/"*.md ~/.claude/rules/
cp "$SCRIPT_DIR/agents/"*.md ~/.claude/agents/
cp "$SCRIPT_DIR/commands/"*.md ~/.claude/commands/
cp "$SCRIPT_DIR/hooks/auto-format.sh" ~/.claude/hooks/
cp "$SCRIPT_DIR/hooks/auto-sync.sh" ~/.claude/hooks/
cp "$SCRIPT_DIR/hooks/safety-gate.sh" ~/.claude/hooks/
cp "$SCRIPT_DIR/hooks/notify.sh" ~/.claude/hooks/
chmod +x ~/.claude/hooks/auto-format.sh
chmod +x ~/.claude/hooks/auto-sync.sh
chmod +x ~/.claude/hooks/safety-gate.sh
chmod +x ~/.claude/hooks/notify.sh
cp "$SCRIPT_DIR/CLAUDE.md" ~/CLAUDE.md
cp "$SCRIPT_DIR/.gitignore_global" ~/.gitignore_global

# Add macOS-specific hooks to settings.json
# Uses yq to merge hooks into settings (install with: brew install yq)
if command -v yq &> /dev/null; then
    HOOKS=$(cat "$SCRIPT_DIR/hooks-macos.json" | yq -o json '.hooks')
    yq -i -o json ".hooks = $HOOKS" ~/.claude/settings.json
    echo "Hooks (macOS) added to settings.json"
else
    echo "WARNING: yq not found. Please manually add hooks from hooks-macos.json to ~/.claude/settings.json"
fi

# Git config
git config --global init.defaultBranch main
git config --global pull.rebase true
git config --global push.autoSetupRemote true
git config --global core.excludesFile ~/.gitignore_global
git config --global alias.wt "worktree add"
git config --global alias.wtl "worktree list"
git config --global alias.wtr "worktree remove"

echo ""
echo "=== Done! ==="
echo "Next steps:"
echo "  1. Install dev tools: brew install swift-format swiftlint golangci-lint"
echo "  2. Install shell tools: brew install fzf eza bat fd tmux htop wget yq"
echo "  3. Start Parry daemon: parry serve"
echo "  4. Run Claude Code and type /self-improve for a full environment check"
