# reindex-codebase.ps1 — SessionStart Hook (async)
# Incrementally re-indexes the codebase for semantic search.
# Only re-embeds files changed since last index. Full reindex only on first run.
# v3: Uses session-reindex.ts (reindex-core.ts) instead of inline full-reindex script.

$ErrorActionPreference = "SilentlyContinue"

$rootDir = "$env:USERPROFILE\proggs"
$mcpDir = Join-Path $rootDir "mcp-code-search"
$tsxExe = "$env:APPDATA\npm\tsx.cmd"

# Guard: required files must exist
if (-not (Test-Path (Join-Path $mcpDir "src\session-reindex.ts"))) { exit 0 }
if (-not (Test-Path $tsxExe)) {
    # Fallback: try npx tsx
    $tsxExe = "npx"
    $tsxArgs = @("tsx")
} else {
    $tsxArgs = @()
}

# Ensure dependencies are installed (async — non-blocking)
# If node_modules is missing, kick off npm install in background and skip reindex this session.
# Next SessionStart will find node_modules and run reindex normally.
$nodeModules = Join-Path $mcpDir "node_modules"
if (-not (Test-Path $nodeModules)) {
    Start-Process -FilePath "npm" -ArgumentList "install" -WorkingDirectory $mcpDir -NoNewWindow `
        -RedirectStandardOutput (Join-Path $env:TEMP "reindex-npm-stdout.log") `
        -RedirectStandardError (Join-Path $env:TEMP "reindex-npm-stderr.log") `
        -ErrorAction SilentlyContinue
    "$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') npm install gestartet (async). Reindex laeuft beim naechsten Session-Start." | Out-File "$env:TEMP\reindex-npm-install-flag.log" -Encoding UTF8
    exit 0
}

# Auto-start Ollama if not running
try {
    $null = Invoke-WebRequest -Uri "http://localhost:11434/api/tags" -TimeoutSec 2 -ErrorAction Stop
} catch {
    $ollamaExe = "$env:LOCALAPPDATA\Programs\Ollama\ollama.exe"
    if (Test-Path $ollamaExe) {
        Start-Process -FilePath $ollamaExe -ArgumentList "serve" -WindowStyle Hidden
        Start-Sleep -Seconds 5
        try {
            $null = Invoke-WebRequest -Uri "http://localhost:11434/api/tags" -TimeoutSec 3 -ErrorAction Stop
        } catch { exit 0 }
    } else { exit 0 }
}

# Ensure snowflake-arctic-embed2 model is available (8192 token context, 1024 dim)
try {
    $models = (Invoke-WebRequest -Uri "http://localhost:11434/api/tags" -TimeoutSec 2).Content | ConvertFrom-Json
    $hasArctic = $models.models | Where-Object { $_.name -match "snowflake-arctic-embed2" }
    if (-not $hasArctic) {
        $ollamaExe = "$env:LOCALAPPDATA\Programs\Ollama\ollama.exe"
        if (Test-Path $ollamaExe) {
            Start-Process -FilePath $ollamaExe -ArgumentList "pull", "snowflake-arctic-embed2" -NoNewWindow -Wait
        }
    }
} catch {}

# Run incremental reindex via session-reindex.ts (TRULY ASYNC — non-blocking)
# Hook returns immediately; reindex completes in background.
# Lock-file pattern prevents parallel reindex processes from concurrent sessions.
$dbDir   = Join-Path $rootDir ".code-search"
$lockFile = Join-Path $dbDir ".reindex.lock"
$logFile  = Join-Path $dbDir "reindex.log"
$maxLockAgeSec = 1800  # 30 minutes

if (-not (Test-Path $dbDir)) {
    New-Item -ItemType Directory -Path $dbDir -Force | Out-Null
}

# Stale-lock detection (PID-basiert + Zeit, analog zur .sh): erkennt tote Reindex-Prozesse
# SOFORT statt erst nach 30 Min. Sicher: ein frischer Lock mit noch unbekannter/pending PID
# wird NIE entfernt (kein faelschliches Killen eines laufenden Reindex).
if (Test-Path $lockFile) {
    $lockPid = ((Get-Content $lockFile -ErrorAction SilentlyContinue | Select-Object -First 1) -as [string]).Trim()
    $lockAge = (Get-Date) - (Get-Item $lockFile).LastWriteTime
    $deadByPid = $false
    if ($lockPid -match '^\d+$') {
        $deadByPid = -not [bool](Get-Process -Id ([int]$lockPid) -ErrorAction SilentlyContinue)
    }
    if ($lockAge.TotalSeconds -ge $maxLockAgeSec -or $deadByPid) {
        # Zu alt ODER Prozess nachweislich tot -> stale -> entfernen
        Remove-Item $lockFile -Force -ErrorAction SilentlyContinue
    } else {
        # Frisch und Prozess lebt (oder PID noch "pending") -> laeuft wirklich -> skip
        exit 0
    }
}

try {
    $reindexScript = Join-Path $mcpDir "src\session-reindex.ts"
    $allArgs = $tsxArgs + @($reindexScript, $rootDir)
    # Platzhalter bis die echte PID feststeht (schuetzt das kurze Fenster vor dem Start)
    "pending" | Out-File $lockFile -Encoding UTF8
    # Detached: no -Wait, no WaitForExit, hook exits immediately while child keeps running.
    $proc = Start-Process -FilePath $tsxExe -ArgumentList $allArgs -WorkingDirectory $mcpDir -WindowStyle Hidden `
        -RedirectStandardOutput $logFile `
        -RedirectStandardError (Join-Path $dbDir "reindex.err") `
        -PassThru -ErrorAction SilentlyContinue
    # PID des Reindex-Prozesses in den Lock schreiben -> Stale-Detection erkennt tote Prozesse (analog .sh)
    if ($proc) { "$($proc.Id)" | Out-File $lockFile -Encoding UTF8 }
} catch {
    Write-Output "Reindex-Hook: EXCEPTION beim Async-Start — $($_.Exception.Message)"
    Remove-Item $lockFile -Force -ErrorAction SilentlyContinue
}

exit 0
