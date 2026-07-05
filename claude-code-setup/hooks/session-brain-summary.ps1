# session-brain-summary: SessionEnd — schickt die Session-Rohdaten (Prompts, Commits, Dateien)
# an den Second-Brain-Agenten (POST /session-log), der sie per LLM zu "gemacht/entschieden/
# gelernt" verdichtet und unter Programmierung/Sessions ins Gehirn legt + den Kern-Block
# "Woran Frank gerade baut" nachzieht (Gruppe D 27/28/32). Duenner Wrapper um
# session-brain-summary.py (EINE Logik fuer beide Plattformen).
# Runs as SessionEnd hook (command, async). Fail-open: blockiert ein Session-Ende NIE.
# Platform: Windows (PowerShell 7+)

. "$PSScriptRoot/hook-log.ps1"
$ErrorActionPreference = "Stop"

try {
    # stdin robust lesen: je nach Invokation liefert mal [Console]::In, mal $input (claude-hooks.md 12.4).
    $raw = ""
    try { $raw = [Console]::In.ReadToEnd() } catch {}
    if ([string]::IsNullOrWhiteSpace($raw)) { try { $raw = $input | Out-String } catch {} }
    if ([string]::IsNullOrWhiteSpace($raw)) { exit 0 }

    $py = Join-Path $PSScriptRoot "session-brain-summary.py"
    if (Test-Path $py) {
        $pyExe = Get-Command python3 -ErrorAction SilentlyContinue
        if (-not $pyExe) { $pyExe = Get-Command python -ErrorAction SilentlyContinue }
        if ($pyExe) {
            $raw | & $pyExe.Source $py 2>$null | Out-Null
        } else {
            Hook-LogWarn "session-brain-summary: kein python3/python im PATH — Session-Log uebersprungen"
        }
    }
} catch {
    # Fehler loggen aber NIEMALS propagieren — dieser Hook darf ein Session-Ende nie stoeren.
    Hook-LogWarn "session-brain-summary failed: $_"
}

# PFLICHT: Jeder Hook MUSS mit exit 0 enden — ausnahmslos.
exit 0
