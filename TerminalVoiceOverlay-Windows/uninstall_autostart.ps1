# uninstall_autostart.ps1 — Entfernt den Autostart fuer TerminalVoiceOverlay
# Ausfuehren: pwsh -File uninstall_autostart.ps1

$startupLink = [System.IO.Path]::Combine(
    [Environment]::GetFolderPath("Startup"),
    "TerminalVoiceOverlay.lnk"
)

if (Test-Path $startupLink) {
    Remove-Item $startupLink -Force
    Write-Host "Autostart entfernt." -ForegroundColor Green
} else {
    Write-Host "Kein Autostart-Eintrag gefunden." -ForegroundColor Yellow
}
