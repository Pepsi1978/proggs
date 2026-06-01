# bug-almanac-index: Blendet bei Session-Start die Liste der vorhandenen Bug-Almanache ein.
# Schicht 1 ("Praesenz") des Bug-Almanach-Systems (siehe ~/proggs/bugs/SYSTEM.md).
# Runs as SessionStart hook (type: command).
# Output: JSON mit hookSpecificOutput.additionalContext (offizielles Schema) -> AI-Kontext.
#         systemMessage -> sichtbare Meldung fuer den Benutzer.
# Platform: Windows (PowerShell 7+)

. "$PSScriptRoot/hook-log.ps1"
$ErrorActionPreference = "Stop"

try {
    # Spam-Schutz-Marker des Guard-Hooks zuruecksetzen -> frische Erinnerung pro Session.
    Get-ChildItem -Path $env:TEMP -Filter "bug-almanac-seen-*.flag" -ErrorAction SilentlyContinue |
        Remove-Item -Force -ErrorAction SilentlyContinue

    $bugsDir = Join-Path $env:USERPROFILE "proggs/bugs"
    if (-not (Test-Path $bugsDir)) { exit 0 }

    $almanachs = Get-ChildItem -Path $bugsDir -Filter "*.md" -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -ne "README.md" -and $_.Name -ne "SYSTEM.md" } |
        Select-Object -ExpandProperty Name
    $list = if ($almanachs) { ($almanachs -join ", ") } else { "(noch keine)" }
    $count = if ($almanachs) { @($almanachs).Count } else { 0 }

    $ctx = "BUG-ALMANACH-SYSTEM aktiv. Vorhandene Almanache in bugs/: $list. " +
        "Vor echter Arbeit an einem technischen Bereich (Chrome, Android, WPF, Swift, TS, Hooks, Gradle ...): " +
        "bugs/README.md pruefen und den passenden Almanach ZUERST lesen. " +
        "Kein Almanach fuer den Bereich? Frank fragen (sein OK abwarten), dann bekannte Bugs recherchieren " +
        "und Almanach in bugs/ anlegen. Gilt nicht fuer trivialen Kleinkram (String, Doku, Versions-Bump)."

    $out = @{
        systemMessage = "Bug-Almanach: $count Almanach(e) aktiv."
        hookSpecificOutput = @{
            hookEventName = "SessionStart"
            additionalContext = $ctx
        }
    }
    Write-Output ($out | ConvertTo-Json -Depth 5 -Compress)
} catch {
    Hook-LogWarn "bug-almanac-index: $_"
}

exit 0
