# Richtet das Vorwärmen am Windows-PC ein (mehrfach ausführbar):
# 1. Gradle-Daemon bleibt 24 Stunden im Leerlauf warm (~/.gradle/gradle.properties, wie am Server).
# 2. Aufgabe "Vorwaermen" in der Aufgabenplanung: bei der Anmeldung (30 s verzögert, startet den Daemon) und alle 10 Minuten.
#    Jeder Lauf spult das Repo vor (nur Fast-Forward) und baut geänderte Apps von Handy/Tablet nach.
# Danach den ersten Lauf mit Start-ScheduledTask anstoßen, nicht aus einer Agenten-Shell (Pipe-Vererbung).
$ErrorActionPreference = 'Stop'

$props = Join-Path $env:USERPROFILE '.gradle\gradle.properties'
New-Item -ItemType Directory -Force -Path (Split-Path $props) | Out-Null
$zeilen = @(if (Test-Path $props) { Get-Content $props -Encoding utf8 })
$zeilen = @($zeilen | Where-Object { $_ -notmatch '^\s*org\.gradle\.daemon\.idletimeout\s*=' })
$zeilen += 'org.gradle.daemon.idletimeout=86400000'
[IO.File]::WriteAllLines($props, [string[]]$zeilen, [Text.UTF8Encoding]::new($false))

$py = (Get-Command python -ErrorAction Stop).Source
$pyw = Join-Path (Split-Path $py) 'pythonw.exe'
if (-not (Test-Path $pyw)) { $pyw = $py }
$skript = Join-Path $PSScriptRoot 'vorwaermen.py'

$aktion = New-ScheduledTaskAction -Execute $pyw -Argument "`"$skript`" lauf"
$anmeldung = New-ScheduledTaskTrigger -AtLogOn -User "$env:USERDOMAIN\$env:USERNAME"
$anmeldung.Delay = 'PT30S'  # Daemon gleich nach dem Windows-Start (Frank, 13.11.2026)
$takt = New-ScheduledTaskTrigger -Once -At (Get-Date).AddMinutes(1) -RepetitionInterval (New-TimeSpan -Minutes 10)
$einst = New-ScheduledTaskSettingsSet -MultipleInstances IgnoreNew -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries `
    -StartWhenAvailable -ExecutionTimeLimit (New-TimeSpan -Hours 2)
$prinzipal = New-ScheduledTaskPrincipal -UserId "$env:USERDOMAIN\$env:USERNAME" -LogonType Interactive -RunLevel Limited
Register-ScheduledTask -TaskName 'Vorwaermen' -Action $aktion -Trigger $anmeldung, $takt -Settings $einst -Principal $prinzipal `
    -Description 'Repo vorspulen und geänderte Android-Apps vorbauen (Werkzeuge/vorwaermen)' -Force | Out-Null
"VORWAERMEN_EINRICHTEN=ok"
