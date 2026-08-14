# Traegt Cover und Titel in alle MP3s in C:\Sono Backup ein.
$projekt = "C:\Users\barwa\proggs\SunoDownload"
$ziel    = "C:\Sono Backup"
$liste   = Join-Path $ziel "suno-liste.json"
$log     = Join-Path $ziel "_cover-log.txt"

Set-Location $projekt
Write-Host ""
Write-Host "  Cover werden eingetragen. Das dauert einige Minuten." -ForegroundColor Cyan
Write-Host ""

node cover-nachtragen.ts $liste $ziel 2>&1 | Tee-Object -FilePath $log

Write-Host ""
Read-Host "  Enter zum Schliessen"
