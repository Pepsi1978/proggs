# compound-stagnation-detector.ps1 — SessionStart Hook
# Event: SessionStart
# Type: command
# Platform: Windows (PowerShell 7+)
#
# Zweck: Warnt bei >14 Tagen ohne neuen Compound Effect.
# Quelle: /self-improve v5.19 Stufe 4 (2026-05-10), Linse "Eleganz" + R6 Verifiable-Outcome-Gate.
# Hintergrund: Ein Compound Effect alle 7-10 Tage ist gesund. Pause >14 Tage = strukturelles Problem
# (z.B. Detektor kaputt, Trigger fehlt). Frank merkt Stagnation nicht — Hook macht sie sichtbar.
#
# Defense in Depth:
# - Jeder Fehler ist non-fatal (set silent), Hook beendet immer mit exit 0
# - Liest nur, schreibt nichts (keine Side-Effects, kein Whiteboard-Insert)
# - Ergaenzt invariant-check.ps1 (der prueft Stale-OFFEN; dieser prueft Stagnation)

$ErrorActionPreference = 'SilentlyContinue'

try {
    $forschungPath = Join-Path $env:USERPROFILE "proggs\Forschung.md"
    if (-not (Test-Path $forschungPath)) { exit 0 }

    $content = Get-Content $forschungPath -Raw -ErrorAction Stop
    # Suche alle "### [YYYY-MM-DD] ..."-Zeilen im "Compound Effect Erfolge"-Block
    $compoundSection = $content -split '## Compound Effect Erfolge' | Select-Object -Last 1
    if (-not $compoundSection) { exit 0 }

    $matches = [regex]::Matches($compoundSection, '###\s*\[(\d{4}-\d{2}-\d{2})\]')
    if ($matches.Count -eq 0) { exit 0 }

    # Letztes Datum extrahieren
    $latestDateStr = $matches[$matches.Count - 1].Groups[1].Value
    try {
        $latestDate = [DateTime]::Parse($latestDateStr)
        $daysSince = ((Get-Date) - $latestDate).Days

        if ($daysSince -gt 14) {
            $additionalContext = @"
COMPOUND-STAGNATION: Letzter dokumentierter Compound Effect ist $daysSince Tage her ($latestDateStr).
Schwelle: 14 Tage. Direktive #2 (Selbstbeobachtung) sagt: Stagnation = strukturelles Problem.
Empfehlung: /self-improve in dieser Session triggern, oder dokumentieren WARUM keine Verbesserung
ablief (z.B. reine Wartung, keine echte Aufgabe).
"@
            # JSON-Output an Gemini CLI (additionalContext-Mechanismus)
            $hookOutput = @{
                hookSpecificOutput = @{
                    hookEventName = "SessionStart"
                    additionalContext = $additionalContext
                }
            } | ConvertTo-Json -Compress
            Write-Output $hookOutput
        }
    } catch { }
} catch { }

exit 0

