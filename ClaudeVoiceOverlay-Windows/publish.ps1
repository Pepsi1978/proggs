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

# Ein Update ueber eine vorhandene publish\.exe gibt es nur nach dem Klick auf "Ja" in
# rebuild-overlay.ps1 -- das setzt Kennung und Freigabedatei, beides nur waehrend seines Laufs. Nur der allererste Build darf ohne ihn laufen.
if ((Test-Path -LiteralPath $exePath) -and -not ($env:OVERLAY_REBUILD_FREIGABE -and (Test-Path -LiteralPath (Join-Path ([IO.Path]::GetTempPath()) "overlay-freigabe-$env:OVERLAY_REBUILD_FREIGABE")))) {
    Write-Host "Kein direktes Publish: Updates nur ueber 'pwsh -File ..\rebuild-overlay.ps1 CVO' (Ja/Nein-Fenster)." -ForegroundColor Red
    Write-Host 'OVERLAY_UPDATE_STATUS=refused (nicht ueber rebuild-overlay.ps1 freigegeben)'
    exit 3
}

# SDK waehlen: die Installation mit dem neuesten SDK, denn sie baut auch aeltere Ziele.
# Die User-Installation gewinnt nur bei Gleichstand. Hatte sie fest Vorrang, scheiterte der
# Build, sobald dort ein aelteres SDK lag als das Projekt braucht (Vorfall 02.11.2026).
function Get-NeuestesDotnetSdk([string]$Programm) {
    try {
        $neuestes = & $Programm --list-sdks 2>$null |
            ForEach-Object { try { [version]($_ -split '[ -]')[0] } catch { $null } } |
            Where-Object { $_ } | Sort-Object -Descending | Select-Object -First 1
        if ($neuestes) { return $neuestes }
    } catch { }
    return [version]'0.0'
}
$sdkDotnet = Join-Path $env:USERPROFILE ".dotnet\dotnet.exe"
$dotnet = "dotnet"
if ((Test-Path $sdkDotnet) -and (Get-NeuestesDotnetSdk $sdkDotnet) -ge (Get-NeuestesDotnetSdk "dotnet")) {
    $env:DOTNET_ROOT = Join-Path $env:USERPROFILE ".dotnet"
    $dotnet = $sdkDotnet
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
