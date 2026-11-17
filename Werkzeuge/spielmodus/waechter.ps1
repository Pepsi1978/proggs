# Game-mode watcher (version: see README.md).
# Runs as scheduled task "Spielmodus-Waechter" in the user context with highest privileges.
# While Steam or Call of Duty runs: stops Microsoft Store installs (InstallService, start type locked)
# and Gaming Services (protected package services: stop only, killed again if they come back),
# ends Store and Xbox app processes. Afterwards restores the previous state.
# Toggle via desktop shortcut "Spielmodus an-aus" (aus.flag).
# Designed to cost almost nothing while gaming: one process snapshot every 4 s, no pipelines,
# no blocking service waits, logging only on state changes.

$ErrorActionPreference = 'Continue'
$daten   = Join-Path $env:LOCALAPPDATA 'Spielmodus'
$log     = Join-Path $daten 'waechter.log'
$sicher  = Join-Path $daten 'starttypen.json'
$flagAus = Join-Path $daten 'aus.flag'
New-Item -ItemType Directory -Force $daten | Out-Null

$ausloeserNamen = @('steam', 'cod')                 # exact process names
$ausloeserMuster = '^(cod\d\d-cod|sp\d\d-cod)$'     # older CoD titles
$dienstGesperrt = 'InstallService'                   # start type can be locked
$dienstGestoppt = @('GamingServices', 'GamingServicesNet')
$stopProz       = @('WinStore.App', 'XboxPcApp', 'XboxPcAppFT', 'gamingservices', 'gamingservicesnet')
$entprellSek    = 20                                 # trigger must be gone this long before unlocking
$pwsh           = (Get-Process -Id $PID).Path

function Write-Log([string]$t) {
    try {
        Add-Content -Path $log -Value ('{0:dd.MM.yyyy HH:mm:ss}  {1}' -f (Get-Date), $t) -Encoding utf8
        if ((Get-Item $log).Length -gt 2MB) { Move-Item $log "$log.alt" -Force }
    } catch { }
}

function Show-Meldung([string]$t) {
    $skript = Join-Path $PSScriptRoot 'meldung.ps1'
    try {
        Start-Process "$env:WINDIR\System32\conhost.exe" -WindowStyle Hidden -ArgumentList "--headless `"$pwsh`" -NoProfile -ExecutionPolicy Bypass -File `"$skript`" -Text `"$t`""
    } catch { Write-Log "Mitteilung fehlgeschlagen: $($_.Exception.Message)" }
}

function Get-Ausloeser {
    # One cheap snapshot; returns the names of running trigger processes.
    $treffer = foreach ($p in [Diagnostics.Process]::GetProcesses()) {
        $n = $p.ProcessName
        if ($ausloeserNamen -contains $n -or $n -match $ausloeserMuster) { $n }
        $p.Dispose()
    }
    return @($treffer | Sort-Object -Unique)
}

function Get-Originaltyp {
    $typ = $null
    if (Test-Path $sicher) { try { $typ = (Get-Content $sicher -Raw | ConvertFrom-Json).$dienstGesperrt } catch { } }
    if (-not $typ -or $typ -eq 'Disabled') { $typ = 'Manual' }   # never treat 'Disabled' as original
    return $typ
}

function Save-Starttypen {
    if (Test-Path $sicher) { return }
    $svc = Get-Service $dienstGesperrt -ErrorAction SilentlyContinue
    $typ = if ($svc -and "$($svc.StartType)" -ne 'Disabled') { "$($svc.StartType)" } else { 'Manual' }
    @{ $dienstGesperrt = $typ } | ConvertTo-Json | Set-Content $sicher -Encoding utf8
    Write-Log "Starttyp gesichert: $dienstGesperrt=$typ"
}

function Stop-Ziele {
    # Non-blocking: -NoWait never hangs on StopPending; processes are killed directly.
    foreach ($d in @($dienstGesperrt) + $dienstGestoppt) {
        $s = Get-Service $d -ErrorAction SilentlyContinue
        if ($s -and $s.Status -ne 'Stopped') { try { Stop-Service $d -Force -NoWait -ErrorAction Stop } catch { } }
    }
    Stop-Process -Name $stopProz -Force -ErrorAction SilentlyContinue
}

function Enable-Sperre {
    $script:gamingWarAktiv = (Get-Service 'GamingServices' -ErrorAction SilentlyContinue).Status -eq 'Running'
    try { Set-Service $dienstGesperrt -StartupType Disabled -ErrorAction Stop } catch { Write-Log "Starttyp $dienstGesperrt nicht setzbar: $($_.Exception.Message)" }
    Stop-Ziele
}

function Disable-Sperre {
    $typ = Get-Originaltyp
    try { Set-Service $dienstGesperrt -StartupType $typ -ErrorAction Stop } catch { Write-Log "Rückstellen $dienstGesperrt fehlgeschlagen: $($_.Exception.Message)" }
    # Only restart Gaming Services if they were running before the lock.
    if ($script:gamingWarAktiv) { Start-Service $dienstGestoppt -ErrorAction SilentlyContinue }
}

Write-Log "Wächter gestartet (PID $PID)"
Save-Starttypen
$gesperrt = $false
$script:gamingWarAktiv = $true
$zuletztGesehen = [datetime]::MinValue
$steamLief = (Get-Ausloeser) -contains 'steam'
$restMeldung = ''

# Reconcile after a crash/reboot during a lock: InstallService must not stay disabled.
if ((Get-Service $dienstGesperrt -ErrorAction SilentlyContinue).StartType -eq 'Disabled') {
    Write-Log 'Altlast: InstallService war noch gesperrt, stelle zurück'
    Disable-Sperre
}

while ($true) {
    $aus    = Test-Path $flagAus
    $laeuft = Get-Ausloeser
    if ($laeuft.Count -gt 0) { $zuletztGesehen = Get-Date }

    $steamJetzt = $laeuft -contains 'steam'
    if ($steamJetzt -and -not $steamLief) {
        if ($aus) { Show-Meldung 'Steam gestartet: Spielmodus ist AUS. Store und Xbox-Downloads sind nicht gesperrt.' }
        else      { Show-Meldung 'Steam gestartet: Spielmodus ist AN. Store und Xbox-Downloads sind gesperrt.' }
        Write-Log ('Steam-Start gemeldet (Spielmodus ' + $(if ($aus) { 'AUS' } else { 'AN' }) + ')')
    }
    $steamLief = $steamJetzt

    $soll = (-not $aus) -and (($laeuft.Count -gt 0) -or ($gesperrt -and ((Get-Date) - $zuletztGesehen).TotalSeconds -lt $entprellSek))

    if ($soll -and -not $gesperrt) {
        Write-Log "Sperre AN (Auslöser: $($laeuft -join ', '))"
        Enable-Sperre; $gesperrt = $true
    } elseif (-not $soll -and $gesperrt) {
        Write-Log ('Sperre AUS (' + $(if ($aus) { 'von Hand ausgeschaltet' } else { 'kein Spiel/Steam mehr' }) + ')')
        Disable-Sperre; $gesperrt = $false; $restMeldung = ''
    }

    if ($gesperrt) {
        $rest = @([Diagnostics.Process]::GetProcesses() | Where-Object { $stopProz -contains $_.ProcessName } | ForEach-Object { $_.ProcessName })
        if ($rest.Count -gt 0) {
            Stop-Ziele
            $txt = ($rest | Sort-Object -Unique) -join ', '
            if ($txt -ne $restMeldung) { Write-Log "Wieder aufgetaucht, beendet: $txt"; $restMeldung = $txt }
        }
    }

    Start-Sleep -Seconds 4
}
