# Baut ZeltWach, legt es nach %LOCALAPPDATA%\Programs\ZeltWach, trägt den Autostart ein,
# legt die Sperren-Verknüpfung auf den Desktop und startet das Tool neu.
$ErrorActionPreference = 'Stop'
$ziel = Join-Path $env:LOCALAPPDATA 'Programs\ZeltWach'
$exe = Join-Path $ziel 'ZeltWach.exe'

Get-Process ZeltWach -ErrorAction SilentlyContinue | Stop-Process -Force

# Einmalig (UAC): Schlüssel für den Fingerabdruck-Schalter anlegen und dem Benutzer Schreibrecht geben.
$regPfad = 'HKLM:\SOFTWARE\Policies\Microsoft\Biometrics\Credential Provider'
$schreibbar = $false
try { $k = [Microsoft.Win32.Registry]::LocalMachine.OpenSubKey('SOFTWARE\Policies\Microsoft\Biometrics\Credential Provider', $true); $schreibbar = $null -ne $k; if ($k) { $k.Close() } } catch {}
# Beim Systemstart gibt ein SYSTEM-Task den Fingerabdruck frei, bevor der Anmeldebildschirm kommt –
# sonst bliebe ein im Zeltmodus gesetztes Enabled=0 über den Neustart hinweg stehen.
# Der Auslöser ist ein Ereignis (Kernel-Boot 27 = jeder Start, auch Schnellstart; Power-Troubleshooter 1 = Aufwachen):
# "Beim Systemstart" feuert bei aktivem Schnellstart nicht.
# Der Task gehört SYSTEM und ist ohne Administratorrechte nicht lesbar. Deshalb merkt sich der Admin-Teil
# den eingerichteten Stand in HKLM\SOFTWARE\ZeltWach; nur wenn der fehlt oder älter ist, kommt die UAC-Abfrage.
$taskStand = 2
$taskFehlt = (Get-ItemProperty 'HKLM:\SOFTWARE\ZeltWach' -ErrorAction SilentlyContinue).TaskStand -ne $taskStand
if (-not $schreibbar -or $taskFehlt) {
    $benutzer = "$env:USERDOMAIN\$env:USERNAME"
    $admin = @"
New-Item -Path '$regPfad' -Force | Out-Null
`$acl = Get-Acl '$regPfad'
`$acl.AddAccessRule((New-Object System.Security.AccessControl.RegistryAccessRule('$benutzer','FullControl','Allow')))
Set-Acl '$regPfad' `$acl
schtasks /create /f /tn 'ZeltWach Fingerabdruck freigeben' /ru SYSTEM /rl HIGHEST /sc ONEVENT /ec System /mo "*[System[(Provider[@Name='Microsoft-Windows-Kernel-Boot'] and EventID=27) or (Provider[@Name='Microsoft-Windows-Power-Troubleshooter'] and EventID=1)]]" /tr 'reg.exe delete \"HKLM\SOFTWARE\Policies\Microsoft\Biometrics\Credential Provider\" /v Enabled /f' | Out-Null
if (`$LASTEXITCODE -eq 0) { New-Item -Path 'HKLM:\SOFTWARE\ZeltWach' -Force | Out-Null; Set-ItemProperty 'HKLM:\SOFTWARE\ZeltWach' -Name TaskStand -Value $taskStand -Type DWord }
"@
    $b64 = [Convert]::ToBase64String([Text.Encoding]::Unicode.GetBytes($admin))
    Start-Process powershell -Verb RunAs -Wait -WindowStyle Hidden -ArgumentList "-NoProfile -EncodedCommand $b64"
}
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
