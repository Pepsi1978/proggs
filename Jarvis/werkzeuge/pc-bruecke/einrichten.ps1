# Richtet die Jarvis-PC-Brücke ein: Aufgabe „JarvisPcBruecke“ startet sie bei jeder Anmeldung unsichtbar und startet
# sie jede Minute neu, falls sie nicht mehr läuft. Läuft ohne Administratorrechte (nur für den angemeldeten Benutzer). Mehrfach ausführbar.
$ErrorActionPreference = 'Stop'
$skript = Join-Path $PSScriptRoot 'jarvis-pc-bruecke.ps1'
$pwsh = (Get-Command pwsh.exe -ErrorAction SilentlyContinue).Source
if (-not $pwsh) { $pwsh = (Get-Command powershell.exe).Source }

# conhost --headless: kein aufblitzendes Konsolenfenster, auch nicht beim Wächter-Start jede Minute.
# Zugleich hängt die Brücke so an keinem Terminal-Fenster: Unter Windows 11 landete die versteckte Konsole sonst in
# Windows Terminal und wurde mit dessen Fenster beendet (0xC000013A, 15.10.2026).
$conhost = Join-Path $env:SystemRoot 'System32\conhost.exe'
$aktion = New-ScheduledTaskAction -Execute $conhost -Argument "--headless `"$pwsh`" -NoProfile -ExecutionPolicy Bypass -File `"$skript`""
$ausloeser = New-ScheduledTaskTrigger -AtLogOn -User "$env:USERDOMAIN\$env:USERNAME"
# Wächter: jede Minute erneut starten. Läuft die Brücke schon, beendet sich die neue sofort (Sperre im Skript).
# Nötig, weil Windows ein Beenden von außen (z. B. 0xC000013A) nicht als Fehler wertet und dann nicht neu startet.
$waechter = New-ScheduledTaskTrigger -Once -At (Get-Date).AddMinutes(1) -RepetitionInterval (New-TimeSpan -Minutes 1)
$ausloeser = @($ausloeser, $waechter)
$einstellungen = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -StartWhenAvailable `
    -ExecutionTimeLimit ([TimeSpan]::Zero) -RestartCount 999 -RestartInterval (New-TimeSpan -Minutes 1) -MultipleInstances IgnoreNew
$haupt = New-ScheduledTaskPrincipal -UserId "$env:USERDOMAIN\$env:USERNAME" -LogonType Interactive -RunLevel Limited
Register-ScheduledTask -TaskName 'JarvisPcBruecke' -Action $aktion -Trigger $ausloeser -Settings $einstellungen -Principal $haupt -Force | Out-Null
Start-ScheduledTask -TaskName 'JarvisPcBruecke'
Write-Output 'JarvisPcBruecke eingerichtet und gestartet. Protokoll: %LOCALAPPDATA%\JarvisPcBruecke\bruecke.log'
