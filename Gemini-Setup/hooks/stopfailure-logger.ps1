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

# Build whiteboard entry
$entry = "### $timestamp — StopFailure: API/Rate-Limit Error — Status: OFFEN"

# Write to whiteboard
try {
    Insert-WhiteboardEntry -Section "Offene Fehler & Probleme" -Entry $entry
    Hook-Log "StopFailure logged to whiteboard"
} catch {
    Hook-LogWarn "whiteboard-insert failed — StopFailure not logged"
}

exit 0
