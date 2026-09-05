# Startet den Download der Songliste nach C:\Suno Backup.
$ErrorActionPreference = "Continue"
$projekt = "C:\Users\barwa\proggs\SunoDownload"
$ziel    = "C:\Suno Backup"
$liste   = Join-Path $ziel "suno-liste.json"
$log     = Join-Path $ziel "_download-log.txt"

if (-not (Test-Path $liste)) {
    $liste = Get-ChildItem "$env:USERPROFILE\Downloads" -Filter "suno-liste*.json" -ErrorAction SilentlyContinue |
             Sort-Object LastWriteTime -Descending | Select-Object -First 1 -ExpandProperty FullName
}
if (-not $liste) { Write-Host "Keine Songliste gefunden." -ForegroundColor Red; Read-Host "Enter"; exit 1 }

Set-Location $projekt
Write-Host ""
Write-Host "  Songliste : $liste" -ForegroundColor Cyan
Write-Host "  Zielordner: $ziel" -ForegroundColor Cyan
Write-Host ""

node suno-download.ts $liste $ziel 2>&1 | Tee-Object -FilePath $log

Write-Host ""
Write-Host "  Fertig." -ForegroundColor Green
Read-Host "  Enter zum Schliessen"
