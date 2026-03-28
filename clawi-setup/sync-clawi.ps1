# sync-clawi.ps1 - Sync Clawi's complete identity/memory baseline between Repo and Local Workspace
param(
    [Parameter(Mandatory=$true)]
    [ValidateSet("pull", "push")]
    $Mode
)

$ErrorActionPreference = "Stop"

$ScriptDir = $PSScriptRoot
$DefaultRepoDir = "/Users/frank/clawi/proggs/clawi-setup"
if ($env:CLAWI_SETUP_REPO_DIR) {
    $RepoDir = $env:CLAWI_SETUP_REPO_DIR
}
elseif ($ScriptDir -eq (Join-Path $env:USERPROFILE ".openclaw\workspace")) {
    $RepoDir = $DefaultRepoDir
}
else {
    $RepoDir = $ScriptDir
}
$WorkspaceDir = Join-Path $env:USERPROFILE ".openclaw\workspace"
$OpenClawHome = Join-Path $env:USERPROFILE ".openclaw"
$LcmDb = Join-Path $OpenClawHome "lcm.db"
$LcmBackupDir = Join-Path $RepoDir "lossless-claw"
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
$DirsToSync = @("memory", "hooks", "agents")

if (-not (Test-Path $WorkspaceDir)) {
    Write-Host "Local OpenClaw workspace not found at $WorkspaceDir" -ForegroundColor Red
    exit 1
}
if (-not (Test-Path $RepoDir)) {
    Write-Host "Clawi setup repo not found at $RepoDir" -ForegroundColor Red
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
        if ($Source -ne $Destination) { Copy-Item $Source $Destination -Force }
        Write-Host "  Synced $(Split-Path $Source -Leaf)"
    }
}

function Copy-DirIfExists {
    param(
        [string]$Source,
        [string]$Destination
    )

    if (Test-Path $Source) {
        if (-not (Test-Path $Destination)) { New-Item -ItemType Directory -Path $Destination -Force | Out-Null }
        if ($Source -ne $Destination) { Copy-Item (Join-Path $Source "*") $Destination -Force -Recurse }
        Write-Host "  Synced $(Split-Path $Source -Leaf)/"
    }
}

function Write-LcmManifest {
    param([string]$OutputPath)

    if (-not (Test-Path $LcmDb)) { return }
    $cfg = Get-Content (Join-Path $OpenClawHome "openclaw.json") -Raw | ConvertFrom-Json
    $pluginEntry = $cfg.plugins.entries.'lossless-claw'
    $pluginInstall = $cfg.plugins.installs.'lossless-claw'
    $hash = (Get-FileHash $LcmDb -Algorithm SHA256).Hash.ToLower()
    $manifest = [ordered]@{
        capturedAt = [DateTime]::UtcNow.ToString('yyyy-MM-ddTHH:mm:ssZ')
        dbPath = $LcmDb
        dbSizeBytes = (Get-Item $LcmDb).Length
        dbSha256 = $hash
        counts = @{}
        pluginEntry = $pluginEntry
        pluginInstall = $pluginInstall
        rawDatabaseBackedUpToGit = $false
        note = 'Only sanitized lossless-claw metadata is backed up to Git. Raw lcm.db is intentionally excluded because it may contain sensitive conversation/config data.'
    }
    $manifest | ConvertTo-Json -Depth 10 | Out-File $OutputPath -Encoding utf8
}

if ($Mode -eq "pull") {
    Write-Host "Pulling Clawi's full baseline from Repo ($RepoDir) to $WorkspaceDir..." -ForegroundColor Cyan
    foreach ($file in $FilesToSync) {
        Copy-IfExists (Join-Path $RepoDir $file) (Join-Path $WorkspaceDir $file)
    }
    foreach ($dir in $DirsToSync) {
        Copy-DirIfExists (Join-Path $RepoDir $dir) (Join-Path $WorkspaceDir $dir)
    }
    if (Test-Path (Join-Path $LcmBackupDir "manifest.json")) {
        Copy-IfExists (Join-Path $LcmBackupDir "manifest.json") (Join-Path $OpenClawHome "lcm.manifest.json")
    }
    Write-Host "Clawi's full baseline is now up to date in local workspace!" -ForegroundColor Green
}
elseif ($Mode -eq "push") {
    Write-Host "Pushing Clawi's full baseline from $WorkspaceDir to Repo ($RepoDir)..." -ForegroundColor Cyan
    foreach ($file in $FilesToSync) {
        Copy-IfExists (Join-Path $WorkspaceDir $file) (Join-Path $RepoDir $file)
    }
    foreach ($dir in $DirsToSync) {
        Copy-DirIfExists (Join-Path $WorkspaceDir $dir) (Join-Path $RepoDir $dir)
    }
    if (Test-Path $LcmDb) {
        if (-not (Test-Path $LcmBackupDir)) { New-Item -ItemType Directory -Path $LcmBackupDir -Force | Out-Null }
        Write-LcmManifest (Join-Path $LcmBackupDir "manifest.json")
        Write-Host "  Synced lossless-claw metadata/"
    }
    Write-Host "Clawi's full baseline is now backed up in the repository!" -ForegroundColor Green
}
