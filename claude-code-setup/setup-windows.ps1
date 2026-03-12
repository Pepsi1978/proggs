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
Copy-Item "$ScriptDir\hooks\auto-format.ps1" "$ClaudeDir\hooks\" -Force
Copy-Item "$ScriptDir\hooks\notify.ps1" "$ClaudeDir\hooks\" -Force
Copy-Item "$ScriptDir\CLAUDE.md" "$env:USERPROFILE\CLAUDE.md" -Force
Copy-Item "$ScriptDir\.gitignore_global" "$env:USERPROFILE\.gitignore_global" -Force

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
