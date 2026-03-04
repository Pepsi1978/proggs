$ErrorActionPreference = "Stop"

$startupPath = [Environment]::GetFolderPath("Startup")
$shortcutPath = Join-Path $startupPath "Claude Overlay Watcher.lnk"

if (Test-Path $shortcutPath) {
    Remove-Item $shortcutPath -Force
    Write-Host "Autostart entfernt: $shortcutPath"
} else {
    Write-Host "Kein Autostart-Eintrag gefunden."
}
