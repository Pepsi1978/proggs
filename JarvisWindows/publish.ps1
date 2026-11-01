# Baut Jarvis Windows HP, installiert nach %LOCALAPPDATA%\Programs\JarvisWindowsHP, richtet den Wecker ein und startet die App.
# Aufruf: pwsh -File publish.ps1            → bauen, installieren, starten
#         pwsh -File publish.ps1 -NurBauen  → nur bauen
param([switch]$NurBauen)
$ErrorActionPreference = 'Stop'
$projekt = $PSScriptRoot
$ziel = Join-Path $env:LOCALAPPDATA 'Programs\JarvisWindowsHP'
$aus = Join-Path $projekt 'publish'

dotnet publish (Join-Path $projekt 'JarvisWindows.csproj') -c Release -r win-x64 --self-contained true -o $aus --nologo -v q
if ($LASTEXITCODE -ne 0) { throw 'Bau fehlgeschlagen' }
if ($NurBauen) { Write-Output "Gebaut: $aus"; return }

Get-Process JarvisWindows -ErrorAction SilentlyContinue | ForEach-Object {
    $null = $_.CloseMainWindow()
    if (-not $_.WaitForExit(3000)) { Stop-Process -Id $_.Id -Force }
}
New-Item -ItemType Directory -Force $ziel | Out-Null
robocopy $aus $ziel /MIR /NFL /NDL /NJH /NJS /NP | Out-Null
if ($LASTEXITCODE -ge 8) { throw "Kopieren fehlgeschlagen ($LASTEXITCODE)" }
$global:LASTEXITCODE = 0

$exe = Join-Path $ziel 'JarvisWindows.exe'
$startmenue = Join-Path ([Environment]::GetFolderPath('Programs')) 'Jarvis Windows HP.lnk'
$sh = New-Object -ComObject WScript.Shell
$lnk = $sh.CreateShortcut($startmenue)
$lnk.TargetPath = $exe
$lnk.WorkingDirectory = $ziel
$lnk.Save()
$desktop = Join-Path ([Environment]::GetFolderPath('Desktop')) 'Jarvis Windows HP.lnk'
$lnk = $sh.CreateShortcut($desktop)
$lnk.TargetPath = $exe
$lnk.WorkingDirectory = $ziel
$lnk.Save()

# Wecker: hält den HP beim Relay als verbunden und startet die App, wenn das Handy es verlangt.
$wecker = Join-Path $projekt 'tools\hp-wecker.ps1'
$pwsh = (Get-Command pwsh -ErrorAction SilentlyContinue).Source
if (-not $pwsh) { $pwsh = (Get-Command powershell).Source }
$aktion = New-ScheduledTaskAction -Execute 'conhost.exe' -Argument "--headless `"$pwsh`" -NoProfile -ExecutionPolicy Bypass -File `"$wecker`""
$ausloeser = New-ScheduledTaskTrigger -AtLogOn -User $env:USERNAME
$regeln = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -StartWhenAvailable -MultipleInstances IgnoreNew -ExecutionTimeLimit ([TimeSpan]::Zero)
Register-ScheduledTask -TaskName 'JarvisWindowsWecker' -Action $aktion -Trigger $ausloeser -Settings $regeln -Force | Out-Null
if ((Get-ScheduledTask -TaskName 'JarvisWindowsWecker').State -ne 'Running') { Start-ScheduledTask -TaskName 'JarvisWindowsWecker' }

Start-Process -FilePath $exe
Write-Output "Installiert und gestartet: $exe"
