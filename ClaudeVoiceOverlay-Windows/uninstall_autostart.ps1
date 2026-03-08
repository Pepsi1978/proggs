# uninstall_autostart.ps1 — Entfernt den Autostart fuer ClaudeVoiceOverlay
# Ausfuehren: pwsh -File uninstall_autostart.ps1

$startupLink = [System.IO.Path]::Combine(
    [Environment]::GetFolderPath("Startup"),
    "ClaudeVoiceOverlay.lnk"
)

if (Test-Path $startupLink) {
    Remove-Item $startupLink -Force
    Write-Host "Autostart entfernt." -ForegroundColor Green
} else {
    Write-Host "Kein Autostart-Eintrag gefunden." -ForegroundColor Yellow
}
