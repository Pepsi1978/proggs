# graph-reindex-on-push.ps1 — PostToolUse Hook (async, Bash matcher)
# Triggers incremental reindex of GitNexus + codebase-memory-mcp after every git push.
# Runs fully detached in the background — does not block the session.
# Platform: Windows (PowerShell)
#
# UPDATED (2026-04-12): Added GitNexus incremental reindex (gitnexus analyze)

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

$repoPath = "$env:USERPROFILE\proggs"

# --- GitNexus incremental reindex (nur geaenderte Dateien) ---
$gitnexusBin = "$env:USERPROFILE\AppData\Roaming\npm\gitnexus.cmd"
if (Test-Path $gitnexusBin) {
    Start-Process -FilePath $gitnexusBin -ArgumentList "analyze", $repoPath, "--skip-agents-md" `
        -WindowStyle Hidden -WorkingDirectory $repoPath `
        -RedirectStandardOutput "$env:TEMP\gitnexus-reindex.log" `
        -RedirectStandardError "$env:TEMP\gitnexus-reindex-err.log"
}

# --- codebase-memory-mcp reindex (falls installiert) ---
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

if ($cmmBin) {
    $jsonArg = "{`"repo_path`": `"$($repoPath -replace '\\', '/')`"}"
    Start-Process -FilePath $cmmBin -ArgumentList "cli", "index_repository", $jsonArg `
        -WindowStyle Hidden -RedirectStandardOutput "$env:TEMP\graph-reindex.log" `
        -RedirectStandardError "$env:TEMP\graph-reindex-err.log"
}

exit 0
