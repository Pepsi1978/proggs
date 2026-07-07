# Entfernt die Chrome-Shutdown-Sidebar wieder:
# - beendet die laufende App
# - loescht den Autostart-Eintrag
# - loescht (optional) das installierte Programm-Verzeichnis

$ErrorActionPreference = 'Stop'

$outDir  = Join-Path $env:LOCALAPPDATA 'ChromeShutdownSidebar'
$runName = 'ChromeShutdownSidebar'
$runKey  = 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Run'

Write-Host '== Beende laufende Instanz ==' -ForegroundColor Cyan
Get-Process 'DesktopSidebar' -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue

Write-Host '== Entferne Autostart-Eintrag ==' -ForegroundColor Cyan
if (Get-ItemProperty -Path $runKey -Name $runName -ErrorAction SilentlyContinue) {
    Remove-ItemProperty -Path $runKey -Name $runName
    Write-Host "Autostart-Eintrag entfernt."
} else {
    Write-Host "Kein Autostart-Eintrag vorhanden."
}

Write-Host '== Entferne Programm-Verzeichnis ==' -ForegroundColor Cyan
if (Test-Path $outDir) {
    Start-Sleep -Milliseconds 300
    Remove-Item -Path $outDir -Recurse -Force -ErrorAction SilentlyContinue
    Write-Host "Verzeichnis entfernt: $outDir"
}

Write-Host ''
Write-Host "Die Sidebar wurde vollstaendig entfernt." -ForegroundColor Green
