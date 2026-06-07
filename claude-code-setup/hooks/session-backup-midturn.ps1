# session-backup-midturn: Mid-Turn-Schwester von session-backup-nudge.
#   Faengt den Fall, dass der Kontext INNERHALB eines langen Turns ueber 80% springt
#   (z.B. Researcher-Schwarm: ein Task-Turn kann +300k Token bringen) — genau da, wo
#   der Stop-Hook (laeuft nur ZWISCHEN Turns) strukturell nicht hinschaut.
# Runs as PostToolUse hook, matcher "Task|Agent" (NICHT Bash: Regression v2.1.123/#55889
#   droppt beim Bash-Matcher alle Kontext-Kanaele -> additionalContext kaeme nie an).
# Teilt sich ctx-Datei UND Marker (ctx-nudged-<sid>) mit session-backup-nudge ->
#   kein Doppel-Feuern (reference_ctx_percent_in_hooks).
# PostToolUse: IMMER exit 0 (claude-hooks.md 1.5: exit 1 blockiert faelschlich).
#   Kontext nur via nested hookSpecificOutput.additionalContext (2.1) + ConvertTo-Json -Depth 5 (2.2).
. "$PSScriptRoot/hook-log.ps1"
$ErrorActionPreference = "Stop"

try {
    $stdin = [Console]::In.ReadToEnd()
    if ([string]::IsNullOrWhiteSpace($stdin)) {
        try { $stdin = $input | Out-String } catch {}
    }
    if ([string]::IsNullOrWhiteSpace($stdin)) { exit 0 }

    $data = $null
    try { $data = $stdin | ConvertFrom-Json -ErrorAction Stop } catch { exit 0 }

    # Input-Guard: PostToolUse braucht tool_name (hook-input-validation)
    if (-not $data.tool_name) { exit 0 }

    $sid = [string]$data.session_id
    if (-not $sid -or $sid -notmatch '^[A-Za-z0-9_-]+$') { exit 0 }

    $stateDir = Join-Path $env:USERPROFILE '.claude\state'
    # Geteilter Marker mit session-backup-nudge: nur EINMAL pro Session anstupsen
    $marker = Join-Path $stateDir "ctx-nudged-$sid"
    if (Test-Path $marker) { exit 0 }

    $ctxFile = Join-Path $stateDir "ctx-$sid"
    if (-not (Test-Path $ctxFile)) { exit 0 }

    $pctRaw = ((Get-Content -Raw -Path $ctxFile -ErrorAction Stop) -replace '\D', '')
    if (-not $pctRaw) { exit 0 }
    $pct = [int]$pctRaw
    if ($pct -lt 80) { exit 0 }

    Set-Content -Path $marker -Value $pct -Encoding ASCII -ErrorAction SilentlyContinue

    $reason = "KONTEXT BEI $pct% MITTEN IM TURN (Schwelle 80%, grosse Komprimierung droht bei 100%). Dieser Hinweis kommt nur EINMAL pro Session. Ein einzelner langer Turn (z.B. Researcher-Schwarm) kann den Kontext schlagartig anheben. Wenn die aktuelle Aufgabe an einem sinnvollen Punkt steht (kein offener Multiple-Choice, keine Rueckfrage an den Benutzer offen): Schliesse den aktuellen Teilschritt ab und fuehre dann den Skill 'session' mit Unterbefehl 'backup' aus, zeige danach den Disketten-Marker (drei Disketten vorne, drei Ausrufezeichen hinten, je eine dicke Linie drueber und drunter) und anschliessend eine FETTE Warnung mit Warndreieck, dass der Benutzer JETZT eine neue Session starten muss (/clear, danach 'session restore'). Wenn gerade ein kritischer Schritt laeuft oder du auf eine Antwort des Benutzers wartest: erst fertig machen bzw. abwarten, das Backup direkt danach."

    $out = @{ hookSpecificOutput = @{ hookEventName = "PostToolUse"; additionalContext = $reason } } | ConvertTo-Json -Depth 5 -Compress
    Write-Output $out
} catch {
    try { Hook-LogWarn "session-backup-midturn: $_" } catch {}
}

exit 0
