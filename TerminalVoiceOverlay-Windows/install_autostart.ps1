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

# Verknuepfung im Autostart-Ordner erstellen — startet den Watcher
# Alte Verknuepfung entfernen falls vorhanden
$oldLink = [System.IO.Path]::Combine([Environment]::GetFolderPath("Startup"), "TerminalVoiceOverlay.lnk")
if (Test-Path $oldLink) { Remove-Item $oldLink; Write-Host "Alte Verknuepfung entfernt." -ForegroundColor Yellow }

$startupLink = [System.IO.Path]::Combine(
    [Environment]::GetFolderPath("Startup"),
    "Spracheingabe - Terminal.lnk"
)

$shell = New-Object -ComObject WScript.Shell
$shortcut = $shell.CreateShortcut($startupLink)
$shortcut.TargetPath = "wscript.exe"
$shortcut.Arguments = """$watcherPath"""
$shortcut.WorkingDirectory = $PSScriptRoot
$shortcut.Description = "Voice Overlay fuer Terminal — Sprache zu Text in PowerShell, CMD, Windows Terminal"
$shortcut.WindowStyle = 7  # Minimiert
$shortcut.Save()

Write-Host "`nAutostart mit Watcher eingerichtet!" -ForegroundColor Green
Write-Host "Verknuepfung: $startupLink"
Write-Host "Watcher: $watcherPath"
Write-Host "Programm: $exePath"
Write-Host "`nDer Watcher startet bei der Windows-Anmeldung und haelt das Overlay am Laufen."
