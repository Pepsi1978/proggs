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

if [ "$local_sha" = "$remote_sha" ]; then
    write_status "Auto-Sync: Alle Dateien aktuell."
    exit 0
fi

# Updates available!
behind=$(git rev-list --count "HEAD..@{u}" 2>/dev/null)
write_status "Auto-Sync: $behind neue Commits auf GitHub gefunden -- aktualisiere..."

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

# Ensure clean exit regardless of stash result
exit 0 2>/dev/null || true

write_status "Auto-Sync: Git Pull erfolgreich."

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
hooks_dir="$SETUP_DIR/hooks"
if [ -d "$hooks_dir" ]; then
    dest_hooks="$CLAUDE_DIR/hooks"
    mkdir -p "$dest_hooks"
    ps1_count=$(find "$hooks_dir" -maxdepth 1 -name "*.ps1" | wc -l | tr -d ' ')
    ts_count=$(find "$hooks_dir" -maxdepth 1 -name "*.ts" | wc -l | tr -d ' ')
    sh_count=$(find "$hooks_dir" -maxdepth 1 -name "*.sh" | wc -l | tr -d ' ')
    # Copy all script files
    find "$hooks_dir" -maxdepth 1 \( -name "*.ps1" -o -name "*.ts" -o -name "*.sh" \) -exec cp {} "$dest_hooks/" \; 2>/dev/null || true
    # Make .sh scripts executable after copying
    find "$dest_hooks" -name "*.sh" -exec chmod +x {} \; 2>/dev/null || true
    # Copy hook subdirectories (e.g. prompt-injection-defender/)
    hook_subdirs_count=0
    while IFS= read -r -d '' subdir; do
        cp -r "$subdir" "$dest_hooks/" 2>/dev/null || true
        hook_subdirs_count=$((hook_subdirs_count + 1))
    done < <(find "$hooks_dir" -mindepth 1 -maxdepth 1 -type d -print0)
    hook_count=$((ps1_count + ts_count + sh_count))
    if [ "$hook_count" -gt 0 ] || [ "$hook_subdirs_count" -gt 0 ]; then
        synced="$synced Hooks(${hook_count}+${hook_subdirs_count} dirs)"
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

# .gitignore_global
gitignore="$SETUP_DIR/.gitignore_global"
if [ -f "$gitignore" ]; then
    cp "$gitignore" "$HOME/.gitignore_global"
    synced="$synced .gitignore"
fi

hook_log "sync complete:$synced"
write_status "Auto-Sync: Lokale Konfiguration aktualisiert:$synced"
write_status "Auto-Sync: Hinweis -- CLAUDE.md und Rules werden erst nach Neustart von Claude Code wirksam."
