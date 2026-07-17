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

$failedClosed = $false
try {
    Enter-VoiceOverlayDeploymentWindow -ProcessName 'TestOverlay' -Port 1 `
        -TimeoutSeconds 1 -PollMilliseconds 50 `
        -ProcessProbe { $true } `
        -Request { throw 'endpoint unavailable' } | Out-Null
}
catch {
    $failedClosed = $_.Exception.Message -like '*sicher abgebrochen*'
}
Assert-True $failedClosed 'Ein laufendes Overlay mit unklarem Status muss fail-closed abbrechen.'

$released = $false
Exit-VoiceOverlayDeploymentWindow -ProcessName 'TestOverlay' -Port 1 -Reserved $true `
    -ProcessProbe { $true } `
    -Request {
        param($port, $action)
        if ($action -eq 'release') { $script:released = $true }
        [pscustomobject]@{ ready = $true }
    }
Assert-True $released 'Eine gehaltene Reservierung muss freigegeben werden.'

Write-Host 'voice-overlay-deploy-guard: 5 Tests bestanden.' -ForegroundColor Green
