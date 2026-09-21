# Baut ZeltWach, legt es nach %LOCALAPPDATA%\Programs\ZeltWach, trägt den Autostart ein,
# legt die Sperren-Verknüpfung auf den Desktop und startet das Tool neu.
$ErrorActionPreference = 'Stop'
$ziel = Join-Path $env:LOCALAPPDATA 'Programs\ZeltWach'
$exe = Join-Path $ziel 'ZeltWach.exe'

Get-Process ZeltWach -ErrorAction SilentlyContinue | Stop-Process -Force
dotnet publish "$PSScriptRoot\ZeltWach.csproj" -c Release -r win-x64 --self-contained false -p:PublishSingleFile=true -o $ziel | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'Build fehlgeschlagen' }

Set-ItemProperty 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Run' -Name ZeltWach -Value "`"$exe`""

$desktop = [Environment]::GetFolderPath('Desktop')
$lnk = (New-Object -ComObject WScript.Shell).CreateShortcut((Join-Path $desktop 'Sperren.lnk'))
$lnk.TargetPath = "$env:WINDIR\System32\rundll32.exe"
$lnk.Arguments = 'user32.dll,LockWorkStation'
$lnk.IconLocation = "$env:WINDIR\System32\imageres.dll,54"
$lnk.Description = 'PC sperren, läuft weiter'
$lnk.Save()

Start-Process $exe
"ZeltWach installiert: $exe"
