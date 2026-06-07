# publish.ps1 — Erzeugt eine einzelne VoiceAgent.exe (self-contained)
# Ausfuehren:  pwsh -File publish.ps1
#
# WICHTIG (Bug-Almanach dotnet-csharp §1.5): KEIN PublishTrimmed / PublishAot fuer WPF
# (WPF nutzt Reflection auf XAML -> Trimming wuerde die App zerstoeren).

$ErrorActionPreference = "Stop"

$proj = Join-Path $PSScriptRoot "src\VoiceAgent\VoiceAgent.csproj"
$out  = Join-Path $PSScriptRoot "publish"

Write-Host "Baue VoiceAgent (Release, self-contained single-file)..." -ForegroundColor Cyan

dotnet publish $proj -c Release -r win-x64 --self-contained true `
    -p:PublishSingleFile=true `
    -p:IncludeNativeLibrariesForSelfExtract=true `
    -p:EnableCompressionInSingleFile=true `
    -o $out

if ($LASTEXITCODE -eq 0) {
    $exe = Get-Item (Join-Path $out "VoiceAgent.exe")
    Write-Host "`nErfolgreich gebaut!" -ForegroundColor Green
    Write-Host "Datei:   $($exe.FullName)"
    Write-Host "Groesse: $([math]::Round($exe.Length / 1MB, 1)) MB"
    Write-Host "`nBeim ersten Start: Einstellungen oeffnen und API-Schluessel eintragen"
    Write-Host "(werden sicher in ~/SK/VoiceAgent/keys.json gespeichert)."
} else {
    Write-Host "`nBuild fehlgeschlagen!" -ForegroundColor Red
    exit 1
}
