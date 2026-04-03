# hook-exit0-guard: Pre-commit check that ALL hooks end with exit 0
# Runs as PreToolUse hook — triggers on Bash tool when git commit is detected
# stdout -> AI context (system-reminder), stderr -> user terminal
# Platform: Windows (PowerShell 7+)

. "$PSScriptRoot/hook-log.ps1"

$ErrorActionPreference = "Stop"

try {
    $input_data = $input | Out-String | ConvertFrom-Json
    $toolName = $input_data.tool_name
    $toolInput = $input_data.tool_input

    # Only trigger on Bash tool with git commit commands
    if ($toolName -ne "Bash") { exit 0 }
    $command = $toolInput.command
    if (-not $command) { exit 0 }
    if ($command -notmatch 'git commit') { exit 0 }

    # Check if any hook files are staged
    $staged = git -C "$env:USERPROFILE\proggs" diff --cached --name-only 2>$null
    if (-not $staged) { exit 0 }

    $hookFiles = $staged | Where-Object { $_ -match '\.(ps1|sh)$' -and $_ -match 'hook' }
    if (-not $hookFiles -or $hookFiles.Count -eq 0) { exit 0 }

    # Check each staged hook file for exit 0
    $problems = @()
    foreach ($file in $hookFiles) {
        $fullPath = Join-Path "$env:USERPROFILE\proggs" $file
        if (-not (Test-Path $fullPath)) { continue }

        $content = Get-Content $fullPath -Raw -ErrorAction SilentlyContinue
        if (-not $content) { continue }

        # Check if file ends with exit 0 (allowing trailing whitespace/newlines)
        if ($content -notmatch 'exit\s+0\s*$') {
            $problems += $file
        }

        # Check for exit 1 in SessionStart hooks
        if ($file -match 'session' -or $file -match 'auto-sync' -or $file -match 'invariant') {
            if ($content -match 'exit\s+1') {
                $problems += "$file (contains exit 1 — forbidden in SessionStart hooks!)"
            }
        }
    }

    if ($problems.Count -gt 0) {
        $list = $problems -join "`n  - "
        $msg = "Hook-Exit0-Guard: WARNUNG — folgende Hook-Dateien haben kein 'exit 0' am Ende:`n  - $list`nBitte 'exit 0' am Ende jeder Hook-Datei hinzufuegen!"
        Write-Output $msg
        [Console]::Error.WriteLine($msg)
        Hook-LogWarn "Hook files missing exit 0: $($problems -join ', ')"
    }
} catch {
    Hook-LogWarn "hook-exit0-guard: $_"
}

exit 0
