# publish.ps1 — Erzeugt eine einzelne .exe (self-contained)
# Ausfuehren: pwsh -File publish.ps1

$ErrorActionPreference = "Stop"

$projectPath = Join-Path $PSScriptRoot "TerminalVoiceOverlay.csproj"
$publishDir = Join-Path $PSScriptRoot "publish"
$exePath = Join-Path $publishDir "TerminalVoiceOverlay.exe"
$deploymentMutex = [System.Threading.Mutex]::new($false, "Local\TerminalVoiceOverlay.Deployment")
$mutexHeld = $false
$reservationHeld = $false
$exitCode = 0
. (Join-Path $PSScriptRoot '..\voice-overlay-deploy-guard.ps1')

try {
    try {
        $mutexHeld = $deploymentMutex.WaitOne([TimeSpan]::FromMinutes(30))
    } catch [System.Threading.AbandonedMutexException] {
        $mutexHeld = $true
    }
    if (-not $mutexHeld) {
        throw "Ein anderes TerminalVoiceOverlay-Deployment ist noch aktiv."
    }
    $reservationHeld = Enter-VoiceOverlayDeploymentWindow -ProcessName 'TerminalVoiceOverlay' -Port 5723

    Write-Host "Baue TerminalVoiceOverlay..." -ForegroundColor Cyan

    # Explizit TerminalVoiceOverlay.csproj angeben — seit dem Move der
    # PromptBoard-Subprojekte ins selbe Verzeichnis liegt zusaetzlich eine
    # PromptBoard.slnx hier, und dotnet publish ohne Pfadargument koennte
    # nicht entscheiden welches Target zu bauen ist.
    & dotnet publish $projectPath -c Release -r win-x64 --self-contained true `
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
    Exit-VoiceOverlayDeploymentWindow -ProcessName 'TerminalVoiceOverlay' -Port 5723 -Reserved $reservationHeld
    if ($mutexHeld) {
        $deploymentMutex.ReleaseMutex()
    }
    $deploymentMutex.Dispose()
}

if ($exitCode -ne 0) {
    exit $exitCode
}
