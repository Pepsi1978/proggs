#!/bin/bash
# Auto-Sync: Syncs Claude Code config from GitHub on every session start
# Runs as SessionStart hook — output is visible to the user
# Works on: macOS, Linux, Termux (Android)
#
# DESIGN: This hook NEVER exits non-zero and NEVER produces errors.
# Every single command is guarded with || true or 2>/dev/null.
# The old EXIT trap from hook-log.sh was the #1 cause of phantom errors.

HOOK_NAME="auto-sync" source "$HOME/.claude/hooks/hook-log.sh" 2>/dev/null || true

REPO_DIR="$HOME/proggs"
SETUP_DIR="$REPO_DIR/claude-code-setup"
CLAUDE_DIR="$HOME/.claude"

# Check prerequisites
if ! command -v git &>/dev/null; then
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
    exit 0
fi

# Fetch latest from remote (quick metadata check)
# Use timeout to prevent hanging on slow connections
if ! git fetch --quiet 2>/dev/null; then
    echo "Auto-Sync: Keine Internetverbindung — uebersprungen."
    exit 0
fi

# Compare local HEAD vs remote tracking branch
LOCAL=$(git rev-parse HEAD 2>/dev/null || echo "none")
REMOTE=$(git rev-parse @{u} 2>/dev/null || echo "")

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

# Stash dirty changes before pull
STASHED=false
if ! git diff --quiet 2>/dev/null || ! git diff --cached --quiet 2>/dev/null; then
    if git stash push -m "auto-sync-$(date +%s)" --quiet 2>/dev/null; then
        STASHED=true
    fi
fi

# Pull with rebase
PULL_OUTPUT=$(git pull --rebase --quiet 2>&1) || true
PULL_RC=${PIPESTATUS[0]:-0}

# Restore stashed changes
if [ "$STASHED" = true ]; then
    git stash pop --quiet 2>/dev/null || {
        echo "Auto-Sync: Hinweis — gestashte Aenderungen in git stash list."
    }
fi

if [ "$PULL_RC" != "0" ] 2>/dev/null; then
    hook_log_error "git pull failed: $PULL_OUTPUT" 2>/dev/null || true
    echo "Auto-Sync: Pull fehlgeschlagen — lokale Dateien unveraendert."
    git rebase --abort 2>/dev/null || true
    exit 0
fi

echo "Auto-Sync: Git Pull erfolgreich."

# --- Sync config files ---
SYNCED=""

# Rules
if [ -d "$SETUP_DIR/rules" ]; then
    mkdir -p "$CLAUDE_DIR/rules" 2>/dev/null || true
    COUNT=$(ls "$SETUP_DIR/rules/"*.md 2>/dev/null | wc -l | tr -d ' ')
    cp "$SETUP_DIR/rules/"*.md "$CLAUDE_DIR/rules/" 2>/dev/null || true
    [ "${COUNT:-0}" -gt 0 ] 2>/dev/null && SYNCED="$SYNCED Rules($COUNT)"
fi

# Agents
if [ -d "$SETUP_DIR/agents" ]; then
    mkdir -p "$CLAUDE_DIR/agents" 2>/dev/null || true
    COUNT=$(ls "$SETUP_DIR/agents/"*.md 2>/dev/null | wc -l | tr -d ' ')
    cp "$SETUP_DIR/agents/"*.md "$CLAUDE_DIR/agents/" 2>/dev/null || true
    [ "${COUNT:-0}" -gt 0 ] 2>/dev/null && SYNCED="$SYNCED Agents($COUNT)"
fi

# Commands + subdirectories
if [ -d "$SETUP_DIR/commands" ]; then
    mkdir -p "$CLAUDE_DIR/commands" 2>/dev/null || true
    COUNT=$(ls "$SETUP_DIR/commands/"*.md 2>/dev/null | wc -l | tr -d ' ')
    cp "$SETUP_DIR/commands/"*.md "$CLAUDE_DIR/commands/" 2>/dev/null || true
    for subdir in "$SETUP_DIR/commands/"*/; do
        [ -d "$subdir" ] || continue
        SUBNAME=$(basename "$subdir")
        mkdir -p "$CLAUDE_DIR/commands/$SUBNAME" 2>/dev/null || true
        cp "$subdir"* "$CLAUDE_DIR/commands/$SUBNAME/" 2>/dev/null || true
        COUNT=$((COUNT + 1))
    done
    [ "${COUNT:-0}" -gt 0 ] 2>/dev/null && SYNCED="$SYNCED Commands($COUNT)"
fi

# Hooks (shell + TypeScript scripts)
if [ -d "$SETUP_DIR/hooks" ]; then
    mkdir -p "$CLAUDE_DIR/hooks" 2>/dev/null || true
    HOOKS_COUNT=0
    for hook in "$SETUP_DIR/hooks/"*.sh "$SETUP_DIR/hooks/"*.ts; do
        [ -f "$hook" ] || continue
        cp "$hook" "$CLAUDE_DIR/hooks/" 2>/dev/null || true
        chmod +x "$CLAUDE_DIR/hooks/$(basename "$hook")" 2>/dev/null || true
        HOOKS_COUNT=$((HOOKS_COUNT + 1))
    done
    [ "$HOOKS_COUNT" -gt 0 ] && SYNCED="$SYNCED Hooks($HOOKS_COUNT)"
fi

# CLAUDE.md
if [ -f "$SETUP_DIR/CLAUDE.md" ]; then
    cp "$SETUP_DIR/CLAUDE.md" "$HOME/CLAUDE.md" 2>/dev/null || true
    cp "$SETUP_DIR/CLAUDE.md" "$REPO_DIR/CLAUDE.md" 2>/dev/null || true
    SYNCED="$SYNCED CLAUDE.md"
fi

# .gitignore_global
if [ -f "$SETUP_DIR/.gitignore_global" ]; then
    cp "$SETUP_DIR/.gitignore_global" "$HOME/.gitignore_global" 2>/dev/null || true
    SYNCED="$SYNCED .gitignore"
fi

# Settings sync from settings-reference.json
REF_SETTINGS="$SETUP_DIR/settings-reference.json"
LOCAL_SETTINGS="$CLAUDE_DIR/settings.json"
if [ -f "$REF_SETTINGS" ] && [ -f "$LOCAL_SETTINGS" ] && command -v python3 &>/dev/null; then
    SETTINGS_CHANGES=$(python3 << 'PYEOF' 2>/dev/null || echo "0"
import json, sys
try:
    ref = json.load(open("$REF_SETTINGS".replace("$REF_SETTINGS", "$REF_SETTINGS")))
    local = json.load(open("$LOCAL_SETTINGS".replace("$LOCAL_SETTINGS", "$LOCAL_SETTINGS")))
    print(0)
except:
    print(0)
PYEOF
    )
    # Use a separate python3 call with proper variable substitution
    SETTINGS_CHANGES=$(python3 -c "
import json, sys, os
try:
    ref = json.load(open(os.path.expanduser('$REF_SETTINGS')))
    local = json.load(open(os.path.expanduser('$LOCAL_SETTINGS')))
    changes = 0
    # Sync enabledPlugins
    ref_plugins = ref.get('enabledPlugins', {})
    local_plugins = local.get('enabledPlugins', {})
    for key, value in ref_plugins.items():
        if key not in local_plugins or local_plugins[key] != value:
            local_plugins[key] = value
            changes += 1
    if changes > 0:
        local['enabledPlugins'] = local_plugins
    # Sync extraKnownMarketplaces
    ref_markets = ref.get('extraKnownMarketplaces', {})
    local_markets = local.get('extraKnownMarketplaces', {})
    for key, value in ref_markets.items():
        if key not in local_markets:
            local_markets[key] = value
            changes += 1
    if ref_markets:
        local['extraKnownMarketplaces'] = local_markets
    # Sync env vars (add missing only)
    ref_env = ref.get('env', {})
    local_env = local.get('env', {})
    for key, value in ref_env.items():
        if key not in local_env:
            local_env[key] = value
            changes += 1
    if ref_env:
        local['env'] = local_env
    # Sync hooks (replace entire section)
    ref_hooks = ref.get('hooks', {})
    if ref_hooks:
        local_hooks = local.get('hooks', {})
        if json.dumps(ref_hooks, sort_keys=True) != json.dumps(local_hooks, sort_keys=True):
            local['hooks'] = ref_hooks
            changes += 1
    if changes > 0:
        json.dump(local, open('$LOCAL_SETTINGS', 'w'), indent=2)
        with open('$LOCAL_SETTINGS', 'a') as f:
            f.write('\n')
    print(changes)
except Exception as e:
    print(0)
" 2>/dev/null || echo "0")

    if [ -n "$SETTINGS_CHANGES" ] && [ "$SETTINGS_CHANGES" -gt 0 ] 2>/dev/null; then
        SYNCED="$SYNCED Settings($SETTINGS_CHANGES)"
    fi
fi

if [ -n "$SYNCED" ]; then
    echo "Auto-Sync: Lokale Konfiguration aktualisiert:$SYNCED"
    echo "Auto-Sync: Hinweis — CLAUDE.md und Rules werden erst nach Neustart von Claude Code wirksam."
else
    echo "Auto-Sync: Git Pull erfolgreich, keine Config-Aenderungen."
fi
exit 0
