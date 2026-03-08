# publish.ps1 — Erzeugt eine einzelne .exe (self-contained)
# Ausfuehren: pwsh -File publish.ps1

$ErrorActionPreference = "Stop"

Write-Host "Baue TerminalVoiceOverlay..." -ForegroundColor Cyan

dotnet publish -c Release -r win-x64 --self-contained true `
    -p:PublishSingleFile=true `
    -p:IncludeNativeLibrariesForSelfExtract=true `
    -p:EnableCompressionInSingleFile=true `
    -o ./publish

if ($LASTEXITCODE -eq 0) {
    $exe = Get-Item ./publish/TerminalVoiceOverlay.exe
    Write-Host "`nErfolgreich gebaut!" -ForegroundColor Green
    Write-Host "Datei: $($exe.FullName)"
    Write-Host "Groesse: $([math]::Round($exe.Length / 1MB, 1)) MB"
    Write-Host "`n.env Datei neben die .exe legen und starten."
} else {
    Write-Host "`nBuild fehlgeschlagen!" -ForegroundColor Red
    exit 1
}
