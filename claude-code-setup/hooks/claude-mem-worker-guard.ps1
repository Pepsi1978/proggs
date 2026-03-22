# claude-mem-worker-guard.ps1 — Pre-warm claude-mem worker before plugin hooks
#
# Problem: When Claude Code starts, both the MCP server and SessionStart hooks
# try to start the worker daemon on port 37777 simultaneously. The second one
# fails with "port in use", causing 2 hook errors on every first session start.
# On subsequent starts the worker is already running, so no errors occur.
#
# Solution: Three-phase approach:
#   Phase 1: Quick health check (instant return if worker already running)
#   Phase 2: Wait 3 seconds for MCP server to start the worker
#   Phase 3: Actively start the worker ourselves if still not running
#
# Hook event: SessionStart (must be the FIRST SessionStart hook in settings.json)
# Platform: Windows

$ErrorActionPreference = 'SilentlyContinue'

# --- Configuration ---
$workerPort = 37777
$workerUrl = "http://127.0.0.1:${workerPort}/api/health"
$maxWaitPassive = 6   # 6 * 500ms = 3 seconds
$maxWaitActive = 8    # 8 * 500ms = 4 seconds

# --- Guards ---

# Only run if claude-mem plugin exists
$pluginCache = Join-Path $env:USERPROFILE ".claude\plugins\cache\thedotmack\claude-mem"
if (-not (Test-Path $pluginCache)) {
    exit 0
}

# Find latest plugin version in cache (survives plugin updates)
$pluginRoot = ""
$versions = Get-ChildItem -Path $pluginCache -Directory -ErrorAction SilentlyContinue | Sort-Object Name
if ($versions.Count -gt 0) {
    $pluginRoot = $versions[-1].FullName
}

# Fallback to marketplace path
if (-not $pluginRoot -or -not (Test-Path (Join-Path $pluginRoot "scripts\worker-service.cjs"))) {
    $mktPath = Join-Path $env:USERPROFILE ".claude\plugins\marketplaces\thedotmack\plugin"
    if (Test-Path (Join-Path $mktPath "scripts\worker-service.cjs")) {
        $pluginRoot = $mktPath
    }
}

# --- Health check function ---
function Test-WorkerHealthy {
    try {
        $response = Invoke-WebRequest -Uri $workerUrl -TimeoutSec 2 -UseBasicParsing -ErrorAction Stop
        return ($response.StatusCode -eq 200)
    } catch {
        return $false
    }
}

# --- Phase 1: Quick check ---
if (Test-WorkerHealthy) {
    exit 0
}

# --- Phase 2: Passive wait (MCP server starts the worker) ---
for ($i = 1; $i -le $maxWaitPassive; $i++) {
    Start-Sleep -Milliseconds 500
    if (Test-WorkerHealthy) {
        exit 0
    }
}

# --- Phase 3: Active start (if MCP server didn't start it) ---
if ($pluginRoot -and (Test-Path (Join-Path $pluginRoot "scripts\worker-service.cjs"))) {
    # Find Bun runtime
    $bunPath = ""
    $candidates = @(
        (Join-Path $env:USERPROFILE ".bun\bin\bun.exe"),
        "C:\Program Files\bun\bun.exe"
    )
    foreach ($candidate in $candidates) {
        if (Test-Path $candidate) {
            $bunPath = $candidate
            break
        }
    }

    # Also check PATH
    if (-not $bunPath) {
        $bunInPath = Get-Command bun -ErrorAction SilentlyContinue
        if ($bunInPath) {
            $bunPath = $bunInPath.Source
        }
    }

    if ($bunPath) {
        $workerScript = Join-Path $pluginRoot "scripts\worker-service.cjs"
        Start-Process -FilePath $bunPath -ArgumentList $workerScript, "--daemon" `
            -WindowStyle Hidden -ErrorAction SilentlyContinue

        for ($i = 1; $i -le $maxWaitActive; $i++) {
            Start-Sleep -Milliseconds 500
            if (Test-WorkerHealthy) {
                exit 0
            }
        }
    }
}

# --- Fallback: Worker didn't start, but don't block the session ---
exit 0
