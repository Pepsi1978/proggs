#!/usr/bin/env pwsh
# UserPromptSubmit Hook: schreibt einen neuen Task-Eintrag in den Ledger.
# Resilient: Input-Validation, Graceful Failure, IMMER exit 0.

$ErrorActionPreference = 'Continue'

try {
    $stdin = [Console]::In.ReadToEnd()
    if (-not $stdin -or $stdin.Trim() -eq '') { exit 0 }

    # Quick-Validation: muss session_id enthalten
    try {
        $parsed = $stdin | ConvertFrom-Json -ErrorAction Stop
        if (-not $parsed.session_id -or [string]::IsNullOrWhiteSpace([string]$parsed.session_id)) {
            exit 0
        }
    } catch {
        exit 0
    }

    # An Python-Helper weiterleiten
    $py = (Get-Command python3 -ErrorAction SilentlyContinue) ?? (Get-Command python -ErrorAction SilentlyContinue)
    if (-not $py) { exit 0 }
    $helper = Join-Path $HOME '.claude/hooks/task-ledger-helper.py'
    if (-not (Test-Path $helper)) { exit 0 }

    $env:PYTHONIOENCODING = 'utf-8'
    $stdin | & $py.Source $helper add 2>$null | Out-Null
} catch {
    # Hook darf Session NIE blockieren
}

exit 0
