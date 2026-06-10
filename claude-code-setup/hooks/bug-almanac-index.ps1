# bug-almanac-index: Blendet bei Session-Start die Liste der vorhandenen Bug-Almanache ein.
# Schicht 1 ("Praesenz") des Bug-Almanach-Systems (siehe ~/proggs/bugs/SYSTEM.md).
# Runs as SessionStart hook (type: command).
# Output: JSON mit hookSpecificOutput.additionalContext (offizielles Schema) -> AI-Kontext.
#         systemMessage -> sichtbare Meldung fuer den Benutzer.
# Platform: Windows (PowerShell 7+)

. "$PSScriptRoot/hook-log.ps1"
$ErrorActionPreference = "Stop"

try {
    # Alle Guard-Marker zuruecksetzen -> frische Erinnerung + frischer Lese-Zwang pro Session.
    # Deckt seen-* (Spam-Schutz), read-* (Stufe-2-"gelesen"-Marker) und disable (Notaus) ab.
    # Der Notaus gilt damit nur fuer die Session in der er gesetzt wurde (Schutz ist danach wieder an).
    Get-ChildItem -Path $env:TEMP -Filter "bug-almanac-*.flag" -ErrorAction SilentlyContinue |
        Remove-Item -Force -ErrorAction SilentlyContinue

    $bugsDir = Join-Path $env:USERPROFILE "proggs/bugs"
    if (-not (Test-Path $bugsDir)) { exit 0 }

    # Almanache liegen in Kategorie-Unterordnern (bugs/<kategorie>/<file>.md). Rekursiv sammeln und
    # als relativen Pfad ab bugs/ anzeigen; alles direkt in bugs/ (README/SYSTEM/OFFENE-*) hat keinen
    # '/' im Relativpfad und faellt damit automatisch raus (robust gegen Slash-Normalisierung).
    $almanachs = Get-ChildItem -Path $bugsDir -Recurse -Filter "*.md" -File -ErrorAction SilentlyContinue |
        ForEach-Object { ($_.FullName -replace '\\','/') -replace '.*?/bugs/', '' } |
        Where-Object { $_ -match '/' } |
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
