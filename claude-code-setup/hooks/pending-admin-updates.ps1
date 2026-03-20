# Pending Admin Updates Generator
# Creates a script with all updates that need admin elevation
# Run the generated script as admin when ready
# Platform: Windows (PowerShell)

. "$PSScriptRoot/hook-log.ps1"
. "$PSScriptRoot/whiteboard-insert.ps1"

$OutputScript = Join-Path $env:USERPROFILE "Desktop" "install-updates.ps1"

$commands = @()
$descriptions = @()

# Check Git
try {
    $gitUpdateAvailable = winget list Git.Git --source winget 2>$null | Select-String "verfügbar|Available"
    if ($gitUpdateAvailable) {
        $commands += 'Write-Host "Updating Git..." -ForegroundColor Cyan; winget upgrade Git.Git --source winget --accept-source-agreements --accept-package-agreements'
        $descriptions += "Git (Security Update)"
    }
} catch {}

# Check .NET SDK
$dotnetInstalled = $false
try { $null = dotnet --version 2>$null; $dotnetInstalled = $true } catch {}
if (-not $dotnetInstalled) {
    $commands += 'Write-Host "Installing .NET SDK 10..." -ForegroundColor Cyan; winget install Microsoft.DotNet.SDK.10 --source winget --accept-source-agreements --accept-package-agreements'
    $descriptions += ".NET SDK 10"
}

# Check Go
$goInstalled = $false
try { $null = go version 2>$null; $goInstalled = $true } catch {}
if (-not $goInstalled) {
    $commands += 'Write-Host "Installing Go..." -ForegroundColor Cyan; winget install GoLang.Go --source winget --accept-source-agreements --accept-package-agreements'
    $descriptions += "Go (Programmiersprache)"
}

# Check Windows security updates via winget
$securityPackages = @(
    "Microsoft.VCRedist.2015+.x64",
    "Microsoft.VCRedist.2015+.x86",
    "Microsoft.DotNet.Runtime.6",
    "Microsoft.DotNet.Runtime.8",
    "Microsoft.DotNet.DesktopRuntime.8"
)
foreach ($pkg in $securityPackages) {
    try {
        $updateAvailable = winget list $pkg --source winget 2>$null | Select-String "verfügbar|Available"
        if ($updateAvailable) {
            $commands += "Write-Host `"Updating $pkg...`" -ForegroundColor Cyan; winget upgrade $pkg --source winget --accept-source-agreements --accept-package-agreements"
            $descriptions += $pkg
        }
    } catch {}
}

if ($commands.Count -eq 0) {
    Hook-Log "no pending admin updates"
    Write-Output "Pending-Admin-Updates: Keine ausstehenden Admin-Updates gefunden."
    exit 0
}

# Generate the script
$scriptContent = @"
# Auto-generated Admin Update Script
# Generated: $(Get-Date -Format "yyyy-MM-dd HH:mm")
# Run this script as Administrator (right-click > Als Administrator ausfuehren)

Write-Host "=== Ausstehende Updates ($($commands.Count) Stueck) ===" -ForegroundColor Yellow
Write-Host ""

$($commands -join "`n`n")

Write-Host ""
Write-Host "=== Alle Updates abgeschlossen ===" -ForegroundColor Green
Write-Host "Druecke eine Taste zum Schliessen..."
`$null = `$Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
"@

$scriptContent | Out-File -FilePath $OutputScript -Encoding utf8
Hook-Log "generated update script: $($commands.Count) updates ($($descriptions -join ', '))"
Write-Output "Pending-Admin-Updates: Script erstellt auf Desktop ($($commands.Count) Updates: $($descriptions -join ', '))"
