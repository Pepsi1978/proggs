# publish.ps1 — Erzeugt eine einzelne .exe (self-contained)
# Ausfuehren: pwsh -File publish.ps1

$ErrorActionPreference = "Stop"

# SDK-Pfad setzen (User-Installation hat Vorrang)
$sdkDotnet = Join-Path $env:USERPROFILE ".dotnet\dotnet.exe"
if (Test-Path $sdkDotnet) {
    $env:DOTNET_ROOT = Join-Path $env:USERPROFILE ".dotnet"
    $dotnet = $sdkDotnet
} else {
    $dotnet = "dotnet"
}

Write-Host "Baue ClaudeVoiceOverlay..." -ForegroundColor Cyan
Write-Host "Verwende: $dotnet" -ForegroundColor Gray

& $dotnet publish -c Release -r win-x64 --self-contained true `
    -p:PublishSingleFile=true `
    -p:IncludeNativeLibrariesForSelfExtract=true `
    -p:EnableCompressionInSingleFile=true `
    -o ./publish

if ($LASTEXITCODE -eq 0) {
    $exe = Get-Item ./publish/ClaudeVoiceOverlay.exe
    Write-Host "`nErfolgreich gebaut!" -ForegroundColor Green
    Write-Host "Datei: $($exe.FullName)"
    Write-Host "Groesse: $([math]::Round($exe.Length / 1MB, 1)) MB"
    Write-Host "`n.env Datei neben die .exe legen und starten."
} else {
    Write-Host "`nBuild fehlgeschlagen!" -ForegroundColor Red
    exit 1
}
