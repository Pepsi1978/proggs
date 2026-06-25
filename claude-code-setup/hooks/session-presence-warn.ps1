# session-presence-warn.ps1: UserPromptSubmit — warnt, wenn eine andere lebende Session im SELBEN
# Projekt arbeitet (Awareness gegen versehentliches Ueberschreiben). Duenner Wrapper um
# session-presence.py (eine Logik fuer beide Plattformen). stdout -> AI context (additionalContext).
# Platform: Windows (PowerShell 7+).
. "$PSScriptRoot/hook-log.ps1"
$ErrorActionPreference = "Stop"

try {
    # stdin robust lesen (claude-hooks.md 12.4): erst [Console]::In, dann $input.
    $raw = ""
    try { $raw = [Console]::In.ReadToEnd() } catch {}
    if ([string]::IsNullOrWhiteSpace($raw)) { try { $raw = $input | Out-String } catch {} }
    if ([string]::IsNullOrWhiteSpace($raw)) { exit 0 }

    $py = Join-Path $PSScriptRoot "session-presence.py"
    if (Test-Path $py) {
        $pyExe = Get-Command python3 -ErrorAction SilentlyContinue
        if (-not $pyExe) { $pyExe = Get-Command python -ErrorAction SilentlyContinue }
        if ($pyExe) {
            $out = $raw | & $pyExe.Source $py warn 2>$null
            if (-not [string]::IsNullOrWhiteSpace($out)) { Write-Output $out }
        }
    }
} catch {
    Hook-LogWarn "session-presence-warn: $_"
}

exit 0
