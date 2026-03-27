# sync-clawi.ps1 - Sync Clawi's complete identity/memory baseline between Repo and Local Workspace
param(
    [Parameter(Mandatory=$true)]
    [ValidateSet("pull", "push")]
    $Mode
)

$ErrorActionPreference = "Stop"

$RepoDir = $PSScriptRoot
$WorkspaceDir = Join-Path $env:USERPROFILE ".openclaw\workspace"
$FilesToSync = @(
    "SOUL.md",
    "IDENTITY.md",
    "DIREKTIVEN.md",
    "AGENTS.md",
    "ENVIRONMENT-FIXES.md",
    "Forschung-clawi.md",
    "README.md",
    "HEARTBEAT.md",
    "MEMORY.md",
    "RECOVERY-OPENCLAW.md",
    "health-report-template.md",
    "sync-clawi.sh",
    "sync-clawi.ps1"
)

if (-not (Test-Path $WorkspaceDir)) {
    Write-Host "Local OpenClaw workspace not found at $WorkspaceDir" -ForegroundColor Red
    exit 1
}

function Copy-IfExists {
    param(
        [string]$Source,
        [string]$Destination
    )

    if (Test-Path $Source) {
        $parent = Split-Path -Parent $Destination
        if ($parent -and -not (Test-Path $parent)) {
            New-Item -ItemType Directory -Path $parent -Force | Out-Null
        }
        Copy-Item $Source $Destination -Force
        Write-Host "  Synced $(Split-Path $Source -Leaf)"
    }
}

if ($Mode -eq "pull") {
    Write-Host "Pulling Clawi's full baseline from Repo to $WorkspaceDir..." -ForegroundColor Cyan
    foreach ($file in $FilesToSync) {
        Copy-IfExists (Join-Path $RepoDir $file) (Join-Path $WorkspaceDir $file)
    }

    $repoMemory = Join-Path $RepoDir "memory"
    $workspaceMemory = Join-Path $WorkspaceDir "memory"
    if (Test-Path $repoMemory) {
        if (-not (Test-Path $workspaceMemory)) { New-Item -ItemType Directory $workspaceMemory -Force | Out-Null }
        Copy-Item (Join-Path $repoMemory "*") $workspaceMemory -Force -Recurse
        Write-Host "  Updated memory/"
    }

    $repoHooks = Join-Path $RepoDir "hooks"
    $workspaceHooks = Join-Path $WorkspaceDir "hooks"
    if (Test-Path $repoHooks) {
        if (-not (Test-Path $workspaceHooks)) { New-Item -ItemType Directory $workspaceHooks -Force | Out-Null }
        Copy-Item (Join-Path $repoHooks "*") $workspaceHooks -Force -Recurse
        Write-Host "  Updated hooks/"
    }

    Write-Host "Clawi's full baseline is now up to date in local workspace!" -ForegroundColor Green
}
elseif ($Mode -eq "push") {
    Write-Host "Pushing Clawi's full baseline from $WorkspaceDir to Repo..." -ForegroundColor Cyan
    foreach ($file in $FilesToSync) {
        Copy-IfExists (Join-Path $WorkspaceDir $file) (Join-Path $RepoDir $file)
    }

    $wsMemory = Join-Path $WorkspaceDir "memory"
    $repoMemory = Join-Path $RepoDir "memory"
    if (Test-Path $wsMemory) {
        if (-not (Test-Path $repoMemory)) { New-Item -ItemType Directory $repoMemory -Force | Out-Null }
        Copy-Item (Join-Path $wsMemory "*") $repoMemory -Force -Recurse
        Write-Host "  Saved memory/"
    }

    $wsHooks = Join-Path $WorkspaceDir "hooks"
    $repoHooks = Join-Path $RepoDir "hooks"
    if (Test-Path $wsHooks) {
        if (-not (Test-Path $repoHooks)) { New-Item -ItemType Directory $repoHooks -Force | Out-Null }
        Copy-Item (Join-Path $wsHooks "*") $repoHooks -Force -Recurse
        Write-Host "  Saved hooks/"
    }

    Write-Host "Clawi's full baseline is now backed up in the repository!" -ForegroundColor Green
}
