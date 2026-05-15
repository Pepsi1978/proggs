# check-pwsh-availability.ps1 (Gemini) ÔÇö Prueft ob pwsh (PowerShell 7+) verfuegbar ist
Write-Host "=== ­ƒÆí Gemini Runtime-Check: PowerShell 7+ ===" -ForegroundColor Cyan

$pwshPath = Get-Command pwsh -ErrorAction SilentlyContinue

if ($pwshPath) {
    $version = pwsh -Version
    Write-Host "  Ô£à pwsh gefunden: $($pwshPath.Source)" -ForegroundColor Green
    Write-Host "  Ô£à Version: $version" -ForegroundColor Green
    exit 0
} else {
    Write-Host "  ÔØî FEHLER: 'pwsh' (PowerShell 7+) wurde nicht gefunden!" -ForegroundColor Red
    Write-Host "`n  Warum das wichtig ist:" -ForegroundColor Yellow
    Write-Host "  Gemini nutzt moderne Funktionen (wie Mutex-Locking und Whiteboard-Integration)," -ForegroundColor Gray
    Write-Host "  die in der alten Windows PowerShell 5.1 nicht stabil funktionieren." -ForegroundColor Gray
    
    Write-Host "`n  So installierst du PowerShell 7:" -ForegroundColor White
    Write-Host "  1. ├ûffne ein Terminal als Administrator" -ForegroundColor Gray
    Write-Host "  2. Gib ein: 'winget install Microsoft.PowerShell'" -ForegroundColor Gray
    Write-Host "  3. Starte das Gemini CLI neu." -ForegroundColor Gray
    
    exit 1
}
