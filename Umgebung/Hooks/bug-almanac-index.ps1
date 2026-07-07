# bug-almanac-index: Blendet bei Session-Start die Liste der vorhandenen Bug-Almanache ein.
# Schicht 1 ("Praesenz") des Bug-Almanach-Systems (siehe ~/proggs/bugs/SYSTEM.md).
# Runs as SessionStart hook (type: command).
# Output: JSON mit hookSpecificOutput.additionalContext (offizielles Schema) -> AI-Kontext.
#         systemMessage -> sichtbare Meldung fuer den Benutzer.
# Platform: Windows (PowerShell 7+)

. "$PSScriptRoot/hook-log.ps1"
$ErrorActionPreference = "Stop"

try {
    # SessionStart-source ermitteln (startup/clear/resume/compact). Bewaehrtes Muster aus session-guard.
    # Marker-Cleanup wird NUR bei einem KONTEXT-ERHALTENDEN Reset (compact/resume) UEBERSPRUNGEN, sonst
    # (startup/clear/unbekannt) wie bisher geloescht. Grund (Frank-Wunsch 2026-06-22): SessionStart feuert
    # auch bei Auto-Compact/Resume (gleiche Arbeitsphase, nur Kontext komprimiert) - dort die "gelesen"-
    # Marker zu loeschen liess den Guard mitten im Arbeitsfluss erneut blocken (nervte mehrfach).
    $sessionSource = ""
    try {
        $stdinRaw = [Console]::In.ReadToEnd()
        if ([string]::IsNullOrWhiteSpace($stdinRaw)) { try { $stdinRaw = $input | Out-String } catch {} }
        if (-not [string]::IsNullOrWhiteSpace($stdinRaw)) {
            $sj = $stdinRaw | ConvertFrom-Json -ErrorAction Stop
            if ($sj.source) { $sessionSource = [string]$sj.source }
        }
    } catch {}

    # Alle Guard-Marker zuruecksetzen -> frische Erinnerung + frischer Lese-Zwang pro echter Session.
    # Deckt seen-* (Spam-Schutz), read-*/full-*/bp-read-* ("gelesen"-Marker) und disable (Notaus) ab.
    # Der Notaus gilt damit nur fuer die Session in der er gesetzt wurde (Schutz ist danach wieder an).
    if ($sessionSource -ne "compact" -and $sessionSource -ne "resume") {
        Get-ChildItem -Path $env:TEMP -Filter "bug-almanac-*.flag" -ErrorAction SilentlyContinue |
            Remove-Item -Force -ErrorAction SilentlyContinue
    }

    $bugsDir = Join-Path $env:USERPROFILE "proggs/bugs"
    if (-not (Test-Path $bugsDir)) { exit 0 }

    # Almanache liegen in Kategorie-Unterordnern (bugs/<kategorie>/<file>.md). Rekursiv sammeln und
    # als relativen Pfad ab bugs/ anzeigen; alles direkt in bugs/ (README/SYSTEM/OFFENE-*) hat keinen
    # '/' im Relativpfad und faellt damit automatisch raus (robust gegen Slash-Normalisierung).
    $almanachs = Get-ChildItem -Path $bugsDir -Recurse -Filter "*.md" -File -ErrorAction SilentlyContinue |
        ForEach-Object { ($_.FullName -replace '\\','/') -replace '.*?/bugs/', '' } |
        Where-Object { $_ -match '/' -and $_ -notmatch '-kurzcheck\.md$' } |
        Sort-Object
    $list = if ($almanachs) { ($almanachs -join ", ") } else { "(noch keine)" }
    $count = if ($almanachs) { @($almanachs).Count } else { 0 }

    $ctx = "BUG-ALMANACH-SYSTEM aktiv (Digest-Modell, 3 Stufen - siehe bugs/SYSTEM.md Paragraf 11). Vorhandene Almanache in bugs/: $list. " +
        "STUFE A (vor der Arbeit): Vor echter Arbeit an einem technischen Bereich (Chrome, Android, WPF, Swift, TS, Hooks, Gradle ...) " +
        "NUR den Kurzcheck lesen: Read auf bugs/<kategorie>/<bereich>.md mit limit=80 (die Kurzcheck-Sektion oben in der Datei), " +
        "DANACH ebenso die zugehoerige best-practices/projekt-code/<kategorie>/best-practices-<bereich>.md mit limit=80. " +
        "Der bug-almanac-guard BLOCKIERT Edit/Write bis beides gelesen ist (Reihenfolge erst Almanach, dann Best Practices; 1x pro Bereich/Session). " +
        "STUFE B (bei JEDEM Fehler im Bereich): SOFORT den VOLLTEXT des Almanachs lesen (Read ohne limit) - ab dem ersten Fehler reicht der Kurzcheck nicht mehr. " +
        "STUFE C (Hochrisiko-Bereiche r8, firebase-billing, claude-hooks, claude-config): schon VOR der Arbeit den VOLLTEXT lesen - der Guard erzwingt das. " +
        "Kein Almanach fuer den Bereich? Frank fragen (sein OK abwarten), dann den Skill 'bug-almanach-recherche' STARTEN " +
        "(das ist der vorgeschriebene Weg, NICHT selbst ad hoc recherchieren). Gilt nicht fuer trivialen Kleinkram (String, Doku, Versions-Bump). " +
        "Notaus bei Fehlalarm: leere Datei bug-almanac-disable.flag im TEMP-Verzeichnis anlegen."

    # ── Almanach-Trigger-Sonde: Reminder zum woechentlichen Auswerten (Direktive #2) ──
    # Zaehlt die block-Ereignisse der Sonde (state/bug-almanac-triggers.jsonl + .jsonl.1) per
    # Textsuche (schnell, kein JSON-Parse pro Zeile). state/almanach-last-review.txt merkt sich
    # Datum + Block-Zahl der letzten Auswertung (vom Skill almanach-trigger-auswertung gesetzt) ->
    # so kann "neue seit letzter Auswertung" gezeigt werden. Nur anzeigen, wenn es neue gibt.
    $reminder = ""
    try {
        $stateDir = Join-Path $env:USERPROFILE ".claude/state"
        $triggerFiles = @((Join-Path $stateDir "bug-almanac-triggers.jsonl.1"),
                          (Join-Path $stateDir "bug-almanac-triggers.jsonl")) |
                        Where-Object { Test-Path $_ }
        if ($triggerFiles) {
            $totalBlocks = 0
            foreach ($tf in $triggerFiles) {
                $totalBlocks += @(Select-String -Path $tf -Pattern '"event":"block"' -ErrorAction SilentlyContinue).Count
            }
            $reviewFile = Join-Path $stateDir "almanach-last-review.txt"
            $prevCount = 0; $reviewDate = $null
            if (Test-Path $reviewFile) {
                try {
                    $rl = @(Get-Content $reviewFile -ErrorAction SilentlyContinue)
                    if ($rl.Count -ge 1) { $reviewDate = $rl[0].Trim() }
                    if ($rl.Count -ge 2) { $prevCount = [int]($rl[1].Trim()) }
                } catch {}
            }
            $newBlocks = $totalBlocks - $prevCount
            if ($newBlocks -lt 0) { $newBlocks = $totalBlocks }  # Rotation/Reset -> auf Gesamt zurueckfallen
            if ($newBlocks -gt 0) {
                $since = if ($reviewDate) {
                    $d = $null
                    try { $d = ([datetime]::Today - [datetime]::Parse($reviewDate).Date).Days } catch {}
                    if ($null -ne $d) { " (seit letzter Auswertung vor $d Tagen)" } else { " (seit letzter Auswertung)" }
                } else { " (noch nie ausgewertet)" }
                $reminder = " | Almanach-Sonde: $newBlocks Unterbrechung(en)$since - sag 'werte die Almanach-Trigger aus'."
            }
        }
    } catch {}

    $out = @{
        systemMessage = "Bug-Almanach: $count Almanach(e) aktiv.$reminder"
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
