# stopfailure-logger.ps1 — Logs API failures to whiteboard with rate limiting
# Triggered by StopFailure hook event (Claude Code v2.1.78+)
# Uses whiteboard-insert.ps1 for section-based writing (no Add-Content!)
# Rate limit: max 1 whiteboard entry per hour to prevent spam

param()

. "$PSScriptRoot/hook-log.ps1"
. "$PSScriptRoot/whiteboard-insert.ps1"

$timestamp = Get-Date -Format "yyyy-MM-dd HH:mm"

# Rate limiting: check if we already logged a StopFailure recently
$rateLimitFile = Join-Path $env:TEMP "claude-stopfailure-last.txt"
if (Test-Path $rateLimitFile) {
    try {
        $lastLog = Get-Item $rateLimitFile
        $elapsed = (Get-Date) - $lastLog.LastWriteTime
        if ($elapsed.TotalMinutes -lt 60) {
            Hook-Log "StopFailure rate-limited (last logged $([int]$elapsed.TotalMinutes)min ago)"
            exit 0
        }
    } catch { }
}

# Read stdin for error details
$errorInput = ""
try {
    if (-not [Console]::IsInputRedirected) {
        $errorInput = "No error details available (stdin empty)"
    } else {
        $errorInput = [Console]::In.ReadToEnd()
        if ([string]::IsNullOrWhiteSpace($errorInput)) {
            $errorInput = "No error details available (stdin empty)"
        }
    }
} catch {
    $errorInput = "Could not read error details: $_"
}

# Truncate long error messages
if ($errorInput.Length -gt 300) {
    $errorInput = $errorInput.Substring(0, 300) + "... (truncated)"
}

# Update rate limit marker
Set-Content -Path $rateLimitFile -Value $timestamp -NoNewline -ErrorAction SilentlyContinue

# Fehlerart aus dem Event-JSON bestimmen (Direktive #3, eigener Vorfall 2026-08-27).
# Vorher trug JEDER Eintrag den Titel "API/Rate-Limit Error" — auch bei
# "authentication_failed". Ein Anmeldeproblem sah im Whiteboard dadurch wie ein
# Rate-Limit aus und schickte die Ursachensuche in die falsche Richtung.
# Gegenstueck zur .sh-Fassung (gleiche Faelle, gleiche Texte).
$fehlerArt = ""
try {
    $fehlerArt = ([string](ConvertFrom-Json $errorInput -ErrorAction Stop).error).Trim().ToLower()
} catch { $fehlerArt = "" }

$anmeldung = @("auth", "login", "credential", "unauthorized", "401")
$limit     = @("rate", "limit", "quota", "429")

if ($anmeldung | Where-Object { $fehlerArt -like "*$_*" }) {
    $titel = "Nicht angemeldet ($(if ($fehlerArt) { $fehlerArt } else { 'authentication_failed' }))"
    $status = "OFFEN (Anmeldung fehlt - kein Rate-Limit)"
    $vorschlag = "Der Konfigurationsordner dieser Sitzung hat keine gueltige Anmeldung. Abgleich starten: python OpenLauncher/Profiles/hooks/claude-login-sync.py - Hintergrund: bugs/claude-tooling/claude-config.md 3.9."
} elseif ($limit | Where-Object { $fehlerArt -like "*$_*" }) {
    $titel = "API/Rate-Limit Error"
    $status = "TRANSIENT (externer API-Rate-Limit, kein Harness-Bug)"
    $vorschlag = "Pruefen ob Rate-Limit temporaer oder dauerhaft. Bei dauerhaftem Fehler: API-Key pruefen."
} else {
    $titel = "API-Fehler ($(if ($fehlerArt) { $fehlerArt } else { 'unbekannt' }))"
    $status = "TRANSIENT (externer API-Fehler, kein Harness-Bug)"
    $vorschlag = "Fehlerart in den Details unten pruefen und passend einordnen."
}

# Build whiteboard entry
# 2026-05-30: Status TRANSIENT statt OFFEN — ein externer API-/Rate-Limit-Fehler ist kein
# reparierbarer Harness-Bug. So blaeht er die OFFEN-Liste nicht auf und der invariant-check
# zaehlt ihn nicht als ungeloesten Fehler. Ein Anmeldefehler ist dagegen sehr wohl
# reparierbar und bleibt deshalb bewusst OFFEN.
# 2026-08-27: Details + Fix-Vorschlag ergaenzt — die .sh-Fassung schrieb sie laengst,
# die PowerShell-Fassung nur die Titelzeile (Cross-Platform-Luecke).
$entry = @"

### $timestamp — StopFailure: $titel — Status: $status
**Quelle:** Hook: StopFailure (command-type, no API dependency)
**Symptom:** Session-Turn endete durch einen API-Fehler
**Details:** $errorInput
**Fix-Vorschlag:** $vorschlag
**Status:** $status
"@

# Write to whiteboard
try {
    Insert-WhiteboardEntry -Section "Offene Fehler & Probleme" -Entry $entry
    Hook-Log "StopFailure logged to whiteboard"
} catch {
    Hook-LogWarn "whiteboard-insert failed — StopFailure not logged"
}

exit 0
