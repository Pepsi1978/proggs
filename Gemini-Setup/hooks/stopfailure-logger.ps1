# stopfailure-logger.ps1 — Logs API failures to whiteboard without requiring API access
# Triggered by StopFailure hook event (Gemini CLI v2.1.78+)
# Uses whiteboard-insert.ps1 for section-based writing (no Add-Content!)

param()

. "$PSScriptRoot/hook-log.ps1"
. "$PSScriptRoot/whiteboard-insert.ps1"

$timestamp = Get-Date -Format "yyyy-MM-dd HH:mm"
# Read stdin for error details (Gemini CLI pipes error info)
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
if ($errorInput.Length -gt 500) {
    $errorInput = $errorInput.Substring(0, 500) + "... (truncated)"
}

# Build whiteboard entry
$entry = @"

### $timestamp — StopFailure: API/Rate-Limit Error — Status: OFFEN
**Quelle:** Hook: StopFailure (command-type, no API dependency)
**Symptom:** Session-Turn endete durch API-Fehler
**Details:** $errorInput
**Fix-Vorschlag:** Pruefen ob Rate-Limit temporaer oder dauerhaft. Bei dauerhaftem Fehler: API-Key pruefen.
**Status:** OFFEN
"@

# Write to whiteboard (whiteboard-insert.ps1 already sourced at top)
try {
    Insert-WhiteboardEntry -Section "Offene Fehler & Probleme" -Entry $entry
} catch {
    Write-Output "[stopfailure-logger] whiteboard-insert failed — error NOT logged to whiteboard. Manual check required."
}

Write-Output "StopFailure logged to whiteboard at $timestamp"

