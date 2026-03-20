# Auto-Sync: Syncs Claude Code config from GitHub on every session start
# Runs as SessionStart hook
# stdout → AI context (system-reminder), stderr → user terminal
# Platform: Windows (PowerShell 7+)

. "$PSScriptRoot/hook-log.ps1"
. "$PSScriptRoot/whiteboard-insert.ps1"

# Write to both stdout (AI context) and stderr (user-visible terminal)
function Write-Status {
    param([string]$Message)
    Write-Output $Message
    [Console]::Error.WriteLine($Message)
}

$RepoDir = Join-Path $env:USERPROFILE "proggs"
$SetupDir = Join-Path $RepoDir "claude-code-setup"
$ClaudeDir = Join-Path $env:USERPROFILE ".claude"

# Check if repo exists
if (-not (Test-Path (Join-Path $RepoDir ".git"))) {
    Hook-LogWarn "~/proggs repo not found — skipped"
    Write-Status "Auto-Sync: ~/proggs Repo nicht gefunden -- uebersprungen."
    exit 0
}

Set-Location $RepoDir

# Fetch latest from remote
$null = git fetch --quiet 2>&1
if ($LASTEXITCODE -ne 0) {
    Hook-LogWarn "git fetch failed — no internet?"
    Write-Status "Auto-Sync: Keine Internetverbindung -- uebersprungen."
    exit 0
}

# Compare local vs remote
$local = git rev-parse HEAD 2>$null
$remote = git rev-parse '@{u}' 2>$null

if ($local -eq $remote) {
    Write-Status "Auto-Sync: Alle Dateien aktuell."
    exit 0
}

# Updates available!
$behind = git rev-list --count "HEAD..@{u}" 2>$null
Write-Status "Auto-Sync: $behind neue Commits auf GitHub gefunden -- aktualisiere..."

# Stash local changes if working tree is dirty (so rebase can proceed)
$dirty = git status --porcelain 2>$null
$stashed = $false
if ($dirty) {
    $null = git stash --include-untracked -m "auto-sync-stash" 2>&1
    if ($LASTEXITCODE -eq 0) {
        $stashed = $true
        Write-Status "Auto-Sync: Lokale Aenderungen temporaer gesichert (git stash)."
    }
}

# Pull with rebase
$null = git pull --rebase --quiet 2>&1
if ($LASTEXITCODE -ne 0) {
    # Restore stash if pull failed
    if ($stashed) { $null = git stash pop 2>&1 }
    Hook-LogError "git pull --rebase failed — merge conflict?"
    $entry = "### $(Get-Date -Format 'yyyy-MM-dd HH:mm') — Hook: auto-sync.ps1 — git pull --rebase fehlgeschlagen (Merge-Konflikt?) — Status: OFFEN"
    Insert-WhiteboardEntry -Section "Offene Fehler & Probleme" -Entry $entry
    Write-Status "Auto-Sync: FEHLER beim Pull (Merge-Konflikt?). Bitte manuell pruefen: cd ~/proggs; git status"
    exit 1
}

# Restore stashed changes
if ($stashed) {
    $null = git stash pop 2>&1
    if ($LASTEXITCODE -ne 0) {
        Hook-LogWarn "stash pop had conflicts"
        Write-Status "Auto-Sync: WARNUNG -- Stash-Restore hatte Konflikte. Bitte manuell pruefen: cd ~/proggs; git stash show"
    } else {
        Write-Status "Auto-Sync: Lokale Aenderungen wiederhergestellt."
    }
}

Write-Status "Auto-Sync: Git Pull erfolgreich."

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

# Commands (including subdirectories like self-improve-ref/)
$commandsDir = Join-Path $SetupDir "commands"
if (Test-Path $commandsDir) {
    $destCommands = Join-Path $ClaudeDir "commands"
    New-Item -ItemType Directory -Force -Path $destCommands | Out-Null
    # Copy top-level .md files
    $commands = @(Get-ChildItem "$commandsDir\*.md" -ErrorAction SilentlyContinue)
    if ($commands.Count -gt 0) {
        Copy-Item "$commandsDir\*.md" $destCommands -Force
    }
    # Copy subdirectories recursively (e.g. self-improve-ref/)
    $subdirs = @(Get-ChildItem $commandsDir -Directory -ErrorAction SilentlyContinue)
    foreach ($subdir in $subdirs) {
        Copy-Item $subdir.FullName $destCommands -Recurse -Force
    }
    $totalCount = $commands.Count + $subdirs.Count
    if ($totalCount -gt 0) {
        $cmdLabel = "Commands($($commands.Count)+$($subdirs.Count) dirs)"
        $synced += $cmdLabel
    }
}

# Hooks (PowerShell + TypeScript + Bash scripts + subdirectories)
$hooksDir = Join-Path $SetupDir "hooks"
if (Test-Path $hooksDir) {
    $destHooks = Join-Path $ClaudeDir "hooks"
    New-Item -ItemType Directory -Force -Path $destHooks | Out-Null
    $ps1Hooks = @(Get-ChildItem "$hooksDir\*.ps1" -ErrorAction SilentlyContinue)
    $tsHooks = @(Get-ChildItem "$hooksDir\*.ts" -ErrorAction SilentlyContinue)
    $shHooks = @(Get-ChildItem "$hooksDir\*.sh" -ErrorAction SilentlyContinue)
    foreach ($hook in ($ps1Hooks + $tsHooks + $shHooks)) {
        Copy-Item $hook.FullName $destHooks -Force
    }
    # Copy hook subdirectories (e.g. prompt-injection-defender/)
    $hookSubdirs = @(Get-ChildItem $hooksDir -Directory -ErrorAction SilentlyContinue)
    foreach ($subdir in $hookSubdirs) {
        Copy-Item $subdir.FullName $destHooks -Recurse -Force
    }
    $hookCount = $ps1Hooks.Count + $tsHooks.Count + $shHooks.Count
    if ($hookCount -gt 0 -or $hookSubdirs.Count -gt 0) {
        $synced += "Hooks($hookCount+$($hookSubdirs.Count) dirs)"
    }
}

# CLAUDE.md — use the REPO version (~/proggs/CLAUDE.md) as authoritative source,
# NOT the setup backup (which may be older if another platform pushed changes).
# The repo copy is always pulled fresh by git pull above.
$repoClaude = Join-Path $RepoDir "CLAUDE.md"
if (Test-Path $repoClaude) {
    Copy-Item $repoClaude (Join-Path $env:USERPROFILE "CLAUDE.md") -Force
    $synced += "CLAUDE.md(from-repo)"
}

# Skills
$skillsDir = Join-Path $SetupDir "skills"
if (Test-Path $skillsDir) {
    $destSkills = Join-Path $ClaudeDir "skills"
    New-Item -ItemType Directory -Force -Path $destSkills | Out-Null
    # Copy skill directories recursively (each skill is a folder with SKILL.md)
    $skillSubdirs = @(Get-ChildItem $skillsDir -Directory -ErrorAction SilentlyContinue)
    foreach ($subdir in $skillSubdirs) {
        Copy-Item $subdir.FullName $destSkills -Recurse -Force
    }
    # Copy top-level skill files
    $skillFiles = @(Get-ChildItem "$skillsDir\*.md" -ErrorAction SilentlyContinue)
    if ($skillFiles.Count -gt 0) {
        Copy-Item "$skillsDir\*.md" $destSkills -Force
    }
    $totalSkills = $skillSubdirs.Count + $skillFiles.Count
    if ($totalSkills -gt 0) {
        $synced += "Skills($totalSkills)"
    }
}

# .gitignore_global
$gitignore = Join-Path $SetupDir ".gitignore_global"
if (Test-Path $gitignore) {
    Copy-Item $gitignore (Join-Path $env:USERPROFILE ".gitignore_global") -Force
    $synced += ".gitignore"
}

$syncedStr = $synced -join " "
Hook-Log "sync complete: $syncedStr"
Write-Status "Auto-Sync: Lokale Konfiguration aktualisiert: $syncedStr"
Write-Status "Auto-Sync: Hinweis -- CLAUDE.md und Rules werden erst nach Neustart von Claude Code wirksam."
