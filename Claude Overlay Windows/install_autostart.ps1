$ErrorActionPreference = "Stop"

$startupPath = [Environment]::GetFolderPath("Startup")
$shortcutPath = Join-Path $startupPath "Claude Overlay Watcher.lnk"
$wscriptPath = "C:\Windows\System32\wscript.exe"

# Beide VBS-Varianten pruefen (neuer Name bevorzugt)
$vbsPath = Join-Path $PSScriptRoot "start_watcher.vbs"
if (-not (Test-Path $vbsPath)) {
    $vbsPath = Join-Path $PSScriptRoot "start_overlay.vbs"
}
if (-not (Test-Path $vbsPath)) {
    Write-Host "FEHLER: Kein VBS-Startskript gefunden!" -ForegroundColor Red
    exit 1
}

# Pruefe ob venv existiert
$venvPython = Join-Path $PSScriptRoot ".venv\Scripts\pythonw.exe"
if (-not (Test-Path $venvPython)) {
    Write-Host "WARNUNG: Python venv nicht gefunden unter $venvPython" -ForegroundColor Yellow
    Write-Host "Bitte zuerst 'python -m venv .venv' und 'pip install -r requirements.txt' ausfuehren." -ForegroundColor Yellow
}

# Alte Verknuepfung entfernen falls vorhanden
if (Test-Path $shortcutPath) {
    Remove-Item $shortcutPath -Force
    Write-Host "Alte Autostart-Verknuepfung entfernt."
}

# Neue Verknuepfung erstellen: wscript.exe startet das VBS komplett unsichtbar
$wsh = New-Object -ComObject WScript.Shell
$shortcut = $wsh.CreateShortcut($shortcutPath)
$shortcut.TargetPath = $wscriptPath
$shortcut.Arguments = """$vbsPath"""
$shortcut.WorkingDirectory = $PSScriptRoot
$shortcut.WindowStyle = 7  # Minimiert, kein Fokus
$shortcut.Description = "Startet den Claude Overlay Watcher (unsichtbar via VBS)"
$shortcut.Save()

Write-Host ""
Write-Host "Autostart eingerichtet!" -ForegroundColor Green
Write-Host "  Verknuepfung: $shortcutPath"
Write-Host "  Startet:      wscript.exe -> $($vbsPath | Split-Path -Leaf) -> pythonw.exe watcher.py"
Write-Host ""
Write-Host "Kette: wscript.exe (unsichtbar) -> VBS -> pythonw.exe (kein Fenster) -> watcher.py"
Write-Host "  => Kein Konsolenfenster, kein Taskleisten-Eintrag."
Write-Host ""
Write-Host "Nach dem naechsten Neustart wird der Watcher automatisch gestartet."
