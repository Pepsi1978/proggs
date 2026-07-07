# claude-mem-worker-guard.ps1 — Universal service readiness & plugin hooks hardener
#
# PRIMARY PURPOSE: Prevent SessionStart hook errors from race conditions between
# MCP servers and plugin hooks. Currently handles claude-mem's worker daemon,
# but designed to be extended for ANY future plugin that starts background services.
#
# ARCHITECTURE (4 phases):
#   Phase 0: Auto-patch ALL plugin hooks.json files (self-healing after updates)
#   Phase 1: Quick health check for all known services
#   Phase 2: Passive wait for services to become ready
#   Phase 3: Active start of services if still not running
#
# Hook event: SessionStart (must be the FIRST SessionStart hook in settings.json)
# Platform: Windows

$ErrorActionPreference = 'SilentlyContinue'

# ============================================================================
# SECTION 1: SERVICE REGISTRY
# ============================================================================

$cmemPort = 37777
$cmemHost = "127.0.0.1"
$cmemSettings = Join-Path $env:USERPROFILE ".claude-mem\settings.json"

if (Test-Path $cmemSettings) {
    try {
        $settings = Get-Content $cmemSettings -Raw | ConvertFrom-Json
        if ($settings.CLAUDE_MEM_WORKER_PORT) { $cmemPort = [int]$settings.CLAUDE_MEM_WORKER_PORT }
        if ($settings.CLAUDE_MEM_WORKER_HOST) { $cmemHost = $settings.CLAUDE_MEM_WORKER_HOST }
    } catch { }
}

# Service registry — extend for future plugins
$services = @(
    @{ Name = "claude-mem-worker"; Host = $cmemHost; Port = $cmemPort }
    # Future: @{ Name = "other-plugin"; Host = "127.0.0.1"; Port = 8080 }
)

# ============================================================================
# SECTION 2: PLUGIN ROOT RESOLVER
# ============================================================================

function Find-PluginRoot {
    param([string]$Vendor, [string]$Marker)

    $cacheDir = Join-Path $env:USERPROFILE ".claude\plugins\cache\$Vendor"
    if (Test-Path $cacheDir) {
        $latest = Get-ChildItem -Path $cacheDir -Directory | Sort-Object Name | Select-Object -Last 1
        if ($latest -and (Test-Path (Join-Path $latest.FullName $Marker))) {
            return $latest.FullName
        }
    }

    $vendorShort = $Vendor.Split('/')[0]
    $mktPath = Join-Path $env:USERPROFILE ".claude\plugins\marketplaces\$vendorShort\plugin"
    if (Test-Path (Join-Path $mktPath $Marker)) { return $mktPath }

    return $null
}

# ============================================================================
# SECTION 3: HEALTH CHECK
# ============================================================================

function Test-ServiceHealthy {
    param([string]$Host, [int]$Port)

    # Method 1: HTTP
    try {
        $r = Invoke-WebRequest -Uri "http://${Host}:${Port}/api/health" -TimeoutSec 2 -UseBasicParsing -ErrorAction Stop
        if ($r.StatusCode -eq 200) { return $true }
    } catch { }

    # Method 2: TCP
    try {
        $tcp = New-Object System.Net.Sockets.TcpClient
        $tcp.Connect($Host, $Port)
        $tcp.Close()
        return $true
    } catch { }

    return $false
}

# ============================================================================
# SECTION 4: AUTO-PATCHER
# ============================================================================

function Patch-PluginHooks {
    param([string]$HooksJson, [string]$Host, [int]$Port)

    if (-not (Test-Path $HooksJson)) { return }
    $content = Get-Content $HooksJson -Raw -ErrorAction SilentlyContinue
    if (-not $content) { return }
    if ($content -match "curl -sf.*:${Port}/api/health.*exit 0") { return }
    if ($content -notmatch 'worker-service\.cjs" start') { return }

    $fallback = "|| { sleep 2 && curl -sf http://${Host}:${Port}/api/health >/dev/null 2>&1 && exit 0 || exit 1; }"
    $patched = $content `
        -replace '(worker-service\.cjs" start)"', "`$1 $fallback""" `
        -replace '(worker-service\.cjs" hook claude-code context)"', "`$1 $fallback"""

    if ($patched -ne $content) {
        Set-Content -Path $HooksJson -Value $patched -NoNewline -ErrorAction SilentlyContinue
    }
}

# ============================================================================
# SECTION 5: MAIN EXECUTION
# ============================================================================

foreach ($svc in $services) {
    $pluginRoot = $null

    switch ($svc.Name) {
        "claude-mem-worker" {
            $pluginRoot = Find-PluginRoot -Vendor "thedotmack/claude-mem" -Marker "scripts\worker-service.cjs"
            if (-not $pluginRoot) { continue }
            Patch-PluginHooks -HooksJson (Join-Path $pluginRoot "hooks\hooks.json") -Host $svc.Host -Port $svc.Port
        }
    }

    # Phase 1: Quick check
    if (Test-ServiceHealthy -Host $svc.Host -Port $svc.Port) { continue }

    # Phase 2: Passive wait (3s)
    $ready = $false
    for ($i = 1; $i -le 6; $i++) {
        Start-Sleep -Milliseconds 500
        if (Test-ServiceHealthy -Host $svc.Host -Port $svc.Port) { $ready = $true; break }
    }
    if ($ready) { continue }

    # Phase 3: Active start
    switch ($svc.Name) {
        "claude-mem-worker" {
            if ($pluginRoot) {
                $workerScript = Join-Path $pluginRoot "scripts\worker-service.cjs"
                $bunPath = ""
                @((Join-Path $env:USERPROFILE ".bun\bin\bun.exe"), "C:\Program Files\bun\bun.exe") | ForEach-Object {
                    if ((Test-Path $_) -and -not $bunPath) { $bunPath = $_ }
                }
                if (-not $bunPath) {
                    $inPath = Get-Command bun -ErrorAction SilentlyContinue
                    if ($inPath) { $bunPath = $inPath.Source }
                }
                if ($bunPath) {
                    Start-Process -FilePath $bunPath -ArgumentList $workerScript, "--daemon" -WindowStyle Hidden -ErrorAction SilentlyContinue
                    for ($i = 1; $i -le 8; $i++) {
                        Start-Sleep -Milliseconds 500
                        if (Test-ServiceHealthy -Host $svc.Host -Port $svc.Port) { break }
                    }
                }
            }
        }
    }
}

exit 0
