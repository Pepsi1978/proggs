# Richtet das Vorwärmen am Windows-PC ein (mehrfach ausführbar):
# 1. Räumt eine frühere globale Leerlauf-Grenze aus ~/.gradle/gradle.properties (24 h gilt nur für den vorgewärmten Daemon).
# 2. Aufgabe "Vorwaermen" in der Aufgabenplanung: bei der Anmeldung (30 s verzögert, startet den Daemon), nach dem Aufwachen
#    aus dem Standby und alle 5 Minuten; unabhängig von OpenLauncher und CLIs.
#    Jeder Lauf spult das Repo vor (nur Fast-Forward) und baut geänderte Apps von Handy/Tablet nach.
# Danach den ersten Lauf mit Start-ScheduledTask anstoßen, nicht aus einer Agenten-Shell (Pipe-Vererbung).
$ErrorActionPreference = 'Stop'

$props = Join-Path $env:USERPROFILE '.gradle\gradle.properties'
New-Item -ItemType Directory -Force -Path (Split-Path $props) | Out-Null
$zeilen = @(if (Test-Path $props) { Get-Content $props -Encoding utf8 })
# Keine globale Leerlauf-Grenze: 24 h gibt vorwaermen.py nur seinem eigenen Daemon mit (Kommandozeile). Global hielten sonst
# auch alle Sitzungs-Daemons paralleler Bauten (je 2-4 GB) einen Tag lang Speicher fest; sie behalten Gradles 3 h.
$zeilen = @($zeilen | Where-Object { $_ -notmatch '^\s*org\.gradle\.daemon\.idletimeout\s*=' })
[IO.File]::WriteAllLines($props, [string[]]$zeilen, [Text.UTF8Encoding]::new($false))

$py = (Get-Command python -ErrorAction Stop).Source
$pyw = Join-Path (Split-Path $py) 'pythonw.exe'
if (-not (Test-Path $pyw)) { $pyw = $py }
$skript = Join-Path $PSScriptRoot 'vorwaermen.py'

$aktion = New-ScheduledTaskAction -Execute $pyw -Argument "`"$skript`" lauf"
$anmeldung = New-ScheduledTaskTrigger -AtLogOn -User "$env:USERDOMAIN\$env:USERNAME"
$anmeldung.Delay = 'PT30S'  # Daemon gleich nach dem Windows-Start (Frank, 13.11.2026)
$takt = New-ScheduledTaskTrigger -Once -At (Get-Date).AddMinutes(1) -RepetitionInterval (New-TimeSpan -Minutes 5)
# Aufwachen aus dem Standby (Power-Troubleshooter, Ereignis 1): sofort abgleichen statt bis zum nächsten Takt warten.
$klasse = Get-CimClass -ClassName MSFT_TaskEventTrigger -Namespace Root/Microsoft/Windows/TaskScheduler
$aufwachen = New-CimInstance -CimClass $klasse -ClientOnly
$aufwachen.Enabled = $true
$aufwachen.Delay = 'PT30S'
$aufwachen.Subscription = '<QueryList><Query Id="0" Path="System"><Select Path="System">*[System[Provider[@Name=''Microsoft-Windows-Power-Troubleshooter''] and EventID=1]]</Select></Query></QueryList>'
$einst = New-ScheduledTaskSettingsSet -MultipleInstances IgnoreNew -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries `
    -StartWhenAvailable -ExecutionTimeLimit (New-TimeSpan -Hours 8)
$prinzipal = New-ScheduledTaskPrincipal -UserId "$env:USERDOMAIN\$env:USERNAME" -LogonType Interactive -RunLevel Limited
Register-ScheduledTask -TaskName 'Vorwaermen' -Action $aktion -Trigger $anmeldung, $takt, $aufwachen -Settings $einst -Principal $prinzipal `
    -Description 'Repo vorspulen und geänderte Android-Apps vorbauen (Werkzeuge/vorwaermen)' -Force | Out-Null
"VORWAERMEN_EINRICHTEN=ok"
