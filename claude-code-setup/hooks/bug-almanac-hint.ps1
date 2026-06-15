# bug-almanac-hint.ps1: passiver UserPromptSubmit-Trigger — scannt Prompt auf Almanach-Bereichs-Stichwoerter
# Runs as UserPromptSubmit hook
# stdout -> AI context (hookSpecificOutput.additionalContext JSON), stderr -> user terminal
# Platform: Windows (PowerShell 7+). Logik in bug-almanac-hint.py (Cross-Platform-Konsistenz, eine Stichwort-Map).
. "$PSScriptRoot/hook-log.ps1"
$ErrorActionPreference = "Stop"

try {
    # stdin robust lesen (claude-hooks 12.4: erst Console.In, dann $input als Fallback)
    $stdin = [Console]::In.ReadToEnd()
    if (-not $stdin) { try { $stdin = $input | Out-String } catch { $stdin = "" } }

    $py = Join-Path $PSScriptRoot "bug-almanac-hint.py"
    if ($stdin -and (Test-Path $py) -and (Get-Command python -ErrorAction SilentlyContinue)) {
        $out = $stdin | python $py 2>$null
        if ($out -and ("$out").Trim()) { Write-Output $out }
    }
} catch {
    Hook-LogWarn "bug-almanac-hint: $_"
}

# PFLICHT: passiver Hint-Hook endet immer graceful.
exit 0
