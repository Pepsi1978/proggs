# Wartet auf suno-liste*.json (Downloads ODER C:\Suno Backup) und startet dann den Download.
$orte     = @("$env:USERPROFILE\Downloads", "C:\Suno Backup", "$env:USERPROFILE\Desktop")
$projekt  = $PSScriptRoot   # der Ordner, in dem dieses Skript liegt
$ziel     = "C:\Suno Backup"
$logdatei = "C:\Suno Backup\_download-log.txt"
$ende     = (Get-Date).AddMinutes(90)

Write-Host ""
Write-Host "  Waechter laeuft." -ForegroundColor Cyan
Write-Host "  Sobald die Songliste gespeichert ist, startet der Download automatisch."
Write-Host "  Ueberwacht: $($orte -join ' | ')"
Write-Host ""

while ((Get-Date) -lt $ende) {
    $liste = $orte | Where-Object { Test-Path $_ } |
             ForEach-Object { Get-ChildItem $_ -Filter "suno-liste*.json" -ErrorAction SilentlyContinue } |
             Sort-Object LastWriteTime -Descending | Select-Object -First 1

    if ($liste) {
        Write-Host "  Liste gefunden: $($liste.FullName)" -ForegroundColor Green
        Start-Sleep -Seconds 2
        Set-Location $projekt
        node suno-download.ts $liste.FullName $ziel *>&1 | Tee-Object -FilePath $logdatei
        Write-Host ""
        Write-Host "  Fertig. Fenster kann geschlossen werden." -ForegroundColor Cyan
        Read-Host "  Enter zum Schliessen"
        exit 0
    }
    Start-Sleep -Seconds 3
}
Write-Host "  Zeitueberschreitung: Es kam keine Songliste an."
Read-Host "  Enter zum Schliessen"
exit 1
