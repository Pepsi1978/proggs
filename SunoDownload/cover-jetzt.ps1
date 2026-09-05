# Traegt Cover und Titel in alle MP3s in C:\Suno Backup ein.
$projekt = $PSScriptRoot   # der Ordner, in dem dieses Skript liegt
$ziel    = "C:\Suno Backup"
$liste   = Join-Path $ziel "suno-liste.json"
$log     = Join-Path $ziel "_cover-log.txt"

Set-Location $projekt
Write-Host ""
Write-Host "  Cover werden eingetragen. Das dauert einige Minuten." -ForegroundColor Cyan
Write-Host ""

node cover-nachtragen.ts $liste $ziel 2>&1 | Tee-Object -FilePath $log

Write-Host ""
Read-Host "  Enter zum Schliessen"
