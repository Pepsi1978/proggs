# Launch Electron apps with Chrome DevTools Protocol enabled
# This enables the Voice Overlay to inject text directly (100% reliable)
# instead of using unreliable clipboard + keyboard simulation (~50%)
#
# Usage: pwsh -File launch-with-cdp.ps1 [claude|codex|cursor|all]

param(
    [string]$App = "all"
)

$port = 9222

function Launch-MsixWithCdp($name, $appFamilyId, $cdpPort) {
    # Check if already running
    $procName = $name -replace ' ',''
    $existing = Get-Process -ErrorAction SilentlyContinue | Where-Object { $_.ProcessName -match $procName }
    if ($existing) {
        Write-Host "  $name laeuft bereits (PID $($existing[0].Id))" -ForegroundColor Cyan
        try {
            $response = Invoke-RestMethod "http://localhost:$cdpPort/json/version" -TimeoutSec 2
            Write-Host "  CDP aktiv auf Port $cdpPort ✓" -ForegroundColor Green
        } catch {
            Write-Host "  CDP NICHT aktiv! Bitte $name schliessen und neu starten mit diesem Skript." -ForegroundColor Red
        }
        return
    }

    # MSIX apps: use shell:AppsFolder protocol which allows passing arguments
    Write-Host "  Starte $name mit CDP auf Port $cdpPort..." -ForegroundColor Green

    # Method 1: Try via the exe directly (works if Developer Mode is on)
    $pkg = Get-AppxPackage | Where-Object { $_.PackageFamilyName -like "$($appFamilyId.Split('!')[0])*" }
    if ($pkg) {
        $exes = Get-ChildItem $pkg.InstallLocation -Recurse -Filter "*.exe" -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -match "^(claude|codex|cursor)\.exe$" }
        if ($exes) {
            $exePath = $exes[0].FullName
            try {
                Start-Process -FilePath $exePath -ArgumentList "--remote-debugging-port=$cdpPort" -ErrorAction Stop
                Write-Host "  Gestartet via: $exePath" -ForegroundColor Gray
                Start-Sleep -Seconds 4
                try {
                    $response = Invoke-RestMethod "http://localhost:$cdpPort/json/version" -TimeoutSec 3
                    Write-Host "  CDP aktiv! Browser: $($response.Browser) ✓" -ForegroundColor Green
                    return
                } catch {
                    Write-Host "  CDP nicht erreichbar nach direktem Start" -ForegroundColor Yellow
                }
            } catch {
                Write-Host "  Direkter Start fehlgeschlagen: $($_.Exception.Message)" -ForegroundColor Yellow
            }
        }
    }

    # Method 2: Fallback — start via shell protocol (no CDP args possible)
    Write-Host "  Starte via shell:AppsFolder (ohne CDP — Fallback auf keybd_event)" -ForegroundColor Yellow
    Start-Process "shell:AppsFolder\$appFamilyId"
}

function Launch-ExeWithCdp($name, $exePath, $cdpPort) {
    if (-not (Test-Path $exePath)) { return $false }

    $procName = [System.IO.Path]::GetFileNameWithoutExtension($exePath)
    $existing = Get-Process -Name $procName -ErrorAction SilentlyContinue
    if ($existing) {
        Write-Host "  $name laeuft bereits (PID $($existing[0].Id))" -ForegroundColor Cyan
        try {
            $response = Invoke-RestMethod "http://localhost:$cdpPort/json/version" -TimeoutSec 2
            Write-Host "  CDP aktiv auf Port $cdpPort ✓" -ForegroundColor Green
        } catch {
            Write-Host "  CDP NICHT aktiv! Bitte $name schliessen und neu starten." -ForegroundColor Red
        }
        return $true
    }

    Write-Host "  Starte $name mit CDP auf Port $cdpPort..." -ForegroundColor Green
    Start-Process -FilePath $exePath -ArgumentList "--remote-debugging-port=$cdpPort"
    Start-Sleep -Seconds 4

    try {
        $response = Invoke-RestMethod "http://localhost:$cdpPort/json/version" -TimeoutSec 3
        Write-Host "  CDP aktiv! Browser: $($response.Browser) ✓" -ForegroundColor Green
    } catch {
        Write-Host "  CDP nicht erreichbar" -ForegroundColor Yellow
    }
    return $true
}

Write-Host "`nVoice Overlay — CDP Launcher" -ForegroundColor White
Write-Host "============================`n" -ForegroundColor White

# Detect installed apps
$claudeMsix = Get-AppxPackage -Name "Claude" -ErrorAction SilentlyContinue
$codexMsix = Get-AppxPackage -Name "OpenAI.Codex" -ErrorAction SilentlyContinue

# Non-MSIX paths
$cursorPaths = @(
    "$env:LOCALAPPDATA\Programs\cursor\Cursor.exe",
    "$env:LOCALAPPDATA\Programs\Cursor\Cursor.exe"
)
$cursorExe = $cursorPaths | Where-Object { Test-Path $_ } | Select-Object -First 1

switch ($App.ToLower()) {
    "claude" {
        if ($claudeMsix) { Launch-MsixWithCdp "Claude" "Claude_pzs8sxrjxfjjc!Claude" $port }
        else { Write-Host "Claude Desktop nicht installiert" -ForegroundColor Red }
    }
    "codex" {
        if ($codexMsix) { Launch-MsixWithCdp "Codex" "OpenAI.Codex_2p2nqsd0c76g0!App" ($port + 1) }
        else { Write-Host "Codex nicht installiert" -ForegroundColor Red }
    }
    "cursor" {
        if ($cursorExe) { Launch-ExeWithCdp "Cursor" $cursorExe ($port + 2) }
        else { Write-Host "Cursor nicht gefunden" -ForegroundColor Red }
    }
    "all" {
        $p = $port
        if ($claudeMsix) { Launch-MsixWithCdp "Claude" "Claude_pzs8sxrjxfjjc!Claude" $p; $p++ }
        if ($codexMsix)  { Launch-MsixWithCdp "Codex" "OpenAI.Codex_2p2nqsd0c76g0!App" $p; $p++ }
        if ($cursorExe)  { Launch-ExeWithCdp "Cursor" $cursorExe $p }
        if (-not $claudeMsix -and -not $codexMsix -and -not $cursorExe) {
            Write-Host "Keine Apps gefunden." -ForegroundColor Red
        }
    }
    default {
        Write-Host "Nutzung: launch-with-cdp.ps1 [claude|codex|cursor|all]" -ForegroundColor Yellow
    }
}

Write-Host "`nTipp: Starte danach das Voice Overlay — es erkennt CDP automatisch." -ForegroundColor Cyan
Write-Host "Ohne CDP: Keyboard-Fallback (~50%). Mit CDP: direkte Textinjektion (~100%).`n" -ForegroundColor Gray
