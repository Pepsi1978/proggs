# reindex-codebase.ps1 — SessionStart Hook (async)
# Re-indexes the codebase for semantic search if files changed since last index.
# Uses pointer-based DB switching: writes to index-N.db, then updates current.txt.
# The old DB stays untouched and available for searches during the entire reindex.
# Old DBs are cleaned up on a best-effort basis (skips locked files on Windows).

$rootDir = "$env:USERPROFILE\proggs"
$dbDir = Join-Path $rootDir ".code-search"
$stampFile = Join-Path $dbDir ".last-index-time"
$pointerFile = Join-Path $dbDir "current.txt"
$mcpDir = Join-Path $rootDir "mcp-code-search"
$bunExe = "$env:USERPROFILE\.bun\bin\bun.exe"

if (-not (Test-Path (Join-Path $mcpDir "src\index.ts"))) { exit 0 }

try {
    $null = Invoke-WebRequest -Uri "http://localhost:11434/api/tags" -TimeoutSec 2 -ErrorAction Stop
} catch { exit 0 }

# Check if re-index is needed
$needsReindex = $false
if (-not (Test-Path $stampFile)) {
    $needsReindex = $true
} else {
    $stampTime = (Get-Item $stampFile).LastWriteTime
    $newerFiles = Get-ChildItem -Path $rootDir -Recurse -Include "*.ts","*.kt","*.rs","*.go","*.cs","*.swift","*.py","*.js","*.json","*.md","*.yaml","*.ps1","*.sh" -ErrorAction SilentlyContinue |
        Where-Object { $_.FullName -notmatch '(node_modules|\.git|\.gradle|build|dist|target|\.cache)' } |
        Where-Object { $_.LastWriteTime -gt $stampTime } |
        Select-Object -First 1
    if ($newerFiles) { $needsReindex = $true }
}

if (-not $needsReindex) { exit 0 }

# Determine next DB filename (index-N.db)
$maxN = 0
if (Test-Path $dbDir) {
    Get-ChildItem -Path $dbDir -Filter "index-*.db" -ErrorAction SilentlyContinue | ForEach-Object {
        if ($_.Name -match '^index-(\d+)\.db$') {
            $n = [int]$Matches[1]
            if ($n -gt $maxN) { $maxN = $n }
        }
    }
}
$nextN = $maxN + 1
$newDbName = "index-$nextN.db"

try {
    $script = @"
import { findCodeFiles, chunkFile } from './src/indexer.ts';
import { generateEmbeddings } from './src/ollama.ts';
import { VectorStore } from './src/store.ts';
import { resolve, join } from 'path';
import { mkdirSync, existsSync, writeFileSync, readdirSync, unlinkSync } from 'fs';

const rootDir = resolve('$($rootDir -replace '\\','/')');
const dbDir = join(rootDir, '.code-search');
if (!existsSync(dbDir)) mkdirSync(dbDir, { recursive: true });

// Write to NEW db file — old one stays untouched for active searches
const newDbName = '$newDbName';
const newDbPath = join(dbDir, newDbName);
const store = new VectorStore(newDbPath);

const files = await findCodeFiles(rootDir);
const allChunks = [];
for (const file of files) {
  const chunks = await chunkFile(file, rootDir);
  allChunks.push(...chunks);
}

const BATCH = 32;
for (let i = 0; i < allChunks.length; i += BATCH) {
  const batch = allChunks.slice(i, i + BATCH);
  const embeddings = await generateEmbeddings(batch.map(c => c.content));
  store.insertBatch(batch, embeddings);
}
store.close();

// Update pointer — this is the "switch" moment (tiny text write, practically instant)
writeFileSync(join(dbDir, 'current.txt'), newDbName);

// Clean up old DB files (best-effort — skip if locked by MCP server)
for (const f of readdirSync(dbDir)) {
  if (f.match(/^index-\d+\.db/) && !f.startsWith(newDbName.replace('.db', ''))) {
    try { unlinkSync(join(dbDir, f)); } catch {}
  }
}
"@

    $tempFile = Join-Path $env:TEMP "reindex-$([guid]::NewGuid().ToString('N').Substring(0,8)).ts"
    Set-Content -Path $tempFile -Value $script -Encoding UTF8
    $process = Start-Process -FilePath $bunExe -ArgumentList "run", $tempFile -WorkingDirectory $mcpDir -NoNewWindow -Wait -PassThru
    Remove-Item $tempFile -ErrorAction SilentlyContinue

    if ($process.ExitCode -eq 0) {
        if (-not (Test-Path $dbDir)) { New-Item -ItemType Directory -Path $dbDir -Force | Out-Null }
        Set-Content -Path $stampFile -Value (Get-Date -Format "o")
        Write-Output "Reindex-Hook: Codebase neu indexiert ($newDbName, pointer-swap)."
    }
} catch {
    # Fail silently
}
