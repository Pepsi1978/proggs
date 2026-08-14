# Nachlade-Lauf: neue Suno-Songs holen, ohne die Nummerierung zu veraendern.
$projekt = "C:\Users\barwa\proggs\SunoDownload"
$ziel    = "C:\Sono Backup"
$log     = Join-Path $ziel "_aktualisieren-log.txt"
$start   = Get-Date

$Host.UI.RawUI.WindowTitle = "SunoDownload - Neue Songs nachladen"
Write-Host ""
Write-Host "  ================================================================" -ForegroundColor Cyan
Write-Host "   SunoDownload - Neue Songs nachladen" -ForegroundColor Cyan
Write-Host "  ================================================================" -ForegroundColor Cyan
Write-Host ""

# Gibt es schon eine frische Songliste (juenger als 15 Minuten)?
$frisch = Get-ChildItem "$env:USERPROFILE\Downloads","$ziel" -Filter "suno-liste*.json" -ErrorAction SilentlyContinue |
          Where-Object { $_.Name -notlike "*-vorher*" -and $_.LastWriteTime -gt (Get-Date).AddMinutes(-15) } |
          Sort-Object LastWriteTime -Descending | Select-Object -First 1

if (-not $frisch) {
    # Schritt 1: Skript in die Zwischenablage und Browser oeffnen
    Get-Content (Join-Path $projekt "bibliothek-holen.js") -Raw -Encoding UTF8 | Set-Clipboard
    Start-Process "https://suno.com/me"

    Write-Host "  Schritt 1 von 2 - einmal im Browser:" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "    Das Skript liegt bereits in der Zwischenablage."
    Write-Host "    Chrome wurde geoeffnet."
    Write-Host ""
    Write-Host "      1. F12 druecken"
    Write-Host "      2. oben auf 'Console' klicken"
    Write-Host "      3. unten in die Zeile klicken, Strg+V, Enter"
    Write-Host "         (falls Chrome meckert: allow pasting tippen, Enter, dann nochmal Strg+V)"
    Write-Host ""
    Write-Host "    Am Ende auf 'Speichern' klicken - Downloads-Ordner genuegt."
    Write-Host ""
    Write-Host "  Ich warte, bis die Liste da ist ..." -ForegroundColor Cyan
    Write-Host ""

    $ende = (Get-Date).AddMinutes(60)
    while ((Get-Date) -lt $ende -and -not $frisch) {
        Start-Sleep -Seconds 3
        $frisch = Get-ChildItem "$env:USERPROFILE\Downloads","$ziel" -Filter "suno-liste*.json" -ErrorAction SilentlyContinue |
                  Where-Object { $_.Name -notlike "*-vorher*" -and $_.LastWriteTime -gt $start } |
                  Sort-Object LastWriteTime -Descending | Select-Object -First 1
    }

    if (-not $frisch) {
        Write-Host "  Es kam keine Songliste an. Bitte neu starten." -ForegroundColor Red
        Read-Host "  Enter zum Schliessen"
        exit 1
    }
}

Write-Host "  Songliste gefunden: $($frisch.FullName)" -ForegroundColor Green
Write-Host ""
Write-Host "  Schritt 2 von 2 - neue Songs werden geladen:" -ForegroundColor Yellow
Write-Host ""

Set-Location $projekt
node aktualisieren.ts $frisch.FullName $ziel 2>&1 | Tee-Object -FilePath $log

Write-Host ""
Read-Host "  Enter zum Schliessen"
