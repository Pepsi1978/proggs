# Richtet einen Aufgabenplaner-Task ein, der Chrome beim Herunterfahren/Neustart
# von Windows hart beendet (taskkill /F /IM chrome.exe /T) - genau das, was auch
# das Desktop-Symbol "Chrome beenden" macht, nur automatisch beim Shutdown.
#
# Zweck: Chrome zeigt beim naechsten Start den "Wiederherstellen"-Dialog und stellt
#        die komplette Sitzung inkl. exakter Video-/Scroll-Position wieder her.
# Trigger: System-Event 1074 (User32) = "Herunterfahren/Neustart wurde angefordert".
#          Dieses Event feuert FRUEH in der Shutdown-Sequenz, bevor Windows Chrome
#          sauber beenden kann -> der harte Kill kommt rechtzeitig.
# Laeuft als SYSTEM -> braucht einmalig Admin-Rechte (UAC).
#
# ROBUST (Lehre aus dem Vorfall 2026-06-04, bei dem der Task spurlos fehlte):
#  1. Selbst-Elevation: ohne Admin startet sich das Skript per UAC neu.
#  2. Echte Nachkontrolle: "ERFOLG" wird NUR geloggt, wenn der Task nach dem
#     Registrieren auch wirklich im System auffindbar ist (kein falsch-positiv).

$ErrorActionPreference = 'Stop'
$taskName = 'Chrome beim Herunterfahren beenden'
$logFile  = Join-Path $env:TEMP 'chrome-shutdown-task-setup.log'

function Log($msg) {
    # Add-Content -Encoding UTF8 statt Tee-Object: Tee-Object haengt in Windows
    # PowerShell 5.1 als UTF-16 an und macht das Log unleserlich (Leerzeichen
    # zwischen jedem Buchstaben). Add-Content -Encoding UTF8 bleibt konsistent.
    $line = "[{0}] {1}" -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'), $msg
    Write-Host $line
    Add-Content -Path $logFile -Value $line -Encoding UTF8
}

# --- Schicht 1: Selbst-Elevation ---------------------------------------------
# Ein Task der als SYSTEM laeuft kann nur mit Admin-Rechten registriert werden.
$identity  = [Security.Principal.WindowsIdentity]::GetCurrent()
$principalCheck = New-Object Security.Principal.WindowsPrincipal($identity)
if (-not $principalCheck.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    Log "Keine Admin-Rechte - starte mit UAC-Abfrage neu..."
    try {
        Start-Process -FilePath 'powershell.exe' -Verb RunAs -ArgumentList @(
            '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', "`"$PSCommandPath`""
        )
        exit 0
    }
    catch {
        Log "FEHLER: UAC abgelehnt oder Elevation fehlgeschlagen: $($_.Exception.Message)"
        exit 1
    }
}

# --- Ab hier laeuft das Skript garantiert mit Admin-Rechten -------------------
try {
    Set-Content -Path $logFile -Value ("[{0}] Setup gestartet (Admin)" -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss')) -Encoding UTF8

    # Eventuell vorhandenen alten Task sauber entfernen (idempotenter Neustart).
    Unregister-ScheduledTask -TaskName $taskName -Confirm:$false -ErrorAction SilentlyContinue

    # Event-Trigger auf Shutdown-Ereignis 1074 (User32).
    $subscription = '<QueryList><Query Id="0" Path="System"><Select Path="System">*[System[Provider[@Name=''User32''] and (EventID=1074)]]</Select></Query></QueryList>'
    $cimClass = Get-CimClass -Namespace ROOT\Microsoft\Windows\TaskScheduler -ClassName MSFT_TaskEventTrigger
    $trigger = New-CimInstance -CimClass $cimClass -ClientOnly
    $trigger.Enabled = $true
    $trigger.Subscription = $subscription

    $action    = New-ScheduledTaskAction -Execute "$env:SystemRoot\System32\taskkill.exe" -Argument '/F /IM chrome.exe /T'
    $principal = New-ScheduledTaskPrincipal -UserId 'S-1-5-18' -LogonType ServiceAccount -RunLevel Highest
    $settings  = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -ExecutionTimeLimit (New-TimeSpan -Minutes 1) -MultipleInstances IgnoreNew

    Register-ScheduledTask -TaskName $taskName -Action $action -Trigger $trigger -Principal $principal -Settings $settings -Description 'Beendet Chrome beim Herunterfahren hart, damit Chrome beim naechsten Start die Sitzung wiederherstellt (inkl. Video-Position).' -Force | Out-Null

    # --- Schicht 2: Echte Nachkontrolle --------------------------------------
    # Nur wenn der Task danach wirklich auffindbar ist, gilt das Setup als Erfolg.
    Start-Sleep -Milliseconds 500
    $check = Get-ScheduledTask -TaskName $taskName -ErrorAction SilentlyContinue
    if ($null -ne $check) {
        Log "ERFOLG: Task '$taskName' registriert UND verifiziert (State: $($check.State), RunAs: $($check.Principal.UserId))."
        exit 0
    }
    else {
        Log "FEHLER: Task nach dem Registrieren NICHT auffindbar - Registrierung war nicht persistent."
        exit 1
    }
}
catch {
    Log "FEHLER: $($_.Exception.Message)"
    exit 1
}
