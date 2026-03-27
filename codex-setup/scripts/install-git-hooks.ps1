$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot = (Resolve-Path (Join-Path $ScriptDir "..\..")).Path
$HooksDir = Join-Path $RepoRoot "codex-setup\hooks"
$PreCommitHook = Join-Path $HooksDir "pre-commit"

if (-not (Test-Path $PreCommitHook)) {
    throw "Missing Codex pre-commit hook: $PreCommitHook"
}

& git config --local core.hooksPath $HooksDir
if ($LASTEXITCODE -ne 0) {
    throw "Failed to configure git hooks path."
}

Write-Host "Configured git hooks path to $HooksDir"
