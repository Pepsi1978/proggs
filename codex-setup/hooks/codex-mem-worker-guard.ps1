# codex-mem-worker-guard.ps1 -- compatibility shim for Codex hook settings
$ErrorActionPreference = "Stop"

try {
    $target = Join-Path $PSScriptRoot "claude-mem-worker-guard.ps1"
    if (Test-Path $target) {
        & $target @args
    }
} catch {
    try {
        $log = Join-Path $PSScriptRoot "hook-log.ps1"
        if (Test-Path $log) {
            . $log
            Hook-LogError "codex-mem-worker-guard shim failed: $($_.Exception.Message)"
        }
    } catch {}
}

exit 0
