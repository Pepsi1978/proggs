# plugin-health-check.ps1 — SessionStart diagnostic for plugin services
#
# Runs AFTER all other SessionStart hooks to detect and log plugin failures.
# When Claude Code shows "startup hook error" without details, this log
# tells you exactly which plugin service was unhealthy at startup.
#
# Hook event: SessionStart (MUST be the LAST SessionStart hook group)
# Platform: Windows

$ErrorActionPreference = "Continue"

# --- Hook logging ---
$logDir = "$env:USERPROFILE\.claude\logs\hooks"
if (-not (Test-Path $logDir)) { New-Item -ItemType Directory -Force -Path $logDir | Out-Null }
$logFile = Join-Path $logDir "$(Get-Date -Format 'yyyy-MM-dd').log"
$hookName = "plugin-health-check"

function Hook-Log($msg) {
    $ts = Get-Date -Format "HH:mm:ss"
    Add-Content -Path $logFile -Value "[$ts] ${hookName}: $msg" -ErrorAction SilentlyContinue
}
function Hook-LogError($msg) {
    $ts = Get-Date -Format "HH:mm:ss"
    Add-Content -Path $logFile -Value "[$ts] ERROR ${hookName}: $msg" -ErrorAction SilentlyContinue
}
function Hook-LogWarn($msg) {
    $ts = Get-Date -Format "HH:mm:ss"
    Add-Content -Path $logFile -Value "[$ts] WARN  ${hookName}: $msg" -ErrorAction SilentlyContinue
}

Hook-Log "started"

$errors = 0
$checks = 0

# --- CHECK 1: claude-mem worker ---
$cmemPort = 37777
$cmemHost = "127.0.0.1"
$cmemSettings = "$env:USERPROFILE\.claude-mem\settings.json"
if (Test-Path $cmemSettings) {
    try {
        $s = Get-Content $cmemSettings -Raw | ConvertFrom-Json
        if ($s.CLAUDE_MEM_WORKER_PORT) { $cmemPort = $s.CLAUDE_MEM_WORKER_PORT }
    } catch { }
}

$checks++
try {
    $r = Invoke-WebRequest -Uri "http://${cmemHost}:${cmemPort}/api/health" -TimeoutSec 3 -UseBasicParsing -ErrorAction Stop
    Hook-Log "PASS claude-mem-worker (${cmemHost}:${cmemPort})"
} catch {
    Hook-LogError "FAIL claude-mem-worker (${cmemHost}:${cmemPort}) — not responding"
    $errors++
}

# --- CHECK 2: Ollama ---
$checks++
try {
    $r = Invoke-WebRequest -Uri "http://localhost:11434/api/tags" -TimeoutSec 3 -UseBasicParsing -ErrorAction Stop
    Hook-Log "PASS ollama (localhost:11434)"
} catch {
    Hook-LogWarn "SKIP ollama (localhost:11434) — not running"
}

# --- CHECK 3: Plugin hooks.json integrity ---
$pluginCache = "$env:USERPROFILE\.claude\plugins\cache\thedotmack\claude-mem"
if (Test-Path $pluginCache) {
    $latestVer = Get-ChildItem $pluginCache -Directory | Sort-Object Name | Select-Object -Last 1
    if ($latestVer) {
        $checks++
        $hooksFile = Join-Path $latestVer.FullName "hooks\hooks.json"
        if (Test-Path $hooksFile) {
            $content = Get-Content $hooksFile -Raw -ErrorAction SilentlyContinue
            if ($content -match "sleep.*curl.*api/health.*exit") {
                Hook-Log "PASS claude-mem hooks.json (patched with fallback)"
            } else {
                Hook-LogWarn "claude-mem hooks.json exists but fallback patch missing"
            }
        } else {
            Hook-LogError "FAIL claude-mem hooks.json missing at $hooksFile"
            $errors++
        }
    }
}

# --- CHECK 4: code-search MCP dependencies ---
$mcpDir = "$env:USERPROFILE\proggs\mcp-code-search"
if (Test-Path $mcpDir) {
    $checks++
    $missing = @()
    if (-not (Test-Path "$mcpDir\src\index.ts")) { $missing += "index.ts" }
    if (-not (Test-Path "$mcpDir\src\db-state.ts")) { $missing += "db-state.ts" }
    if (-not (Test-Path "$mcpDir\src\reindex-core.ts")) { $missing += "reindex-core.ts" }
    if (-not (Test-Path "$mcpDir\node_modules\better-sqlite3")) { $missing += "node_modules/better-sqlite3" }

    if ($missing.Count -eq 0) {
        Hook-Log "PASS code-search MCP (all source files present)"
    } else {
        Hook-LogError "FAIL code-search MCP — missing: $($missing -join ', ')"
        $errors++
    }
}

# --- SUMMARY ---
if ($errors -eq 0) {
    Hook-Log "health check complete: $checks checks passed, 0 errors"
} else {
    Hook-LogError "health check complete: $errors/$checks FAILED — check log above for details"
}

exit 0
