#requires -Version 5.1
<#
    Richtet den normalen Start von Codex Desktop ein (OHNE Administratorrechte):

      1. Autostart-Aufgabe "Codex Desktop - Start im System-Tray"
         -> laeuft bei der Anmeldung mit normalen Rechten, startet Codex im Tray
      2. Desktop-Verknuepfung "Codex.lnk"
         -> startet Codex sichtbar

    Beide zeigen auf Start-Codex.ps1 in DIESEM Repo-Ordner. Die fruehere Admin-Aufgabe
    wird entfernt; dafuer ist einmalig eine UAC-Abfrage noetig.
#>
# Version 2.0.0 - 30.09.2026, 18:00 Uhr
$ErrorActionPreference = 'Stop'

$identitaet = [Security.Principal.WindowsIdentity]::GetCurrent()
if (-not (New-Object Security.Principal.WindowsPrincipal($identitaet)).IsInRole(
        [Security.Principal.WindowsBuiltInRole]::Administrator)) {
    Start-Process -FilePath (Join-Path $env:WINDIR 'System32\WindowsPowerShell\v1.0\powershell.exe') `
                  -ArgumentList ('-NoLogo -NoProfile -ExecutionPolicy Bypass -File "' + $PSCommandPath + '"') `
                  -Verb RunAs
    exit 0
}

$wurzel   = $PSScriptRoot
$launcher = Join-Path $wurzel 'Start-Codex.ps1'
$icon     = Join-Path $wurzel 'codex.ico'
if (-not (Test-Path -LiteralPath $launcher)) { throw "Launcher fehlt: $launcher" }

$ps        = Join-Path $env:WINDIR 'System32\WindowsPowerShell\v1.0\powershell.exe'
$basisArgs = '-NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -WindowStyle Hidden -File "' + $launcher + '"'
$taskName  = 'Codex Desktop - Start im System-Tray'

# --- Autostart-Aufgabe ------------------------------------------------------
Get-ScheduledTask -ErrorAction SilentlyContinue |
    Where-Object { $_.TaskName -match '^Codex Desktop .+Start im System-Tray$' } |
    ForEach-Object { Unregister-ScheduledTask -TaskName $_.TaskName -Confirm:$false }

$aktion    = New-ScheduledTaskAction -Execute $ps -Argument ($basisArgs + ' -Background')
$ausloeser = New-ScheduledTaskTrigger -AtLogOn -User $env:USERNAME
$ausloeser.Delay = 'PT20S'
# RunLevel Limited: normale Rechte. Nie Highest - sonst laeuft Codex ohne Paket-Identitaet
# bzw. erhoeht, und der eingebaute Updater funktioniert nicht.
$prinzipal = New-ScheduledTaskPrincipal -UserId $identitaet.User.Value -LogonType Interactive -RunLevel Limited
$optionen  = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries `
                                          -ExecutionTimeLimit ([TimeSpan]::Zero) -StartWhenAvailable

Register-ScheduledTask -TaskName $taskName -Action $aktion -Trigger $ausloeser `
                       -Principal $prinzipal -Settings $optionen `
                       -Description 'Startet Codex Desktop bei der Anmeldung normal im System-Tray.' | Out-Null

# --- Desktop-Verknuepfung (ohne "Als Administrator ausfuehren") ---------------
$ziel = Join-Path ([Environment]::GetFolderPath('Desktop')) 'Codex.lnk'
if (Test-Path -LiteralPath $ziel) { Remove-Item -LiteralPath $ziel -Force }
$link = (New-Object -ComObject WScript.Shell).CreateShortcut($ziel)
$link.TargetPath       = $ps
$link.Arguments        = $basisArgs
$link.WorkingDirectory = $env:USERPROFILE
$link.WindowStyle      = 7
$link.Description      = 'Codex Desktop starten'
if (Test-Path -LiteralPath $icon) { $link.IconLocation = "$icon,0" }
$link.Save()

Write-Host ''
Write-Host 'Fertig.' -ForegroundColor Green
Write-Host "  Autostart-Aufgabe   : $taskName (normale Rechte)"
Write-Host "  Desktop-Verknuepfung: $ziel"
Write-Host "  Launcher            : $launcher"
Write-Host ''
