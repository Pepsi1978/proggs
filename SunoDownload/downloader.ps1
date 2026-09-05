# Suno-Downloader: die ganze Bibliothek in einem Lauf.
# Node macht alles selbst; der Browser liefert nur die Songliste und die von Suno
# ausgestellten Download-Links. Ein Einzeiler in der Konsole genuegt dafuer.
$projekt = "C:\Users\barwa\proggs\SunoDownload"
$ziel    = if ($args.Count -gt 0 -and $args[0] -notlike "--*") { $args[0] } else { "C:\Sono Backup" }
$log     = Join-Path $ziel "_downloader-log.txt"

$Host.UI.RawUI.WindowTitle = "SunoDownload - Songs holen"
Write-Host ""
Write-Host "  ================================================================" -ForegroundColor Cyan
Write-Host "   SunoDownload - die ganze Bibliothek in einem Lauf" -ForegroundColor Cyan
Write-Host "  ================================================================" -ForegroundColor Cyan

if (-not (Test-Path $ziel)) { New-Item -ItemType Directory -Path $ziel -Force | Out-Null }

# Das Ziel steht am Ende genau einmal - die uebrigen Schalter (--limit, --freischalten) bleiben.
$schalter = @($args | Where-Object { $_ -ne $ziel })

Set-Location $projekt
node downloader.ts @schalter $ziel 2>&1 | Tee-Object -FilePath $log

Write-Host ""
Read-Host "  Enter zum Schliessen"
