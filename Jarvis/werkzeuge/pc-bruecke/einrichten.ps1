# Richtet die Jarvis-PC-Brücke ein: Aufgabe „JarvisPcBruecke“ startet sie bei jeder Anmeldung unsichtbar und startet
# sie neu, falls sie endet. Läuft ohne Administratorrechte (nur für den angemeldeten Benutzer). Mehrfach ausführbar.
$ErrorActionPreference = 'Stop'
$skript = Join-Path $PSScriptRoot 'jarvis-pc-bruecke.ps1'
$pwsh = (Get-Command pwsh.exe -ErrorAction SilentlyContinue).Source
if (-not $pwsh) { $pwsh = (Get-Command powershell.exe).Source }

$aktion = New-ScheduledTaskAction -Execute $pwsh -Argument "-NoProfile -WindowStyle Hidden -ExecutionPolicy Bypass -File `"$skript`""
$ausloeser = New-ScheduledTaskTrigger -AtLogOn -User "$env:USERDOMAIN\$env:USERNAME"
$einstellungen = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -StartWhenAvailable `
    -ExecutionTimeLimit ([TimeSpan]::Zero) -RestartCount 999 -RestartInterval (New-TimeSpan -Minutes 1) -MultipleInstances IgnoreNew
$haupt = New-ScheduledTaskPrincipal -UserId "$env:USERDOMAIN\$env:USERNAME" -LogonType Interactive -RunLevel Limited
Register-ScheduledTask -TaskName 'JarvisPcBruecke' -Action $aktion -Trigger $ausloeser -Settings $einstellungen -Principal $haupt -Force | Out-Null
Start-ScheduledTask -TaskName 'JarvisPcBruecke'
Write-Output 'JarvisPcBruecke eingerichtet und gestartet. Protokoll: %LOCALAPPDATA%\JarvisPcBruecke\bruecke.log'
