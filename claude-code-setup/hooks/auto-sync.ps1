# Auto-Sync: Syncs Claude Code config from GitHub on every session start
# Runs as SessionStart hook — output is visible to the user
# Platform: Windows (PowerShell)

$RepoDir = Join-Path $env:USERPROFILE "proggs"
$SetupDir = Join-Path $RepoDir "claude-code-setup"
$ClaudeDir = Join-Path $env:USERPROFILE ".claude"

# Check if repo exists
if (-not (Test-Path (Join-Path $RepoDir ".git"))) {
    Write-Output "Auto-Sync: ~/proggs Repo nicht gefunden — uebersprungen."
    exit 0
}

Set-Location $RepoDir

# Fetch latest from remote
$null = git fetch --quiet 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Output "Auto-Sync: Keine Internetverbindung — uebersprungen."
    exit 0
}

# Compare local vs remote
$local = git rev-parse HEAD 2>$null
$remote = git rev-parse '@{u}' 2>$null

if ($local -eq $remote) {
    Write-Output "Auto-Sync: Alle Dateien aktuell."
    exit 0
}

# Updates available!
$behind = git rev-list --count "HEAD..@{u}" 2>$null
Write-Output "Auto-Sync: $behind neue Commits auf GitHub gefunden — aktualisiere..."

# Stash local changes if working tree is dirty (so rebase can proceed)
$dirty = git status --porcelain 2>$null
$stashed = $false
if ($dirty) {
    $null = git stash --include-untracked -m "auto-sync-stash" 2>&1
    if ($LASTEXITCODE -eq 0) {
        $stashed = $true
        Write-Output "Auto-Sync: Lokale Aenderungen temporaer gesichert (git stash)."
    }
}

# Pull with rebase
$null = git pull --rebase --quiet 2>&1
if ($LASTEXITCODE -ne 0) {
    # Restore stash if pull failed
    if ($stashed) { $null = git stash pop 2>&1 }
    Write-Output "Auto-Sync: FEHLER beim Pull (Merge-Konflikt?). Bitte manuell pruefen: cd ~/proggs; git status"
    exit 1
}

# Restore stashed changes
if ($stashed) {
    $null = git stash pop 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Output "Auto-Sync: WARNUNG — Stash-Restore hatte Konflikte. Bitte manuell pruefen: cd ~/proggs; git stash show"
    } else {
        Write-Output "Auto-Sync: Lokale Aenderungen wiederhergestellt."
    }
}

Write-Output "Auto-Sync: Git Pull erfolgreich."

# --- Sync config files from setup backup to active Claude Code config ---

$synced = @()

# Rules
$rulesDir = Join-Path $SetupDir "rules"
if (Test-Path $rulesDir) {
    New-Item -ItemType Directory -Force -Path (Join-Path $ClaudeDir "rules") | Out-Null
    $rules = @(Get-ChildItem "$rulesDir\*.md" -ErrorAction SilentlyContinue)
    if ($rules.Count -gt 0) {
        Copy-Item "$rulesDir\*.md" (Join-Path $ClaudeDir "rules") -Force
        $synced += "Rules($($rules.Count))"
    }
}

# Agents
$agentsDir = Join-Path $SetupDir "agents"
if (Test-Path $agentsDir) {
    New-Item -ItemType Directory -Force -Path (Join-Path $ClaudeDir "agents") | Out-Null
    $agents = @(Get-ChildItem "$agentsDir\*.md" -ErrorAction SilentlyContinue)
    if ($agents.Count -gt 0) {
        Copy-Item "$agentsDir\*.md" (Join-Path $ClaudeDir "agents") -Force
        $synced += "Agents($($agents.Count))"
    }
}

# Commands
$commandsDir = Join-Path $SetupDir "commands"
if (Test-Path $commandsDir) {
    New-Item -ItemType Directory -Force -Path (Join-Path $ClaudeDir "commands") | Out-Null
    $commands = @(Get-ChildItem "$commandsDir\*.md" -ErrorAction SilentlyContinue)
    if ($commands.Count -gt 0) {
        Copy-Item "$commandsDir\*.md" (Join-Path $ClaudeDir "commands") -Force
        $synced += "Commands($($commands.Count))"
    }
}

# Hooks (PowerShell scripts only)
$hooksDir = Join-Path $SetupDir "hooks"
if (Test-Path $hooksDir) {
    New-Item -ItemType Directory -Force -Path (Join-Path $ClaudeDir "hooks") | Out-Null
    $hooks = @(Get-ChildItem "$hooksDir\*.ps1" -ErrorAction SilentlyContinue)
    if ($hooks.Count -gt 0) {
        foreach ($hook in $hooks) {
            Copy-Item $hook.FullName (Join-Path $ClaudeDir "hooks") -Force
        }
        $synced += "Hooks($($hooks.Count))"
    }
}

# CLAUDE.md
$claudeMd = Join-Path $SetupDir "CLAUDE.md"
if (Test-Path $claudeMd) {
    Copy-Item $claudeMd (Join-Path $env:USERPROFILE "CLAUDE.md") -Force
    Copy-Item $claudeMd (Join-Path $RepoDir "CLAUDE.md") -Force
    $synced += "CLAUDE.md"
}

# .gitignore_global
$gitignore = Join-Path $SetupDir ".gitignore_global"
if (Test-Path $gitignore) {
    Copy-Item $gitignore (Join-Path $env:USERPROFILE ".gitignore_global") -Force
    $synced += ".gitignore"
}

$syncedStr = $synced -join " "
Write-Output "Auto-Sync: Lokale Konfiguration aktualisiert: $syncedStr"
Write-Output "Auto-Sync: Hinweis — CLAUDE.md und Rules werden erst nach Neustart von Claude Code wirksam."
