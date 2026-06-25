# session-presence-guard.ps1: PreToolUse (Edit|Write) — Datei-Waechter gegen Ueberschreiben.
# Wenn die Zieldatei GERADE von einer anderen lebenden Session bearbeitet wird, lehnt der Hook
# den Edit ab (permissionDecision:deny + Begruendung; kein Warten -> kein Deadlock). Duenner
# Wrapper um session-presence.py. stdout -> spec-konformes deny-JSON. Platform: Windows (PowerShell 7+).
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
            $out = $raw | & $pyExe.Source $py guard 2>$null
            if (-not [string]::IsNullOrWhiteSpace($out)) { Write-Output $out }
        }
    }
} catch {
    # FAIL-OPEN: interner Fehler darf die Session nie blockieren.
    Hook-LogWarn "session-presence-guard: $_"
}

exit 0
