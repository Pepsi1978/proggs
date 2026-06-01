# session-backup-nudge: Stupst Claude bei >=88% Kontext einmal pro Session an,
#   ein 'session backup' zu machen, BEVOR die grosse Komprimierung (100%) zuschlaegt.
# Runs as Stop hook.
# stdout -> JSON {"decision":"block","reason":...} (reason geht als Anweisung an Claude)
# Platform: Windows (PowerShell 7+)
#
# Der exakte Kontext-Prozentsatz kommt NICHT im Stop-Input — er wird von der
# Statusline (statusline.ps1/.sh) pro Session nach ~/.claude/state/ctx-<sid> geschrieben.
# Schleifenschutz: (1) stop_hook_active==true -> raus; (2) Nudge-Marker pro Session.

. "$PSScriptRoot/hook-log.ps1"
$ErrorActionPreference = "Stop"

try {
    $stdin = [Console]::In.ReadToEnd()
    if ([string]::IsNullOrWhiteSpace($stdin)) { exit 0 }

    $data = $null
    try { $data = $stdin | ConvertFrom-Json -ErrorAction Stop } catch { exit 0 }

    # Schleifenschutz 1: Claude macht bereits aufgrund eines Stop-Hooks weiter
    if ($data.stop_hook_active -eq $true) { exit 0 }

    $sid = [string]$data.session_id
    if (-not $sid -or $sid -notmatch '^[A-Za-z0-9_-]+$') { exit 0 }

    $stateDir = Join-Path $env:USERPROFILE '.claude\state'
    $ctxFile = Join-Path $stateDir "ctx-$sid"
    if (-not (Test-Path $ctxFile)) { exit 0 }

    $pctRaw = ((Get-Content -Raw -Path $ctxFile -ErrorAction Stop) -replace '\D', '')
    if (-not $pctRaw) { exit 0 }
    $pct = [int]$pctRaw
    if ($pct -lt 88) { exit 0 }

    # Schleifenschutz 2: nur EINMAL pro Session anstupsen
    $marker = Join-Path $stateDir "ctx-nudged-$sid"
    if (Test-Path $marker) { exit 0 }
    Set-Content -Path $marker -Value $pct -Encoding ASCII -ErrorAction SilentlyContinue

    $reason = "KONTEXT BEI $pct% (Schwelle 88%, grosse Komprimierung droht bei 100%). Dieser Hinweis kommt nur EINMAL pro Session. Wenn die aktuelle Aufgabe VOLLSTAENDIG abgeschlossen ist (kein offener Multiple-Choice, keine Rueckfrage an den Benutzer offen): Fuehre JETZT den Skill 'session' mit Unterbefehl 'backup' aus, zeige danach den Disketten-Marker (drei Disketten vorne, drei Ausrufezeichen hinten, je eine dicke Linie drueber und drunter) und anschliessend eine FETTE Warnung mit Warndreieck, dass der Benutzer JETZT eine neue Session starten muss (/clear, danach 'session restore'). Wenn die Aufgabe noch laeuft oder du auf eine Antwort des Benutzers wartest: erst fertig machen bzw. abwarten, das Backup danach."

    $out = @{ decision = "block"; reason = $reason } | ConvertTo-Json -Compress
    Write-Output $out
} catch {
    Hook-LogWarn "session-backup-nudge: $_"
}

exit 0
