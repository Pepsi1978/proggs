# pre-push-diff-sanity.ps1: Vor jedem git push die zu pushenden Commits analysieren
# und auffaellige Diffs (viele Loeschungen, viele Dateien) als Warnung anzeigen.
# Verhindert dass Rebase-Artefakte oder Loeschungen aus parallelen Sessions
# unbemerkt gepusht werden.
#
# Event: PreToolUse (Bash matcher)
# Schwellen: >7 Dateien geaendert, >3 Datei-Loeschungen, oder >500 Zeilen geloescht.
# Verhalten: NICHT blockierend (exit 0) — nur Warnung als additionalContext.
#
# Direktive #3 Resilient Bugfixing (Poka-Yoke Stufe 1 — Warnung).
# Verwandt: feedback_no_panic_restore_in_parallel_sessions, parallel-sessions-git.md
#
# Platform: Windows (PowerShell 7+)

$ErrorActionPreference = 'SilentlyContinue'
# UTF-8-Ausgabe erzwingen: sonst verfaelscht PowerShell Sonderzeichen auf stdout
# (Pfeil/Blitz/Gluehbirne -> ? bzw. als 0x1A JSON-Bruch). try/catch = Graceful Degradation.
try { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8 } catch {}

# --- Input lesen ---
$hookInput = [Console]::In.ReadToEnd()
if ([string]::IsNullOrWhiteSpace($hookInput)) { exit 0 }

# --- Schneller Vorab-Check (95% der Bash-Aufrufe sind nicht 'git push') ---
if ($hookInput -notmatch '\bgit\s+push\b') { exit 0 }

# --- JSON parsen ---
try {
    $data = $hookInput | ConvertFrom-Json -ErrorAction Stop
} catch { exit 0 }

if (-not $data -or -not $data.tool_input -or -not $data.tool_input.command) { exit 0 }
$cmd = $data.tool_input.command

# Final-Check auf dem geparsten command (verhindert false-positives in Argumenten)
if ($cmd -notmatch '\bgit\s+push\b') { exit 0 }

# --- Ab hier: write-path. hook-log laden. ---
try { . "$PSScriptRoot/hook-log.ps1" } catch { }

# Working Directory ermitteln
$cwd = $null
if ($data.tool_input.PSObject.Properties.Name -contains 'cwd' -and $data.tool_input.cwd) {
    $cwd = $data.tool_input.cwd
} else {
    $cwd = (Get-Location).Path
}

# Git-Bash-Pfade in Windows-Pfade konvertieren
if ($cwd -match '^/([a-zA-Z])/(.*)$') {
    $cwd = "$($Matches[1].ToUpper()):\$($Matches[2] -replace '/', '\')"
}

if (-not [System.IO.Directory]::Exists($cwd)) { exit 0 }

try {

    # Upstream-Branch ermitteln
    $upstream = & git -C $cwd rev-parse --abbrev-ref --symbolic-full-name '@{upstream}' 2>$null
    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($upstream)) {
        # Kein Upstream gesetzt -> First push, kein Diff verfuegbar -> skip
        exit 0
    }
    $upstream = $upstream.Trim()

    # Zu pushende Commits (HEAD relativ zu Upstream)
    $commitsRaw = & git -C $cwd log "$upstream..HEAD" --format='%H||%s' 2>$null
    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($commitsRaw)) {
        # Nichts zu pushen -> skip
        exit 0
    }

    $commitList = $commitsRaw -split "`n" | Where-Object { $_ -match '\|\|' }
    if ($commitList.Count -eq 0) { exit 0 }

    # --- Schwellen ---
    $THRESHOLD_FILES = 7
    $THRESHOLD_DELETED_FILES = 3
    $THRESHOLD_LINES_DELETED = 500

    # --- Pro Commit analysieren ---
    $warnings = @()
    $summary = @()

    foreach ($line in $commitList) {
        $parts = $line -split '\|\|', 2
        $hash = $parts[0].Trim()
        $msg = if ($parts.Length -gt 1) { $parts[1] } else { '' }
        if ([string]::IsNullOrWhiteSpace($hash)) { continue }

        # Diff-Stat ermitteln (Summary-Zeile am Ende)
        $statOut = & git -C $cwd diff --stat "$hash^..$hash" 2>$null
        if ($LASTEXITCODE -ne 0) { continue }

        $statLast = ($statOut | Select-Object -Last 1)
        if ([string]::IsNullOrWhiteSpace($statLast)) { continue }
        if ($statLast -notmatch '(\d+)\s+files?\s+changed') { continue }
        $filesChanged = [int]$Matches[1]

        $deletions = 0
        if ($statLast -match '(\d+)\s+deletions?\(-\)') { $deletions = [int]$Matches[1] }
        $insertions = 0
        if ($statLast -match '(\d+)\s+insertions?\(\+\)') { $insertions = [int]$Matches[1] }

        # Anzahl komplett geloeschter Dateien (D-Filter)
        $deletedFilesArr = & git -C $cwd diff --diff-filter=D --name-only "$hash^..$hash" 2>$null
        $deletedFiles = @($deletedFilesArr | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
        $deletedCount = $deletedFiles.Count

        $shortHash = $hash.Substring(0, [Math]::Min(7, $hash.Length))
        $summary += "  $shortHash : $filesChanged files, +$insertions/-$deletions lines, $deletedCount deletions  ($msg)"

        # Auffaellig?
        $reasons = @()
        if ($filesChanged -gt $THRESHOLD_FILES) {
            $reasons += "$filesChanged Dateien geaendert (Schwelle: $THRESHOLD_FILES)"
        }
        if ($deletedCount -gt $THRESHOLD_DELETED_FILES) {
            $reasons += "$deletedCount Datei-Loeschungen (Schwelle: $THRESHOLD_DELETED_FILES)"
        }
        if ($deletions -gt $THRESHOLD_LINES_DELETED) {
            $reasons += "$deletions Zeilen geloescht (Schwelle: $THRESHOLD_LINES_DELETED)"
        }

        if ($reasons.Count -gt 0) {
            $warning = "  Commit $shortHash : $msg`n    Grund: $($reasons -join ', ')"
            if ($deletedCount -gt 0) {
                $top = $deletedFiles | Select-Object -First 10
                $rest = if ($deletedCount -gt 10) { "  (+ $($deletedCount - 10) weitere)" } else { '' }
                $warning += "`n    Geloeschte Dateien:`n      - $($top -join "`n      - ")$rest"
            }
            $warnings += $warning
        }
    }

    # --- Ausgabe ---
    if ($warnings.Count -gt 0) {
        $msg = @"
[pre-push-diff-sanity] AUFFAELLIGE COMMITS in diesem git push:

$($warnings -join "`n`n")

Push-Uebersicht (alle $($commitList.Count) Commit(s)):
$($summary -join "`n")

Wenn diese Loeschungen unbeabsichtigt sind oder aus einer parallelen Session
stammen, JETZT pruefen BEVOR der push durchlaeuft:
  git log -1 --stat        # genaue Aenderungen sehen
  git reset --soft HEAD~1  # letzten Commit zurueckziehen (NICHT bei bereits gepushten)

Memory-Hinweis: feedback_no_panic_restore_in_parallel_sessions.md beschreibt
das typische Muster wenn Loeschungen aus paralleler Session in den eigenen
Commit-Diff wandern.
"@
        Write-Output $msg
        [Console]::Error.WriteLine($msg)
        try { Hook-Log "pre-push-diff-sanity: $($warnings.Count) auffaellige Commit(s) (von $($commitList.Count))" } catch { }
    } else {
        # Alles unauffaellig -> still bleiben (keine Spam-Ausgabe)
        try { Hook-Log "pre-push-diff-sanity: $($commitList.Count) Commit(s) ok" } catch { }
    }

} catch {
    try { Hook-LogWarn "pre-push-diff-sanity: $_" } catch { }
}

# PFLICHT: exit 0 — Hook darf push NICHT blockieren
exit 0
