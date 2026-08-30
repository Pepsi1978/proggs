#Requires -Version 7
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'voice-overlay-deploy-guard.ps1')

function Assert-True([bool]$Condition, [string]$Message) {
    if (-not $Condition) { throw $Message }
}

$requestCalled = $false
$notRunning = Enter-VoiceOverlayDeploymentWindow -ProcessName 'TestOverlay' -Port 1 `
    -ProcessProbe { $false } -Request { $script:requestCalled = $true }
Assert-True (-not $notRunning) 'Ein nicht laufendes Overlay darf keine Reservierung melden.'
Assert-True (-not $requestCalled) 'Ohne Prozess darf kein Endpoint aufgerufen werden.'

$prepareCalls = 0
$reserved = Enter-VoiceOverlayDeploymentWindow -ProcessName 'TestOverlay' -Port 1 `
    -TimeoutSeconds 2 -PollMilliseconds 1 `
    -ProcessProbe { $true } `
    -Request {
        param($port, $action)
        $script:prepareCalls++
        [pscustomobject]@{ ready = $script:prepareCalls -ge 3 }
    }
Assert-True $reserved 'Der Guard muss nach dem Busy-Zustand reservieren.'
Assert-True ($prepareCalls -eq 3) 'Der Guard muss bis zur atomaren Freigabe pollen.'

$legacyStatusCalls = 0
$legacy = Enter-VoiceOverlayDeploymentWindow -ProcessName 'TestOverlay' -Port 1 `
    -TimeoutSeconds 2 -PollMilliseconds 1 -LegacyIdleMilliseconds 1 `
    -ProcessProbe { $true } `
    -Request {
        param($port, $action)
        if ($action -eq 'prepare') { throw 'legacy endpoint' }
        $script:legacyStatusCalls++
        [pscustomobject]@{ busy = $script:legacyStatusCalls -lt 2 }
    }
Assert-True (-not $legacy) 'Der Legacy-Fallback darf keine atomare Reservierung vortaeuschen.'
Assert-True ($legacyStatusCalls -ge 2) 'Der Legacy-Fallback muss Busy abwarten und Idle stabil bestaetigen.'

# Offener Port, aber keine Antwort = das Overlay haengt -> fail-closed.
$failedClosed = $false
try {
    Enter-VoiceOverlayDeploymentWindow -ProcessName 'TestOverlay' -Port 1 `
        -TimeoutSeconds 5 -PollMilliseconds 10 -UnreadableGraceSeconds 0 `
        -ProcessProbe { $true } `
        -PortProbe { $true } `
        -Request { throw 'endpoint unavailable' } | Out-Null
}
catch {
    $failedClosed = $_.Exception.Message -like '*sicher abgebrochen*'
}
Assert-True $failedClosed 'Ein haengendes Overlay mit offenem Status-Port muss fail-closed abbrechen.'

# Kein Listener = das Overlay bietet den Schutz gar nicht an. Dann gibt es
# nichts zu reservieren, und das Deployment darf SOFORT weiter — genau der
# Fall, der frueher zehn Minuten Stillstand ausloeste (Vorfall 30.08.2026).
$noListenerStart = Get-Date
$noListener = Enter-VoiceOverlayDeploymentWindow -ProcessName 'TestOverlay' -Port 1 `
    -TimeoutSeconds 30 -PollMilliseconds 10 `
    -ProcessProbe { $true } `
    -PortProbe { $false } `
    -Request { throw 'endpoint unavailable' }
$noListenerSeconds = ((Get-Date) - $noListenerStart).TotalSeconds
Assert-True (-not $noListener) 'Ohne Listener darf keine Reservierung vorgetaeuscht werden.'
Assert-True ($noListenerSeconds -lt 2) "Ohne Listener muss der Guard sofort zurueckkehren, nicht bis zum Timeout warten (brauchte $([int]$noListenerSeconds) s)."

# Der Port wird erst geprueft, wenn BEIDE Endpunkte schweigen — ein
# antwortendes Overlay darf nie am Port haengenbleiben.
$portProbeCalls = 0
$reservedWithPort = Enter-VoiceOverlayDeploymentWindow -ProcessName 'TestOverlay' -Port 1 `
    -TimeoutSeconds 2 -PollMilliseconds 1 `
    -ProcessProbe { $true } `
    -PortProbe { $script:portProbeCalls++; $true } `
    -Request { [pscustomobject]@{ ready = $true } }
Assert-True $reservedWithPort 'Ein antwortendes Overlay muss weiterhin reservieren.'
Assert-True ($portProbeCalls -eq 0) 'Der Port darf nur geprueft werden, wenn kein Endpunkt antwortet.'

$released = $false
Exit-VoiceOverlayDeploymentWindow -ProcessName 'TestOverlay' -Port 1 -Reserved $true `
    -ProcessProbe { $true } `
    -Request {
        param($port, $action)
        if ($action -eq 'release') { $script:released = $true }
        [pscustomobject]@{ ready = $true }
    }
Assert-True $released 'Eine gehaltene Reservierung muss freigegeben werden.'

Write-Host 'voice-overlay-deploy-guard: 9 Tests bestanden.' -ForegroundColor Green
