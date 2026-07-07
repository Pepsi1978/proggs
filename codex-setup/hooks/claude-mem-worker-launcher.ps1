# codex-mem-worker-launcher.ps1 — Windows counterpart for codex-mem worker startup
#
# On macOS this is called by launchd. On Windows, codex-mem starts
# its worker differently (via the plugin lifecycle). This script exists
# for cross-platform parity and can be called manually if the worker
# needs to be restarted.
#
# RESILIENCE FEATURES:
# - Resolves plugin path at runtime (survives plugin updates)
# - Searches multiple bun/node locations
# - Checks if worker is already running (prevents duplicate processes)
# - Never exit 1 — always exit 0 to prevent caller disruption

$ErrorActionPreference = "SilentlyContinue"

# --- Pre-flight: check if worker already running ---
$cmemPort = 37777
$settingsPath = Join-Path $env:USERPROFILE ".codex-mem\settings.json"
if (Test-Path $settingsPath) {
    try {
        $settings = Get-Content $settingsPath -Raw | ConvertFrom-Json
        if ($settings.CODEX_MEM_WORKER_PORT) {
            $cmemPort = [int]$settings.CODEX_MEM_WORKER_PORT
        }
    } catch { }
}

try {
    $health = Invoke-RestMethod -Uri "http://127.0.0.1:${cmemPort}/api/health" -TimeoutSec 2
    Write-Host "[codex-mem] Worker already running on port $cmemPort"
    exit 0
} catch { }

# --- Find plugin directory ---
$pluginDir = Join-Path $env:USERPROFILE ".codex\plugins\cache\thedotmack\codex-mem"
if (-not (Test-Path $pluginDir)) {
    Write-Host "[codex-mem] Plugin not installed at $pluginDir"
    exit 0
}

$latestVersion = Get-ChildItem -Path $pluginDir -Directory | Sort-Object Name | Select-Object -Last 1
if (-not $latestVersion) {
    Write-Host "[codex-mem] No plugin versions found"
    exit 0
}

$workerScript = Join-Path $latestVersion.FullName "scripts\worker-service.cjs"
if (-not (Test-Path $workerScript)) {
    Write-Host "[codex-mem] worker-service.cjs not found at $workerScript"
    exit 0
}

# --- Find bun or node ---
$runtime = $null
$bunPath = Join-Path $env:USERPROFILE ".bun\bin\bun.exe"
if (Test-Path $bunPath) { $runtime = $bunPath }
elseif (Get-Command bun -ErrorAction SilentlyContinue) { $runtime = "bun" }
elseif (Get-Command node -ErrorAction SilentlyContinue) { $runtime = "node" }

if (-not $runtime) {
    Write-Host "[codex-mem] Neither bun nor node found"
    exit 0
}

Write-Host "[codex-mem] Starting worker: $runtime $workerScript --daemon"
Start-Process -FilePath $runtime -ArgumentList "$workerScript","--daemon" -NoNewWindow
exit 0
