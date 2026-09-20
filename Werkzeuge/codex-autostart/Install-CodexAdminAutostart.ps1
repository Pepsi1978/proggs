#requires -Version 5.1
<#
    Richtet den Administrator-Start von Codex Desktop ein:

      1. Autostart-Aufgabe "Codex Desktop - Start im System-Tray"
         -> laeuft bei der Anmeldung mit hoechsten Rechten (ohne UAC-Abfrage)
         -> startet Codex versteckt im Tray
      2. Desktop-Verknuepfung "Codex.lnk"
         -> startet Codex sichtbar mit Administratorrechten

    Beide zeigen auf Start-CodexAdmin.ps1 in DIESEM Repo-Ordner.
    Einfach ausfuehren; das Skript holt sich Adminrechte selbst.
#>
# Version 1.2.0 - 20.09.2026, 12:24 Uhr
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
$launcher = Join-Path $wurzel 'Start-CodexAdmin.ps1'
$icon     = Join-Path $wurzel 'codex.ico'
if (-not (Test-Path -LiteralPath $launcher)) { throw "Launcher fehlt: $launcher" }

$ps        = Join-Path $env:WINDIR 'System32\WindowsPowerShell\v1.0\powershell.exe'
$basisArgs = '-NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -WindowStyle Hidden -File "' + $launcher + '"'
$taskName  = 'Codex Desktop - Start im System-Tray'

# --- Autostart-Aufgabe ------------------------------------------------------
# Alte Aufgaben ueber ein Muster suchen, nicht ueber den exakten Namen: eine frueher
# angelegte Fassung hiess "Codex Desktop <Gedankenstrich> Start im System-Tray", und
# Sonderzeichen ueberleben den Weg durch Datei-Kodierungen nicht zuverlaessig.
Get-ScheduledTask -ErrorAction SilentlyContinue |
    Where-Object { $_.TaskName -match '^Codex Desktop .+Start im System-Tray$' } |
    ForEach-Object { Unregister-ScheduledTask -TaskName $_.TaskName -Confirm:$false }

$aktion    = New-ScheduledTaskAction -Execute $ps -Argument ($basisArgs + ' -Background')
$ausloeser = New-ScheduledTaskTrigger -AtLogOn -User $env:USERNAME
$ausloeser.Delay = 'PT20S'   # dem System nach der Anmeldung Luft lassen
$prinzipal = New-ScheduledTaskPrincipal -UserId $identitaet.User.Value -LogonType Interactive -RunLevel Highest
$optionen  = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries `
                                          -ExecutionTimeLimit ([TimeSpan]::Zero) -StartWhenAvailable

Register-ScheduledTask -TaskName $taskName -Action $aktion -Trigger $ausloeser `
                       -Principal $prinzipal -Settings $optionen `
                       -Description 'Startet Codex Desktop bei der Anmeldung mit Administratorrechten im System-Tray.' | Out-Null

# --- Desktop-Verknuepfung ---------------------------------------------------
$ziel = Join-Path ([Environment]::GetFolderPath('Desktop')) 'Codex.lnk'
$link = (New-Object -ComObject WScript.Shell).CreateShortcut($ziel)
$link.TargetPath       = $ps
$link.Arguments        = $basisArgs
$link.WorkingDirectory = $env:USERPROFILE
$link.WindowStyle      = 7    # minimiert starten, das PowerShell-Fenster bleibt unsichtbar
$link.Description      = 'Codex Desktop mit Administratorrechten starten'
if (Test-Path -LiteralPath $icon) { $link.IconLocation = "$icon,0" }
$link.Save()

# Im .lnk das "Als Administrator ausfuehren"-Bit setzen (Byte 21, Flag 0x20).
$bytes = [IO.File]::ReadAllBytes($ziel)
$bytes[21] = $bytes[21] -bor 0x20
[IO.File]::WriteAllBytes($ziel, $bytes)

Write-Host ''
Write-Host 'Fertig.' -ForegroundColor Green
Write-Host "  Autostart-Aufgabe : $taskName"
Write-Host "  Desktop-Verknuepfung: $ziel"
Write-Host "  Launcher          : $launcher"
Write-Host ''
