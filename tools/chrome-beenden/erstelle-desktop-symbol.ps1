# Erstellt ein Desktop-Symbol, das den kompletten Chrome-Browser beendet.
# Doppelklick auf das Symbol -> alle chrome.exe-Prozesse (inkl. Tabs/Kindprozesse) werden erzwungen beendet.
# Reproduzierbar: Dieses Script kann auf jedem Windows-Rechner erneut ausgefuehrt werden, um das Symbol neu anzulegen.

$ErrorActionPreference = 'Stop'

# Desktop-Pfad plattformrobust ermitteln
$desktop = [Environment]::GetFolderPath('Desktop')
$linkPath = Join-Path $desktop 'Chrome beenden.lnk'

# taskkill.exe ist der Befehl, der Chrome beendet
$taskkill = Join-Path $env:SystemRoot 'System32\taskkill.exe'

# Chrome-Icon fuer das Symbol suchen (Fallback: taskkill-Icon)
$iconCandidates = @(
    "$env:ProgramFiles\Google\Chrome\Application\chrome.exe",
    "${env:ProgramFiles(x86)}\Google\Chrome\Application\chrome.exe",
    "$env:LocalAppData\Google\Chrome\Application\chrome.exe"
)
$iconPath = $taskkill
foreach ($c in $iconCandidates) {
    if (Test-Path $c) { $iconPath = $c; break }
}

# Verknuepfung anlegen
$shell = New-Object -ComObject WScript.Shell
$shortcut = $shell.CreateShortcut($linkPath)
$shortcut.TargetPath = $taskkill
$shortcut.Arguments = '/F /IM chrome.exe /T'
$shortcut.IconLocation = "$iconPath,0"
$shortcut.WindowStyle = 7          # 7 = minimiert (kein stoerendes Fenster)
$shortcut.Description = 'Beendet den kompletten Chrome-Browser (alle Fenster und Tabs)'
$shortcut.WorkingDirectory = $env:SystemRoot
$shortcut.Save()

Write-Output "Symbol erstellt: $linkPath"
Write-Output "Icon: $iconPath"
