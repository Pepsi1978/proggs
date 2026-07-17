#Requires -Version 7
# Version 1.0.0 - 17.07.2026, 17:50 Uhr

function Enter-VoiceOverlayDeploymentWindow {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)][string]$ProcessName,
        [Parameter(Mandatory)][int]$Port,
        [int]$TimeoutSeconds = 600,
        [int]$PollMilliseconds = 500,
        [int]$LegacyIdleMilliseconds = 2000,
        [scriptblock]$ProcessProbe,
        [scriptblock]$Request
    )

    if (-not $ProcessProbe) {
        $ProcessProbe = {
            param($name)
            @(Get-Process -Name $name -ErrorAction SilentlyContinue).Count -gt 0
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

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    $announcedBusy = $false
    $announcedUnavailable = $false
    $legacyIdleSince = $null
    while ((Get-Date) -lt $deadline) {
        try {
            $response = & $Request $Port 'prepare'
            if ($response.ready -eq $true) {
                Write-Host "  Aufnahme-Schutz reserviert: $ProcessName ist idle; neue Aufnahmen bleiben bis zum Deployment gesperrt." -ForegroundColor Green
                return $true
            }
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
                if ($legacyStatus.busy -eq $true) {
                    $legacyIdleSince = $null
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
                if (-not $announcedUnavailable) {
                    Write-Host "  Status von $ProcessName ist nicht sicher lesbar. Fail-closed: kein Build und kein Kill." -ForegroundColor Yellow
                    $announcedUnavailable = $true
                }
            }
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
