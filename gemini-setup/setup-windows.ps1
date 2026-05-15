# Gemini CLI Setup - Windows
# Copies all configuration files to the correct locations

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$GeminiDir = "$env:USERPROFILE\.gemini"

Write-Host "=== Gemini CLI Setup (Windows) ===" -ForegroundColor Cyan

# Create directories
New-Item -ItemType Directory -Force -Path "$GeminiDir\rules" | Out-Null
New-Item -ItemType Directory -Force -Path "$GeminiDir\agents" | Out-Null
New-Item -ItemType Directory -Force -Path "$GeminiDir\commands" | Out-Null
New-Item -ItemType Directory -Force -Path "$GeminiDir\hooks" | Out-Null

# Copy configs
if (Test-Path "$ScriptDir\settings.json") {
    Copy-Item "$ScriptDir\settings.json" "$GeminiDir\settings.json" -Force
}
Copy-Item "$ScriptDir\rules\*.md" "$GeminiDir\rules\" -Force
Copy-Item "$ScriptDir\agents\*.md" "$GeminiDir\agents\" -Force
Copy-Item "$ScriptDir\commands\*.md" "$GeminiDir\commands\" -Force
# Copy ALL hooks
Copy-Item "$ScriptDir\hooks\*.ps1" "$GeminiDir\hooks\" -Force
Copy-Item "$ScriptDir\hooks\*.sh" "$GeminiDir\hooks\" -Force
Copy-Item "$ScriptDir\hooks\*.ts" "$GeminiDir\hooks\" -Force -ErrorAction SilentlyContinue
if (Test-Path "$ScriptDir\hooks\prompt-injection-defender") {
    New-Item -ItemType Directory -Force -Path "$GeminiDir\hooks\prompt-injection-defender" | Out-Null
    Copy-Item "$ScriptDir\hooks\prompt-injection-defender\*" "$GeminiDir\hooks\prompt-injection-defender\" -Force -Recurse
}
$hookCount = (Get-ChildItem "$GeminiDir\hooks\*.ps1" -ErrorAction SilentlyContinue).Count
Write-Host "Hooks copied ($hookCount .ps1 files)" -ForegroundColor Green

if (Test-Path "$ScriptDir\GEMINI.md") {
    Copy-Item "$ScriptDir\GEMINI.md" "$env:USERPROFILE\GEMINI.md" -Force
}
if (Test-Path "$ScriptDir\.gitignore_global") {
    Copy-Item "$ScriptDir\.gitignore_global" "$env:USERPROFILE\.gitignore_global" -Force
}

# Copy command subdirectories
$cmdSubDirs = Get-ChildItem "$ScriptDir\commands" -Directory -ErrorAction SilentlyContinue
foreach ($dir in $cmdSubDirs) {
    $targetDir = "$GeminiDir\commands\$($dir.Name)"
    New-Item -ItemType Directory -Force -Path $targetDir | Out-Null
    Copy-Item "$($dir.FullName)\*" $targetDir -Force -Recurse
}

# Install pre-push git hook
$repoDir = Split-Path -Parent $ScriptDir
$gitHooksDir = Join-Path $repoDir ".git\hooks"
if (Test-Path $gitHooksDir) {
    $prePushDst = Join-Path $gitHooksDir "pre-push"
    $prePushContent = @'
#!/bin/sh
# Pre-push hook: automatic fetch + rebase BEFORE push
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
                git rebase --abort 2>/dev/null
                [ "$DIRTY" -eq 1 ] && git stash pop --quiet 2>/dev/null
                exit 1
            fi
            [ "$DIRTY" -eq 1 ] && git stash pop --quiet 2>/dev/null
        fi
    fi
fi
git lfs pre-push "$@"
'@
    $prePushContent | Set-Content $prePushDst -Encoding UTF8 -NoNewline
    Write-Host "Git pre-push hook installed" -ForegroundColor Green
}

# Copy custom skills
$skillDirs = Get-ChildItem "$ScriptDir\skills" -Directory -ErrorAction SilentlyContinue
foreach ($skill in $skillDirs) {
    $targetDir = "$GeminiDir\skills\$($skill.Name)"
    if (-not (Test-Path $targetDir)) { New-Item -ItemType Directory -Force -Path $targetDir | Out-Null }
    Copy-Item "$($skill.FullName)\*" $targetDir -Force -Recurse
}
if ($skillDirs) { Write-Host "Custom skills copied" -ForegroundColor Green }

# Add Windows-specific hooks to settings.json
if (Test-Path "$GeminiDir\settings.json") {
    $settingsContent = Get-Content "$GeminiDir\settings.json" -Raw
    if ($settingsContent.Trim() -ne "") {
        $settings = $settingsContent | ConvertFrom-Json
        if (Test-Path "$ScriptDir\hooks-windows.json") {
            $winHooks = Get-Content "$ScriptDir\hooks-windows.json" -Raw | ConvertFrom-Json
            $settings | Add-Member -MemberType NoteProperty -Name "hooks" -Value $winHooks.hooks -Force
            $settings | ConvertTo-Json -Depth 10 | Set-Content "$GeminiDir\settings.json" -Encoding UTF8
            Write-Host "Hooks (Windows) added to settings.json" -ForegroundColor Green
        }
    }
}

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

Write-Host ""
Write-Host "=== Done! ===" -ForegroundColor Cyan
Write-Host "Run Gemini CLI and type /self-improve for a full environment check"
