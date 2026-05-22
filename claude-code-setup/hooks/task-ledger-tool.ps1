#!/usr/bin/env pwsh
# PostToolUse Hook: haengt Files/Commits an aktuellen Task-Eintrag.
# Matcher in settings.json beschraenkt auf Edit|Write|MultiEdit|Bash.
# Resilient: Input-Validation, Graceful Failure, IMMER exit 0.

$ErrorActionPreference = 'Continue'

try {
    $stdin = [Console]::In.ReadToEnd()
    if (-not $stdin -or $stdin.Trim() -eq '') { exit 0 }

    try {
        $parsed = $stdin | ConvertFrom-Json -ErrorAction Stop
        if (-not $parsed.session_id -or [string]::IsNullOrWhiteSpace([string]$parsed.session_id)) {
            exit 0
        }
        if (-not $parsed.tool_name) { exit 0 }
    } catch {
        exit 0
    }

    $py = (Get-Command python3 -ErrorAction SilentlyContinue) ?? (Get-Command python -ErrorAction SilentlyContinue)
    if (-not $py) { exit 0 }
    $helper = Join-Path $HOME '.claude/hooks/task-ledger-helper.py'
    if (-not (Test-Path $helper)) { exit 0 }

    $env:PYTHONIOENCODING = 'utf-8'
    $stdin | & $py.Source $helper update 2>$null | Out-Null
} catch {
}

exit 0
