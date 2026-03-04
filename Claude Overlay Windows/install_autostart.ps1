$ErrorActionPreference = "Stop"

$startupPath = [Environment]::GetFolderPath("Startup")
$shortcutPath = Join-Path $startupPath "Claude Overlay Watcher.lnk"
$targetPath = Join-Path $PSScriptRoot "start_watcher.bat"

$wsh = New-Object -ComObject WScript.Shell
$shortcut = $wsh.CreateShortcut($shortcutPath)
$shortcut.TargetPath = $targetPath
$shortcut.WorkingDirectory = $PSScriptRoot
$shortcut.WindowStyle = 7
$shortcut.Description = "Startet den Claude Overlay Watcher"
$shortcut.Save()

Write-Host "Autostart eingerichtet: $shortcutPath"
