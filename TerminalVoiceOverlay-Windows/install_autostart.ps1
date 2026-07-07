# install_autostart.ps1 — Richtet den Autostart mit Watcher fuer TerminalVoiceOverlay ein
# Ausfuehren: pwsh -File install_autostart.ps1

$ErrorActionPreference = "Stop"

$exePath = Join-Path $PSScriptRoot "publish\TerminalVoiceOverlay.exe"
$watcherPath = Join-Path $PSScriptRoot "watcher.vbs"

if (-not (Test-Path $exePath)) {
    Write-Host "Fehler: $exePath nicht gefunden." -ForegroundColor Red
    Write-Host "Bitte zuerst 'pwsh -File publish.ps1' ausfuehren." -ForegroundColor Yellow
    exit 1
}

if (-not (Test-Path $watcherPath)) {
    Write-Host "Fehler: $watcherPath nicht gefunden." -ForegroundColor Red
    exit 1
}

# .env neben die .exe kopieren falls noch nicht vorhanden
$envSource = Join-Path $PSScriptRoot ".env"
$envTarget = Join-Path $PSScriptRoot "publish\.env"
if ((Test-Path $envSource) -and -not (Test-Path $envTarget)) {
    Copy-Item $envSource $envTarget
    Write-Host ".env nach publish/ kopiert." -ForegroundColor Cyan
}

# Alte Verknuepfung aus Autostart-Ordner entfernen falls vorhanden
$startup = [Environment]::GetFolderPath("Startup")
$oldLinks = @("TerminalVoiceOverlay.lnk", "Spracheingabe - Terminal.lnk")
foreach ($old in $oldLinks) {
    $oldPath = Join-Path $startup $old
    if (Test-Path $oldPath) { Remove-Item $oldPath; Write-Host "Alte Verknuepfung entfernt: $old" -ForegroundColor Yellow }
}

# Alte Aufgabe loeschen falls vorhanden
Unregister-ScheduledTask -TaskName "Spracheingabe - Terminal" -Confirm:$false -ErrorAction SilentlyContinue

# Aufgabenplaner-Eintrag erstellen (zeigt korrekten Namen im Autostart)
$action = New-ScheduledTaskAction `
    -Execute "wscript.exe" `
    -Argument """$watcherPath""" `
    -WorkingDirectory $PSScriptRoot
$trigger = New-ScheduledTaskTrigger -AtLogOn -User $env:USERNAME
$settings = New-ScheduledTaskSettingsSet `
    -AllowStartIfOnBatteries `
    -DontStopIfGoingOnBatteries `
    -ExecutionTimeLimit ([TimeSpan]::Zero)
Register-ScheduledTask `
    -TaskName "Spracheingabe - Terminal" `
    -Action $action `
    -Trigger $trigger `
    -Settings $settings `
    -Description "Voice Overlay fuer Terminal — Sprache zu Text in PowerShell, CMD, Windows Terminal" | Out-Null

Write-Host "`nAutostart mit Watcher eingerichtet!" -ForegroundColor Green
Write-Host "Aufgabe: Spracheingabe - Terminal" -ForegroundColor White
Write-Host "Watcher: $watcherPath"
Write-Host "Programm: $exePath"
Write-Host "`nDer Watcher startet bei der Windows-Anmeldung und haelt das Overlay am Laufen."
Write-Host "Sichtbar unter: Einstellungen > Apps > Autostart" -ForegroundColor Cyan
