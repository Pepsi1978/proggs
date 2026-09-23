# Richtet den adb-WLAN-Wachhund in der Windows-Aufgabenplanung ein (idempotent, ohne Adminrechte).
# Läuft bei jeder Anmeldung und danach alle 5 Minuten unsichtbar: adb-wlan.ps1 -Leise.
# Damit ist das Handy nach PC-Neustart, Netzwechsel oder abgerissener Verbindung von selbst wieder da.
# Log: %LOCALAPPDATA%\adb-wlan\adb-wlan.log
$Name = "adb-wlan-wachhund"
$Skript = Join-Path $PSScriptRoot "adb-wlan.ps1"

# conhost --headless: kein aufblitzendes Fenster alle 5 Minuten
$Aktion = New-ScheduledTaskAction -Execute "conhost.exe" `
    -Argument "--headless powershell.exe -NoProfile -ExecutionPolicy Bypass -File `"$Skript`" -Leise"

$Anmeldung = New-ScheduledTaskTrigger -AtLogOn -User "$env:USERDOMAIN\$env:USERNAME"
$Anmeldung.Delay = "PT1M"   # WLAN ist direkt nach der Anmeldung oft noch nicht da
$Wiederholung = New-ScheduledTaskTrigger -Once -At (Get-Date).AddMinutes(1) `
    -RepetitionInterval (New-TimeSpan -Minutes 5)

$Einstellungen = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries `
    -StartWhenAvailable -MultipleInstances IgnoreNew -ExecutionTimeLimit (New-TimeSpan -Minutes 4)

$Prinzipal = New-ScheduledTaskPrincipal -UserId "$env:USERDOMAIN\$env:USERNAME" -LogonType Interactive -RunLevel Limited

Register-ScheduledTask -TaskName $Name -Action $Aktion -Trigger @($Anmeldung, $Wiederholung) `
    -Settings $Einstellungen -Principal $Prinzipal -Force | Out-Null

$t = Get-ScheduledTask -TaskName $Name
Write-Host "Wachhund '$Name' eingerichtet: $($t.State)"
