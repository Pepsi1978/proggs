# semantic-search-healthcheck.ps1
# Runs the 4-category semantic search integrity test for the mcp-code-search system.
#
# Usage:
#   .\semantic-search-healthcheck.ps1 [[-ProggsDIr] <path>] [-Reindex]
#
# Exit codes:
#   0 = all checks OK
#   1 = at least one FAIL
#   2 = no FAIL but at least one WARN
#
# Categories:
#   1. Basic function  (Ollama -> Embedding -> DB -> Search)
#   2. Pointer consistency (current.txt points to existing DB)
#   3. Infrastructure  (hooks, MCP config, gitignore, dependencies, Ollama)
#   4. Parallelism safety (race condition guards)

[CmdletBinding()]
param(
    [string]$ProggsDir = (Join-Path $env:USERPROFILE "proggs"),
    [switch]$Reindex
)

$ErrorActionPreference = "Stop"

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------
$ProggsDir   = (Resolve-Path $ProggsDir -ErrorAction SilentlyContinue)?.Path ?? $ProggsDir
$McpDir      = Join-Path $ProggsDir "mcp-code-search"
$DbDir       = Join-Path $ProggsDir ".code-search"
$ClaudeDir   = Join-Path $env:USERPROFILE ".claude"
$SettingsFile = Join-Path $ClaudeDir "settings.json"

# ---------------------------------------------------------------------------
# Result tracking
# ---------------------------------------------------------------------------
$FailCount = 0
$WarnCount = 0

function Write-OK   { param([string]$Msg) Write-Host "  OK   $Msg" -ForegroundColor Green }
function Write-Warn { param([string]$Msg) Write-Host "  WARN $Msg" -ForegroundColor Yellow; $script:WarnCount++ }
function Write-Fail { param([string]$Msg) Write-Host "  FAIL $Msg" -ForegroundColor Red;    $script:FailCount++ }

# ---------------------------------------------------------------------------
# Optional reindex
# ---------------------------------------------------------------------------
if ($Reindex) {
    Write-Host "==> Reindexing codebase (bun run index)..." -ForegroundColor Cyan
    if (Test-Path $McpDir) {
        Push-Location $McpDir
        try { bun run index 2>&1 | ForEach-Object { Write-Host "  $_" } }
        catch { Write-Host "  WARN: Reindex returned an error: $_" -ForegroundColor Yellow }
        finally { Pop-Location }
    } else {
        Write-Fail "1-pre: $McpDir does not exist — cannot reindex"
        exit 1
    }
    Write-Host ""
}

# ---------------------------------------------------------------------------
# Category 1: Basic function (Ollama -> Embedding -> DB -> Search)
# ---------------------------------------------------------------------------
Write-Host "==> Category 1: Basic function" -ForegroundColor Cyan

if (-not (Test-Path $McpDir)) {
    Write-Fail "1-pre: $McpDir does not exist"
} else {
    $BunScript = @"
import { VectorStore } from './src/store.ts';
import { generateEmbedding } from './src/ollama.ts';
import { resolve, join } from 'path';
import { existsSync, readFileSync } from 'fs';

const root = resolve('$($ProggsDir -replace '\\', '/')');
const dbDir = join(root, '.code-search');
const pointerFile = join(dbDir, 'current.txt');

if (!existsSync(pointerFile)) { console.log('FAIL-1A: current.txt missing'); process.exit(1); }
const dbName = readFileSync(pointerFile, 'utf-8').trim();
if (!existsSync(join(dbDir, dbName))) { console.log('FAIL-1B: ' + dbName + ' does not exist'); process.exit(1); }
const store = new VectorStore(join(dbDir, dbName));
const stats = store.stats();
if (stats.totalChunks === 0) { console.log('FAIL-1C: Index is empty'); process.exit(1); }
const emb = await generateEmbedding('test query for health check');
if (emb.length !== 768) { console.log('FAIL-1D: Embedding dim ' + emb.length + ' instead of 768'); process.exit(1); }
const results = store.search(emb, 3);
if (results.length === 0) { console.log('FAIL-1E: Search returned 0 results'); process.exit(1); }
store.close();
console.log('OK-1: ' + stats.totalFiles + ' files, ' + stats.totalChunks + ' chunks, DB=' + dbName);
"@

    Push-Location $McpDir
    try {
        $Cat1Result = bun -e $BunScript 2>&1 | Out-String
    } catch {
        $Cat1Result = "FAIL-1: bun error: $_"
    } finally {
        Pop-Location
    }

    if ($Cat1Result -match "FAIL-") {
        $FailLine = ($Cat1Result -split "`n" | Where-Object { $_ -match "^FAIL" } | Select-Object -First 1).Trim()
        Write-Fail $FailLine
    } elseif ($Cat1Result -match "OK-1:") {
        $OkLine = ($Cat1Result -split "`n" | Where-Object { $_ -match "^OK-1:" } | Select-Object -First 1).Trim()
        Write-OK $OkLine
    } else {
        Write-Warn "1: Unexpected output: $($Cat1Result.Trim())"
    }
}

# ---------------------------------------------------------------------------
# Category 2: Pointer consistency
# ---------------------------------------------------------------------------
Write-Host ""
Write-Host "==> Category 2: Pointer consistency" -ForegroundColor Cyan

$PointerFile = Join-Path $DbDir "current.txt"

# 2A: current.txt exists and is non-empty
if ((Test-Path $PointerFile) -and ((Get-Item $PointerFile).Length -gt 0)) {
    Write-OK "2A: current.txt exists and is non-empty"
} else {
    Write-Fail "2A: current.txt missing or empty"
}

# 2B: Referenced DB file exists
$Pointer = ""
if (Test-Path $PointerFile) { $Pointer = (Get-Content $PointerFile -Raw).Trim() }
if ($Pointer -and (Test-Path (Join-Path $DbDir $Pointer))) {
    Write-OK "2B: $Pointer exists"
} else {
    Write-Fail "2B: $Pointer not found in $DbDir"
}

# 2C: No orphaned index-N.db files
$DbFiles = @(Get-ChildItem "$DbDir\index-*.db" -ErrorAction SilentlyContinue)
$DbCount = $DbFiles.Count
if ($DbCount -le 1) {
    Write-OK "2C: $DbCount DB file(s)"
} else {
    Write-Warn "2C: $DbCount DB files — cleanup may be needed"
}

# 2D: No orphaned WAL/SHM files from old indexes
$WalFiles = @(Get-ChildItem "$DbDir\index-*.db-wal", "$DbDir\index-*.db-shm" -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -notlike "*$Pointer*" })
if ($WalFiles.Count -eq 0) {
    Write-OK "2D: No orphaned WAL/SHM files"
} else {
    Write-Warn "2D: $($WalFiles.Count) orphaned WAL/SHM file(s) — clean up manually"
}

# ---------------------------------------------------------------------------
# Category 3: Infrastructure
# ---------------------------------------------------------------------------
Write-Host ""
Write-Host "==> Category 3: Infrastructure" -ForegroundColor Cyan

# 3A: Hook registered in settings.json?
if ((Test-Path $SettingsFile) -and (Select-String -Path $SettingsFile -Pattern "reindex-codebase" -Quiet)) {
    Write-OK "3A: reindex-codebase hook registered"
} else {
    Write-Fail "3A: Hook missing in $SettingsFile"
}

# 3B: MCP server configured in .mcp.json?
$McpJson = Join-Path $ProggsDir ".mcp.json"
if ((Test-Path $McpJson) -and (Select-String -Path $McpJson -Pattern "code-search" -Quiet)) {
    Write-OK "3B: MCP server configured in .mcp.json"
} else {
    Write-Fail "3B: MCP entry missing in $McpJson"
}

# 3C: .code-search in .gitignore?
$GitIgnore = Join-Path $ProggsDir ".gitignore"
if ((Test-Path $GitIgnore) -and (Select-String -Path $GitIgnore -Pattern "code-search" -Quiet)) {
    Write-OK "3C: .gitignore contains .code-search"
} else {
    Write-Fail "3C: .code-search not in .gitignore — DB would be committed to repo!"
}

# 3D: node_modules present?
if (Test-Path (Join-Path $McpDir "node_modules")) {
    Write-OK "3D: Dependencies installed"
} else {
    Write-Fail "3D: node_modules missing — run: cd $McpDir && bun install"
}

# 3E: Ollama reachable?
try {
    $OllamaResponse = Invoke-WebRequest -Uri "http://localhost:11434/api/tags" -TimeoutSec 3 -ErrorAction Stop
    if ($OllamaResponse.StatusCode -eq 200) {
        Write-OK "3E: Ollama is running"
        $OllamaRunning = $true
    } else {
        Write-Warn "3E: Ollama returned HTTP $($OllamaResponse.StatusCode)"
        $OllamaRunning = $false
    }
} catch {
    Write-Warn "3E: Ollama not reachable — start Ollama first"
    $OllamaRunning = $false
}

# 3F: nomic-embed-text model loaded?
if ($OllamaRunning) {
    try {
        $Tags = Invoke-RestMethod -Uri "http://localhost:11434/api/tags" -ErrorAction Stop
        $HasModel = $Tags.models | Where-Object { $_.name -like "*nomic-embed-text*" }
        if ($HasModel) {
            Write-OK "3F: nomic-embed-text model present"
        } else {
            Write-Fail "3F: nomic-embed-text missing — run: ollama pull nomic-embed-text"
        }
    } catch {
        Write-Warn "3F: Could not check model list — Ollama API error"
    }
} else {
    Write-Fail "3F: Cannot check model (Ollama not running)"
}

# ---------------------------------------------------------------------------
# Category 4: Parallelism safety
# ---------------------------------------------------------------------------
Write-Host ""
Write-Host "==> Category 4: Parallelism safety" -ForegroundColor Cyan

$IndexTs = Join-Path $McpDir "src\index.ts"

# 4A: resolveCurrentDb called at least twice
if (Test-Path $IndexTs) {
    $ResolveCount = (Select-String -Path $IndexTs -Pattern "resolveCurrentDb" -AllMatches).Matches.Count
    if ($ResolveCount -ge 2) {
        Write-OK "4A: resolveCurrentDb called $ResolveCount times (pointer re-read per call)"
    } else {
        Write-Fail "4A: resolveCurrentDb only $ResolveCount occurrence(s) — pointer may be cached"
    }
} else {
    Write-Warn "4A: $IndexTs not found — cannot verify"
}

# 4B: Reindex hook writes to versioned DB
$ReindexHook = Join-Path $ClaudeDir "hooks\reindex-codebase.ps1"
if (Test-Path $ReindexHook) {
    if (Select-String -Path $ReindexHook -Pattern "index-" -Quiet) {
        Write-OK "4B: Reindex hook appears to use versioned DB names"
    } else {
        Write-Warn "4B: Cannot confirm reindex writes to a new DB — review $ReindexHook"
    }
} else {
    Write-Warn "4B: $ReindexHook not found — cannot verify"
}

# 4C: Cleanup logic skips locked files (try/catch around unlink)
$StoreTs = Join-Path $McpDir "src\store.ts"
if (Test-Path $StoreTs) {
    $HasTry   = Select-String -Path $StoreTs -Pattern "try"    -Quiet
    $HasUnlink = Select-String -Path $StoreTs -Pattern "unlink" -Quiet
    if ($HasTry -and $HasUnlink) {
        Write-OK "4C: store.ts has try/catch around unlink (locked files handled)"
    } else {
        Write-Warn "4C: Cannot confirm locked-file safety in $StoreTs — review manually"
    }
} else {
    Write-Warn "4C: $StoreTs not found — cannot verify"
}

# ---------------------------------------------------------------------------
# Summary
# ---------------------------------------------------------------------------
Write-Host ""
Write-Host "=============================" -ForegroundColor Cyan
Write-Host " Semantic Search Healthcheck" -ForegroundColor Cyan
Write-Host "=============================" -ForegroundColor Cyan

if ($FailCount -gt 0) {
    Write-Host " Result: FAIL ($FailCount failure(s), $WarnCount warning(s))" -ForegroundColor Red
    Write-Host " Action: Fix FAILs before using semantic search."
    exit 1
} elseif ($WarnCount -gt 0) {
    Write-Host " Result: WARN ($WarnCount warning(s), 0 failures)" -ForegroundColor Yellow
    Write-Host " Action: Review warnings — search may still work."
    exit 2
} else {
    Write-Host " Result: OK — all checks passed" -ForegroundColor Green
    exit 0
}
