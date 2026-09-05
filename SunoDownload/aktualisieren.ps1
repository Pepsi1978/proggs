# Nachlade-Lauf: neue Suno-Songs holen, ohne die Nummerierung zu veraendern.
$projekt = $PSScriptRoot   # der Ordner, in dem dieses Skript liegt
$ziel    = "C:\Suno Backup"
$log     = Join-Path $ziel "_aktualisieren-log.txt"
$start   = Get-Date

$Host.UI.RawUI.WindowTitle = "SunoDownload - Neue Songs nachladen"
Write-Host ""
Write-Host "  ================================================================" -ForegroundColor Cyan
Write-Host "   SunoDownload - Neue Songs nachladen" -ForegroundColor Cyan
Write-Host "  ================================================================" -ForegroundColor Cyan
Write-Host ""

# Gibt es schon eine frische Songliste (juenger als 15 Minuten)?
# Sie zaehlt nur, wenn sie Download-Links enthaelt - ohne die laesst sich kein Song
# laden, weil Suno keine direkten Adressen mehr herausgibt.
$frisch = Get-ChildItem "$env:USERPROFILE\Downloads","$ziel" -Filter "suno-liste*.json" -ErrorAction SilentlyContinue |
          Where-Object {
              $_.Name -notlike "*-vorher*" -and $_.LastWriteTime -gt (Get-Date).AddMinutes(-15) -and
              (Select-String -Path $_.FullName -Pattern '"download_url"' -SimpleMatch -Quiet)
          } |
          Sort-Object LastWriteTime -Descending | Select-Object -First 1

if (-not $frisch) {
    # Schritt 1: Skript in die Zwischenablage und Browser oeffnen.
    # Vorher wird der Zeitstempel des juengsten bereits gesicherten Songs eingesetzt.
    # Nur fuer neuere Songs holt das Skript dann Download-Links - private Songs sperrt
    # das Suno-CDN aus, und bei mehreren tausend Songs waere das sonst endlos.
    $seit = ""
    $bestandDatei = Join-Path $ziel "_bestand.json"
    $alteListe = Get-ChildItem "$env:USERPROFILE\Downloads","$ziel" -Filter "suno-liste*.json" -ErrorAction SilentlyContinue |
                 Where-Object { $_.Name -notlike "*-vorher*" } |
                 Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if ((Test-Path $bestandDatei) -and $alteListe) {
        try {
            $bestand = (Get-Content $bestandDatei -Raw -Encoding UTF8 | ConvertFrom-Json).eintraege
            $bekannt = @{}
            foreach ($p in $bestand.PSObject.Properties) { $bekannt[$p.Name] = $true }
            $juengste = Get-Content $alteListe.FullName -Raw -Encoding UTF8 | ConvertFrom-Json |
                        Where-Object { $bekannt.ContainsKey($_.id) -and $_.created_at } |
                        ForEach-Object { [datetime]$_.created_at } |
                        Sort-Object -Descending | Select-Object -First 1
            if ($juengste) { $seit = $juengste.ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ss.fffZ") }
        } catch { $seit = "" }
    }

    $skript = Get-Content (Join-Path $projekt "bibliothek-holen.js") -Raw -Encoding UTF8
    if ($seit) {
        $skript = $skript.Replace("__SEIT__", $seit)
        Write-Host "  Es werden nur Songs nach dem $($juengste.ToLocalTime().ToString('dd.MM.yyyy HH:mm')) als neu behandelt." -ForegroundColor DarkGray
    }
    $skript | Set-Clipboard
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
                  Where-Object {
                      $_.Name -notlike "*-vorher*" -and $_.LastWriteTime -gt $start -and
                      (Select-String -Path $_.FullName -Pattern '"download_url"' -SimpleMatch -Quiet)
                  } |
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
