# self-improve-sync.ps1
# Syncs local Claude Code configuration files to the proggs repo and pushes.
#
# Usage:
#   .\self-improve-sync.ps1 [[-CommitMessage] <string>]
#
# Default commit message: "Self-improve sync (PowerShell)"
#
# What this syncs:
#   - ~/.claude/rules/        -> ~/proggs/claude-code-setup/rules/
#   - ~/.claude/agents/       -> ~/proggs/claude-code-setup/agents/
#   - ~/.claude/commands/     -> ~/proggs/claude-code-setup/commands/
#   - ~/.claude/hooks/        -> ~/proggs/claude-code-setup/hooks/
#   - ~/.claude/skills/       -> ~/proggs/claude-code-setup/skills/
#   - ~/.claude/agent-memory/shared/MEMORY.md -> ~/proggs/claude-code-setup/agent-memory/shared/
#   - ~/CLAUDE.md             -> ~/proggs/CLAUDE.md

[CmdletBinding()]
param(
    [string]$CommitMessage = "Self-improve sync (PowerShell)"
)

$ErrorActionPreference = "Stop"

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------
$ProggsDir  = Join-Path $env:USERPROFILE "proggs"
$ClaudeDir  = Join-Path $env:USERPROFILE ".claude"
$SetupDir   = Join-Path $ProggsDir "claude-code-setup"
$ClaudeMd   = Join-Path $env:USERPROFILE "CLAUDE.md"

# ---------------------------------------------------------------------------
# Validation
# ---------------------------------------------------------------------------
if (-not (Test-Path (Join-Path $ProggsDir ".git"))) {
    Write-Error "$ProggsDir is not a git repository"
    exit 1
}

if (-not (Test-Path $ClaudeDir)) {
    Write-Error "$ClaudeDir does not exist"
    exit 1
}

Write-Host "==> Syncing Claude Code configuration to repo..." -ForegroundColor Cyan
Write-Host "    Source:  $ClaudeDir"
Write-Host "    Target:  $SetupDir"
Write-Host "    Repo:    $ProggsDir"
Write-Host ""

# ---------------------------------------------------------------------------
# Helper: safe copy with directory creation
# ---------------------------------------------------------------------------
function Copy-Glob {
    param(
        [string]$SourceDir,
        [string]$Pattern,
        [string]$DestDir
    )
    $null = New-Item -ItemType Directory -Force -Path $DestDir
    $files = @(Get-ChildItem -Path $SourceDir -Filter $Pattern -ErrorAction SilentlyContinue)
    if ($files.Count -gt 0) {
        $files | Copy-Item -Destination $DestDir -Force
        Write-Host "    Copied $($files.Count) file(s) ($Pattern)"
    } else {
        Write-Host "    (no $Pattern files found)"
    }
}

function Copy-DirSafe {
    param([string]$Source, [string]$Dest)
    if (Test-Path $Source) {
        $null = New-Item -ItemType Directory -Force -Path (Split-Path $Dest -Parent)
        Copy-Item -Path $Source -Destination $Dest -Recurse -Force
        Write-Host "    Copied directory: $(Split-Path $Source -Leaf)"
    } else {
        Write-Host "    (source not found: $Source)"
    }
}

# ---------------------------------------------------------------------------
# Copy files
# ---------------------------------------------------------------------------

# Rules (Markdown, platform-independent)
Write-Host "--> Copying rules..." -ForegroundColor Gray
Copy-Glob -SourceDir "$ClaudeDir\rules" -Pattern "*.md" -DestDir "$SetupDir\rules"

# Agents
Write-Host "--> Copying agents..." -ForegroundColor Gray
Copy-Glob -SourceDir "$ClaudeDir\agents" -Pattern "*.md" -DestDir "$SetupDir\agents"

# Commands (self-improve.md + reference directory)
Write-Host "--> Copying commands..." -ForegroundColor Gray
$null = New-Item -ItemType Directory -Force -Path "$SetupDir\commands"
$SiFile = "$ClaudeDir\commands\self-improve.md"
if (Test-Path $SiFile) {
    Copy-Item $SiFile -Destination "$SetupDir\commands\" -Force
    Write-Host "    Copied self-improve.md"
} else {
    Write-Host "    (self-improve.md not found)"
}
Copy-DirSafe -Source "$ClaudeDir\commands\self-improve-ref" -Dest "$SetupDir\commands\self-improve-ref"

# Hooks (all variants: .sh, .ps1, .ts + subdirectories)
Write-Host "--> Copying hooks..." -ForegroundColor Gray
$null = New-Item -ItemType Directory -Force -Path "$SetupDir\hooks"
Copy-Glob -SourceDir "$ClaudeDir\hooks" -Pattern "*.sh"  -DestDir "$SetupDir\hooks"
Copy-Glob -SourceDir "$ClaudeDir\hooks" -Pattern "*.ps1" -DestDir "$SetupDir\hooks"
Copy-Glob -SourceDir "$ClaudeDir\hooks" -Pattern "*.ts"  -DestDir "$SetupDir\hooks"
# Subdirectory: prompt-injection-defender
Copy-DirSafe -Source "$ClaudeDir\hooks\prompt-injection-defender" -Dest "$SetupDir\hooks\prompt-injection-defender"

# Skills
Write-Host "--> Copying skills..." -ForegroundColor Gray
if (Test-Path "$ClaudeDir\skills") {
    Copy-DirSafe -Source "$ClaudeDir\skills" -Dest "$SetupDir\skills"
} else {
    Write-Host "    (skills directory not found)"
}

# CLAUDE.md (root of repo)
Write-Host "--> Copying CLAUDE.md..." -ForegroundColor Gray
if (Test-Path $ClaudeMd) {
    Copy-Item $ClaudeMd -Destination "$ProggsDir\CLAUDE.md" -Force
    Write-Host "    Copied CLAUDE.md"
} else {
    Write-Host "    (~/CLAUDE.md not found)"
}

# Shared knowledge hub (MEMORY.md only — no separate files)
Write-Host "--> Copying MEMORY.md..." -ForegroundColor Gray
$null = New-Item -ItemType Directory -Force -Path "$SetupDir\agent-memory\shared"
$MemFile = "$ClaudeDir\agent-memory\shared\MEMORY.md"
if (Test-Path $MemFile) {
    Copy-Item $MemFile -Destination "$SetupDir\agent-memory\shared\" -Force
    Write-Host "    Copied MEMORY.md"
} else {
    Write-Host "    (MEMORY.md not found)"
}

Write-Host ""

# ---------------------------------------------------------------------------
# Git sync
# ---------------------------------------------------------------------------
Write-Host "==> Pulling latest changes..." -ForegroundColor Cyan
Push-Location $ProggsDir
try {
    $GitPull = git pull --rebase 2>&1
    Write-Host $GitPull
    if ($LASTEXITCODE -ne 0) {
        Write-Error "git pull --rebase failed — resolve conflicts first"
        exit 1
    }

    Write-Host ""
    Write-Host "==> Staging changes..." -ForegroundColor Cyan
    git add $SetupDir $ProggsDir\CLAUDE.md 2>&1 | Out-Null

    # Re-stage after auto-formatters may have modified staged files (race condition fix)
    Start-Sleep -Seconds 1
    git add $SetupDir $ProggsDir\CLAUDE.md 2>&1 | Out-Null

    # Check if there is anything to commit
    $Status = git status --porcelain $SetupDir $ProggsDir\CLAUDE.md 2>&1
    if (-not ($Status | Where-Object { $_ -match '.' })) {
        Write-Host "==> Nothing to commit — configuration is already up to date." -ForegroundColor Green
        exit 0
    }

    Write-Host "==> Committing..." -ForegroundColor Cyan
    # Determine next commit number
    $LogLines = git log --oneline 2>&1
    $LastNum  = ($LogLines | Select-String -Pattern '#(\d+)' | ForEach-Object {
        $_.Matches[0].Groups[1].Value -as [int]
    } | Sort-Object | Select-Object -Last 1)
    if ($null -eq $LastNum) { $LastNum = 0 }
    $NextNum = "{0:D3}" -f ($LastNum + 1)

    git commit -m "#$NextNum - $CommitMessage"
    if ($LASTEXITCODE -ne 0) {
        Write-Error "git commit failed"
        exit 1
    }

    Write-Host ""
    Write-Host "==> Pushing..." -ForegroundColor Cyan
    git push
    if ($LASTEXITCODE -ne 0) {
        Write-Error "git push failed"
        exit 1
    }

    Write-Host ""
    Write-Host "==> Done. Configuration synced and pushed." -ForegroundColor Green

} finally {
    Pop-Location
}
