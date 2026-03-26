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

# Ensure dependencies are installed
$nodeModules = Join-Path $mcpDir "node_modules"
if (-not (Test-Path $nodeModules)) {
    $npmProc = Start-Process -FilePath "npm" -ArgumentList "install" -WorkingDirectory $mcpDir -NoNewWindow -PassThru
    if ($npmProc) {
        $npmProc.WaitForExit(120000)
        if ($npmProc.ExitCode -ne 0) {
            Write-Output "Reindex-Hook: npm install failed (exit $($npmProc.ExitCode))"
            exit 0
        }
    }
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

# Ensure nomic-embed-text model is available
try {
    $models = (Invoke-WebRequest -Uri "http://localhost:11434/api/tags" -TimeoutSec 2).Content | ConvertFrom-Json
    $hasNomic = $models.models | Where-Object { $_.name -match "nomic-embed-text" }
    if (-not $hasNomic) {
        $ollamaExe = "$env:LOCALAPPDATA\Programs\Ollama\ollama.exe"
        if (Test-Path $ollamaExe) {
            Start-Process -FilePath $ollamaExe -ArgumentList "pull", "nomic-embed-text" -NoNewWindow -Wait
        }
    }
} catch {}

# Run incremental reindex via session-reindex.ts
try {
    $reindexScript = Join-Path $mcpDir "src\session-reindex.ts"
    $allArgs = $tsxArgs + @($reindexScript, $rootDir)
    $process = Start-Process -FilePath $tsxExe -ArgumentList $allArgs -WorkingDirectory $mcpDir -NoNewWindow -PassThru -RedirectStandardOutput (Join-Path $env:TEMP "reindex-stdout.log") -RedirectStandardError (Join-Path $env:TEMP "reindex-stderr.log")
    if ($process) {
        $completed = $process.WaitForExit(300000)  # 5 minute timeout
        if (-not $completed) {
            $process.Kill()
            Write-Output "Reindex-Hook: TIMEOUT nach 300s — Prozess beendet."
            exit 0
        }
    }

    if ($process.ExitCode -eq 0) {
        $stdout = Get-Content (Join-Path $env:TEMP "reindex-stdout.log") -Raw -ErrorAction SilentlyContinue
        Write-Output "Reindex-Hook: $stdout"
        # C1 (ported from Gemini): Write index summary to whiteboard
        try {
            . "$PSScriptRoot/whiteboard-insert.ps1"
            $files = if ($stdout -match '(\d+)\s*files') { $Matches[1] } else { "?" }
            $chunks = if ($stdout -match '(\d+)\s*chunks') { $Matches[1] } else { "?" }
            $ts = Get-Date -Format "yyyy-MM-dd HH:mm"
            Replace-WhiteboardEntry -Section "Performance & Optimierung" -MatchPattern "Code-Suche Index" -Entry "- **[$ts] Code-Suche Index:** $files Dateien, $chunks Chunks indexiert."
        } catch { }
    } else {
        $stderr = Get-Content (Join-Path $env:TEMP "reindex-stderr.log") -Raw -ErrorAction SilentlyContinue
        Write-Output "Reindex-Hook: FEHLER (exit $($process.ExitCode)): $stderr"
    }
} catch {
    Write-Output "Reindex-Hook: EXCEPTION — $($_.Exception.Message)"
}
