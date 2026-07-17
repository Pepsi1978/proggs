# publish.ps1 — Erzeugt eine einzelne .exe (self-contained)
# Ausfuehren: pwsh -File publish.ps1

$ErrorActionPreference = "Stop"
$projectPath = Join-Path $PSScriptRoot "ClaudeVoiceOverlay.csproj"
$publishDir = Join-Path $PSScriptRoot "publish"
$exePath = Join-Path $publishDir "ClaudeVoiceOverlay.exe"
$deploymentMutex = [System.Threading.Mutex]::new($false, "Local\ClaudeVoiceOverlay.Deployment")
$mutexHeld = $false
$reservationHeld = $false
$exitCode = 0
. (Join-Path $PSScriptRoot '..\voice-overlay-deploy-guard.ps1')

# SDK-Pfad setzen (User-Installation hat Vorrang)
$sdkDotnet = Join-Path $env:USERPROFILE ".dotnet\dotnet.exe"
if (Test-Path $sdkDotnet) {
    $env:DOTNET_ROOT = Join-Path $env:USERPROFILE ".dotnet"
    $dotnet = $sdkDotnet
} else {
    $dotnet = "dotnet"
}

try {
    try {
        $mutexHeld = $deploymentMutex.WaitOne([TimeSpan]::FromMinutes(30))
    } catch [System.Threading.AbandonedMutexException] {
        $mutexHeld = $true
    }
    if (-not $mutexHeld) {
        throw "Ein anderes ClaudeVoiceOverlay-Deployment ist noch aktiv."
    }
    $reservationHeld = Enter-VoiceOverlayDeploymentWindow -ProcessName 'ClaudeVoiceOverlay' -Port 5724

    Write-Host "Baue ClaudeVoiceOverlay..." -ForegroundColor Cyan
    Write-Host "Verwende: $dotnet" -ForegroundColor Gray

    & $dotnet publish $projectPath -c Release -r win-x64 --self-contained true `
        -p:PublishSingleFile=true `
        -p:IncludeNativeLibrariesForSelfExtract=true `
        -p:EnableCompressionInSingleFile=true `
        -o $publishDir

    if ($LASTEXITCODE -ne 0) {
        throw "Build fehlgeschlagen (Exit-Code $LASTEXITCODE)."
    }
    if (-not (Test-Path -LiteralPath $exePath -PathType Leaf)) {
        throw "Build meldete Erfolg, aber $exePath wurde nicht erzeugt."
    }

    $exe = Get-Item -LiteralPath $exePath
    Write-Host "`nErfolgreich gebaut!" -ForegroundColor Green
    Write-Host "Datei: $($exe.FullName)"
    Write-Host "Groesse: $([math]::Round($exe.Length / 1MB, 1)) MB"
    Write-Host "`n.env Datei neben die .exe legen und starten."
} catch {
    Write-Host "`nPublish fehlgeschlagen: $($_.Exception.Message)" -ForegroundColor Red
    $exitCode = 1
} finally {
    Exit-VoiceOverlayDeploymentWindow -ProcessName 'ClaudeVoiceOverlay' -Port 5724 -Reserved $reservationHeld
    if ($mutexHeld) {
        $deploymentMutex.ReleaseMutex()
    }
    $deploymentMutex.Dispose()
}

if ($exitCode -ne 0) {
    exit $exitCode
}
