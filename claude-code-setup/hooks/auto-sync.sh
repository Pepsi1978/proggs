#!/usr/bin/env bash
# Auto-Sync: Syncs Claude Code config from GitHub on every session start
# Runs as SessionStart hook
# stdout → AI context (system-reminder), stderr → user terminal
# Platform: macOS / Linux (bash equivalent of auto-sync.ps1)

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/hook-log.sh"
source "$SCRIPT_DIR/whiteboard-insert.sh"

# Write to both stdout (AI context) and stderr (user-visible terminal)
write_status() {
    echo "$1"
    echo "$1" >&2
}

REPO_DIR="$HOME/proggs"
SETUP_DIR="$REPO_DIR/claude-code-setup"
CLAUDE_DIR="$HOME/.claude"

# Check if repo exists
if [ ! -d "$REPO_DIR/.git" ]; then
    hook_log_warn "~/proggs repo not found — skipped"
    write_status "Auto-Sync: ~/proggs Repo nicht gefunden -- uebersprungen."
    exit 0
fi

cd "$REPO_DIR" || exit 0

# Fetch latest from remote
if ! git fetch --quiet 2>/dev/null; then
    hook_log_warn "git fetch failed — no internet?"
    write_status "Auto-Sync: Keine Internetverbindung -- uebersprungen."
    exit 0
fi

# Compare local vs remote
local_sha=$(git rev-parse HEAD 2>/dev/null)
remote_sha=$(git rev-parse '@{u}' 2>/dev/null)

# .mcp.json — platform-specific sync (macOS version from mcp-macos.json)
# The .mcp.json is NOT tracked in git anymore — each platform generates its own.
mcp_source="$SETUP_DIR/mcp-macos.json"
mcp_dest="$REPO_DIR/.mcp.json"
if [ -f "$mcp_source" ]; then
    if ! diff -q "$mcp_source" "$mcp_dest" > /dev/null 2>&1; then
        cp "$mcp_source" "$mcp_dest"
        hook_log ".mcp.json synced from mcp-macos.json"
    fi
fi

# Always ensure code-search MCP server dependencies are installed (regardless of new commits)
mcp_cs_dir="$REPO_DIR/mcp-code-search"
if [ -d "$mcp_cs_dir" ] && [ ! -d "$mcp_cs_dir/node_modules" ]; then
    if [ -x /opt/homebrew/bin/bun ]; then
        (cd "$mcp_cs_dir" && /opt/homebrew/bin/bun install --silent 2>/dev/null) && \
            hook_log "code-search node_modules installed" || true
    fi
fi

if [ "$local_sha" = "$remote_sha" ]; then
    write_status "Auto-Sync: Alle Dateien aktuell."
    exit 0
fi

# Updates available!
behind=$(git rev-list --count "HEAD..@{u}" 2>/dev/null)
write_status "Auto-Sync: $behind neue Commits auf GitHub gefunden -- aktualisiere..."

# Preview: Show what's coming before pulling (ported from Codex session-start-sync)
diff_stat=$(git diff --stat "HEAD..@{u}" 2>/dev/null)
if [ -n "$diff_stat" ]; then
    write_status "Auto-Sync: Eingehende Aenderungen:"
    write_status "$diff_stat"
fi

# Stash local changes if working tree is dirty (so rebase can proceed)
dirty=$(git status --porcelain 2>/dev/null)
stashed=false
if [ -n "$dirty" ]; then
    if git stash --include-untracked -m "auto-sync-stash" 2>/dev/null; then
        stashed=true
        write_status "Auto-Sync: Lokale Aenderungen temporaer gesichert (git stash)."
    fi
fi

# Pull with rebase
if ! git pull --rebase --quiet 2>/dev/null; then
    # Restore stash if pull failed
    if [ "$stashed" = true ]; then
        git stash pop 2>/dev/null || true
    fi
    hook_log_error "git pull --rebase failed — merge conflict?"
    entry="### $(date '+%Y-%m-%d %H:%M') — Hook: auto-sync.sh — git pull --rebase fehlgeschlagen (Merge-Konflikt?) — Status: OFFEN"
    insert_whiteboard_entry "Offene Fehler & Probleme" "$entry"
    write_status "Auto-Sync: FEHLER beim Pull (Merge-Konflikt?). Bitte manuell pruefen: cd ~/proggs; git status"
    exit 1
fi

# Restore stashed changes
if [ "$stashed" = true ]; then
    if ! git stash pop 2>/dev/null; then
        hook_log_warn "stash pop had conflicts — continuing anyway"
        write_status "Auto-Sync: WARNUNG -- Stash-Restore hatte Konflikte. Bitte manuell pruefen: cd ~/proggs; git stash show"
        # Don't fail the hook — stash conflicts are non-fatal, the user can resolve later
    else
        write_status "Auto-Sync: Lokale Aenderungen wiederhergestellt."
    fi
fi

write_status "Auto-Sync: Git Pull erfolgreich."

# C2 (ported from Gemini): Write cross-CLI delta summary to whiteboard after pull
codex_changes=$(git log --oneline "$local_sha..$remote_sha" -- codex-setup/ 2>/dev/null | wc -l | tr -d ' ')
gemini_changes=$(git log --oneline "$local_sha..$remote_sha" -- Gemini-Setup/ 2>/dev/null | wc -l | tr -d ' ')
if [ "$codex_changes" -gt 0 ] 2>/dev/null || [ "$gemini_changes" -gt 0 ] 2>/dev/null; then
    cli_parts=""
    [ "$codex_changes" -gt 0 ] 2>/dev/null && cli_parts="Codex($codex_changes)"
    [ "$gemini_changes" -gt 0 ] 2>/dev/null && cli_parts="$cli_parts Gemini($gemini_changes)"
    cli_parts=$(echo "$cli_parts" | sed 's/^ //' | sed 's/ /, /')
    ts=$(date '+%Y-%m-%d %H:%M')
    source "$SCRIPT_DIR/whiteboard-insert.sh" 2>/dev/null || true
    if command -v replace_whiteboard_entry &>/dev/null; then
        replace_whiteboard_entry "Forschung & Intelligence" "Cross-CLI Delta" "- **[$ts] Cross-CLI Delta:** $cli_parts neue Commits — Bruecke starten fuer Details."
        write_status "Auto-Sync: Cross-CLI Delta erkannt: $cli_parts"
    fi
fi

# --- Sync config files from setup backup to active Claude Code config ---

synced=""

# Rules
rules_dir="$SETUP_DIR/rules"
if [ -d "$rules_dir" ]; then
    mkdir -p "$CLAUDE_DIR/rules"
    rules_count=$(find "$rules_dir" -maxdepth 1 -name "*.md" | wc -l | tr -d ' ')
    if [ "$rules_count" -gt 0 ]; then
        cp "$rules_dir"/*.md "$CLAUDE_DIR/rules/" 2>/dev/null || true
        synced="$synced Rules($rules_count)"
    fi
fi

# Agents
agents_dir="$SETUP_DIR/agents"
if [ -d "$agents_dir" ]; then
    mkdir -p "$CLAUDE_DIR/agents"
    agents_count=$(find "$agents_dir" -maxdepth 1 -name "*.md" | wc -l | tr -d ' ')
    if [ "$agents_count" -gt 0 ]; then
        cp "$agents_dir"/*.md "$CLAUDE_DIR/agents/" 2>/dev/null || true
        synced="$synced Agents($agents_count)"
    fi
fi

# Commands (including subdirectories like self-improve-ref/)
commands_dir="$SETUP_DIR/commands"
if [ -d "$commands_dir" ]; then
    dest_commands="$CLAUDE_DIR/commands"
    mkdir -p "$dest_commands"
    # Copy top-level .md files
    cmd_count=$(find "$commands_dir" -maxdepth 1 -name "*.md" | wc -l | tr -d ' ')
    if [ "$cmd_count" -gt 0 ]; then
        cp "$commands_dir"/*.md "$dest_commands/" 2>/dev/null || true
    fi
    # Copy subdirectories recursively (e.g. self-improve-ref/)
    subdirs_count=0
    while IFS= read -r -d '' subdir; do
        cp -r "$subdir" "$dest_commands/" 2>/dev/null || true
        subdirs_count=$((subdirs_count + 1))
    done < <(find "$commands_dir" -mindepth 1 -maxdepth 1 -type d -print0)
    total_cmd=$((cmd_count + subdirs_count))
    if [ "$total_cmd" -gt 0 ]; then
        synced="$synced Commands(${cmd_count}+${subdirs_count} dirs)"
    fi
fi

# Hooks (PowerShell + TypeScript + Bash scripts + subdirectories)
# Content-based sync: only update hooks that actually changed in incoming commits.
# Preserves local modifications that haven't been pushed to the setup-repo yet.
hooks_dir="$SETUP_DIR/hooks"
if [ -d "$hooks_dir" ]; then
    dest_hooks="$CLAUDE_DIR/hooks"
    mkdir -p "$dest_hooks"

    # Build list of hooks that actually changed in the incoming commits
    changed_hooks_file=$(mktemp)
    git diff --name-only "$local_sha" "$remote_sha" -- claude-code-setup/hooks/ 2>/dev/null | \
        while IFS= read -r f; do basename "$f"; done > "$changed_hooks_file" 2>/dev/null

    hook_copied=0
    hook_preserved=0
    preserved_names=""

    while IFS= read -r repo_hook; do
        hook_name=$(basename "$repo_hook")
        local_hook="$dest_hooks/$hook_name"

        if [ ! -f "$local_hook" ]; then
            # New hook — always copy
            cp "$repo_hook" "$local_hook"
            hook_copied=$((hook_copied + 1))
        elif diff -q "$repo_hook" "$local_hook" > /dev/null 2>&1; then
            # Content identical — already in sync, skip
            :
        elif grep -qxF "$hook_name" "$changed_hooks_file" 2>/dev/null; then
            # Content differs AND hook changed in incoming commits — update from repo
            cp "$repo_hook" "$local_hook"
            hook_copied=$((hook_copied + 1))
            hook_log "Hook updated from repo: $hook_name"
        else
            # Content differs but hook was NOT changed in pull — preserve local version
            hook_preserved=$((hook_preserved + 1))
            preserved_names="$preserved_names $hook_name"
        fi
    done < <(find "$hooks_dir" -maxdepth 1 \( -name "*.ps1" -o -name "*.ts" -o -name "*.sh" \) -print)

    rm -f "$changed_hooks_file"

    # Make .sh scripts executable after copying
    find "$dest_hooks" -name "*.sh" -exec chmod +x {} \; 2>/dev/null || true

    # Copy hook subdirectories (e.g. prompt-injection-defender/)
    hook_subdirs_count=0
    while IFS= read -r -d '' subdir; do
        cp -r "$subdir" "$dest_hooks/" 2>/dev/null || true
        hook_subdirs_count=$((hook_subdirs_count + 1))
    done < <(find "$hooks_dir" -mindepth 1 -maxdepth 1 -type d -print0)

    total_hooks=$(find "$hooks_dir" -maxdepth 1 \( -name "*.ps1" -o -name "*.ts" -o -name "*.sh" \) | wc -l | tr -d ' ')
    if [ "$hook_copied" -gt 0 ] || [ "$hook_subdirs_count" -gt 0 ]; then
        synced="$synced Hooks(${hook_copied}upd/${total_hooks}total+${hook_subdirs_count}dirs)"
    else
        synced="$synced Hooks(0upd/${total_hooks}total+${hook_subdirs_count}dirs)"
    fi

    if [ "$hook_preserved" -gt 0 ]; then
        hook_log "Preserved $hook_preserved local hook(s) with uncommitted changes:$preserved_names"
        write_status "Auto-Sync: $hook_preserved Hook(s) mit lokalen Aenderungen beibehalten (commit+push to sync):$preserved_names"
    fi
fi

# CLAUDE.md — use the REPO version (~/proggs/CLAUDE.md) as authoritative source,
# NOT the setup backup (which may be older if another platform pushed changes).
# The repo copy is always pulled fresh by git pull above.
repo_claude="$REPO_DIR/CLAUDE.md"
if [ -f "$repo_claude" ]; then
    cp "$repo_claude" "$HOME/CLAUDE.md"
    synced="$synced CLAUDE.md(from-repo)"
fi

# Skills
skills_dir="$SETUP_DIR/skills"
if [ -d "$skills_dir" ]; then
    dest_skills="$CLAUDE_DIR/skills"
    mkdir -p "$dest_skills"
    # Copy skill directories recursively (each skill is a folder with SKILL.md)
    skill_subdirs=0
    while IFS= read -r -d '' subdir; do
        cp -r "$subdir" "$dest_skills/" 2>/dev/null || true
        skill_subdirs=$((skill_subdirs + 1))
    done < <(find "$skills_dir" -mindepth 1 -maxdepth 1 -type d -print0)
    # Copy top-level skill files
    skill_files=$(find "$skills_dir" -maxdepth 1 -name "*.md" | wc -l | tr -d ' ')
    if [ "$skill_files" -gt 0 ]; then
        cp "$skills_dir"/*.md "$dest_skills/" 2>/dev/null || true
    fi
    total_skills=$((skill_subdirs + skill_files))
    if [ "$total_skills" -gt 0 ]; then
        synced="$synced Skills($total_skills)"
    fi
fi

# .mcp.json — platform-specific sync after pull (macOS from mcp-macos.json)
mcp_source="$SETUP_DIR/mcp-macos.json"
mcp_dest="$REPO_DIR/.mcp.json"
if [ -f "$mcp_source" ]; then
    if ! diff -q "$mcp_source" "$mcp_dest" > /dev/null 2>&1; then
        cp "$mcp_source" "$mcp_dest"
        synced="$synced .mcp.json(macos)"
    fi
fi

# .gitignore_global
gitignore="$SETUP_DIR/.gitignore_global"
if [ -f "$gitignore" ]; then
    cp "$gitignore" "$HOME/.gitignore_global"
    synced="$synced .gitignore"
fi

# GitHub Personal Access Token — auto-refresh from gh CLI
# Writes to settings.local.json (NOT settings.json — that gets committed to the repo)
gh_token=$(gh auth token 2>/dev/null)
if [ -n "$gh_token" ]; then
    local_settings="$CLAUDE_DIR/settings.local.json"
    if [ -f "$local_settings" ]; then
        current_token=$(python3 -c "
import json, sys
try:
    d = json.load(open('$local_settings', encoding='utf-8'))
    print(d.get('env', {}).get('GITHUB_PERSONAL_ACCESS_TOKEN', ''))
except: pass
" 2>/dev/null)
        if [ "$current_token" != "$gh_token" ]; then
            python3 -c "
import json, tempfile, os
path = '$local_settings'
d = json.load(open(path, encoding='utf-8'))
if 'env' not in d: d['env'] = {}
d['env']['GITHUB_PERSONAL_ACCESS_TOKEN'] = '$gh_token'
tmp = path + '.tmp'
with open(tmp, 'w', encoding='utf-8') as f:
    json.dump(d, f, indent=2, ensure_ascii=False)
    f.write('\n')
os.replace(tmp, path)
" 2>/dev/null && synced="$synced GH-Token(refreshed)" && hook_log "GitHub PAT refreshed"
        fi
    fi
fi

hook_log "sync complete:$synced"
write_status "Auto-Sync: Lokale Konfiguration aktualisiert:$synced"
write_status "Auto-Sync: Hinweis -- CLAUDE.md und Rules werden erst nach Neustart von Claude Code wirksam."
