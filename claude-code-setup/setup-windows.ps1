# Claude Code Setup - Windows
# Copies all configuration files to the correct locations

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ClaudeDir = "$env:USERPROFILE\.claude"

Write-Host "=== Claude Code Setup (Windows) ===" -ForegroundColor Cyan

# Create directories
New-Item -ItemType Directory -Force -Path "$ClaudeDir\rules" | Out-Null
New-Item -ItemType Directory -Force -Path "$ClaudeDir\agents" | Out-Null
New-Item -ItemType Directory -Force -Path "$ClaudeDir\commands" | Out-Null
New-Item -ItemType Directory -Force -Path "$ClaudeDir\hooks" | Out-Null

# Copy configs
Copy-Item "$ScriptDir\settings.json" "$ClaudeDir\settings.json" -Force
Copy-Item "$ScriptDir\rules\*.md" "$ClaudeDir\rules\" -Force
Copy-Item "$ScriptDir\agents\*.md" "$ClaudeDir\agents\" -Force
Copy-Item "$ScriptDir\commands\*.md" "$ClaudeDir\commands\" -Force
# Copy ALL hooks (not just specific ones — all .ps1, .sh, .ts, and subdirectories)
Copy-Item "$ScriptDir\hooks\*.ps1" "$ClaudeDir\hooks\" -Force
Copy-Item "$ScriptDir\hooks\*.sh" "$ClaudeDir\hooks\" -Force
Copy-Item "$ScriptDir\hooks\*.ts" "$ClaudeDir\hooks\" -Force -ErrorAction SilentlyContinue
if (Test-Path "$ScriptDir\hooks\prompt-injection-defender") {
    New-Item -ItemType Directory -Force -Path "$ClaudeDir\hooks\prompt-injection-defender" | Out-Null
    Copy-Item "$ScriptDir\hooks\prompt-injection-defender\*" "$ClaudeDir\hooks\prompt-injection-defender\" -Force -Recurse
}
$hookCount = (Get-ChildItem "$ClaudeDir\hooks\*.ps1" -ErrorAction SilentlyContinue).Count
Write-Host "Hooks copied ($hookCount .ps1 files)" -ForegroundColor Green

Copy-Item "$ScriptDir\CLAUDE.md" "$env:USERPROFILE\CLAUDE.md" -Force
Copy-Item "$ScriptDir\.gitignore_global" "$env:USERPROFILE\.gitignore_global" -Force

# Copy command subdirectories (self-improve-ref/, tool-check-ref/)
$cmdSubDirs = Get-ChildItem "$ScriptDir\commands" -Directory -ErrorAction SilentlyContinue
foreach ($dir in $cmdSubDirs) {
    $targetDir = "$ClaudeDir\commands\$($dir.Name)"
    New-Item -ItemType Directory -Force -Path $targetDir | Out-Null
    Copy-Item "$($dir.FullName)\*" $targetDir -Force -Recurse
}

# Install pre-push git hook (auto fetch+rebase before every push)
# This hook is NOT tracked by git — it must be installed after every clone
$repoDir = Split-Path -Parent $ScriptDir
$gitHooksDir = Join-Path $repoDir ".git\hooks"
if (Test-Path $gitHooksDir) {
    $prePushSrc = Join-Path $ScriptDir "hooks\pre-commit"  # Template reference
    $prePushDst = Join-Path $gitHooksDir "pre-push"
    $prePushContent = @'
#!/bin/sh
# Pre-push hook: automatic fetch + rebase BEFORE push
# Prevents push rejections when other CLIs (Codex, Gemini, macOS) pushed in the meantime
# Installed by setup-windows.ps1 — reinstall after every clone

REMOTE="$1"
if [ "$REMOTE" = "origin" ] || [ -z "$REMOTE" ]; then
    if git fetch origin 2>/dev/null; then
        REMOTE_MAIN=$(git rev-parse origin/main 2>/dev/null) || true
        BASE=$(git merge-base HEAD origin/main 2>/dev/null) || true
        if [ -n "$REMOTE_MAIN" ] && [ -n "$BASE" ] && [ "$BASE" != "$REMOTE_MAIN" ]; then
            echo "[pre-push] Behind origin/main — rebasing..."
            DIRTY=0
            if ! git diff --quiet 2>/dev/null || ! git diff --cached --quiet 2>/dev/null; then
                git stash --quiet 2>/dev/null
                DIRTY=1
            fi
            if git rebase origin/main --quiet 2>/dev/null; then
                echo "[pre-push] Rebase successful."
            else
                echo "[pre-push] ERROR: Rebase conflict! Push aborted."
                echo "[pre-push] Fix manually: git rebase --abort && git fetch origin && git rebase origin/main"
                git rebase --abort 2>/dev/null
                [ "$DIRTY" -eq 1 ] && git stash pop --quiet 2>/dev/null
                exit 1
            fi
            [ "$DIRTY" -eq 1 ] && git stash pop --quiet 2>/dev/null
        fi
    fi
fi
# Git LFS pre-push check
command -v git-lfs >/dev/null 2>&1 || { printf >&2 "\n%s\n\n" "This repository is configured for Git LFS but 'git-lfs' was not found on your path."; exit 2; }
git lfs pre-push "$@"
'@
    $prePushContent | Set-Content $prePushDst -Encoding UTF8 -NoNewline
    Write-Host "Git pre-push hook installed (auto fetch+rebase before push)" -ForegroundColor Green
} else {
    Write-Host "Warning: .git/hooks not found — pre-push hook not installed" -ForegroundColor Yellow
}

# Copy custom skills
$skillDirs = Get-ChildItem "$ScriptDir\skills" -Directory -ErrorAction SilentlyContinue
foreach ($skill in $skillDirs) {
    $targetDir = "$ClaudeDir\skills\$($skill.Name)"
    New-Item -ItemType Directory -Force -Path $targetDir | Out-Null
    Copy-Item "$($skill.FullName)\*" $targetDir -Force -Recurse
}
if ($skillDirs) { Write-Host "Custom skills copied ($($skillDirs.Count) skills)" -ForegroundColor Green }

# Add Windows-specific hooks to settings.json
$settings = Get-Content "$ClaudeDir\settings.json" -Raw | ConvertFrom-Json
$winHooks = Get-Content "$ScriptDir\hooks-windows.json" -Raw | ConvertFrom-Json
$settings | Add-Member -MemberType NoteProperty -Name "hooks" -Value $winHooks.hooks -Force
$settings | ConvertTo-Json -Depth 10 | Set-Content "$ClaudeDir\settings.json" -Encoding UTF8

Write-Host "Hooks (Windows) added to settings.json" -ForegroundColor Green

# Git config
git config --global init.defaultBranch main
git config --global pull.rebase true
git config --global push.autoSetupRemote true
git config --global core.excludesFile "$env:USERPROFILE\.gitignore_global"
git config --global core.autocrlf true
git config --global core.longpaths true
git config --global rerere.enabled true
git config --global fetch.prune true
git config --global diff.algorithm histogram
git config --global merge.conflictstyle zdiff3
git config --global alias.wt "worktree add"
git config --global alias.wtl "worktree list"
git config --global alias.wtr "worktree remove"

Write-Host ""
Write-Host "=== Done! ===" -ForegroundColor Cyan
Write-Host "Next steps:"
Write-Host "  1. Install .NET SDK: winget install Microsoft.DotNet.SDK.10"
Write-Host "  2. Install Rust: winget install Rustlang.Rustup"
Write-Host "  3. Install Go: winget install GoLang.Go"
Write-Host "  4. Install Node: winget install OpenJS.NodeJS"
Write-Host "  5. Run Claude Code and type /self-improve for a full environment check"
