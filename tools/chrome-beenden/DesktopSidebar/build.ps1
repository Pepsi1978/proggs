# Baut die Chrome-Shutdown-Sidebar als self-contained Single-File-.exe,
# richtet den Autostart ein und startet sie sofort.
# Erneut ausfuehrbar: aktualisiert die App bei jedem Lauf.

$ErrorActionPreference = 'Stop'

$projDir = $PSScriptRoot
$proj    = Join-Path $projDir 'DesktopSidebar.csproj'
$outDir  = Join-Path $env:LOCALAPPDATA 'ChromeShutdownSidebar'
$exe     = Join-Path $outDir 'DesktopSidebar.exe'
$runName = 'ChromeShutdownSidebar'

Write-Host '== Beende laufende Instanz (falls vorhanden) ==' -ForegroundColor Cyan
Get-Process 'DesktopSidebar' -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
Start-Sleep -Milliseconds 300

Write-Host '== Baue self-contained Single-File-.exe ==' -ForegroundColor Cyan
dotnet publish $proj -c Release -o $outDir
if ($LASTEXITCODE -ne 0) { throw "dotnet publish ist fehlgeschlagen (Exit $LASTEXITCODE)." }
if (-not (Test-Path $exe)) { throw "Erwartete .exe nicht gefunden: $exe" }

Write-Host '== Richte Autostart ein (immer an) ==' -ForegroundColor Cyan
$runKey = 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Run'
Set-ItemProperty -Path $runKey -Name $runName -Value ('"{0}"' -f $exe)

Write-Host '== Starte die Sidebar ==' -ForegroundColor Cyan
Start-Process -FilePath $exe

Write-Host ''
Write-Host "Fertig. Die Sidebar laeuft und startet ab jetzt automatisch mit Windows." -ForegroundColor Green
Write-Host "EXE:       $exe"
Write-Host "Autostart: $runKey\$runName"
Write-Host "Bedienung: Maus an den rechten Rand des Haupt-Monitors -> Sidebar faehrt aus."
