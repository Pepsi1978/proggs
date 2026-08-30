#Requires -Version 7
# Version 1.1.0 - 30.08.2026, 11:54 Uhr

<#
    AUFNAHME-SCHUTZ VOR DEM DEPLOYMENT

    Der Guard verhindert, dass ein Rebuild ein Overlay killt, waehrend Frank
    gerade diktiert. Dafuer fragt er das laufende Overlay ueber seinen
    lokalen Status-Port, ob es idle ist.

    WARUM ES DEN PORT-TEST GIBT (Vorfall 30.08.2026):
    Antwortete ein Overlay nicht, pollte der Guard stumm bis zum
    Gesamt-Timeout — 600 Sekunden — und brach dann ab. Die Meldung
    "Fail-closed: kein Build und kein Kill" erschien dabei sofort, das
    Skript stand danach aber noch zehn Minuten still, ohne etwas zu tun.
    Jeder Rebuild kostete so zehn Minuten und lieferte am Ende nichts.

    Ursache war eine fehlende Unterscheidung: "antwortet nicht" wurde
    behandelt wie "koennte gerade aufnehmen". In Wahrheit gibt es zwei
    voellig verschiedene Faelle, und der offene Port trennt sie sauber:

      KEIN LISTENER auf dem Port
        Das Overlay bietet den Schutz-Endpunkt gar nicht an — eine
        aeltere Fassung, oder es ist nur noch der Watchdog uebrig. Es gibt
        nichts zu reservieren und nichts zu schuetzen. Das Deployment
        faehrt ohne Reservierung fort, genau wie beim Legacy-Pfad.

      LISTENER DA, ABER STUMM
        Das Overlay laeuft und sollte antworten, tut es aber nicht — es
        haengt. Nur hier ist fail-closed richtig, und auch hier nach
        Sekunden statt nach Minuten.
#>

function Enter-VoiceOverlayDeploymentWindow {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)][string]$ProcessName,
        [Parameter(Mandatory)][int]$Port,
        # Ein Diktat dauert selten laenger als zwei Minuten. Frueher standen
        # hier 600 s — bei einem stummen Overlay hiess das zehn Minuten
        # Stillstand fuer nichts.
        [int]$TimeoutSeconds = 180,
        [int]$PollMilliseconds = 500,
        [int]$LegacyIdleMilliseconds = 2000,
        # So lange darf ein Overlay MIT offenem Port stumm bleiben, bevor es
        # als haengend gilt. Deckt den Moment kurz nach dem Start ab, in dem
        # der Listener noch hochfaehrt.
        [int]$UnreadableGraceSeconds = 10,
        # Alle so viele Sekunden eine Fortschrittszeile, damit sichtbar
        # bleibt, dass gewartet wird und worauf.
        [int]$ProgressIntervalSeconds = 5,
        [scriptblock]$ProcessProbe,
        [scriptblock]$PortProbe,
        [scriptblock]$Request
    )

    if (-not $ProcessProbe) {
        $ProcessProbe = {
            param($name)
            @(Get-Process -Name $name -ErrorAction SilentlyContinue).Count -gt 0
        }
    }
    # Lauscht ueberhaupt jemand auf dem Status-Port? Das trennt "bietet den
    # Schutz gar nicht an" von "haengt". Ein reiner TCP-Verbindungsversuch
    # gegen 127.0.0.1 antwortet in Millisekunden — auch das Nein.
    if (-not $PortProbe) {
        $PortProbe = {
            param($probePort)
            try {
                $client = [System.Net.Sockets.TcpClient]::new()
                $connect = $client.ConnectAsync('127.0.0.1', $probePort)
                $open = $connect.Wait(500) -and $client.Connected
                $client.Close()
                return $open
            }
            catch { return $false }
        }
    }
    if (-not $Request) {
        $Request = {
            param($requestPort, $action)
            if ($action -eq 'status') {
                Invoke-RestMethod -Method Get -Uri "http://127.0.0.1:$requestPort/recording/status" `
                    -TimeoutSec 2 -ErrorAction Stop
            } else {
                Invoke-RestMethod -Method Post -Uri "http://127.0.0.1:$requestPort/deployment/$action" `
                    -TimeoutSec 2 -ErrorAction Stop
            }
        }
    }

    if (-not (& $ProcessProbe $ProcessName)) {
        return $false
    }

    $started = Get-Date
    $deadline = $started.AddSeconds($TimeoutSeconds)
    $announcedBusy = $false
    $legacyIdleSince = $null
    $unreadableSince = $null
    $lastProgress = $started
    $waitingFor = 'eine Antwort des Overlays'

    while ((Get-Date) -lt $deadline) {
        try {
            $response = & $Request $Port 'prepare'
            if ($response.ready -eq $true) {
                Write-Host "  Aufnahme-Schutz reserviert: $ProcessName ist idle; neue Aufnahmen bleiben bis zum Deployment gesperrt." -ForegroundColor Green
                return $true
            }
            $unreadableSince = $null
            $waitingFor = 'das Ende der laufenden Aufnahme'
            if (-not $announcedBusy) {
                Write-Host "  $ProcessName nimmt auf, transkribiert oder fuegt Text ein. Deployment wartet..." -ForegroundColor Yellow
                $announcedBusy = $true
            }
        }
        catch {
            if (-not (& $ProcessProbe $ProcessName)) {
                return $false
            }
            try {
                $legacyStatus = & $Request $Port 'status'
                $unreadableSince = $null
                if ($legacyStatus.busy -eq $true) {
                    $legacyIdleSince = $null
                    $waitingFor = 'das Ende der laufenden Aufnahme (alter Busy-Guard)'
                    if (-not $announcedBusy) {
                        Write-Host "  $ProcessName nutzt den alten Busy-Guard und ist beschaeftigt. Deployment wartet..." -ForegroundColor Yellow
                        $announcedBusy = $true
                    }
                } else {
                    if ($null -eq $legacyIdleSince) { $legacyIdleSince = Get-Date }
                    if (((Get-Date) - $legacyIdleSince).TotalMilliseconds -ge $LegacyIdleMilliseconds) {
                        Write-Host "  Legacy-Migration: $ProcessName war stabil idle. Deployment darf fortfahren; vor dem Kill wird erneut geprueft." -ForegroundColor Green
                        return $false
                    }
                }
            }
            catch {
                # Weder der neue noch der alte Endpunkt antwortet. Jetzt
                # entscheidet der Port, welcher der beiden Faelle vorliegt —
                # siehe Kopf der Datei. Ohne diese Unterscheidung endete hier
                # jeder Lauf in zehn Minuten Stillstand.
                if (-not (& $PortProbe $Port)) {
                    Write-Host "  $ProcessName bietet keinen Aufnahme-Schutz an (niemand lauscht auf Port $Port) — vermutlich eine aeltere Fassung oder nur noch der Waechter." -ForegroundColor Yellow
                    Write-Host "  Es gibt nichts zu reservieren. Deployment faehrt ohne Reservierung fort; vor dem Kill wird erneut geprueft." -ForegroundColor Green
                    return $false
                }

                if ($null -eq $unreadableSince) {
                    $unreadableSince = Get-Date
                    Write-Host "  $ProcessName lauscht auf Port $Port, antwortet aber nicht. Warte bis zu $UnreadableGraceSeconds s auf eine Antwort..." -ForegroundColor Yellow
                }
                if (((Get-Date) - $unreadableSince).TotalSeconds -ge $UnreadableGraceSeconds) {
                    throw "Aufnahme-Schutz fuer ${ProcessName}: der Status-Port $Port ist offen, antwortet aber seit $UnreadableGraceSeconds s nicht — das Overlay haengt. Fail-closed, kein Build und kein Kill. Deployment sicher abgebrochen."
                }
                $waitingFor = "eine Antwort auf Port $Port"
            }
        }

        if (((Get-Date) - $lastProgress).TotalSeconds -ge $ProgressIntervalSeconds) {
            $elapsed = [int]((Get-Date) - $started).TotalSeconds
            Write-Host "  ... ${ProcessName}: warte seit $elapsed s auf $waitingFor (Abbruch nach $TimeoutSeconds s)" -ForegroundColor DarkGray
            $lastProgress = Get-Date
        }
        Start-Sleep -Milliseconds $PollMilliseconds
    }

    throw "Aufnahme-Schutz fuer $ProcessName nach $TimeoutSeconds Sekunden nicht freigegeben. Deployment sicher abgebrochen."
}

function Exit-VoiceOverlayDeploymentWindow {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)][string]$ProcessName,
        [Parameter(Mandatory)][int]$Port,
        [Parameter(Mandatory)][bool]$Reserved,
        [scriptblock]$ProcessProbe,
        [scriptblock]$Request
    )

    if (-not $Reserved) { return }
    if (-not $ProcessProbe) {
        $ProcessProbe = {
            param($name)
            @(Get-Process -Name $name -ErrorAction SilentlyContinue).Count -gt 0
        }
    }
    if (-not $Request) {
        $Request = {
            param($requestPort, $action)
            Invoke-RestMethod -Method Post -Uri "http://127.0.0.1:$requestPort/deployment/$action" `
                -TimeoutSec 2 -ErrorAction Stop
        }
    }

    if (-not (& $ProcessProbe $ProcessName)) { return }
    try {
        & $Request $Port 'release' | Out-Null
    }
    catch {
        Write-Host "  WARNUNG: Deployment-Sperre von $ProcessName konnte nicht freigegeben werden: $($_.Exception.Message)" -ForegroundColor Yellow
    }
}
