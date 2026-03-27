# graph-reindex-on-push.ps1 — PostToolUse Hook (async, Bash matcher)
# Triggers incremental codebase-memory-mcp reindex after every git push.
# Runs fully detached in the background — does not block the session.
# Platform: Windows (PowerShell)

$ErrorActionPreference = "SilentlyContinue"

# Read JSON from stdin
$hookInput = $input | Out-String

if (-not $hookInput) { exit 0 }

try {
    $data = $hookInput | ConvertFrom-Json
    $cmd = $data.tool_input.command
} catch {
    exit 0
}

if (-not $cmd) { exit 0 }

# Only trigger on git push commands
if ($cmd -notmatch 'git\s+push') { exit 0 }

# Find the codebase-memory-mcp binary
$cmmBin = $null
$candidates = @(
    "$env:USERPROFILE\.local\bin\codebase-memory-mcp.exe",
    "$env:USERPROFILE\bin\codebase-memory-mcp.exe",
    (Get-Command codebase-memory-mcp -ErrorAction SilentlyContinue).Source
)
foreach ($c in $candidates) {
    if ($c -and (Test-Path $c)) {
        $cmmBin = $c
        break
    }
}

if (-not $cmmBin) {
    Write-Host "codebase-memory-mcp not found — graph reindex skipped"
    exit 0
}

# Determine repo path
$repoPath = "$env:USERPROFILE\proggs"

# Run incremental reindex as detached background job
$jsonArg = "{`"repo_path`": `"$($repoPath -replace '\\', '/')`"}"
Start-Process -FilePath $cmmBin -ArgumentList "cli", "index_repository", $jsonArg `
    -WindowStyle Hidden -RedirectStandardOutput "$env:TEMP\graph-reindex.log" `
    -RedirectStandardError "$env:TEMP\graph-reindex-err.log"

exit 0
