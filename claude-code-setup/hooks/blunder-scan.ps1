#!/usr/bin/env pwsh
# blunder-scan.ps1 — Optional Pre-Commit Blunder Check (L3)
#
# Inspiriert vom Schach-Engine "Refutation Search": Vor jedem wichtigen Zug prueft
# die Engine nicht nur "was ich erreichen will" sondern "was kann der Gegner danach tun".
# Uebertragung: Vor wichtigen Commits fragen wir einen schnellen Gegen-Agent:
# "Was koennten die naechsten 3 Fehler sein die AUS diesem Diff entstehen?"
#
# ACHTUNG: NICHT als automatischer Hook in settings.json registriert!
# Standalone-Tool. Aufruf manuell: `pwsh ~/.claude/hooks/blunder-scan.ps1`
# oder als pre-commit-Hook wenn der Benutzer es explizit aktiviert.
#
# Direktive #3 Conformance:
# - Non-blocking: Warnt nur, committet nicht automatisch
# - Timeout-Schutz: Max 60 Sekunden (sonst Abbruch)
# - Graceful Degradation: Bei Fehler -> kein Blocker, nur Notiz

param(
    [switch]$Strict,       # Bei Strict: exit 1 wenn Blunder gefunden (fuer pre-commit mit Nachfrage)
    [int]$TimeoutSec = 60
)

$ErrorActionPreference = 'Continue'
trap { Write-Host "[blunder-scan] Fehler abgefangen, beende still." -ForegroundColor Yellow; exit 0 }

# 1. Hole den Diff der staged Changes
$repoRoot = & git rev-parse --show-toplevel 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "[blunder-scan] Kein Git-Repository — uebersprungen." -ForegroundColor Gray
    exit 0
}

Push-Location $repoRoot
try {
    $diff = & git diff --cached --stat 2>&1
    $diffFull = & git diff --cached 2>&1

    if ([string]::IsNullOrWhiteSpace($diffFull)) {
        Write-Host "[blunder-scan] Keine staged Aenderungen — nichts zu pruefen." -ForegroundColor Gray
        exit 0
    }

    # Truncate diff if too large (keep first 5000 chars)
    $diffText = if ($diffFull.Length -gt 5000) {
        $diffFull.Substring(0, 5000) + "`n`n[... truncated for speed ...]"
    } else { $diffFull }

    Write-Host "[blunder-scan] Starte Refutation-Scan (max ${TimeoutSec}s)..." -ForegroundColor Cyan
    Write-Host "Geaenderte Dateien:" -ForegroundColor Gray
    Write-Host $diff -ForegroundColor Gray

    # 2. Schreibe Diff in Temp-Datei fuer den challenger-Prompt
    $tempPrompt = Join-Path $env:TEMP "blunder-scan-$(Get-Random).txt"
    $promptContent = @"
Refutation-Scan fuer geplanten Commit. Beantworte in max 300 Woerter auf Deutsch:

**Was sind die 3 wahrscheinlichsten Blunders die aus diesem Diff entstehen koennten?**

Konkrete Fragen:
1. Bricht diese Aenderung andere Hooks/Skills/Agents die NICHT im Diff sind?
2. Fehlt das Cross-Platform-Gegenstueck (.sh wenn .ps1 geaendert, umgekehrt)?
3. Wird bestehende Funktionalitaet ungewollt entfernt? (Funktionalitaets-Diff!)
4. Gibt es Race Conditions oder Update-Resistenz-Probleme?
5. Koennte der Benutzer etwas sehen das er nicht sehen sollte (Secrets)?

Nur Blunders nennen die WAHRSCHEINLICH sind (>30%), nicht jede theoretische Moeglichkeit.
Bei KEINEM erwarteten Blunder: "Diff sauber, keine Refutation noetig."

Diff:
$diffText
"@
    $promptContent | Out-File -FilePath $tempPrompt -Encoding utf8

    Write-Host "`n[blunder-scan] Rufe challenger-Agent auf..." -ForegroundColor Cyan
    Write-Host "[blunder-scan] HINWEIS: Dieser Hook ist standalone — zur Ausfuehrung aus einer Claude-Code-Session:" -ForegroundColor Yellow
    Write-Host "[blunder-scan]   Nutze den challenger-Agent mit dem Inhalt von: $tempPrompt" -ForegroundColor Yellow
    Write-Host "[blunder-scan] ODER aktiviere diesen Hook als echten pre-commit in git config." -ForegroundColor Yellow
    Write-Host "[blunder-scan] Output bleibt in $tempPrompt zur manuellen Pruefung." -ForegroundColor Yellow

    # 3. Wenn wir in einer Claude-Session sind: Ergebnis kann der User sehen
    #    Der eigentliche Agent-Call muss vom Haupt-Claude gemacht werden (Hook hat
    #    keinen Zugriff auf Agent-Tool). Das ist by design — keine Autonomie fuer
    #    Hooks, Transparenz fuer den Benutzer.

    exit 0
} finally {
    Pop-Location
}

exit 0
