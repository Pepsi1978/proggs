#!/usr/bin/env pwsh
# Stop Hook: setzt aktuellen Task auf paused/committed/done.
# Resilient: Input-Validation, Graceful Failure, IMMER exit 0.

$ErrorActionPreference = 'Continue'

try {
    # Fix L1.7 + L1.8: UTF-8 Encoding fuer stdin/stdout/pipe.
    $utf8NoBom = [System.Text.UTF8Encoding]::new($false)
    [Console]::InputEncoding = $utf8NoBom
    [Console]::OutputEncoding = $utf8NoBom
    $OutputEncoding = $utf8NoBom

    $stdin = [Console]::In.ReadToEnd()
    if (-not $stdin -or $stdin.Trim() -eq '') { exit 0 }

    try {
        $parsed = $stdin | ConvertFrom-Json -ErrorAction Stop
        if (-not $parsed.session_id -or [string]::IsNullOrWhiteSpace([string]$parsed.session_id)) {
            exit 0
        }
    } catch {
        exit 0
    }

    $py = (Get-Command python3 -ErrorAction SilentlyContinue) ?? (Get-Command python -ErrorAction SilentlyContinue)
    if (-not $py) { exit 0 }
    $helper = Join-Path $HOME '.claude/hooks/task-ledger-helper.py'
    if (-not (Test-Path $helper)) { exit 0 }

    $env:PYTHONIOENCODING = 'utf-8'
    $stdin | & $py.Source $helper close 2>$null | Out-Null
} catch {
}

exit 0
