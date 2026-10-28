# Einmalig: richtet die Windows-Fernsteuerung ein. Aufgabe „JarvisFernsteuerung“ startet sie bei jeder Anmeldung
# unsichtbar in der Benutzersitzung (nur dort gibt es Bild und Eingaben) und prüft jede Minute, ob sie noch läuft.
# Ohne Administratorrechte. Mehrfach ausführbar.
# Token: derselbe wie am Mac, liegt in ~/SK/Jarvis/fernsteuerung-token.txt (fehlt er, vom Handy holen:
#   adb -s <IP>:5555 shell "run-as de.frank.jarvis cat files/fernsteuerung-token.txt" > ~/SK/Jarvis/fernsteuerung-token.txt)
$ErrorActionPreference = 'Stop'
$token = Join-Path $HOME 'SK\Jarvis\fernsteuerung-token.txt'
if (-not (Test-Path $token)) { throw "Token fehlt: $token" }
# Virtueller Bildschirm für den zugeklappten Laptop (virtueller_bildschirm.py): Parsec-Treiber, einmalig mit Adminrechten.
if (-not (Get-PnpDevice -FriendlyName 'Parsec Virtual Display Adapter' -ErrorAction SilentlyContinue)) {
    Start-Process winget -Verb RunAs -Wait -WindowStyle Hidden -ArgumentList 'install --id Parsec.ParsecVDD -e --silent --accept-package-agreements --accept-source-agreements --disable-interactivity'
}
$skript = Join-Path $PSScriptRoot 'starten.ps1'
$pwsh = (Get-Command pwsh.exe -ErrorAction SilentlyContinue).Source
if (-not $pwsh) { $pwsh = (Get-Command powershell.exe).Source }
# conhost --headless: kein aufblitzendes Konsolenfenster beim Wächter-Start jede Minute (wie JarvisPcBruecke).
$conhost = Join-Path $env:SystemRoot 'System32\conhost.exe'
$aktion = New-ScheduledTaskAction -Execute $conhost -Argument "--headless `"$pwsh`" -NoProfile -ExecutionPolicy Bypass -File `"$skript`""
$anmeldung = New-ScheduledTaskTrigger -AtLogOn -User "$env:USERDOMAIN\$env:USERNAME"
$waechter = New-ScheduledTaskTrigger -Once -At (Get-Date).AddMinutes(1) -RepetitionInterval (New-TimeSpan -Minutes 1)
$einstellungen = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -StartWhenAvailable `
    -ExecutionTimeLimit ([TimeSpan]::Zero) -MultipleInstances IgnoreNew
$haupt = New-ScheduledTaskPrincipal -UserId "$env:USERDOMAIN\$env:USERNAME" -LogonType Interactive -RunLevel Limited
Register-ScheduledTask -TaskName 'JarvisFernsteuerung' -Action $aktion -Trigger @($anmeldung, $waechter) -Settings $einstellungen -Principal $haupt -Force | Out-Null
Start-ScheduledTask -TaskName 'JarvisFernsteuerung'
Write-Output 'JarvisFernsteuerung eingerichtet und gestartet. Protokoll: %LOCALAPPDATA%\JarvisFernsteuerung\'
