# Prueft den Shutdown-Task "Chrome beim Herunterfahren beenden" und schreibt das
# Ergebnis in eine Log-Datei, die jede normale Session lesen kann.
#
# WICHTIG: Ein als SYSTEM laufender Task kann NUR mit Admin-Rechten abgefragt
# werden. Ohne Admin liefert schtasks "Zugriff verweigert" und Get-ScheduledTask
# gibt still nichts zurueck -> es SIEHT aus als waere der Task weg, ist er aber
# nicht. Darum eleviert sich dieses Skript selbst (Lehre aus 2026-06-04).
#
# Nach dem naechsten echten Neustart zeigt "Zuletzt gelaufen" + "Letztes Ergebnis"
# definitiv, ob der Task beim Herunterfahren tatsaechlich ausgefuehrt wurde.

$ErrorActionPreference = 'Stop'
$taskName = 'Chrome beim Herunterfahren beenden'
$logFile  = Join-Path $env:TEMP 'chrome-shutdown-task-verify.log'

# --- Selbst-Elevation --------------------------------------------------------
$identity  = [Security.Principal.WindowsIdentity]::GetCurrent()
$principalCheck = New-Object Security.Principal.WindowsPrincipal($identity)
if (-not $principalCheck.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    Start-Process -FilePath 'powershell.exe' -Verb RunAs -ArgumentList @(
        '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', "`"$PSCommandPath`""
    )
    exit 0
}

$t = Get-ScheduledTask -TaskName $taskName -ErrorAction SilentlyContinue
if ($null -ne $t) {
    $info = Get-ScheduledTaskInfo -TaskName $taskName
    $a    = $t.Actions
    $sub  = [string]$t.Triggers.Subscription
    $resultText = switch ($info.LastTaskResult) {
        0       { '0 (Erfolg - Chrome wurde beendet)' }
        267011  { '267011 (Task wurde seit Einrichtung noch nie ausgefuehrt)' }
        default { "$($info.LastTaskResult)" }
    }
    $lines = @(
        'GEFUNDEN: ja',
        "Status: $($t.State)",
        "Laeuft als: $($t.Principal.UserId)",
        "Aktion: $($a.Execute) $($a.Arguments)",
        "Trigger auf Shutdown-Event 1074: $([bool]($sub -match 'EventID=1074'))",
        "Zuletzt gelaufen: $($info.LastRunTime)",
        "Letztes Ergebnis: $resultText"
    )
}
else {
    $lines = @('GEFUNDEN: nein - Task ist NICHT registriert. Setup-Skript erneut ausfuehren.')
}

$lines | ForEach-Object { Write-Host $_ }
Set-Content -Path $logFile -Value $lines -Encoding UTF8
Start-Sleep -Seconds 4   # Fenster kurz offen lassen, falls per Doppelklick gestartet
